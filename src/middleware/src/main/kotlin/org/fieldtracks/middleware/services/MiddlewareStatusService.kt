package org.fieldtracks.middleware.services


import org.eclipse.paho.client.mqttv3.IMqttClient

class MiddlewareStatusService(private val client: IMqttClient, private val authService: AuthService) {

    enum class Status {
        DISCONNECTED, INSTALLER, RUNNING
    }

    fun currentStatus(): Status {
        return if(!client.isConnected) {
            Status.DISCONNECTED
        } else if(authService.adminPasswordSet()) {
            Status.RUNNING
        } else {
            Status.INSTALLER
        }
    }

}
