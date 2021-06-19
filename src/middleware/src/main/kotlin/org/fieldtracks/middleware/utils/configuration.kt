package org.fieldtracks.middleware.utils

import org.slf4j.LoggerFactory

open class AppConfiguration(val mqttUrl: String,
                            val mqttUser: String,
                            val mqttPassword: String,
                            val simulateStones: Boolean,
                            val contactTimeoutInHours: Long,
                            val eddystoneNetworkId: String)

object ENV_CONFIGURATION: AppConfiguration(
    mqttUrl = System.getProperty("MQTT_URL", ""),
    mqttUser = System.getProperty("MQTT_USER") ?: "",
    mqttPassword = System.getProperty("MQTT_PASSWORD") ?: "",
    contactTimeoutInHours= 48L, // Keep offline stones for about 2 days
    simulateStones = System.getProperty("SIMULATE_STONES", "FALSE").toUpperCase() == "TRUE",
    eddystoneNetworkId = System.getProperty("EDDYSTONE_NETWORK", "F2:85:7C:93:3A:C5:D7:00:68:C3")
) {
    val log = LoggerFactory.getLogger(AppConfiguration::class.java)
    init {
        require(mqttUrl != "") {"MQTT_URL not set. Broker unknown"}
        if(mqttUser.isBlank()) {
            log.warn("MQTT_USER not set. Not sending username")
        }
        if(mqttPassword.isBlank()) {
            log.warn("MQTT_PASSWORD not set. Not sending password")
        }
        val loggedPassword = if (mqttPassword.isNotBlank()) { "[***]" } else { "" }
        log.info("Starting process Broker-URL: '${mqttUrl}', User: '$mqttUser', Password: '$loggedPassword'")
    }
}

