package com.rskickbox.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipInputStream

private const val RS_WAKE_MODEL_URL_V188 =
    "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
private const val RS_WAKE_MODEL_DIR_V188 = "vosk-model-small-en-us-0.15"
private const val RS_WAKE_SAMPLE_RATE_V188 = 16000
private const val RS_WAKE_MODEL_VERSION_V188 = "en-us-0.15"

object RsOfflineWakeModelV188 {
    fun installed(context: Context): Boolean {
        val root = File(context.filesDir, "rs_voice_models")
        val dir = File(root, RS_WAKE_MODEL_DIR_V188)
        return File(dir, "conf").exists() && File(dir, "am").exists()
    }

    fun path(context: Context): String =
        File(File(context.filesDir, "rs_voice_models"), RS_WAKE_MODEL_DIR_V188).absolutePath

    fun version(context: Context): String =
        if(installed(context)) RS_WAKE_MODEL_VERSION_V188 else ""

    suspend fun ensure(
        context: Context,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if(installed(context)) return@runCatching path(context)

            val root = File(context.filesDir, "rs_voice_models").apply { mkdirs() }
            val zipFile = File(root, "rs-wake-model.tmp.zip")
            val partial = File(root, RS_WAKE_MODEL_DIR_V188 + ".partial")
            if(partial.exists()) partial.deleteRecursively()
            partial.mkdirs()

            val connection = (URL(RS_WAKE_MODEL_URL_V188).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "RS-KICKBOXING-Android")
            }
            connection.connect()
            if(connection.responseCode !in 200..299) {
                error("Silent wake model download failed: HTTP " + connection.responseCode)
            }

            val total = connection.contentLengthLong.coerceAtLeast(1L)
            var downloaded = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(zipFile).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while(true) {
                        val read = input.read(buffer)
                        if(read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(((downloaded * 100L) / total).toInt().coerceIn(0, 100))
                    }
                }
            }
            connection.disconnect()

            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zip ->
                while(true) {
                    val entry = zip.nextEntry ?: break
                    val relative = entry.name
                        .removePrefix(RS_WAKE_MODEL_DIR_V188 + "/")
                        .trimStart('/')
                    if(relative.isBlank()) {
                        zip.closeEntry()
                        continue
                    }
                    val out = File(partial, relative)
                    val canonicalRoot = partial.canonicalPath + File.separator
                    val canonicalOut = out.canonicalPath
                    require(canonicalOut.startsWith(canonicalRoot)) { "Invalid wake model archive path." }
                    if(entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { fos ->
                            val buffer = ByteArray(32 * 1024)
                            while(true) {
                                val read = zip.read(buffer)
                                if(read <= 0) break
                                fos.write(buffer, 0, read)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
            zipFile.delete()

            val finalDir = File(root, RS_WAKE_MODEL_DIR_V188)
            if(finalDir.exists()) finalDir.deleteRecursively()
            require(partial.renameTo(finalDir)) { "Could not activate silent wake model." }
            require(installed(context)) { "Silent wake model is incomplete." }
            onProgress(100)
            finalDir.absolutePath
        }
    }
}

class RsOfflineWakeEngineV188(
    private val context: Context,
    private val onWake: (String) -> Unit,
    private val onState: (String) -> Unit = {}
) {
    private val running = AtomicBoolean(false)
    private var worker: Thread? = null
    private var audioRecord: AudioRecord? = null
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    fun start(): Result<Unit> = runCatching {
        if(running.get()) return@runCatching
        require(RsOfflineWakeModelV188.installed(context)) { "Silent wake model is not installed." }
        require(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        ) { "Microphone permission required." }

        LibVosk.setLogLevel(LogLevel.WARNINGS)
        val localModel = Model(RsOfflineWakeModelV188.path(context))
        val grammar = """[
            "wake up rs","rs wake up","hey rs","ok rs","rs hey","rs ok",
            "wake up rs open dashboard","wake up rs open chat","wake up rs open music",
            "wake up rs open ai","wake up rs open homework","wake up rs open progress",
            "wake up rs open settings","wake up rs open groups","wake up rs open community",
            "rs wake up open dashboard","rs wake up open chat","rs wake up open music",
            "rs wake up open ai","rs wake up open homework","rs wake up open progress",
            "rs wake up open settings","rs wake up open groups","rs wake up open community",
            "hey rs open dashboard","hey rs open chat","hey rs open music",
            "hey rs open ai","hey rs open homework","hey rs open progress",
            "hey rs open settings","hey rs open groups","hey rs open community",
            "[unk]"
        ]""".replace("\n"," ").replace(Regex("\\s+")," ")
        val localRecognizer = Recognizer(localModel, RS_WAKE_SAMPLE_RATE_V188.toFloat(), grammar)

        val min = AudioRecord.getMinBufferSize(
            RS_WAKE_SAMPLE_RATE_V188,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        require(min > 0) { "Audio input is not available." }
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            RS_WAKE_SAMPLE_RATE_V188,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(min * 2, 4096)
        )
        require(record.state == AudioRecord.STATE_INITIALIZED) { "Could not initialize silent wake microphone." }

        model = localModel
        recognizer = localRecognizer
        audioRecord = record
        running.set(true)
        onState("SILENT_WAKE_READY")

        worker = Thread({
            val buffer = ShortArray(2048)
            try {
                record.startRecording()
                while(running.get()) {
                    val count = record.read(buffer, 0, buffer.size)
                    if(count <= 0) continue
                    val complete = localRecognizer.acceptWaveForm(buffer, count)
                    val json = if(complete) localRecognizer.result else localRecognizer.partialResult
                    val key = if(complete) "text" else "partial"
                    val heard = runCatching { JSONObject(json).optString(key) }.getOrDefault("")
                    if(heard.isNotBlank() && rsOfflineWakeMatchV188(heard)) {
                        running.set(false)
                        onState("SILENT_WAKE_HEARD")
                        onWake(heard)
                        break
                    }
                }
            } catch (_: Throwable) {
                onState("SILENT_WAKE_ERROR")
            } finally {
                runCatching { record.stop() }
            }
        }, "rs-offline-wake-v188").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running.set(false)
        runCatching { audioRecord?.stop() }
        runCatching { worker?.join(600) }
        worker = null
        runCatching { recognizer?.close() }
        runCatching { model?.close() }
        recognizer = null
        model = null
        runCatching { audioRecord?.release() }
        audioRecord = null
    }
}

private fun rsOfflineWakeMatchV188(text: String): Boolean {
    val normalized = text.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
    return listOf(
        "wake up rs",
        "rs wake up",
        "hey rs",
        "ok rs",
        "rs hey",
        "rs ok"
    ).any { normalized.contains(it) }
}
