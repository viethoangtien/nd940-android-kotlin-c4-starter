package com.udacity.project4.locationreminders.geofence

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("GeofenceBroadcastReceiver action: ${intent.action}")
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            GeofenceTransitionsJobIntentService.enqueueWork(context, intent)

            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            geofencingEvent?.let {
                if (geofencingEvent.hasError()) {
                    val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                    Log.e(TAG, errorMessage)
                    return
                }

                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Log.v(TAG, context.getString(R.string.geofence_entered))
                    val fenceId = when {
                        geofencingEvent.triggeringGeofences!!.isNotEmpty() ->
                            geofencingEvent.triggeringGeofences!![0].requestId

                        else -> {
                            Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                            return
                        }
                    }
                    val foundIndex = GeofencingConstants.LANDMARK_DATA.indexOfFirst {
                        it.id == fenceId
                    }
                    if (-1 == foundIndex) {
                        Log.e(TAG, "Unknown Geofence: Abort Mission")
                        return
                    }
                    val notificationManager = ContextCompat.getSystemService(
                        context,
                        NotificationManager::class.java
                    ) as NotificationManager

                    notificationManager.sendGeofenceEnteredNotification(
                        context, foundIndex
                    )
                }
            }
        }
    }

    companion object {
        private const val ACTION_GEOFENCE_EVENT = "ACTION_GEOFENCE_EVENT"
        private const val REQUEST_CODE = 0

        fun getBroadcast(context: Context): PendingIntent {
            return Intent(context, GeofenceBroadcastReceiver::class.java).let {
                it.action = ACTION_GEOFENCE_EVENT
                PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }
    }
}