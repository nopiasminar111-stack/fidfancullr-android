package com.fidfanstudios.fidfancullr.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.fidfanstudios.fidfancullr.data.PhotoFile
import com.fidfanstudios.fidfancullr.data.PhotoFormats
import com.fidfanstudios.fidfancullr.data.PhotoGroup

/**
 * Scans a SAF tree Uri (the chosen inbox folder) and groups files that share
 * a filename stem, e.g. DSC_1042.JPG + DSC_1042.NEF become one PhotoGroup.
 */
object PhotoGrouper {

    fun scan(context: Context, inboxUri: Uri): List<PhotoGroup> {
        val root = DocumentFile.fromTreeUri(context, inboxUri) ?: return emptyList()
        val files = root.listFiles()
            .filter { it.isFile }
            .mapNotNull { doc ->
                val name = doc.name ?: return@mapNotNull null
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext !in PhotoFormats.SUPPORTED_EXTENSIONS) return@mapNotNull null
                PhotoFile(
                    uri = doc.uri,
                    documentId = doc.uri.lastPathSegment ?: doc.uri.toString(),
                    name = name,
                    extension = ext,
                    sizeBytes = doc.length(),
                    lastModified = doc.lastModified()
                )
            }

        return files
            .groupBy { it.name.substringBeforeLast('.') }
            .map { (stem, groupFiles) ->
                PhotoGroup(
                    stem = stem,
                    // JPEG first so the primary preview is cheap to decode.
                    files = groupFiles.sortedBy { f ->
                        if (f.extension in PhotoFormats.JPEG_EXTENSIONS) 0 else 1
                    }
                )
            }
            .sortedBy { it.stem }
    }
}
