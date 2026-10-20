package com.marfolog.noteswidgetformarkdown.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.marfolog.noteswidgetformarkdown.BuildConfig
import com.marfolog.noteswidgetformarkdown.R
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Play flavour: stores the feedback form as one Firestore document — the answers and the app
 * version, no identifier of the user, the device or any note. Only ever called from "Send", so it
 * does not depend on the analytics switch. The foss twin opens a GitHub discussion instead.
 */
object FeedbackSender {

    @StringRes
    val FOOTER_RES = R.string.feedback_footer_play

    @StringRes
    val FAILED_RES = R.string.feedback_failed_play

    private const val AREA = "Feedback"
    private const val COLLECTION = "feedback"
    private const val TIMEOUT_MILLIS = 15_000L

    /**
     * Memory cache only: a send that failed must not sit on disk and go out on a later start,
     * after the user was told it failed and maybe sent it again.
     */
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()
        }
    }

    suspend fun send(context: Context, form: FeedbackForm): FeedbackSendResult {
        val now = System.currentTimeMillis()
        if (!FeedbackReminder.canSend(context, now)) return FeedbackSendResult.TooSoon
        if (FeedbackReminder.isPending(context, now)) return FeedbackSendResult.Pending
        // Firestore would queue the write and wait for a network; offline is a plain failure here.
        if (!isOnline(context)) return FeedbackSendResult.Failed

        val document = FeedbackPayload.of(form, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) +
            (FeedbackPayload.FIELD_CREATED_AT to FieldValue.serverTimestamp())
        FeedbackReminder.markPending(context, now)
        return when (withTimeoutOrNull(TIMEOUT_MILLIS) { store(document) }) {
            true -> {
                FeedbackReminder.markSent(context, now)
                FeedbackSendResult.Sent
            }
            false -> {
                AppLog.d(AREA, "Feedback not sent")
                FeedbackReminder.clearPending(context)
                FeedbackSendResult.Failed
            }
            null -> {
                AppLog.d(AREA, "Feedback send timed out")
                FeedbackSendResult.Pending
            }
        }
    }

    private suspend fun store(document: Map<String, Any?>): Boolean =
        runCatching { firestore.collection(COLLECTION).add(document) }.getOrNull()?.succeeded() == true

    private fun isOnline(context: Context): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun <T> Task<T>.succeeded(): Boolean = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task -> continuation.resume(task.isSuccessful) }
    }
}
