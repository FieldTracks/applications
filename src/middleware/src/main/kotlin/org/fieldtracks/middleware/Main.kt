package org.fieldtracks.middleware

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.pair
import com.github.ajalt.clikt.parameters.types.int
import org.eclipse.paho.client.mqttv3.*
import org.fieldtracks.middleware.services.NameService
import org.fieldtracks.middleware.services.ScanService
import org.fieldtracks.middleware.services.SimulatorService
import org.fieldtracks.middleware.services.StoneStatusService
import org.slf4j.LoggerFactory
import java.util.*

class Middleware(
    scanIntervalSeconds: Int,
    reportMaxAge: Int,
    beaconMaxAge: Int,
    private val mqttURL: String,
    private val mqttUser: String?,
    private val mqttPassword: String?,
    simulate: Pair<Int,Int>?,
    flushNames: Boolean,
    flushGraph: Boolean,
    flushBeaconStatus : Boolean
) {

    private val client = MqttClient(mqttURL,"middleware-${UUID.randomUUID()}")
    private val nameService = NameService(client, flushNames = flushNames)

    private val services = listOf(
        nameService,
        StoneStatusService(client, scanIntervalSeconds),
        if(simulate == null) {
            ScanService(client, scanIntervalSeconds,reportMaxAge,beaconMaxAge, flushGraph = flushGraph, flushBeaconStatus = flushBeaconStatus, nameService::resolve)
        } else {
            SimulatorService(client,scanIntervalSeconds,simulate.first,simulate.second, flushGraph = flushGraph)
        }
    )
    private val logger = LoggerFactory.getLogger(Middleware::class.java)

    fun start() {
        logger.info("Starting - connecting to server")
        val options = MqttConnectOptions()
        options.serverURIs = arrayOf(mqttURL)
        if(mqttUser != null) {
            options.userName = mqttUser
        }
        if(mqttPassword != null) {
            options.password = mqttPassword.toCharArray()
        }

        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.isHttpsHostnameVerificationEnabled = true

        client.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                logger.warn("Connection Lost", cause)
                services.forEach { it.connectionLost()}
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) { }

            override fun deliveryComplete(token: IMqttDeliveryToken?) { }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if(reconnect) {
                    logger.info("Re-connected to server")
                } else {
                    logger.info("Connected to server")
                }
                services.forEach { it.connectCompleted(reconnect)}

            }
        })
        while(true) {
            try {
                if(!client.isConnected) {
                    client.connect(options)
                }
            } catch (e: Exception) {
                logger.error("{} - retrying in 10 seconds", e.localizedMessage)
            }
            Thread.sleep(10_000)
        }
    }
}

fun createObjectMapper(): ObjectMapper {

    return  ObjectMapper()
        .registerKotlinModule()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,false)
        .registerModule(JavaTimeModule())!!
}

fun main(args: Array<String>) {
    middleware().main(args)
}
class middleware:CliktCommand() {
    private val scanIntervalSeconds: Int by option("-i","--interval", help="Scan interval in seconds (default: 8)").int().default(8)
    private val beaconAgeSeconds: Int by option("-ba","--beacon-age-max", help="Time a beacon is offline before being excluded in seconds (default: 48 * 3600 = 172800)").int().default(172800)
    private val maxReportAge: Int by option("-ra","--report-age-max", help="Maximum age of a scan report in seconds (default: 8)").int().default(30)
    private val mqttURL: String by option("-s","--server-url-mqtt", help = "MQTT Server c.f. https://www.eclipse.org/paho/clients/java/").default("tcp://localhost:1883")
    private val simulate: Pair<Int,Int>? by option("-sim","--simulate", help = "Simulate stones, beacons - do not process reports").int().pair()
    private val mqttUser: String? by option("-u","--user-mqtt", help = "MQTT User")
    private val mqttPassword: String? by option("-p","--password-mqtt", help = "MQTT Password")
    private val flushNames: Boolean by option("-fn","--flush-names", help = "Flush aggregated names in topic").flag(default = false)
    private val flushBeaconStatus: Boolean by option("-fs","--flush-beacon-status", help = "Flush aggregated beacon status topic").flag(default = false)
    private val flushGraph: Boolean by option("-fg","--flush-graph", help = "Flush aggregated graph in topic").flag(default = false)
    override fun run() {
        Middleware(scanIntervalSeconds = scanIntervalSeconds,
            reportMaxAge = maxReportAge,
            beaconMaxAge = beaconAgeSeconds,
            mqttURL = mqttURL,
            mqttUser = mqttUser,
            mqttPassword = mqttPassword,
            simulate = simulate,
            flushNames = flushNames,
            flushGraph = flushGraph,
            flushBeaconStatus = flushBeaconStatus
        ).start()
    }
}


