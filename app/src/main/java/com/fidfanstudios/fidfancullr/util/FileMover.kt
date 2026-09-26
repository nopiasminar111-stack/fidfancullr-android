import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.fidfanstudios.fidfancullr.data.PhotoGroup

object FileMover {

    fun moveGroupTo(
        context: Context,
        inboxUri: Uri,
        group: PhotoGroup,
        destinationFolderName: String,
        prefix: String = "",
        suffix: String = ""
    ): Boolean {
        val root = DocumentFile.fromTreeUri(context, inboxUri) ?: return false

        val destDir = root.findFile(destinationFolderName)
            ?: root.createDirectory(destinationFolderName)
            ?: return false

        var ok = true

        for (file in group.files) {
            val moved = try {
                DocumentsContract.moveDocument(
                    context.contentResolver,
                    file.uri,
                    root.uri,
                    destDir.uri
                ) != null
            } catch (_: Exception) {
                copyThenDelete(context, file.uri, destDir)
            }

            if (!moved) {
                ok = false
            } else if (prefix.isNotEmpty() || suffix.isNotEmpty()) {
                val movedFile = destDir.findFile(file.name)

                if (movedFile != null) {
                    val newName =
                        prefix +
                            file.name.substringBeforeLast('.') +
                            suffix +
                            "." +
                            file.name.substringAfterLast('.')

                    runCatching {
                        DocumentsContract.renameDocument(
                            context.contentResolver,
                            movedFile.uri,
                            newName
                        )
                    }
                }
            }
        }

        return ok
    }

    fun moveGroupBack(
        context: Context,
        inboxUri: Uri,
        group: PhotoGroup,
        destinationFolderName: String,
        prefix: String = "",
        suffix: String = ""
    ): Boolean {
        val root = DocumentFile.fromTreeUri(context, inboxUri) ?: return false
        val dest = root.findFile(destinationFolderName) ?: return false

        var ok = true

        for (original in group.files) {
            val renamed =
                prefix +
                    original.name.substringBeforeLast('.') +
                    suffix +
                    "." +
                    original.name.substringAfterLast('.')

            val file =
                dest.findFile(renamed)
                    ?: dest.findFile(original.name)
                    ?: continue

            val moved = try {
                DocumentsContract.moveDocument(
                    context.contentResolver,
                    file.uri,
                    dest.uri,
                    root.uri
                ) != null
            } catch (_: Exception) {
                copyThenDelete(context, file.uri, root)
            }

            if (!moved) {
                ok = false
            }
        }

        return ok
    }

    private fun copyThenDelete(
        context: Context,
        sourceUri: Uri,
        destDir: DocumentFile
    ): Boolean {
        return try {
            val source =
                DocumentFile.fromSingleUri(context, sourceUri)
                    ?: return false

            val name = source.name ?: return false

            val newFile =
                destDir.createFile(
                    source.type ?: "application/octet-stream",
                    name
                ) ?: return false

            val input =
                context.contentResolver.openInputStream(sourceUri)
                    ?: return false

            val output =
                context.contentResolver.openOutputStream(newFile.uri)
                    ?: return false

            input.use { inputStream ->
                output.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            source.delete()
        } catch (_: Exception) {
            false
        }
    }
}