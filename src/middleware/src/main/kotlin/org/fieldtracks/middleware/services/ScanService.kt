package org.fieldtracks.middleware.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.concurrent.timerTask

class ScanService(
    private val client: IMqttClient,
    private val scanIntervalSeconds: Int,
    private val maxReportAgeSeconds: Int,
    private val maxBeaconAgeSeconds: Int,
) {

    private val timer: Timer = Timer()
    private val logger = LoggerFactory.getLogger(ScanService::class.java)

    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
    private val reportQueue = ConcurrentLinkedQueue<ScanReportMessage>()
    private val statistics = ScanServiceStatistics(client)

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
                        if(parsed.age_in_seconds() > maxReportAgeSeconds) {
                            logger.info("Older than {} seconds - discarding: '{}'",maxReportAgeSeconds,parsed)
                        } else {
                            reportQueue.add(parsed)
                        }
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
                try {
                    aggregate()
                } catch (t: Throwable){
                    logger.error("Error aggregating data ", t)
                }
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
            currentGraph = currentGraph.update(entries,maxBeaconAgeSeconds)
            client.publish("Aggregated/scan",mapper.writeValueAsBytes(currentGraph),1,true)
        } else {
            logger.info("No scan-reports received during the last {} second(s)",scanIntervalSeconds)
        }
        statistics.update(entries)
    }

}

data class ScanReportMessage (val stoneId: String,  val reportIdTimeStamp: Instant, val messageSeqNum: Int, val beaconData: List<ScanReportBeaconData>) {

    fun age_in_seconds(): Long {
        return Duration.between(reportIdTimeStamp,Instant.now()).seconds
    }


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
                if(type + offset - 1 >= content.size ) {
                    logger.warn("Not enough data in report. Discarding beacon data - expected {} - actual: {}",type,content.size - offset -1)
                } else {
                    val beaconId = BigInteger(1,content.sliceArray(offset+2 until offset+2+type))
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

    fun update(newReports: Set<ScanReportMessage>, maxBeaconAgeSeconds: Int): ScanGraph {
        logger.debug("Updating graph - new data: '{}'",newReports)
        logger.debug("Old Graph '{}'",this)


        val newGraph = ScanGraph(ArrayList(), ArrayList())
        val rssiMap = HashMap<String, ArrayList<Pair<Int, ScanReportMessage>>>() // Beacon-ID maps to List Paris: RSSI to Report that has it

        val beaconIDs = HashSet<String>()
        val oldLinks = this.links.associateBy { it.target }.toMutableMap() // Convention - the target is always the beacon
        val oldNodes = this.nodes.associateBy { it.id }.toMutableMap()

        logger.debug("Associating each beacon with all detected RSSIs")
        newReports.forEach { report ->
            logger.debug("Processing report from '{}': ID: '{}', MID: '{}'",report.stoneId,report.reportIdTimeStamp,report.messageSeqNum)
            oldNodes.remove(report.stoneId)
            newGraph.nodes.add(GraphNode(report.stoneId, report.reportIdTimeStamp, offline = false, stone = true))
            report.beaconData.forEach { beacon ->
                val idStr = beacon.id.toString(16)
                oldNodes.remove(idStr)
                val data = rssiMap.computeIfAbsent(idStr) { ArrayList() }
                data += beacon.rssi to report
                beaconIDs += idStr
                logger.debug("Beacon {} detected at {}: '{}' dBm",idStr,report.stoneId,beacon.rssi)
            }
        }

        logger.debug("Building graph by strongest RSSI")
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
            logger.debug("Beacon '{}' is at '{}' with RSSI '{}'",link.target,link.source,link.detectedRssi)
        }

        oldNodes.values.forEach {
            newGraph.nodes += it.copy(offline = true)
            if(!it.stone) { // Ignore links of offline stones - care about the beacon, only
                val link = oldLinks[it.id] // Ignore inconsistencies
                if(it.ageInSeconds() > maxBeaconAgeSeconds) {
                    logger.info("Beacon older than {} seconds - discarding {}", maxBeaconAgeSeconds, it)
                } else if (link != null) {
                    newGraph.links += link.copy(offline = true)
                    logger.debug("Offline beacon '{}' moved to '{}' - last contact '{}'",link.target,link.source,it.lastSeen)
                }
            }
        }
        logger.debug("Emitting new Graph {}", newGraph)
        return newGraph
    }
}

data class GraphNode(
    val id: String,
    val lastSeen: Instant,
    val offline: Boolean,
    val stone: Boolean,
) {
    fun ageInSeconds(): Long =
        Duration.between(lastSeen,Instant.now()).seconds

}

data class GraphLink(
    val source: String,
    val target: String,
    val detectedRssi: Int,
    val offline: Boolean,
    val value: Int = 1,
)