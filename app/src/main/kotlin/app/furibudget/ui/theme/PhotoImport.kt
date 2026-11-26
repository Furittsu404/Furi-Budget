package app.furibudget.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.palette.graphics.Palette
import java.io.File
import kotlin.math.max

/** A background photo copied into app storage plus accent candidates taken from it. */
class ImportedPhoto(val path: String, val colors: List<Int>, val luma: Float)

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
        return ImportedPhoto(out.absolutePath, colorsOf(bitmap), lumaOf(bitmap))
    }

    /** Mean brightness from a 24 px thumbnail, 0 (black) to 1 (white). */
    private fun lumaOf(bitmap: Bitmap): Float {
        val t = Bitmap.createScaledBitmap(bitmap, 24, 24, true)
        var sum = 0.0
        for (y in 0 until 24) for (x in 0 until 24) {
            val px = t.getPixel(x, y)
            sum += 0.299 * ((px shr 16) and 0xFF) + 0.587 * ((px shr 8) and 0xFF) + 0.114 * (px and 0xFF)
        }
        return (sum / (24 * 24 * 255.0)).toFloat()
    }

    private fun colorsOf(bitmap: Bitmap): List<Int> {
        val p = Palette.from(bitmap).maximumColorCount(16).generate()
        return listOfNotNull(
            p.vibrantSwatch, p.lightVibrantSwatch, p.darkVibrantSwatch, p.mutedSwatch, p.dominantSwatch,
        ).map { it.rgb or 0xFF000000.toInt() }.distinct().take(5)
    }
}
