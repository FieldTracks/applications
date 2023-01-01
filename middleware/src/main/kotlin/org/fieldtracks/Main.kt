package org.fieldtracks

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.annotations.CommandLineArguments
import io.smallrye.config.ConfigMapping
import java.util.Optional
import javax.enterprise.context.ApplicationScoped
import javax.ws.rs.Produces


class Main {

    @Produces
    @ApplicationScoped
    fun parseCli(@CommandLineArguments args: Array<String>): FlushConfiguration {
        val cfg = FlushConfiguration()
        cfg.main(args)
        return cfg;
    }
}

@ConfigMapping(prefix = "middleware")
interface MiddlewareConfiguration {
    fun scanIntervalSeconds(): Int
    fun reportMaxAgeSeconds(): Int
    fun beaconMaxAgeSeconds(): Int
    fun mqttURL(): String
    fun mqttUser(): Optional<String>
    fun mqttPassword(): Optional<String>

    fun firmwareDownloadDir(): String

    // https://github.com/smallrye/smallrye-config/issues/844
    fun mqttUserO() = mqttUser().orElse(null)
    fun mqttPasswordO() = mqttPassword().orElse(null)
}

class FlushConfiguration(): CliktCommand(){
    val flushNames: Boolean by option("-fn","--flush-names", help = "Flush aggregated names in topic").flag(default = false)
    val flushBeaconStatus: Boolean by option("-fs","--flush-beacon-status", help = "Flush aggregated beacon status topic").flag(default = false)
    val flushGraph: Boolean by option("-fg","--flush-graph", help = "Flush aggregated graph in topic").flag(default = false)
    val flushUser: Boolean by option("-fu","--flush-users", help = "Delete all users, i.e. reset admin password").flag(default = false)

    override fun run() {
        // Parsing is done by framework. To be left empty to fulfill the API contract
    }
}

fun main(args: Array<String>) {
    Quarkus.run(*args)
}
object ChannelNames{
    const val aggregatedGraph = "aggregatedGraph"
    const val aggregatedBeaconStatusReport = "aggregatedBeaconStatusReport"
}
