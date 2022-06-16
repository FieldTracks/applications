package org.fieldtracks.middleware.services

import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.timerTask

abstract class ScheduledServiceBase(
    private val initialDelaySeconds: Int,
    private val intervalSeconds: Int,
) {

    private val logger = LoggerFactory.getLogger(ScheduledServiceBase::class.java)
    private val timer: Timer = Timer()

    fun connectionLost() {
        modifyTimer(false)
    }

    fun connectCompleted(reconnect: Boolean) {
        onMqttConnected(reconnect)
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

    abstract fun onTimerTriggered()
    abstract fun onMqttConnected(reconnect: Boolean)

}