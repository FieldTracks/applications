package org.fieldtracks.http

import com.github.sardine.SardineFactory
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.subscription.MultiEmitter
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
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
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull


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
                it.complete()
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
        val sardine = if(cfg.jellingstoneWebdavUser().isPresent || cfg.jellingstoneWebdavPassword().isPresent) {
            SardineFactory.begin(cfg.jellingstoneWebdavUser().orElse(""),cfg.jellingstoneWebdavPassword().orElse(""))
        } else {
            SardineFactory.begin()
        }
        val downloadFolder = cfg.firmwareDownloadDir()

        sardine.list(cfg.jellingstoneWebdavUrl(),1,true).forEach { webdavResrouce ->
            val target = Paths.get(downloadFolder, webdavResrouce.name)
            if(target.extension.lowercase() == "zip") {
                logWriter.emit("Found release ${webdavResrouce.name} - last change: ${webdavResrouce.modified} \n")
                if ((!target.exists() || webdavResrouce.modified.time > target.getLastModifiedTime().toMillis())) {
                    try {
                        downloadFirmware(webdavResrouce.href, target, logWriter)
                        validateZIPFile(target, logWriter)
                    } catch (e: Exception) {
                        logger.warn("Bogus download {} - deleting ", target, e)
                        Files.deleteIfExists(target)
                    }
                    logWriter.emit("Is valid\n")
                } else {
                    logWriter.emit("Already downloaded - skipping\n")
                }
            }
        }
    }


    fun downloadFirmware(path: URI, target: java.nio.file.Path, logWriter: MultiEmitter<in String>) {
        logWriter.emit("Downloading ${path.toASCIIString()} \n")
        val request = HttpGet(toAbsoluteURLPath(path))
        val cProv = BasicCredentialsProvider()
        val user = cfg.jellingstoneWebdavUser().orElse("")
        val password = cfg.jellingstoneWebdavPassword().orElse("")
        cProv.setCredentials(AuthScope.ANY,UsernamePasswordCredentials(user,password))
        val client = HttpClientBuilder.create().setDefaultCredentialsProvider(cProv).build()

        client.execute(request).use {
            Files.copy(it.entity.content, target)
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

    fun validateZIPFile(file: java.nio.file.Path, logWriter: MultiEmitter<in String>) {
        logWriter.emit("Validating \n")
        val expected = mutableSetOf("JellingStone/JellingStone.bin", "JellingStone/bootloader.bin", "JellingStone/partition-table.bin")

        ZipFile(file.toFile()).use { zipFile ->
            zipFile.entries().iterator().forEach {
                it.crc
                if(!expected.remove(it.name)) {
                    val msg = "ZIP-Entry ${it.name} is not supposed to be in archive"
                    logWriter.emit("Corrupt ZIP-file $file ${msg}\n")
                    throw RuntimeException(msg)
                }
            }
        }
        if(expected.isNotEmpty()) {
            val msg = "Missing artifacts in archive: ${expected.joinToString(",") { it }}"
            logWriter.emit(msg)
            throw RuntimeException(msg)
        }
    }

    data class FirmwareFile(
        val version: String,
        val build: String,
        val path: String,
    )
}
