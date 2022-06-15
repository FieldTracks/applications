package org.fieldtracks.middleware

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.eclipse.paho.client.mqttv3.*
import org.fieldtracks.middleware.services.ScanService
import org.slf4j.LoggerFactory
import java.util.*

class Middleware(
    scanIntervalSeconds: Int,
    mqttURL: String,
    private val mqttUser: String?,
    private val mqttPassword: String?,
) {


    private val client = MqttClient(mqttURL,"middleware-${UUID.randomUUID()}")
    private val scanService = ScanService(client, scanIntervalSeconds)

    private val logger = LoggerFactory.getLogger(Middleware::class.java)

    fun start() {
        logger.error("Starting - connecting to server")
        val options = MqttConnectOptions()
        if(mqttUser != null) {
            options.userName = mqttUser
        }
        if(mqttPassword != null) {
            options.password = mqttPassword.toCharArray()
        }

        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        options.isHttpsHostnameVerificationEnabled = true

        client.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                logger.warn("Connection Lost", cause)
                scanService.connectionLost()
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) { }

            override fun deliveryComplete(token: IMqttDeliveryToken?) { }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if(reconnect) {
                    logger.info("Re-connected to server")
                } else {
                    logger.info("Connected to server")
                }
                scanService.connectComplete(reconnect)
            }
        })

        while(true) {
            try {
                client.connect(options);
            } catch (t: Throwable) {
                logger.error("Unable to connect to MQTT-Server - retrying in 10 seconds - '{}'", t.toString())
                Thread.sleep(10_000)
            }
        }

    }

    class middleware:CliktCommand() {
        private val scanIntervalSeconds: Int by option("-i","--interval", help="Scan interval in seconds (default: 8)").int().default(8)
        private val mqttURL: String by option("-s","--server-url-mqtt", help = "MQTT Server c.f. https://www.eclipse.org/paho/clients/java/").default("tcp://localhost:1884")
        private val mqttUser: String? by option("-u","--user-mqtt", help = "MQTT User")
        private val mqttPassword: String? by option("-p","--password-mqtt", help = "MQTT Password")

        override fun run() {
            Middleware(scanIntervalSeconds,mqttURL,mqttUser,mqttPassword).start()
        }
    }

}

fun main(args: Array<String>) {
    Middleware.middleware().main(args)
}


