package org.fieldtracks.middleware.model

import org.fieldtracks.mqtt.NameResolver
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant

data class ScanGraph(val nodes: ArrayList<GraphNode>, val links: ArrayList<GraphLink>, val timeStmp: Instant) {


    companion object {
        val logger = LoggerFactory.getLogger(ScanGraph::class.java)!!
    }

    fun update(newReports: Set<ScanReportMessage>, maxBeaconAgeSeconds: Int, nameResolver: NameResolver): ScanGraph {
        logger.trace("Updating graph - new data: '{}'",newReports)
        logger.trace("Old Graph '{}'",this)


        val newGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())
        val rssiMap = HashMap<String, ArrayList<Pair<Int, ScanReportMessage>>>() // Beacon-ID maps to List Paris: RSSI to Report that has it

        val beaconIDs = HashSet<String>()
        val oldLinks = this.links.associateBy { it.target }.toMutableMap() // Convention - the target is always the beacon
        val oldNodes = this.nodes.associateBy { it.id }.toMutableMap()

        logger.trace("Associating each beacon with all detected RSSIs")
        newReports.forEach { report ->
            logger.debug("Processing report from '{}': ID: '{}', MID: '{}'",report.stoneId,report.reportIdTimeStamp,report.messageSeqNum)
            oldNodes.remove(report.stoneId)
            newGraph.nodes.add(GraphNode(report.stoneId, nameResolver.resolve(report.stoneId), report.reportIdTimeStamp, offline = false, stone = true))
            report.beaconData.filter { it.type != 8 }.forEach { beacon -> // Ignore eddystone EID, i.e. type == 8
                val idStr = beacon.id.toString(16)
                oldNodes.remove(idStr)
                val data = rssiMap.computeIfAbsent(idStr) { ArrayList() }
                data += beacon.rssi to report
                beaconIDs += idStr
                logger.debug("Beacon {} detected at {}: '{}' dBm",idStr,report.stoneId,beacon.rssi)
            }
        }

        beaconIDs.forEach { beaconId ->
            // Report only the strongest stone
            val bestReport = rssiMap[beaconId]!!.maxByOrNull { it.first }!!
            newGraph.nodes += GraphNode(beaconId, nameResolver.resolve(beaconId),bestReport.second.reportIdTimeStamp, offline = false, stone = false)
            val link = GraphLink(
                source = bestReport.second.stoneId,
                target = beaconId,
                detectedRssi = bestReport.first,
                offline = false)
            newGraph.links += link
            logger.debug("Beacon '{}' is at '{}' with RSSI '{}'",link.target,link.source,link.detectedRssi)
        }

        oldNodes.values.forEach {
            newGraph.nodes += it.copy(offline = true, name = nameResolver.resolve(it.id))
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
        logger.trace("Emitting new Graph {}", newGraph)
        return newGraph
    }
}

data class GraphNode(
    val id: String,
    val name: String,
    val lastSeen: Instant,
    val offline: Boolean,
    val stone: Boolean,
) {
    fun ageInSeconds(): Long =
        Duration.between(lastSeen, Instant.now()).seconds

}

data class GraphLink(
    val source: String,
    val target: String,
    val detectedRssi: Int,
    val offline: Boolean,
    val value: Int = 1,
)
