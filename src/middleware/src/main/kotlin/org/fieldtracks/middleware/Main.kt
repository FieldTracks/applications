package org.fieldtracks.middleware

import org.eclipse.paho.client.mqttv3.*
import org.slf4j.LoggerFactory
import java.lang.System.getProperty
import java.nio.charset.Charset
import java.util.*
import java.util.zip.Inflater


class MqttProcess(url: String, private val user:String?, private val password: String?) {
    private val uuid = UUID.randomUUID()
    private val log = LoggerFactory.getLogger(MqttProcess::class.java)
    private val mqttClient = MqttAsyncClient(url, "ft-middleware-$uuid")
    private val router = MessageRouter(mqttClient)

    fun start() {
        mqttClient.setCallback(Callback(router))
        val options = MqttConnectOptions()
        options.userName = user
        options.password = password?.toCharArray()
        options.connectionTimeout = 5
        options.isAutomaticReconnect = true
        options.isHttpsHostnameVerificationEnabled = true
        try {
            mqttClient.connect(options).waitForCompletion()
        } catch (e : Exception) {
            log.error("Error connecting to MQTT-Broker. Note: Paho-exceptions such as 'already connected' do necessarily reflect the cause (e.g. wrong credentials)", e);
        }
        router.subscribe()
    }
}
private class Callback(val router: MessageRouter): MqttCallback {
    val log = LoggerFactory.getLogger(MqttProcess::class.java)
    override fun connectionLost(cause: Throwable) {
        log.error("Connection lost", cause)
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        log.info("Got message. Topic: $topic, size: ${message.payload.size}")
        log.debug("Trying to decompress")
        val content = inflate(message.payload)
        router.process(topic,content)
    }

    private fun inflate(content: ByteArray): String {
        try {
            val inflater = Inflater()
            val result = ByteArray(500 * 1024) // 500 KByte must be enough. Zero all of it
            inflater.setInput(content)
            val length = inflater.inflate(result)
            inflater.end()
            return String(result,0,length, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            log.warn("Unable to decompress message. Passing raw data")
            return String(content, Charset.forName("UTF-8"))
        }

    }
    override fun deliveryComplete(token: IMqttDeliveryToken) {
        log.info("Deliver completed for message id: ${token.message.id}")
    }
}
fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger(MqttProcess::class.java)
    try {
        log.info("Starting MQTT Broker using environmental variables / properties: MQTT_URL, MQTT_USER, MQTT_PASSWORD")
        val url = getProperty("MQTT_URL")
        val user = getProperty("MQTT_USER") ?: ""
        val password = getProperty("MQTT_PASSWORD") ?: ""
        require(url != null) {"MQTT_URL not set. Broker unknown"}
        if(user.isBlank()) {
            log.warn("MQTT_USER not set. Not sending username")
        }
        if(password.isBlank()) {
            log.warn("MQTT_PASSWORD not set. Not sending password")
        }
        val loggedPassword = if (password.isNotBlank()) { "[***]" } else { "" }
        log.info("Starting process Broker-URL: '${url}', User: '$user', Password: '$loggedPassword'")
        MqttProcess(url,user,password).start()
    } catch (e: Exception) {
        log.error("Error in main-loop. Terminating middleware", e)
    }
}


