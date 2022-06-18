package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class StoneStatusService(client: IMqttClient, scanIntervalSeconds: Int)
    : ServiceBase(scanIntervalSeconds,scanIntervalSeconds, client, emptyList()) {

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
                publishMQTTJson("Aggregated/status", data)
                client.publish("Aggregated/status", mapper.writeValueAsBytes(data),1,true)
            } catch (e: Exception) {
                logger.error("Failed aggregating status", e)
            }
        }
    }

    override fun onMqttConnectedInitially() {
        subscribeJSONMqtt("JellingStone/status/#") {topic, report: StoneStatusReport ->
            val stone = topic.removePrefix("JellingStone/status/")
            incoming.add(stone to report)
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