package it.polimi.webeep.core.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import it.polimi.webeep.core.common.FileUtils
import java.io.File

/** The receiving app sees [displayName] built from the human [title], not the hashed name stored on disk. */
internal fun Context.fileProviderUri(file: File, title: String): Uri =
    FileProvider.getUriForFile(this, "$packageName.fileprovider", file, FileUtils.buildDisplayName(title, file.name))

/** Opens [file], presented as [title], in another app through the system chooser. Returns false if nothing can open it. */
fun openWithExternalApp(context: Context, file: File, mimeType: String, title: String, chooserTitle: String?): Boolean = try {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(context.fileProviderUri(file, title), mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
    true
} catch (_: ActivityNotFoundException) {
    false
} catch (_: IllegalArgumentException) {
    false
}

/** Shares [file], presented as [title], through the system chooser. Returns false if no app can take it. */
fun shareFile(context: Context, file: File, mimeType: String, title: String, chooserTitle: String): Boolean = try {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, context.fileProviderUri(file, title))
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
