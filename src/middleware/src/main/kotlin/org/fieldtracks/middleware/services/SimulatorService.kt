package org.fieldtracks.middleware.services

import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.fieldtracks.middleware.model.GraphLink
import org.fieldtracks.middleware.model.GraphNode
import org.fieldtracks.middleware.model.ScanGraph
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.time.Instant
import java.util.concurrent.ThreadLocalRandom

class SimulatorService(
    private val client: IMqttClient,
    scanIntervalSeconds: Int,
    private val stoneCnt: Int,
    beaconCnt: Int,
    @Volatile private var flushGraph: Boolean
): ScheduledServiceBase(initialDelaySeconds = scanIntervalSeconds, intervalSeconds = scanIntervalSeconds) {

    private val rnd = ThreadLocalRandom.current()
    private val mapper = createObjectMapper()
    private val logger = LoggerFactory.getLogger(SimulatorService::class.java)

    private val stoneNodes = (1..stoneCnt).map {
            BigInteger(48,1,rnd).toString(16) // 48-Bit Mac
    }

    private val beaconNodes = (1 .. beaconCnt). map {
        BigInteger(48,1,rnd).toString(16) // 6-Byte instance-id, own namespace
    }

    override fun onTimerTriggered() {
        val now = Instant.now()


        val graphNodes = stoneNodes.map {
            GraphNode(id = it, lastSeen = now, offline = false, stone = true)
        } + beaconNodes.map {
            GraphNode(id = it, lastSeen = now, offline = false, stone = false)
        }

        val graphLinks = beaconNodes.map {
            GraphLink(
                source = stoneNodes[rnd.nextInt(0,stoneCnt)],
                target = it,
                detectedRssi = rnd.nextInt(-90,-30),
                offline = false
            )
        }
        val graph = ScanGraph(ArrayList(graphNodes), ArrayList(graphLinks))
        publish(graph)

    }

    override fun onMqttConnected(reconnect: Boolean) {
        if(!reconnect && flushGraph) {
            publish(ScanGraph(ArrayList(),ArrayList()))
            flushGraph = false
        }
    }

    private fun publish(graph: ScanGraph) {
        try {
            client.publish("Aggregated/scan",mapper.writeValueAsBytes(graph),1,true)
        } catch(e: Exception) {
            logger.error("Error publishing simulated graph", e)
        }


    }
}