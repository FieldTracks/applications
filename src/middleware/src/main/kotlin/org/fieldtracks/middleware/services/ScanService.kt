package org.fieldtracks.middleware.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.concurrent.timerTask

class ScanService(
    private val client: IMqttClient,
    private val scanIntervalSeconds: Int = 8
) {

    private val timer: Timer = Timer()
    private val logger = LoggerFactory.getLogger(ScanService::class.java)

    private val mapper = ObjectMapper().registerKotlinModule()
    private val reportQueue = ConcurrentLinkedQueue<ScanReportMessage>()

    @Volatile
    private var currentGraph = ScanGraph(ArrayList(), ArrayList())

    fun connectionLost() {
        modifyTimer(false)
    }

    fun connectComplete(reconnect: Boolean) {
        if(!reconnect) {
            client.subscribe("JellingStone/scan/#") { topic, msg ->
                try {
                    val stone = topic.removePrefix("JellingStone/scan/")
                    val parsed = ScanReportMessage.parse(stone, msg.payload)
                    if(parsed != null) {
                        reportQueue.add(parsed)
                    }
                } catch (t: Throwable) {
                    logger.warn("Skipping message due to error in topic '{} - message: '{}'",topic,msg,t)
                }
            }
            client.subscribe("Aggregated/scan") { topic, msg ->
                try {
                    this.currentGraph = mapper.readValue(msg.payload, ScanGraph::class.java)
                } catch (t: Throwable) {
                    logger.warn("Skipping message due to error in topic '{} - message: '{}'",topic,msg,t)
                }

            }
        }
        modifyTimer(true)
    }

    @Synchronized
    fun modifyTimer(enableTimer: Boolean) {
        if(enableTimer) {
            logger.debug("Enabling timer")
            timer.scheduleAtFixedRate(timerTask {
                aggregate()
            },scanIntervalSeconds * 1000L ,scanIntervalSeconds * 1000L)
        } else {
            logger.debug("Disabling timer")
            timer.cancel()
        }
    }

    private fun aggregate() {
        val entries = HashSet<ScanReportMessage>()
        entries.addAll(reportQueue.toList())
        if(entries.isNotEmpty()) {
            reportQueue.removeAll(entries)
            currentGraph = currentGraph.update(entries)
            client.publish("Aggregated/scan",mapper.writeValueAsBytes(currentGraph),1,true)
        } else {
            logger.info("No scan-reports received during the last {} second(s)",scanIntervalSeconds)
        }
    }

}

data class ScanReportMessage (val stoneId: String,  val reportIdTimeStamp: Instant, val messageSeqNum: Int, val beaconData: List<ScanReportBeaconData>) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScanReportMessage::class.java)!!

        fun parse(stone: String, content: ByteArray): ScanReportMessage? {
            return if(content.size < 7 || content.size > 1100) {
                logger.warn("Invalid report size {} - discarding message",content.size)
                null
            } else if (content[0].toInt() != 0x01) {
                logger.warn("Invalid report version {} - discarding message", content[0])
                null
            } else {
                ScanReportMessage(
                    stoneId = stone,
                    reportIdTimeStamp =  Instant.ofEpochSecond(BigInteger(content.sliceArray(1..4)).toLong()),
                    messageSeqNum = content[5].toInt(),
                    beaconData = parseData(content)
                )
            }
        }

        private fun parseData(content: ByteArray): ArrayList<ScanReportBeaconData> {
            val resultList = ArrayList<ScanReportBeaconData>(content[6].toInt())
            var offset = 7
            while(offset < content.size) {
                val type = content[offset].toInt()
                val rssi = content[offset +1].toInt() - 100
                if(type + offset - 1 < content.size ) {
                    logger.warn("Not enough data in report. Discarding beacon data - expected {} - actual: {}",type,content.size - offset -1)
                } else {
                    val beaconId = BigInteger(content.sliceArray(offset+2 until offset+2+type))
                    resultList.add(ScanReportBeaconData(type,rssi,beaconId))
                }
                offset += 2+type
            }
            return resultList
        }

    }
}

data class ScanReportBeaconData(val type: Int, val rssi: Int, val id: BigInteger)

data class ScanGraph(val nodes: ArrayList<GraphNode>, val links: ArrayList<GraphLink>) {

    companion object {
        val logger = LoggerFactory.getLogger(ScanGraph::class.java)!!
    }

    fun update(newReports: Set<ScanReportMessage>): ScanGraph {
        logger.info("Updating graph - new data: '{}'",newReports)
        logger.info("Old Graph '{}'",this)


        val newGraph = ScanGraph(ArrayList(), ArrayList())
        val rssiMap = HashMap<String, ArrayList<Pair<Int, ScanReportMessage>>>() // Beacon-ID maps to List Paris: RSSI to Report that has it

        val beaconIDs = HashSet<String>()
        val oldLinks = this.links.associateBy { it.target }.toMutableMap() // Convention - the target is always the beacon
        val oldNodes = this.nodes.associateBy { it.id }.toMutableMap()

        // Build up data:
        // For each data: Associate RSSI by Report
        newReports.forEach { report ->
            oldNodes.remove(report.stoneId)
            newGraph.nodes.add(GraphNode(report.stoneId, report.reportIdTimeStamp, offline = false, stone = true))
            report.beaconData.forEach { beacon ->
                val idStr = beacon.id.toString(16)
                oldNodes.remove(idStr)
                val data = rssiMap.computeIfAbsent(idStr) { ArrayList() }
                data += beacon.rssi to report
                beaconIDs += idStr
            }
        }

        beaconIDs.forEach { beaconId ->
            // Report only the strongest stone
            val bestReport = rssiMap[beaconId]!!.maxByOrNull { it.first }!!
            newGraph.nodes += GraphNode(beaconId,bestReport.second.reportIdTimeStamp, offline = false, stone = false)
            val link = GraphLink(
                source = bestReport.second.stoneId,
                target = beaconId,
                detectedRssi = bestReport.first,
                offline = false)
            newGraph.links += link
        }

        oldNodes.values.forEach {
            newGraph.nodes += it.copy(offline = true)
            if(!it.stone) { // Ignore links of offline stones - care about the beacon, only
                val link = oldLinks[it.id] // Ignore inconsistencies
                if (link != null) {
                    newGraph.links += link.copy(offline = true)
                }
            }
        }
        logger.info("Emitting new Graph {}", newGraph)
        return newGraph
    }
}

data class GraphNode(
    val id: String,
    val lastSeen: Instant,
    val offline: Boolean,
    val stone: Boolean,
)

data class GraphLink(
    val source: String,
    val target: String,
    val detectedRssi: Int,
    val offline: Boolean,
    val value: Int = 1,
)