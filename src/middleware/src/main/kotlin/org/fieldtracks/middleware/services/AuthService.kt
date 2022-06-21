package org.fieldtracks.middleware.services

import io.netty.handler.codec.base64.Base64Encoder
import io.vertx.ext.auth.HashingStrategy
import org.eclipse.paho.client.mqttv3.IMqttClient
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64


class AuthService(client: IMqttClient, flushUser: Boolean) : ServiceBase(client, flushTopics = vT(flushUser to "Middleware/User")) {

    private val strategy: HashingStrategy = HashingStrategy.load()
    private val random = SecureRandom()
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Volatile
    private var users = ApplicationUsers()

    override fun onMqttConnectedInitially() {
        subscribeJSONMqtt("Middleware/User") { _, users: ApplicationUsers ->
            this.users = users
        }
    }

    fun adminPasswordSet(): Boolean {
        return !users.passwordByUser["admin"].isNullOrBlank()
    }

    fun authenticate(user: String, password: String): Boolean {
        try {
            val admin = users.passwordByUser["admin"]
            return if (user != "admin") {
                logger.info("Rejecting username {}", user)
                false
            } else if(admin != null) {
                if (strategy.verify(admin,password)) {
                    logger.info("Correct password for {}", user)
                    true
                } else {
                    logger.info("Failed password for {}", user)
                    false
                }
            } else {
                logger.info("Created admin user")
                val salt = ByteArray(16)
                random.nextBytes(salt)
                val encodedSalt = String(Base64.getEncoder().encode(salt))

                users.passwordByUser["admin"] = strategy.hash("pbkdf2",null,encodedSalt,password)
                publishMQTTJson("Middleware/User",users)
                true
            }

        } catch (e: java.lang.RuntimeException) {
           logger.error("Error authenticating", e)
        }
        return false
    }

}

data class ApplicationUsers(
    val passwordByUser: HashMap<String, String> = HashMap()
)


