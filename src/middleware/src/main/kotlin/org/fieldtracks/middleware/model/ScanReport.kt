package org.fieldtracks.middleware.model

import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

data class ScanReportMessage (val stoneId: String, val reportIdTimeStamp: Instant, val messageSeqNum: Int, val beaconData: List<ScanReportBeaconData>) {

    fun ageInSeconds(): Long {
        return Duration.between(reportIdTimeStamp, Instant.now()).seconds
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
                val timestamp = Instant.ofEpochSecond(BigInteger(content.sliceArray(1..4)).toLong())
                ScanReportMessage(
                    stoneId = stone,
                    reportIdTimeStamp = timestamp,
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
                if(type + offset  >= content.size ) {
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

