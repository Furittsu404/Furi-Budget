package app.furibudget.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.SoundPool
import android.net.Uri
import app.furibudget.SoundCatalog
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Plays the short UI sounds. Uses the sonification usage, so the phone's own ringer and touch-sound settings
 * still apply. Sounds load the first time they are needed and stay loaded.
 */
class SoundPlayer(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        ).build()
    private val ids = HashMap<String, Int>()
    private val ready = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(ready) { ready += id } }
    }

    private fun customFile(event: String) = File(File(context.filesDir, "sounds"), "$event.snd")

    fun hasCustom(event: String) = customFile(event).isFile

    private fun key(style: String, event: String) = if (style == STYLE_CUSTOM) "custom:$event" else "$style:$event"

    private fun load(style: String, event: String): Int {
        if (style == STYLE_CUSTOM) {
            val f = customFile(event)
            return if (f.isFile) pool.load(f.absolutePath, 1) else -1
        }
        val res = SoundCatalog.res(style, event) ?: return -1
        return pool.load(context, res, 1)
    }

    /** [volume] is 0..1. Silent, unknown and not-yet-loaded sounds do nothing; the next call plays them once loaded. */
    fun play(style: String, event: String, volume: Float) {
        if (style == STYLE_SILENT) return
        val id = synchronized(ids) { ids.getOrPut(key(style, event)) { load(style, event) } }
        if (id <= 0) return
        val ok = synchronized(ready) { id in ready }
        if (ok) pool.play(id, volume, volume, 1, 0, 1f)
    }

    /** Copies a picked audio file in as the custom sound for [event]. Null on success, otherwise why it was refused. */
    suspend fun importCustom(uri: Uri, event: String): ImportError? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "sounds").apply { mkdirs() }
            val tmp = File(dir, "$event.tmp")
            var bytes = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        bytes += n
                        if (bytes > MAX_BYTES) { tmp.delete(); return@withContext ImportError.TOO_BIG }
                        out.write(buf, 0, n)
                    }
                }
            } ?: return@withContext ImportError.UNREADABLE
            val ms = MediaMetadataRetriever().run {
                try { setDataSource(tmp.absolutePath); extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() } catch (_: Exception) { null } finally { release() }
            }
            if (ms == null) { tmp.delete(); return@withContext ImportError.UNREADABLE }
            if (ms > MAX_MS) { tmp.delete(); return@withContext ImportError.TOO_LONG }
            val dest = customFile(event)
            if (!tmp.renameTo(dest)) { tmp.copyTo(dest, overwrite = true); tmp.delete() }
            synchronized(ids) { ids.remove(key(STYLE_CUSTOM, event))?.let { pool.unload(it) } }
            null
        }.getOrElse { ImportError.UNREADABLE }
    }

    fun clearCustom(event: String) {
        customFile(event).delete()
        synchronized(ids) { ids.remove(key(STYLE_CUSTOM, event))?.let { pool.unload(it) } }
    }

    enum class ImportError { TOO_BIG, TOO_LONG, UNREADABLE }

    companion object {
        const val STYLE_SILENT = "silent"
        const val STYLE_CUSTOM = "custom"
        const val MAX_BYTES = 1_000_000L
        const val MAX_MS = 3_500L
    }
}
