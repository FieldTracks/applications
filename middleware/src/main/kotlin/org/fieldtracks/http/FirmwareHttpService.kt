package org.fieldtracks.http

import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.MultiEmitter
import io.vertx.core.file.AsyncFile
import org.fieldtracks.MiddlewareConfiguration
import org.kohsuke.github.GitHub
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.walk

@Path("/firmware")
@ApplicationScoped
class FirmwareHttpService {

    private val logger = LoggerFactory.getLogger(FirmwareHttpService::class.java)

    private val firmwareRegexp = "JellingStone-(.+)\\+(.+)".toRegex()

    @Inject
    protected lateinit var cfg: MiddlewareConfiguration

    @Produces("text/plain")
    @Path("/update_repo")
    @GET
    fun updateLocalRepositoryFromGithub(): Multi<String> {
       return Multi.createFrom().emitter {
           thread(start = true) {
               downloadFromGithub(it)
           }
        }
    }

    @Path("/")
    @GET
    fun listAvailableFiles(): HashMap<String, FirmwareFile> {
        return availableFirmware()
    }

    @Path("{name}")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadFirmware(@PathParam("name") name: String): Response {
        val entry = availableFirmware()[name]
        return if(entry != null ) {
            Response.ok(File(entry.path)).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }

    }

    fun availableFirmware(): HashMap<String, FirmwareFile> {
        val files = HashMap<String, FirmwareFile>()
        File(cfg.firmwareDownloadDir()).walk().forEach {
            if(it.canRead() && it.nameWithoutExtension.matches(firmwareRegexp)) {
                val result = firmwareRegexp.find(it.name)!!.groupValues
                files[it.name] = FirmwareFile(version = result[0], build = result[1], path = it.canonicalPath)
            }
        }
        return files

    }

    fun downloadFromGithub(logWriter: MultiEmitter<in String>) {
        logWriter.emit("Connecting to github\n")
        val github = GitHub.connectAnonymously()
        val downloadFolder = cfg.firmwareDownloadDir()
        logWriter.emit("Retrieving repository\n")
        github.getRepository("fieldtracks/JellingStone").listReleases().forEach { ghRelease ->
            logWriter.emit("Checking releases")
            val firmwareArchive = ghRelease.listAssets().firstOrNull { it.name.startsWith("JellingStone")}
            if (firmwareArchive != null) {
                logWriter.emit("Found release ${ghRelease.name} - ${ghRelease.published_at}\n")
                val target = Paths.get(downloadFolder,firmwareArchive.name)
                if (!target.exists()) {
                    logWriter.emit("Downloading ${firmwareArchive.browserDownloadUrl}\n")
                    logger.info("Downloading ${firmwareArchive.browserDownloadUrl}")
                    URL(firmwareArchive.browserDownloadUrl).openStream().use {
                        Files.copy(it, target)
                    }
                }
                logWriter.emit("Validating\n")
                var valid = false
                try {
                    ZipFile(target.toFile()).use { zipFile ->
                            var entries = 0
                            zipFile.entries().iterator().forEach {
                                it.crc
                                entries++
                            }
                            valid = entries > 0
                        }
                    assert(valid)
                } catch (e: Exception) {
                    logger.warn("Corrupt ZIP-file {} - deleting ", target, e)
                    logWriter.emit("Corrupt ZIP-file $target ${e.message} - deleting \n")
                    Files.deleteIfExists(target)
                }
                logWriter.emit("Is valid\n")

            }
        }
        logWriter.complete()
    }

    data class FirmwareFile(
        val version: String,
        val build: String,
        val path: String,
    )
}
