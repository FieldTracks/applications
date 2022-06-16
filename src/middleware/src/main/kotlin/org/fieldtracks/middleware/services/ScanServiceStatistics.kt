package org.fieldtracks.middleware.services


// TODO Probably, we need per-stone stats instead of per aggregation-run stats.
// TODO Keeping this comment out to freeze the ideas.

//class ScanServiceStatistics() {
//
//    private val logger = LoggerFactory.getLogger(ScanServiceStatistics::class.java)
//    private var oldData = emptySet<ScanReportMessage>()
//    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())
//
//    fun update(data: Set<ScanReportMessage>) {
//        val oldStones = oldData.map { it.stoneId }.toSet()
//        val newStones = oldData.map { it.stoneId }.toSet()
//
//        val oldDataByBeacon = oldData.flatMap { stone -> stone.beaconData.map { it.id to stone } }.toMap()
//        val newDataByBeacon = data.flatMap { stone -> stone.beaconData.map { it.id to stone } }.toMap()
//
//        val addedStones = newStones.filter { !oldStones.contains(it) }
//        val removedStones = oldStones.filter { !newStones.contains(it) }
//
//        val addedBeacons = newDataByBeacon.keys.filter { !oldDataByBeacon.containsKey(it) }
//        val removedBeacons = oldDataByBeacon.keys.filter { !newDataByBeacon.containsKey(it) }
//
//        val newDataByStone = HashMap<StatKey,MutableList<ScanReportMessage>>()
//        data.forEach {report ->
//            val key = StatKey(report.stoneId,report.reportIdTimeStamp)
//            val reports = newDataByStone.computeIfAbsent(key) { ArrayList() }
//            reports += report
//        }
//
//        val duplicateReportsFromStones = newDataByStone.keys.groupBy { it.stone }.filter { it.value.size > 1 }.keys
//        val incompleteReportsFromStones = ArrayList<String>()
//
//        newDataByStone.values.forEach { report ->
//            report.sortBy { it.messageSeqNum }
//            if(report[0].messageSeqNum > 0) {
//                logger.warn("Missing final message in report {}",report )
//                incompleteReportsFromStones += report[0].stoneId
//            } else if (report.last().messageSeqNum + 1 != report[0].messageSeqNum * -1) {
//                logger.warn("Missing 2nd-last message in report {}",report )
//                incompleteReportsFromStones += report[0].stoneId
//            } else if(report.size > 1) {
//                for (i in 1 until report.size ) {
//                    if(report[i].messageSeqNum != i ) {
//                        logger.warn("Missing message sequence number {} in report {}" , i,report )
//                        incompleteReportsFromStones += report[0].stoneId
//                    }
//                }
//            }
//        }
//
//        val stats = ReportStatistics(
//            dataReceived = data.isNotEmpty(),
//            addedStones = addedStones,
//            removedStones = removedStones,
//            addedBeacons = addedBeacons.map { it.toString(16) },
//            removedBeacons = removedBeacons.map { it.toString(16) },
//            duplicateReportsFromStones = duplicateReportsFromStones.toList(),
//            incompleteReportsFromStones = incompleteReportsFromStones
//        )
//       oldData = data
//        client.publish("middleware/aggregation",mapper.writeValueAsBytes(stats),0,true)
//    }

//}

//data class ReportStatistics(
//    val timestamp: Instant = Instant.now(),
//    val dataReceived: Boolean,
//    val addedStones: List<String>,
//    val removedStones : List<String>,
//    val addedBeacons: List<String>,
//    val removedBeacons: List<String>,
//    val duplicateReportsFromStones: List<String>,
//    val incompleteReportsFromStones: List<String>
//)
//
//data class StatKey(
//    val stone: String,
//    val timestamp: Instant
//)