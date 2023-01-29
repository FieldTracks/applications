package org.fieldtracks.http

import com.github.sardine.SardineFactory
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import org.fieldtracks.MiddlewareConfiguration
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URI
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
import kotlin.io.path.extension
import kotlin.io.path.getLastModifiedTime


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
                downloadFromWebdav(it)
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
        return if (entry != null) {
            Response.ok(File(entry.path)).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }

    }

    fun availableFirmware(): HashMap<String, FirmwareFile> {
        val files = HashMap<String, FirmwareFile>()
        File(cfg.firmwareDownloadDir()).walk().forEach {
            if (it.canRead() && it.nameWithoutExtension.matches(firmwareRegexp)) {
                val result = firmwareRegexp.find(it.name)!!.groupValues
                files[it.name] = FirmwareFile(version = result[0], build = result[1], path = it.canonicalPath)
            }
        }
        return files

    }

    @Synchronized
    fun downloadFromWebdav(logWriter: MultiEmitter<in String>) {
        logWriter.emit("Connecting to ${cfg.jellingstoneWebdavUrl()}\n")
        val sardine = SardineFactory.begin()
        val downloadFolder = cfg.firmwareDownloadDir()

        sardine.list(cfg.jellingstoneWebdavUrl(),1,true).forEach { webdavResrouce ->
        val target = Paths.get(downloadFolder, webdavResrouce.name)
        if(target.extension.lowercase() == "zip") {
            logWriter.emit("Found release ${webdavResrouce.name} - last change: ${webdavResrouce.modified} \n")
            if ((!target.exists() || webdavResrouce.modified.time > target.getLastModifiedTime().toMillis())) {
                logWriter.emit("Downloading ${webdavResrouce.href} \n")
                val path = toAbsoluteURLPath(webdavResrouce.href)
                URL(path).openStream().use {
                    Files.copy(it, target)
                }
                logWriter.emit("Validating \n")
                try {
                    validateZIPFile(target)
                } catch (e: Exception) {
                    logger.warn("Corrupt ZIP-file {} - deleting ", target, e)
                    logWriter.emit("Corrupt ZIP-file $target ${e.message} - deleting \n")
                    Files.deleteIfExists(target)
                }
                logWriter.emit("Is valid\n")

            } else {
                logWriter.emit("Already downloaded - skipping\n")
            }
        }

    }


        try {

        } catch (e: Exception) {
            logger.warn("Error downloading firmware", e)
            logWriter.emit(e.message)
        }
    }

    fun toAbsoluteURLPath(relativeWebDAVURI: URI): String {
        val u = URL(cfg.jellingstoneWebdavUrl())
        val p = if (u.port != -1) {
            ":${u.port}"
        } else {
            ""
        }
        return "${u.protocol}://${u.host}${p}/$relativeWebDAVURI"
    }

    fun validateZIPFile(file: java.nio.file.Path) {
        val expected = mutableSetOf("JellingStone/JellingStone.bin", "JellingStone/bootloader.bin", "JellingStone/partition-table.bin")

        ZipFile(file.toFile()).use { zipFile ->
            zipFile.entries().iterator().forEach {
                it.crc
                if(!expected.remove(it.name)) {
                    throw java.lang.RuntimeException("ZIP-Entry ${it.name} is not supposed to be in archive")
                }
            }
        }
        if(expected.isNotEmpty()) {
            throw RuntimeException("Missing artifacts in archive: ${expected.joinToString(",") { it }}")
        }

    }

    data class FirmwareFile(
        val version: String,
        val build: String,
        val path: String,
    )
}
