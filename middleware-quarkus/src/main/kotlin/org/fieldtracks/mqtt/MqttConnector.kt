package org.fieldtracks.mqtt

import org.eclipse.paho.client.mqttv3.*
import org.fieldtracks.MiddlewareConfiguration
import org.slf4j.LoggerFactory
import java.util.*
import javax.enterprise.context.ApplicationScoped
import kotlin.concurrent.thread

@ApplicationScoped
class MqttConnector(
    cfg: MiddlewareConfiguration
) {
    val mqttClient =  MqttClient(cfg.mqttURL(),"middleware-${UUID.randomUUID()}")

    private val options = MqttConnectOptions()

    private val logger = LoggerFactory.getLogger(MqttConnector::class.java)

   init {
       options.serverURIs = arrayOf(cfg.mqttURL())
       if(cfg.mqttUserO() != null) {
           options.userName = cfg.mqttUserO()
       }
       if(cfg.mqttPasswordO() != null) {
           options.password = cfg.mqttPasswordO().toCharArray()
       }
       options.isAutomaticReconnect = true
       options.isCleanSession = true
       options.isHttpsHostnameVerificationEnabled = true

   }

    fun connectBlocking(mqttServices: List<AbstractServiceBase>) {
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

