package it.polimi.webeep.feature.auth.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsService

/**
 * Opens Polimi SSO in Chrome (or the default browser).
 * Custom Tabs must be allowed to hand off to the CIE ID app, otherwise
 * "Entra con CIE" stays stuck on the web page.
 */
object SsoBrowser {

    private val CHROME_PACKAGES = listOf(
        "com.android.chrome",
        "com.google.android.apps.chrome",
        "com.chrome.beta",
        "com.chrome.dev"
    )

    fun open(context: Context, url: String) {
        val uri = Uri.parse(url)
        if (launchCustomTab(context, uri)) {
            return
        }
        val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(viewIntent)
    }

    /**
     * Opens the SSO flow in the full browser instead of a Custom Tab. Fallback for
     * devices where the Custom Tab does not hand off to the CIE ID app.
     */
    fun openInExternalBrowser(context: Context, url: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            resolveCustomTabsPackage(context)?.let(::setPackage)
        }
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            context.startActivity(viewIntent.setPackage(null))
        }
    }

    private fun launchCustomTab(context: Context, uri: Uri): Boolean {
        return try {
            val customTabs = CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setShowTitle(true)
                .setInstantAppsEnabled(true)
                .setSendToExternalDefaultHandlerEnabled(true)
                .build()
            customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            resolveCustomTabsPackage(context)?.let { packageName ->
                customTabs.intent.setPackage(packageName)
            }
            customTabs.launchUrl(context, uri)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveCustomTabsPackage(context: Context): String? {
        val pm = context.packageManager
        CHROME_PACKAGES.firstOrNull { isInstalled(pm, it) && supportsCustomTabs(pm, it) }?.let { return it }

        val serviceIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION)
        val resolved = pm.queryIntentServices(serviceIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved.firstOrNull()?.serviceInfo?.packageName
    }

    private fun isInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun supportsCustomTabs(pm: PackageManager, packageName: String): Boolean {
        val serviceIntent = Intent(CustomTabsService.ACTION_CUSTOM_TABS_CONNECTION).setPackage(packageName)
        return pm.queryIntentServices(serviceIntent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }
}
