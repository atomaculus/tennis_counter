package com.example.tenniscounter.mobile.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import com.example.tenniscounter.mobile.data.local.MatchEntity
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MatchShareManager {
    const val WIDTH = 1080
    const val HEIGHT = 1920

    data class ShareRenderModel(
        val data: ShareCardData,
        val photoBitmap: Bitmap?
    )

    suspend fun prepareShareRenderModel(context: Context, match: MatchEntity): Result<ShareRenderModel> =
        runCatching {
            val data = ShareCardData(
                scoreText = match.finalScoreText,
                setScoresText = match.setScoresText,
                durationText = "Duration: ${formatDuration(match.durationSeconds)}",
                dateText = formatDate(match.createdAt),
                photoUri = match.photoUri
            )

            val photoBitmap = withContext(Dispatchers.IO) {
                loadPhotoBitmap(context, data.photoUri)
            }
            ShareRenderModel(data = data, photoBitmap = photoBitmap)
        }

    fun captureAttachedViewArea(
        sourceView: View,
        captureRect: Rect
    ): Result<Bitmap> = runCatching {
        if (sourceView.width <= 0 || sourceView.height <= 0) {
            error("Cannot capture bitmap: source view has invalid size")
        }

        val fullBitmap = Bitmap.createBitmap(
            sourceView.width,
            sourceView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(fullBitmap)
        sourceView.draw(canvas)

        val safeRect = Rect(
            captureRect.left.coerceIn(0, fullBitmap.width - 1),
            captureRect.top.coerceIn(0, fullBitmap.height - 1),
            captureRect.right.coerceIn(1, fullBitmap.width),
            captureRect.bottom.coerceIn(1, fullBitmap.height)
        )
        if (safeRect.width() <= 0 || safeRect.height() <= 0) {
            fullBitmap.recycle()
            error("Cannot capture bitmap: capture bounds are invalid")
        }

        val cropped = Bitmap.createBitmap(
            fullBitmap,
            safeRect.left,
            safeRect.top,
            safeRect.width(),
            safeRect.height()
        )
        fullBitmap.recycle()

        if (cropped.width == WIDTH && cropped.height == HEIGHT) {
            cropped
        } else {
            val scaled = Bitmap.createScaledBitmap(cropped, WIDTH, HEIGHT, true)
            cropped.recycle()
            scaled
        }
    }

    suspend fun shareRenderedBitmap(context: Context, bitmap: Bitmap): Result<Unit> = runCatching {
        val uri = withContext(Dispatchers.IO) {
            saveBitmapToCacheAndGetUri(context, bitmap)
        }
        withContext(Dispatchers.Main) {
            shareUri(context, uri)
        }
        Unit
    }

    private fun loadPhotoBitmap(context: Context, photoUri: String?): Bitmap? {
        if (photoUri.isNullOrBlank()) return null
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, Uri.parse(photoUri))
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSize(WIDTH, HEIGHT)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveBitmapToCacheAndGetUri(context: Context, bitmap: Bitmap): Uri {
        val shareDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val outputFile = File(shareDir, "match_share_${System.currentTimeMillis()}.png")
        FileOutputStream(outputFile).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
    }

    private fun shareUri(context: Context, uri: Uri) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Share match result")
        context.startActivity(chooser)
    }

    private fun formatDate(timestampMillis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
