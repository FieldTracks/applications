package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.fieldtracks.middleware.model.ScanGraph
import org.fieldtracks.middleware.model.ScanReportMessage
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class ScanService(
    private val client: IMqttClient,
    private val scanIntervalSeconds: Int,
    private val maxReportAgeSeconds: Int,
    private val maxBeaconAgeSeconds: Int,
    @Volatile private var flushGraph: Boolean,
    private val nameResolver: (String) -> String
): ScheduledServiceBase(scanIntervalSeconds,scanIntervalSeconds) {

    private val logger = LoggerFactory.getLogger(ScanService::class.java)

    private val mapper = createObjectMapper()
    private val reportQueue = ConcurrentLinkedQueue<ScanReportMessage>()

    @Volatile
    private var currentGraph = ScanGraph(ArrayList(), ArrayList())

    private val aggregationTopic = "Aggregated/scan"

    override fun onMqttConnected(reconnect: Boolean) {
        if(!reconnect) {
            if(flushGraph) {
                try {
                    publishCurrentGraph()
                    flushGraph=false
                } catch (e: Exception) {
                    logger.error("Unable to flush graph",e)
                }
            }
            client.subscribe("JellingStone/scan/#") { topic, msg ->
                try {
                    val stone = topic.removePrefix("JellingStone/scan/")
                    val parsed = ScanReportMessage.parse(stone, msg.payload)!!
                    if(parsed.ageInSeconds() > maxReportAgeSeconds) {
                        logger.info("Older than {} seconds - discarding: '{}'",maxReportAgeSeconds,parsed)
                    } else {
                        reportQueue.add(parsed)
                    }
                } catch (t: Throwable) {
                    logger.warn("Skipping message due to error in topic '{} - message: '{}'",topic,msg,t)
                }
            }
            client.subscribe(aggregationTopic) { topic, msg ->
                try {
                    this.currentGraph = mapper.readValue(msg.payload, ScanGraph::class.java)
                } catch (t: Throwable) {
                    logger.warn("Skipping message due to error in topic '{} - message: '{}'",topic,msg,t)
                }

            }
        }
        modifyTimer(true)
    }


    override fun onTimerTriggered() {
        val entries = HashSet<ScanReportMessage>()
        entries.addAll(reportQueue.toList())
        if(entries.isNotEmpty()) {
            reportQueue.removeAll(entries)
            currentGraph = currentGraph.update(entries,maxBeaconAgeSeconds,nameResolver)
            publishCurrentGraph()
        } else {
            logger.info("No scan-reports received during the last {} second(s)",scanIntervalSeconds)
        }
    }

    private fun publishCurrentGraph() {
        try {
            client.publish(aggregationTopic,mapper.writeValueAsBytes(currentGraph),1,true)
        } catch(e: Exception) {
            logger.error("Error publishing scan graph", e)
        }

    }

}

