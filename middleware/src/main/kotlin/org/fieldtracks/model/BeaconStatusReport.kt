package org.fieldtracks.middleware.model

import org.fieldtracks.mqtt.NameResolver
import java.math.BigInteger
import java.time.Instant

data class BeaconStatusReport(
    val status: HashMap<String, BeaconStatus> = HashMap()
) {
    fun update(reports: Set<ScanReportMessage>, nameResolver: NameResolver): BeaconStatusReport {
        val newStatus = HashMap<String, BeaconStatus>()
        val offlineBeacons = status.map { BigInteger(it.key,16) }.toMutableSet()

        val detectionsByBeacon = reports.flatMap { report ->
            report.beaconData.map { beaconData ->
                offlineBeacons.remove(beaconData.id)
                BeaconDetection(beacon = beaconData.id, seen = report.reportIdTimeStamp, rssi = beaconData.rssi, stoneId = report.stoneId)
            }
        }.groupBy { it.beacon }

        for(beaconEntry in detectionsByBeacon) {
            val beaconId = beaconEntry.key
            val lastSeen = beaconEntry.value.maxOf { it.seen }
            val detections = beaconEntry.value.map {
                BeaconStatusStone(it.stoneId,nameResolver.resolve(it.stoneId),it.rssi)
            }
            val bestStone = detections.maxByOrNull { it.rssi }!!

            offlineBeacons.remove(beaconId)
            newStatus[beaconId.toString(16)] = BeaconStatus(lastSeen, bestStone, ArrayList(detections) )

        }
        for (beaconId in offlineBeacons) {
            val idStr = beaconId.toString(16)
            val oldStatus = status[idStr]!!

            newStatus[idStr] = oldStatus.copy(detections = ArrayList())
        }
        return BeaconStatusReport(newStatus)
    }
}

data class BeaconStatus(
    val lastSeen: Instant,
    val selectedStone: BeaconStatusStone,
    val detections: ArrayList<BeaconStatusStone>
)

data class BeaconStatusStone(
    val stoneId: String,
    val stoneName: String,
    val rssi: Int,
)


private data class BeaconDetection(
    val beacon: BigInteger,
    val seen: Instant,
    val rssi: Int,
    val stoneId: String,
)
