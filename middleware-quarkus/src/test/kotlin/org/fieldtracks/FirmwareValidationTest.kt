package org.fieldtracks

import io.smallrye.mutiny.subscription.MultiEmitter
import org.fieldtracks.http.FirmwareHttpService
import org.mockito.Mockito
import java.lang.RuntimeException
import java.nio.file.Path
import java.util.zip.ZipException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class FirmwareValidationTest {

    private val service = FirmwareHttpService()

    val emitter = Mockito.mock(MultiEmitter::class.java)

    @Test
    fun negativeBrokenStructure() {
        val resource = this.javaClass.classLoader.getResource("firmwareZipsForValidation/wrong-structure.zip")
        val path = Path.of(resource.toURI())
        val emitter = emitter as MultiEmitter<in String>
        assertFailsWith(RuntimeException::class) {
            service.validateZIPFile(path,emitter)
        }
    }

    @Test
    fun negativeIncompleteDownload() {
        val resource = this.javaClass.classLoader.getResource("firmwareZipsForValidation/missing-90KiB.zip")
        val path = Path.of(resource.toURI())
        val emitter = emitter as MultiEmitter<in String>
        assertFailsWith(ZipException::class) {
            service.validateZIPFile(path,emitter)
        }
    }

    @Test
    fun postiveCorrectFirmware() {
        val resource = this.javaClass.classLoader.getResource("firmwareZipsForValidation/JellingStone-latest+github-main.zip")
        val path = Path.of(resource.toURI())
        val emitter = emitter as MultiEmitter<in String>
        service.validateZIPFile(path,emitter)
    }


}