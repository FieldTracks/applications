package org.fieldtracks.middleware.connectors

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.JWTAuthHandler
import org.fieldtracks.middleware.services.AuthService
import org.fieldtracks.middleware.services.MiddlewareStatusService
import org.slf4j.LoggerFactory
import java.io.File
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.KeyStore.ProtectionParameter
import javax.crypto.KeyGenerator


class HttpConnector(private val authService: AuthService,
                    private val keyStorePath: String,
                    private val keyStoreSecret: CharArray) {

    private val logger = LoggerFactory.getLogger(HttpConnector::class.java)

    private val vertx = Vertx.vertx()
    private val server = vertx.createHttpServer()
    private var jwtAuthProvider: JWTAuth
    private var router = Router.router(vertx)


    fun connectNonBlocking(middlewareStatusService: MiddlewareStatusService){
//        val deployment = VertxResteasyDeployment()
//        deployment.start()
//        serviceSingletons.forEach { deployment.registry.addSingletonResource(it) }
//        val vertxHandler = VertxRequestHandler(vertx, deployment,"/api/resources/")


        router.route("/api/login")
            .handler { ctx ->
                val user = ctx.request().getParam("user")
                val passwd = ctx.request().getParam("passwd")
                if (authService.authenticate(user, passwd)) {
                    ctx.response().end(jwtAuthProvider.generateToken(JsonObject().put("sub", user)))
                } else {
                    ctx.fail(401)
                }
            }
        router.route("/api/status")
            .respond { ctx -> Future.succeededFuture(JsonObject().put("status",middlewareStatusService.currentStatus()))}
        router.route("/api/resources/private/*").handler(JWTAuthHandler.create(jwtAuthProvider))
//        router.route("/api/resources/*").handler {
//            ctx -> vertxHandler.handle(ctx.request())
//        }

        server
            .requestHandler(router)
            .listen(8080, "localhost")

    }

    init {

        val keyStore = KeyStore.getInstance("jceks")

        val storeFile = File(keyStorePath)

        if(!storeFile.exists()) {
            logger.info("Generating JWT key and keystore in '{}'", keyStorePath)
            val protParam: ProtectionParameter = PasswordProtection(keyStoreSecret)

            keyStore.load(null,keyStoreSecret) // Creates an empty keystore

            val keygen =  KeyGenerator.getInstance("HmacSHA256")
            keygen.init(2048)
            val key = keygen.generateKey()
            keyStore.setEntry("HS256",KeyStore.SecretKeyEntry(key),protParam)
            storeFile.outputStream().use { fos ->
                keyStore.store(fos,keyStoreSecret)
            }
        }

        val config = JWTAuthOptions()
            .setKeyStore(
                KeyStoreOptions()
                    .setType("jceks")
                    .setPath(keyStorePath)
                    .setPassword(String(keyStoreSecret))
            )
        this.jwtAuthProvider = JWTAuth.create(vertx, config)
    }
}
