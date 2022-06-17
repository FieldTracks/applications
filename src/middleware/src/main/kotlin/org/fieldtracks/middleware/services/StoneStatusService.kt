package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class StoneStatusService(private val client: IMqttClient, scanIntervalSeconds: Int): ScheduledServiceBase(scanIntervalSeconds,scanIntervalSeconds) {

    private val mapper = createObjectMapper()
    private val logger = LoggerFactory.getLogger(StoneStatusService::class.java)

    private val incoming = ConcurrentLinkedQueue<Pair<String,StoneStatusReport>>()
    private val statusByNodeId = ConcurrentHashMap<String,StoneStatusReport>()

    override fun onTimerTriggered() {
        if(!incoming.isEmpty()) {
            try {
                val reportsByStone = incoming.toSet()
                incoming.removeAll(reportsByStone)
                reportsByStone.forEach { statusByNodeId[it.first] = it.second }
                val data = AggregatedStatusReport(statusByNodeId.toMap())
                client.publish("Aggregated/status", mapper.writeValueAsBytes(data),1,true)
            } catch (e: Exception) {
                logger.error("Failed aggregating status", e)
            }
        }
    }

    override fun onMqttConnected(reconnect: Boolean) {
        if(!reconnect) {
            client.subscribe("JellingStone/status/#") {topic, data ->
                try {
                    val stone = topic.removePrefix("JellingStone/scan/")
                    val report = mapper.readValue(data.payload, StoneStatusReport::class.java)
                    incoming.add(stone to report)
                } catch(e: Exception) {
                    logger.error("Unable to parse status {}",data, e)
                }
            }
        }
    }
}

data class AggregatedStatusReport(
    val status: Map<String,StoneStatusReport>
)

data class StoneStatusReport(
    val appInfo: StoneStatusReportAppDesc,
    val memInfo: StoneStatusReportMemInfo,
    val nvs: StoneStatusReportNVS,
    val timestamp: Instant,
    val uptimeSeconds: Number,
)

data class StoneStatusReportAppDesc(
    val version: String,
    val projectName: String,
    val time: String,
    val date: String,
    val idfVersion: String,
)

data class StoneStatusReportMemInfo(
    val freeHeapSize: Double,
    val minHeapSize: Double,
)

data class StoneStatusReportNVS(
    val wlanSsid: String,
    val mqttUrl: String,
    val mqttUser: String,
    val eddystoneNamespace: String,
    val eddystoneInstanceId: String,
)