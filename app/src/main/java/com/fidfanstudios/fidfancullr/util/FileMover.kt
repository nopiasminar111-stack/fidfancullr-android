package com.fidfanstudios.fidfancullr.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.fidfanstudios.fidfancullr.data.PhotoGroup

/**
 * Moves every file belonging to a PhotoGroup (e.g. the JPG and its paired NEF)
 * together into a destination subfolder of the inbox, using
 * DocumentsContract.moveDocument so it's a fast provider-side move rather
 * than a copy + delete.
 */
object FileMover {

    fun moveGroupTo(
        context: Context,
        inboxUri: Uri,
        group: PhotoGroup,
        destinationFolderName: String
    ): Boolean {
        val root = DocumentFile.fromTreeUri(context, inboxUri) ?: return false
        val destDir = root.findFile(destinationFolderName)
            ?: root.createDirectory(destinationFolderName)
            ?: return false

        val resolver = context.contentResolver
        var allSucceeded = true

        for (file in group.files) {
            val success = try {
                DocumentsContract.moveDocument(
                    resolver,
                    file.uri,
                    root.uri,
                    destDir.uri
                ) != null
            } catch (e: Exception) {
                // Fallback: some providers don't support moveDocument; copy + delete instead.
                copyThenDelete(context, file.uri, destDir)
            }
            if (!success) allSucceeded = false
        }
        return allSucceeded
    }

    private fun copyThenDelete(context: Context, sourceUri: Uri, destDir: DocumentFile): Boolean {
        return try {
            val sourceDoc = DocumentFile.fromSingleUri(context, sourceUri) ?: return false
            val name = sourceDoc.name ?: return false
            val mime = sourceDoc.type ?: "application/octet-stream"
            val newFile = destDir.createFile(mime, name) ?: return false

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    input.copyTo(output)
                }
            }
            sourceDoc.delete()
        } catch (e: Exception) {
            false
        }
    }
}
