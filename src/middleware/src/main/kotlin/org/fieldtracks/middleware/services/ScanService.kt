package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.model.BeaconStatusReport
import org.fieldtracks.middleware.model.ScanGraph
import org.fieldtracks.middleware.model.ScanReportMessage
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ScanService(
    client: IMqttClient,
    private val scanIntervalSeconds: Int,
    private val maxReportAgeSeconds: Int,
    private val maxBeaconAgeSeconds: Int,
    flushGraph: Boolean, flushBeaconStatus: Boolean,
    private val nameResolver: NameResolver
): ServiceBase(client, Schedule(scanIntervalSeconds,scanIntervalSeconds), vT(flushGraph to "Aggregated/scan", flushBeaconStatus to "Aggregated/beaconStatus" )) {

    private val logger = LoggerFactory.getLogger(ScanService::class.java)

    private val reportQueue = ConcurrentLinkedQueue<ScanReportMessage>()

    @Volatile
    private var currentGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())

    @Volatile
    private var currentBeaconStatus = BeaconStatusReport(HashMap())

    private val aggregationTopic = "Aggregated/scan"


    override fun onTimerTriggered() {
        val entries = HashSet<ScanReportMessage>()
        entries.addAll(reportQueue.toList())
        if(entries.isNotEmpty()) {
            reportQueue.removeAll(entries)
            updateGraph(entries)
            updateBeaconStatus(entries)

        } else {
            logger.info("No scan-reports received during the last {} second(s)",scanIntervalSeconds)
        }
    }

    private fun updateGraph(entries: Set<ScanReportMessage>) {
        currentGraph = currentGraph.update(entries,maxBeaconAgeSeconds,nameResolver)
        publishMQTTJson(aggregationTopic, currentGraph)
    }

    private fun updateBeaconStatus(entries: Set<ScanReportMessage>) {
        currentBeaconStatus = currentBeaconStatus.update(entries,nameResolver)
        publishMQTTJson("Aggregated/beaconStatus", currentBeaconStatus)
    }

    override fun onMqttConnectedInitially() {
        val converter = { topic: String, data: ByteArray ->
            val stone = topic.removePrefix("JellingStone/scan/")
            ScanReportMessage.parse(stone,data)!!
        }

        subscribeMappedMQTT("JellingStone/scan/#", converter) { _: String, msg: ScanReportMessage ->
            if (msg.ageInSeconds() > maxReportAgeSeconds) {
                logger.info("Older than {} seconds - discarding: '{}'", maxReportAgeSeconds, msg)
            } else {
                reportQueue.add(msg)
            }
        }
        subscribeJSONMqtt(aggregationTopic) { _, data: ScanGraph ->
            this.currentGraph = data
        }
    }

}

