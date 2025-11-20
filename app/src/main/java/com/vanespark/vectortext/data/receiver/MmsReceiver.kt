package com.vanespark.vectortext.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

/**
 * Broadcast receiver for incoming MMS messages
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("MMS received: ${intent.action}")
        // TODO: Implement MMS handling in Phase 1
    }
}
