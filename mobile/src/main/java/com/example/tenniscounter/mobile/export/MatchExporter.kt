package com.example.tenniscounter.mobile.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.tenniscounter.mobile.data.local.MatchEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MatchExporter {

    /**
     * Exports a list of matches to a CSV file and returns a share intent.
     */
    fun exportToCsv(context: Context, matches: List<MatchEntity>): Intent? {
        if (matches.isEmpty()) return null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.appendLine("Date,Score,Sets Detail,Duration (min)")

        for (match in matches) {
            val date = dateFormat.format(Date(match.createdAt))
            val durationMin = match.durationSeconds / 60.0
            val setsDetail = match.setScoresText?.replace(",", ";") ?: ""
            val score = match.finalScoreText.replace(",", ";")
            sb.appendLine("$date,$score,$setsDetail,${"%.1f".format(durationMin)}")
        }

        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val fileName = "playce_matches_${System.currentTimeMillis()}.csv"
        val file = File(exportDir, fileName)
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PLAYCE Match History")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
