package com.vanespark.vertext.domain.service

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages SMS permissions and default messaging app status
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS
        )

        val REQUIRED_PERMISSIONS_API_33 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS + Manifest.permission.POST_NOTIFICATIONS
        } else {
            REQUIRED_PERMISSIONS
        }
    }

    /**
     * Check if all required SMS permissions are granted
     */
    fun hasAllSmsPermissions(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS_API_33
        } else {
            REQUIRED_PERMISSIONS
        }

        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get list of permissions that are not yet granted
     */
    fun getMissingPermissions(): List<String> {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS_API_33
        } else {
            REQUIRED_PERMISSIONS
        }

        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if app is the default SMS app
     */
    fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses RoleManager
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) ?: false
        } else {
            // Android 9 and below use Telephony
            @Suppress("DEPRECATION")
            android.provider.Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    /**
     * Check if the app can request to become the default SMS app
     */
    fun canRequestDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleAvailable(RoleManager.ROLE_SMS) ?: false
        } else {
            true // Pre-Q versions always allow this
        }
    }

    /**
     * Check if all permissions are granted AND app is default SMS app
     */
    fun isFullySetup(): Boolean {
        return hasAllSmsPermissions() && isDefaultSmsApp()
    }

    /**
     * Get a human-readable permission name
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.SEND_SMS -> "Send SMS"
            Manifest.permission.RECEIVE_SMS -> "Receive SMS"
            Manifest.permission.READ_SMS -> "Read SMS"
            Manifest.permission.READ_CONTACTS -> "Read Contacts"
            Manifest.permission.POST_NOTIFICATIONS -> "Show Notifications"
            else -> permission.substringAfterLast(".")
        }
    }

    /**
     * Get a description for why a permission is needed
     */
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.SEND_SMS ->
                "Required to send text messages to your contacts"
            Manifest.permission.RECEIVE_SMS ->
                "Required to receive incoming text messages"
            Manifest.permission.READ_SMS ->
                "Required to read your message history and display conversations"
            Manifest.permission.READ_CONTACTS ->
                "Required to show contact names and photos in conversations"
            Manifest.permission.POST_NOTIFICATIONS ->
                "Required to show notifications for new messages"
            else -> "Required for app functionality"
        }
    }
}
