package app.furibudget.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import java.io.File
import kotlin.math.max

/** A background photo copied into app storage plus accent candidates taken from it. */
class ImportedPhoto(val path: String, val colors: List<Int>)

object PhotoImport {
    private const val MAX_SIDE = 1600

    /** Decodes [uri] at no more than [MAX_SIDE] px, saves it as JPEG under filesDir and removes [previous]. Null if unreadable. */
    fun import(context: Context, uri: Uri, previous: String?): ImportedPhoto? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream returns null when only reading bounds, so check the stream itself.
        val opened = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds); true } ?: return null
        if (!opened) return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val dir = File(context.filesDir, "backgrounds").apply { mkdirs() }
        val out = File(dir, "bg_${System.currentTimeMillis()}.jpg")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        previous?.let { File(it).takeIf { f -> f.exists() && f.parentFile == dir }?.delete() }
        return ImportedPhoto(out.absolutePath, colorsOf(bitmap))
    }

    private fun colorsOf(bitmap: Bitmap): List<Int> {
        val p = Palette.from(bitmap).maximumColorCount(16).generate()
        return listOfNotNull(
            p.vibrantSwatch, p.lightVibrantSwatch, p.darkVibrantSwatch, p.mutedSwatch, p.dominantSwatch,
        ).map { it.rgb or 0xFF000000.toInt() }.distinct().take(5)
    }
}
