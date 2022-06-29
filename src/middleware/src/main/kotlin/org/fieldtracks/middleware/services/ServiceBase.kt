package org.fieldtracks.middleware.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.fieldtracks.middleware.createObjectMapper
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiFunction
import kotlin.concurrent.timerTask

abstract class ServiceBase(
    val client: IMqttClient,
    private val schedule: Schedule? = null,
    flushTopics: List<String> = emptyList())
 {

     private val logger = LoggerFactory.getLogger(ServiceBase::class.java)

     @Volatile
     private var timer = Timer()

    private var topicsToBeFlushed = ConcurrentLinkedQueue(flushTopics)
    val objectMapper: ThreadLocal<ObjectMapper> = ThreadLocal.withInitial { createObjectMapper() }

    fun connectionLost() {
        if(schedule != null ){
            modifyTimer(false)
        }
    }

    fun connectCompleted(reconnect: Boolean) {
        topicsToBeFlushed.forEach {
            client.publish(it, ByteArray(0),0,true)
            topicsToBeFlushed.remove(it)
        }
        if(!reconnect) {
            onMqttConnectedInitially()
        }
        if(schedule != null ){
            modifyTimer(true)
        }
    }

    @Synchronized
    fun modifyTimer(enableTimer: Boolean) {
        if(schedule == null) {
            return
        }
        if(enableTimer) {
            logger.debug("Enabling timer")
            timer.schedule(timerTask {
                try {
                    onTimerTriggered()
                } catch (t: Throwable){
                    logger.error("Error aggregating data", t)
                }
            },schedule.initialDelaySeconds * 1000L ,schedule.intervalSeconds * 1000L)
        } else {
            logger.debug("Disabling timer")
            timer.cancel()
            timer = Timer()
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

    open fun onTimerTriggered() {

    }

     abstract fun onMqttConnectedInitially()
}
data class Schedule(
    val initialDelaySeconds: Int,
    val intervalSeconds: Int,
)


// Stupid helper for nice constructor syntax - is there a better way in kotlin?
// That looks like stdlib-code .. but, is it included in the language already
fun <T> vT(vararg entryByFlag: Pair<Boolean, T>): List<T> {
    return entryByFlag.mapNotNull {
        if(it.first) {
            it.second
        } else {
            null
        }
    }
}

typealias NameResolver = (String) -> String
