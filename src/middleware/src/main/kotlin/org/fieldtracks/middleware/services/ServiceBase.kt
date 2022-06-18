package org.fieldtracks.middleware.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiFunction
import kotlin.collections.ArrayList
import kotlin.concurrent.timerTask
import kotlin.reflect.KClass

abstract class ServiceBase(
    private val initialDelaySeconds: Int,
    private val intervalSeconds: Int,
    val client: IMqttClient,
    flushTopics: List<String> = emptyList()
) {


    private var topicsToBeFlushed = ConcurrentLinkedQueue(flushTopics)

    private val logger = LoggerFactory.getLogger(ServiceBase::class.java)
    private val timer: Timer = Timer()
    val objectMapper: ThreadLocal<ObjectMapper> = ThreadLocal.withInitial { createObjectMapper() }

    fun connectionLost() {
        modifyTimer(false)
    }

    fun connectCompleted(reconnect: Boolean) {
        topicsToBeFlushed.forEach {
            client.publish(it, ByteArray(0),0,true)
            topicsToBeFlushed.remove(it)
        }
        if(!reconnect) {
            onMqttConnectedInitially()
        }
        modifyTimer(true)
    }

    @Synchronized
    fun modifyTimer(enableTimer: Boolean) {
        if(enableTimer) {
            logger.debug("Enabling timer")
            timer.schedule(timerTask {
                try {
                    onTimerTriggered()
                } catch (t: Throwable){
                    logger.error("Error aggregating data", t)
                }
            },initialDelaySeconds * 1000L ,intervalSeconds * 1000L)
        } else {
            logger.debug("Disabling timer")
            timer.cancel()
        }
    }

    inline fun <reified T> subscribeJSONMqtt(mqttTopic: String, noinline listener: (topic: String, data: T) -> Unit) {
        val converter = { _: String, data: ByteArray ->
                objectMapper.get().readValue(data, T::class.java)
        }
        subscribeMappedMQTT(mqttTopic, converter,listener)
    }

    fun <T> subscribeMappedMQTT(mqttTopic: String,  converter: BiFunction<String, ByteArray, T>, listener: (topic: String, data: T) -> Unit) {
        client.subscribe(mqttTopic) { topic, msg ->
            if (msg == null || msg.payload == null) {
                logger.warn("Discarding empty message in {} - message: {}", topic, msg)
            }
            try {
                val converted = converter.apply(topic, msg.payload)
                listener(topic, converted)
            } catch (t: Throwable) {
                logger.warn("Skipping message due to error in topic '{} - message: '{}'", topic, msg, t)
            }
        }
    }


    fun publishMQTTJson(mqttTopic: String, data: Any, qos: Int = 1, retain: Boolean = true ){
        try {
            client.publish(mqttTopic,objectMapper.get().writeValueAsBytes(data),qos,retain)
        } catch (e: Exception) {
            logger.error("Error publishing message {}", data, e)
        }
    }

    abstract fun onTimerTriggered()
    abstract fun onMqttConnectedInitially()
}

// Stupid helper for nice constructor syntax - is there a better way in kotlin?
// That looks like stdlib-code .. but, is it included in the language already
fun <T> vT(vararg topicList: Pair<Boolean, T>): List<T> {
    return topicList.mapNotNull {
        if(it.first) {
            it.second
        } else {
            null
        }
    }
}

typealias NameResolver = (String) -> String