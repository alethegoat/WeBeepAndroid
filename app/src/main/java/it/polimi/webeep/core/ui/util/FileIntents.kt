package it.polimi.webeep.core.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

private fun Context.fileProviderUri(file: File) = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

/** Opens [file] in another app through the system chooser. Returns false if nothing can open it. */
fun openWithExternalApp(context: Context, file: File, mimeType: String, chooserTitle: String?): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(context.fileProviderUri(file), mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: IllegalArgumentException) {
    false
}

fun shareFile(context: Context, file: File, mimeType: String, title: String, chooserTitle: String): Boolean = try {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, context.fileProviderUri(file))
        putExtra(Intent.EXTRA_TITLE, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: IllegalArgumentException) {
    false
}
