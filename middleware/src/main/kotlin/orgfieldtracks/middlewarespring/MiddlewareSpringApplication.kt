package orgfieldtracks.middlewarespring

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import orgfieldtracks.middlewarespring.config.WebsocketConfig

@SpringBootApplication
@Import(WebsocketConfig::class)
class MiddlewareSpringApplication

fun main(args: Array<String>) {
	runApplication<MiddlewareSpringApplication>(*args)
}
