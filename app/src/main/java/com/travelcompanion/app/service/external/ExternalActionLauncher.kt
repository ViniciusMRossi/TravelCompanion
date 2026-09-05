package com.travelcompanion.app.service.external

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ComponentName
import android.content.IntentFilter
import android.content.IntentSender
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import java.io.File

/**
 * Opens specialist apps contextually and always comes back here.
 *
 * External apps stay external: Travel Companion never reimplements maps,
 * dialer or browser. Every launch has a fallback so a missing app is not a
 * dead end on the road.
 */
class ExternalActionLauncher(private val context: Context) {

    private var pendingChoice: BroadcastReceiver? = null

    fun open(uri: String, fallbackUri: String? = null): Boolean {
        if (launch(uri)) return true
        if (fallbackUri != null && launch(fallbackUri)) return true
        return false
    }

    /**
     * Opens the dialer with a number filled in, and stops there.
     *
     * `ACTION_DIAL`, never `ACTION_CALL`: dialling immediately would need the
     * `CALL_PHONE` permission, and this repository has just finished removing
     * a permission it declared and never used. The traveller presses the call
     * button themselves, which is also the right shape for a screen whose
     * numbers include emergency services (D064).
     */
    /**
     * Hands one memory's audio file to the system share sheet.
     *
     * The sheet belongs to Android: this app does not draw one of its own, so
     * there is no list of apps to maintain, no brand logo to ship and no
     * destination this app has an opinion about. The file is exposed through
     * the `FileProvider` whose paths are `files/memories/` and nothing else,
     * and the read permission travels with the Intent and expires with it.
     * The local file is never moved, renamed or removed by sharing (D082).
     *
     * [onSharedTo] is how the row can say "Enviada para WhatsApp": the chooser
     * reports the component the traveller picked, and this turns it into that
     * app's own name. It is best-effort by nature — a chooser dismissed
     * without a choice reports nothing, and the row simply stays as it was.
     */
    fun shareAudio(
        file: File,
        title: String,
        subtitle: String,
        onSharedTo: (String) -> Unit = {},
    ): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.memories", file)
            .buildUpon()
            // What the share sheet calls the file. Without it the traveller is
            // offered the memory's id, which is a UUID.
            .appendQueryParameter(MemoryFileProvider.DISPLAY_NAME, "$title.m4a")
            .build()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = AUDIO_MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, subtitle)
            // The label the preview reads, alongside EXTRA_TITLE: which of the
            // two a given Android version shows is the system's business.
            clipData = ClipData.newUri(context.contentResolver, title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, null, chosenDestination(onSharedTo))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalArgumentException) {
        // The file is not under a path the provider exposes, which is the
        // provider doing its job rather than an error to show anyone.
        false
    }

    /**
     * Hands a packaged document to whatever app on the phone reads it.
     *
     * `ACTION_VIEW` with a read grant that expires with the Intent, the same
     * narrow door `shareAudio` opens in the other direction (D082). This app
     * renders no PDF: a viewer is a specialist app and stays external, like
     * the maps and the dialer above.
     *
     * False when the file is not in this build or the phone has nothing that
     * opens it — a caller can then say so instead of a dead end at a counter.
     */
    fun openDocument(file: File, mimeType: String?): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.memories", file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: DOCUMENT_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(view)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: IllegalArgumentException) {
        // Outside the paths the provider exposes: the provider doing its job.
        false
    }

    /**
     * Listens once for which destination the chooser handed the file to.
     *
     * One receiver at a time: a chooser the traveller backs out of never
     * fires, so the next share replaces the one still waiting rather than
     * leaving both registered.
     */
    private fun chosenDestination(onSharedTo: (String) -> Unit): IntentSender {
        pendingChoice?.let { runCatching { context.unregisterReceiver(it) } }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(from: Context, intent: Intent) {
                runCatching { context.unregisterReceiver(this) }
                pendingChoice = null
                destinationName(intent)?.let(onSharedTo)
            }
        }
        pendingChoice = receiver
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(chosenAction),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(chosenAction).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        ).intentSender
    }

    /** The destination's own name, as the system knows it. Never a logo. */
    private fun destinationName(intent: Intent): String? {
        val component: ComponentName = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_CHOSEN_COMPONENT,
            ComponentName::class.java,
        ) ?: return null
        return runCatching {
            val manager = context.packageManager
            manager.getApplicationLabel(manager.getApplicationInfo(component.packageName, 0)).toString()
        }.getOrNull()
    }

    private val chosenAction: String get() = "${context.packageName}.SHARE_DESTINATION_CHOSEN"

    fun dial(number: String): Boolean = try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number.trim()))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private fun launch(uri: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

/** m4a, which is what `MediaAudioRecorder` writes (brief §22). */
private const val AUDIO_MIME = "audio/mp4"

/** What a packaged document is, when the manifest does not say. */
private const val DOCUMENT_MIME = "application/pdf"
