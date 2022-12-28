package org.fieldtracks.mqtt

import io.quarkus.arc.DefaultBean
import org.fieldtracks.FlushConfiguration
import org.fieldtracks.MiddlewareConfiguration
import org.fieldtracks.middleware.model.BeaconStatusReport
import org.fieldtracks.middleware.model.ScanGraph
import org.fieldtracks.middleware.model.ScanReportMessage
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@ApplicationScoped
@Named("scanMqttService")
@DefaultBean
class ScanMqttService(): AbstractServiceBase() {

    private val logger = LoggerFactory.getLogger(ScanMqttService::class.java)

    private val reportQueue = ConcurrentLinkedQueue<ScanReportMessage>()

    @Volatile
    private var currentGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())

    @Volatile
    private var currentBeaconStatus = BeaconStatusReport(HashMap())

    private val aggregationTopic = "Aggregated/scan"

    @Inject
    protected lateinit var fCfg: FlushConfiguration

    @Inject
    protected lateinit var cfg: MiddlewareConfiguration

    @Inject
    protected lateinit var nameResolver: NameResolver

    override val schedule: Schedule
        get() = Schedule(cfg.scanIntervalSeconds())

    override val flushTopics: List<String>
        get() = vT(fCfg.flushGraph to "Aggregated/scan", fCfg.flushBeaconStatus to "Aggregated/beaconStatus" )

    override fun onTimerTriggered() {
        val entries = HashSet<ScanReportMessage>()
        entries.addAll(reportQueue.toList())
        if(entries.isNotEmpty()) {
            reportQueue.removeAll(entries)
            updateGraph(entries)
            updateBeaconStatus(entries)

        } else {
            logger.info("No scan-reports received during the last {} second(s)",cfg.scanIntervalSeconds())
        }
    }

    private fun updateGraph(entries: Set<ScanReportMessage>) {
        currentGraph = currentGraph.update(entries,cfg.beaconMaxAgeSeconds(),nameResolver)
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
            if (msg.ageInSeconds() > cfg.reportMaxAgeSeconds()) {
                logger.info("Older than {} seconds - discarding: '{}'", cfg.reportMaxAgeSeconds(), msg)
            } else {
                reportQueue.add(msg)
            }
        }
        subscribeJSONMqtt(aggregationTopic) { _, data: ScanGraph ->
            this.currentGraph = data
        }
    }

}

