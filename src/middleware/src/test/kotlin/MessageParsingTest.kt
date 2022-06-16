package org.fieldtracks.middleware

import org.fieldtracks.middleware.services.ScanReportBeaconData
import org.fieldtracks.middleware.services.ScanReportMessage
import java.math.BigInteger
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageParsingTest {

    val stoneId = "30:ae:a4:44:a6:52"

    @Test
    fun parseEmptyMessage() {
        val report = ScanReportMessage.parse(stoneId,loadFile("empty_report.bin"))!!
        val timeStamp = ZonedDateTime.of(2022, 6, 13, 19, 40, 2, 0, ZoneId.of("UTC")).toInstant()

        assertEquals(stoneId,report.stoneId)
        assertEquals(timeStamp,report.reportIdTimeStamp)
        assertEquals(-1, report.messageSeqNum)
        assertEquals(emptyList(),report.beaconData)
    }

    @Test
    fun parseTwoAltBeacons() {
        val report = ScanReportMessage.parse(stoneId,loadFile("two_altbeacon_report.bin"))!!

        val beacon0 = ScanReportBeaconData(type = 20, rssi = -59, id = BigInteger("a7bf6e89484a4b3ea64739e0d2db2775000b000c",16))
        val beacon1 = ScanReportBeaconData(type = 20, rssi = -60, id = BigInteger("912020fc2b1945b0a96e1d82a8200a6a00170018",16))

        assertEquals(beacon0,report.beaconData[0])
        assertEquals(beacon1,report.beaconData[1])
    }

    @Test
    fun parseTwoAltBeaconSecondRemovedByTruncatedMessage() {
        val raw = loadFile("two_altbeacon_report.bin")
        val report = ScanReportMessage.parse(stoneId,raw.copyOfRange(0,raw.lastIndex -1))!!
        val beacon0 = ScanReportBeaconData(type = 20, rssi = -59, id = BigInteger("a7bf6e89484a4b3ea64739e0d2db2775000b000c",16))

        assertEquals(beacon0,report.beaconData[0])
    }


    @Test
    fun parseEddyStoneUID() {
        // Four variants: 0: Foreign organisation, own organisation with: 1: Full size instance, 2: 1-Byte instance, 3: 2-Byte instance.
        val report = ScanReportMessage.parse(stoneId,loadFile("eddystone_uid_beacons.bin"))!!

        // The four variants
        val beacon0 = ScanReportBeaconData(type = 6, rssi = -71, id = BigInteger("AABBCC", 16) ) // Own namespace, full instance
        val beacon1 = ScanReportBeaconData(type = 2, rssi = -64, id = BigInteger("BBCC", 16) ) // Own namespace, 2-byte instance
        val beacon2 = ScanReportBeaconData(type = 1, rssi = -64, id = BigInteger("AA", 16) ) // Own namespace, 1-byte instance
        val beacon3 = ScanReportBeaconData(type = 16, rssi = -63, id = BigInteger("d163bd5507a4531a4ad2000000000000", 16) ) // Foreign namespace

        assertEquals(listOf(beacon0,beacon1,beacon2,beacon3),report.beaconData)

    }

    @Test
    fun parseEddyStoneEID() {
        // Four variants: 0: Foreign organisation, own organisation with: 1: Full size instance, 2: 1-Byte instance, 3: 2-Byte instance.
        val report = ScanReportMessage.parse(stoneId,loadFile("eddystone_eid_beacon.bin"))!!
        val beacon0 = ScanReportBeaconData(type = 8, rssi = -65, id = BigInteger("3b02272729db145b", 16) ) // Own namespace, full instance

        assertEquals(beacon0,report.beaconData[0])
    }

    @Test
    fun invalidVersion() {
        val data = loadFile("empty_report.bin")
        data[0] = 0x42 // Protocol version 0x42
        assertNull(ScanReportMessage.parse(stoneId,data))
    }

    @Test
    fun truncatedMessage() {
        val raw = loadFile("empty_report.bin")
        val data = raw.copyOf(raw.lastIndex -1)
        assertNull(ScanReportMessage.parse(stoneId,data))
    }

    @Test
    fun messageTooLarge() {
        val firstPart = loadFile("empty_report.bin")
        val data = ByteArray(2000)
        firstPart.copyInto(data)
        assertNull(ScanReportMessage.parse(stoneId,data))
    }


    private fun loadFile(fileName: String): ByteArray {
        val msgData = this::class.java.classLoader.getResourceAsStream(fileName)
        return msgData!!.readAllBytes()!!
    }
}