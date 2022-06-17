package org.fieldtracks.middleware.services

import com.fasterxml.jackson.annotation.JsonProperty
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
    @JsonProperty("app_info") val appInfo: StoneStatusReportAppDesc,
    @JsonProperty("mem_info") val memInfo: StoneStatusReportMemInfo,
    val nvs: StoneStatusReportNVS,
    @JsonProperty("date_time") val timestamp: Instant,
    @JsonProperty("uptime_seconds") val uptimeSeconds: Number,
)

data class StoneStatusReportAppDesc(
    val version: String,
    @JsonProperty("project_name") val projectName: String,
    val time: String,
    val date: String,
    @JsonProperty("idf_ver") val idfVersion: String,
)

data class StoneStatusReportMemInfo(
    @JsonProperty("free_heap_size") val freeHeapSize: Double,
    @JsonProperty("min_heap_size") val min_heap_size: Double,
)

data class StoneStatusReportNVS(
    @JsonProperty("WLAN_SSID") val wlanSsid: String,
    @JsonProperty("MQTT_URL") val mqttUrl: String,
    @JsonProperty("MQTT_USER") val mqttUser: String,
    @JsonProperty("BLE_EDDY_ORG") val eddystoneNamespaceId: String,
    @JsonProperty("BLE_EDDY_INST") val eddystoneInstanceId: String,
)