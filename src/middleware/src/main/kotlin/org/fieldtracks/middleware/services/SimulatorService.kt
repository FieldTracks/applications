package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.model.GraphLink
import org.fieldtracks.middleware.model.GraphNode
import org.fieldtracks.middleware.model.ScanGraph
import java.math.BigInteger
import java.time.Instant
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.random.asJavaRandom

class SimulatorService(client: IMqttClient,
    scanIntervalSeconds: Int,
    private val stoneCnt: Int,
    beaconCnt: Int,
    flushGraph: Boolean
): ServiceBase(client,Schedule(scanIntervalSeconds,scanIntervalSeconds), vT(flushGraph to "Aggregated/scan" )) {

    private val macRandomness = Random(42).asJavaRandom()
    private val rssiRandomness = Random(23)
    private val beaconTypeRandomness = Random(4711)
    private val beaconIdRandomness = Random(0).asJavaRandom()
    private val linkRandomness = Random(1)

    private val stoneNodes = (1..stoneCnt).map {
            randomMac()
    }

    private val beaconNodes = (1 .. beaconCnt). map {
        randomBeacon()
    }

    override fun onTimerTriggered() {
        val now = Instant.now()


        val graphNodes = stoneNodes.map {
            GraphNode(id = it, lastSeen = now, offline = false, stone = true, name = it)
        } + beaconNodes.map {
            GraphNode(id = it, lastSeen = now, offline = false, stone = false, name = it)
        }

        val graphLinks = beaconNodes.map {
            GraphLink(
                source = stoneNodes[linkRandomness.nextInt(0,stoneCnt)],
                target = it,
                detectedRssi = rssiRandomness.nextInt(-90,-30),
                offline = false
            )
        }
        val graph = ScanGraph(ArrayList(graphNodes), ArrayList(graphLinks), Instant.now())
        publish(graph)

    }

    override fun onMqttConnectedInitially() {
    }

    private fun publish(graph: ScanGraph) {
        publishMQTTJson("Aggregated/scan", graph)
    }

    private fun randomMac():String {
        val c = BigInteger(48,0,macRandomness).toByteArray()
        return String.format("%02x:%02x:%02x:%02x:%02x:%02x",c[0],c[1],c[2],c[3],c[4],c[5])

    }

    private fun randomBeacon(): String {
        val type = beaconTypeRandomness.nextInt(10)
        val id = when(type) {
            0 -> 20 // Alt-Beacon
            1 -> 16 // Eddystone, other network
            2 -> 1 // Eddystone, small instance
            3 -> 6 // Eddystone, full instance
            else -> 2 // Eddystone, 2-Byte instance
        }
        return BigInteger(id*8,0,beaconIdRandomness).toString(16)

    }

}
