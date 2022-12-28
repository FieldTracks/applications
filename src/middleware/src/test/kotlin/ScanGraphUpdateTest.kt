package org.fieldtracks.middleware

import org.fieldtracks.middleware.model.*
import java.math.BigInteger
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanGraphUpdateTest {

    @Test
    fun emptyGraphNoUpdate() {
        val emptyGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())
        val result = emptyGraph.update(emptySet(), Int.MAX_VALUE) { it }
        assertEquals(emptyGraph.links,result.links)
        assertEquals(emptyGraph.nodes,result.nodes)
    }

    @Test
    fun newBeaconAppearing() {
        val stmp = Instant.now()
        val emptyGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())
        val newBeacon = ScanReportBeaconData(type = 1, rssi = -42, id = BigInteger("FF",16))
        val report1 = ScanReportMessage("stone-23", stmp,-1, listOf(newBeacon))
        val result = emptyGraph.update(setOf(report1), Int.MAX_VALUE) { it }

        val nodesInGraph = listOf(
            GraphNode("stone-23","stone-23",stmp, offline = false, stone = true),
            GraphNode("ff","ff",stmp, offline = false, stone = false)
        )

        val linksInGraph = listOf(
            GraphLink(source = "stone-23", target = "ff",-42,false)
        )

        assertEquals(nodesInGraph, result.nodes)
        assertEquals(linksInGraph, result.links)
    }

    @Test
    fun beaconGoingDark() {
        val stmp = Instant.now()
        val emptyGraph = ScanGraph(ArrayList(), ArrayList(),Instant.now())
        val newBeacon = ScanReportBeaconData(type = 1, rssi = -42, id = BigInteger("FF",16))
        val report1 = ScanReportMessage("stone-23", stmp,-1, listOf(newBeacon))
        val report2 = ScanReportMessage("stone-23", stmp,-1, emptyList())
        val result = emptyGraph
            .update(setOf(report1), Int.MAX_VALUE)  { it }
            .update(setOf(report2), Int.MAX_VALUE)  { it }

        val nodesInGraph = listOf(
            GraphNode("stone-23","stone-23",stmp, offline = false, stone = true),
            GraphNode("ff","ff",stmp, offline = true, stone = false)
        )

        val linksInGraph = listOf(
            GraphLink(source = "stone-23", target = "ff",-42,true)
        )

        assertEquals(nodesInGraph, result.nodes)
        assertEquals(linksInGraph, result.links)
    }

    @Test
    fun stoneGoingDark() {
        val stmp = Instant.now()
        val emptyGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())
        val newBeacon = ScanReportBeaconData(type = 1, rssi = -42, id = BigInteger("FF",16))
        val report1 = ScanReportMessage("stone-23", stmp,-1, listOf(newBeacon))
        val result = emptyGraph
            .update(setOf(report1), Int.MAX_VALUE) {it}
            .update(emptySet(), Int.MAX_VALUE) {it}

        val nodesInGraph = listOf(
            GraphNode("stone-23","stone-23",stmp, offline = true, stone = true),
            GraphNode("ff","ff",stmp, offline = true, stone = false)
        )

        val linksInGraph = listOf(
            GraphLink(source = "stone-23", target = "ff",-42,true)
        )

        assertEquals(nodesInGraph, result.nodes)
        assertEquals(linksInGraph, result.links)
    }

    @Test
    fun beaconMovingStrongerSignal() {
        val stmp = Instant.now()
        val emptyGraph = ScanGraph(ArrayList(), ArrayList(), Instant.now())
        val beacon = ScanReportBeaconData(type = 1, rssi = -42, id = BigInteger("FF",16))
        val beaconStrongSignal = ScanReportBeaconData(type = 1, rssi = -23, id = BigInteger("FF",16))

        val oldReports = setOf(
            ScanReportMessage("stone-23", stmp,-1, listOf(beacon)),
            ScanReportMessage("stone-42", stmp,-1, emptyList())
        )

        val newReports = setOf(
            ScanReportMessage("stone-23", stmp,-1, listOf(beacon)),
            ScanReportMessage("stone-42", stmp,-1, listOf(beaconStrongSignal))
        )


        val oldResult = emptyGraph.update(oldReports, Int.MAX_VALUE) {it}
        val newResult = oldResult.update(newReports, Int.MAX_VALUE) {it}

        val nodesInGraph = listOf(
            GraphNode("stone-23","stone-23",stmp, offline = false, stone = true),
            GraphNode("stone-42","stone-42",stmp, offline = false, stone = true),
            GraphNode("ff","ff",stmp, offline = false, stone = false)
        )

        val oldLinksInGraph = listOf(
            GraphLink(source = "stone-23", target = "ff",-42,false)
        )
        val newLinksInGraph = listOf(
            GraphLink(source = "stone-42", target = "ff",-23,false)
        )

        assertEquals(nodesInGraph, newResult.nodes)
        assertEquals(nodesInGraph, oldResult.nodes)
        assertEquals(oldLinksInGraph, oldResult.links)
        assertEquals(newLinksInGraph, newResult.links)
    }

}
