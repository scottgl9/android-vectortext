package com.vanespark.vertext.data.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import timber.log.Timber

/**
 * Service for handling "Quick Reply" from notifications
 * Required for default SMS app functionality
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("HeadlessSmsSendService started: ${intent?.action}")
        // TODO: Implement quick reply handling in Phase 1
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
