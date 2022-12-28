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
import org.fieldtracks.middleware.connectors.HttpConnector
import org.fieldtracks.middleware.connectors.MqttConnector
import org.fieldtracks.middleware.services.*

class Middleware(
    scanIntervalSeconds: Int,
    reportMaxAge: Int,
    beaconMaxAge: Int,
    mqttURL: String,
    mqttUser: String?,
    mqttPassword: String?,
    simulate: Pair<Int,Int>?,
    flushNames: Boolean,
    flushGraph: Boolean,
    flushBeaconStatus : Boolean,
    keyStorePath: String,
    keyStoreSecret: CharArray,
    flushUser: Boolean
) {
    private val mqttConnector = MqttConnector(mqttURL,mqttUser,mqttPassword)

    private val nameService = NameService(mqttConnector.mqttClient, flushNames = flushNames)
    private val authService = AuthService(mqttConnector.mqttClient,flushUser)
    private val middlewareStatusService = MiddlewareStatusService(mqttConnector.mqttClient,authService)
    private val stoneStatusService = StoneStatusService(mqttConnector.mqttClient, scanIntervalSeconds)
    private val aggregatorService = if(simulate == null) {
        ScanService(mqttConnector.mqttClient, scanIntervalSeconds,reportMaxAge,beaconMaxAge, flushGraph = flushGraph, flushBeaconStatus = flushBeaconStatus, nameService::resolve)
    } else {
        SimulatorService(mqttConnector.mqttClient,scanIntervalSeconds,simulate.first,simulate.second, flushGraph = flushGraph)
    }
    private val httpConnector = HttpConnector(authService,keyStorePath,keyStoreSecret)

    private val mqttServices = listOf(nameService,stoneStatusService,aggregatorService,authService)

    fun start() {
        httpConnector.connectNonBlocking(middlewareStatusService)
        mqttConnector.connectBlocking(mqttServices)
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
    private val flushUser: Boolean by option("-fu","--flush-users", help = "Delete all users, i.e. reset admin password").flag(default = false)
    private val keyStorePath: String by option("-kp", "--keystore-path", help="Path to keystore (default: keystore)").default("keystore")
    private val keyStoreSecret: String by option("-ks", "--keystore-secret", help="Secret of the keystore (empty default").default("")


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
            flushBeaconStatus = flushBeaconStatus,
            keyStorePath = keyStorePath,
            keyStoreSecret = keyStoreSecret.toCharArray(),
            flushUser = flushUser


        ).start()
    }
}


