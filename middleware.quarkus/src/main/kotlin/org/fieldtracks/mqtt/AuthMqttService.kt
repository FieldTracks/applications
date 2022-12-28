package org.fieldtracks.mqtt

import io.vertx.ext.auth.HashingStrategy
import org.fieldtracks.FlushConfiguration
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class AuthMqttService: AbstractServiceBase() {

    @Inject
    protected lateinit var fCfg: FlushConfiguration

    private val strategy: HashingStrategy = HashingStrategy.load()
    private val random = SecureRandom()


    override val schedule: Schedule? = null

    override val flushTopics: List<String>
        get() = vT(fCfg.flushUser to "Middleware/User")

    private val logger = LoggerFactory.getLogger(AuthMqttService::class.java)

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

    fun authenticate(user: String?, password: String?): Boolean {
        try {
            val admin = users.passwordByUser["admin"]
            return if(user == null || password == null)  {
                logger.info("No username or password provided")
                false
            } else if (user != "admin") {
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


