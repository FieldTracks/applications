package org.fieldtracks.middleware.connectors

import org.eclipse.paho.client.mqttv3.*
import org.fieldtracks.middleware.services.ServiceBase
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.thread


class MqttConnector(
    mqttUrl: String,
    mqttUser: String?,
    mqttPassword: String?,
) {
    val mqttClient =  MqttClient(mqttUrl,"middleware-${UUID.randomUUID()}")

    private val options = MqttConnectOptions()

    private val logger = LoggerFactory.getLogger(MqttConnector::class.java)

   init {
       options.serverURIs = arrayOf(mqttUrl)
       if(mqttUser != null) {
           options.userName = mqttUser
       }
       if(mqttPassword != null) {
           options.password = mqttPassword.toCharArray()
       }
       options.isAutomaticReconnect = true
       options.isCleanSession = true
       options.isHttpsHostnameVerificationEnabled = true

   }

    fun connectBlocking(mqttServices: List<ServiceBase>) {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectionLost(cause: Throwable?) {
                logger.warn("Connection Lost", cause)
                mqttServices.forEach {
                    try {
                        it.connectionLost()
                    } catch (e: Exception) {
                        logger.error("Error",e)
                    }
                }
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) { }

            override fun deliveryComplete(token: IMqttDeliveryToken?) { }

            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                if(reconnect) {
                    logger.info("Re-connected to server")
                } else {
                    logger.info("Connected to server")
                }
                // N.B. the mqtt client's thread models does not support sending messages from handlers in the same thread
                thread(start = true) {
                    mqttServices.forEach {
                        it.connectCompleted(reconnect)
                    }
                }
            }
        })

        while(true) {
            try {
                if(!mqttClient.isConnected) {
                    mqttClient.connect(options)
                }
            } catch (e: Exception) {
                logger.error("{} - retrying in 10 seconds", e.localizedMessage)
            }
            Thread.sleep(10_000)
        }
    }
}

