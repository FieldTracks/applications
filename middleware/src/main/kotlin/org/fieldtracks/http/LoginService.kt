package org.fieldtracks.http

import io.smallrye.jwt.build.Jwt
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.fieldtracks.mqtt.AuthMqttService
import java.time.Duration
import javax.inject.Inject
import javax.json.Json
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.Response

@Path("/login")
class LoginService {


    @Inject
    protected lateinit var authService: AuthMqttService

    @ConfigProperty(name = "mp.jwt.verify.issuer")
    protected lateinit var jwtIssuer: String

    @POST
    fun authenticate(loginDTO: LoginDTO): Response {
        return if(authService.authenticate(loginDTO.user, loginDTO.password)) {
            val token = generateJWTToken(loginDTO.user)
            Response
                .ok(JWTToken(token))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        } else {
            Response
                .status(Response.Status.UNAUTHORIZED)
        }.build()
    }


    private fun generateJWTToken(user: String): String {
        val userObj = Json.createObjectBuilder().add("sub", user).build()
        return Jwt.claims(userObj)
            .issuer(jwtIssuer)
            .expiresIn(Duration.ofDays(2))
            .sign()
    }
}

data class JWTToken(val token: String)
data class LoginDTO(val user: String, val password: String)
