package org.fieldtracks.middleware

import org.eclipse.paho.client.mqttv3.*
import org.fieldtracks.middleware.services.ScanService
import org.slf4j.LoggerFactory
import java.util.*

class Middleware {

    private val client = MqttClient("tcp://localhost:1883","middleware-${UUID.randomUUID()}")
    private val scanService = ScanService(client)

    val logger = LoggerFactory.getLogger(Middleware::class.java)
    fun start() {
        logger.error("Starting - connecting to server")
        val options = MqttConnectOptions()
        options.isAutomaticReconnect = true
        options.isCleanSession = true
        options.connectionTimeout = 10
        client.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                scanService.connectionLost()
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) { }

            override fun deliveryComplete(token: IMqttDeliveryToken?) { }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                scanService.connectComplete(reconnect)
            }
        })
        client.connect(options)
    }
}

fun main() {
    Middleware().start()
}

