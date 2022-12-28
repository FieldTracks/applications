package org.fieldtracks.middleware

import org.slf4j.LoggerFactory
import org.testcontainers.containers.DockerComposeContainer
import java.io.File


class SimulatingTest {

    val stones = 16
    val beacons = 64

    private val logger = LoggerFactory.getLogger(SimulatingTest::class.java)

    fun runSimulation() {
        logger.warn("Starting simulation - this requires a MQTT-broker at 127.0.0.1:1883")

        val middleware = Middleware(
            scanIntervalSeconds = 8,
            reportMaxAge = 32,
            beaconMaxAge = 32,
            mqttURL = "ws://localhost:9001",
            mqttUser = null,
            mqttPassword = null,
            simulate = stones to beacons,
            flushNames = true,
            flushGraph = true,
            flushBeaconStatus = true,
            keyStorePath = "/tmp/keystore",
            keyStoreSecret = "insecure".toCharArray(),
            flushUser = true
        )


        middleware.start()
    }
}
fun main() {
    SimulatingTest().runSimulation()
}
