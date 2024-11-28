package com.udacity.project4.locationreminders

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.fragment.NavHostFragment
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.authentication.AuthenticationState
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.databinding.ActivityRemindersBinding
import timber.log.Timber

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemindersBinding

    private val viewModel by viewModels<RemindersViewModel>()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private val foregroundLocationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Only approximate location access granted.
        val foregroundCoarseLocationApproved =
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        // Precise location access granted.
        val foregroundFineLocationApproved =
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        Timber.d(
            "foregroundLocationPermissionRequest callback " +
                    "foregroundCoarseLocationApproved: $foregroundCoarseLocationApproved, " +
                    "foregroundFineLocationApproved: $foregroundFineLocationApproved"
        )
        when {
            foregroundCoarseLocationApproved && foregroundFineLocationApproved -> {
                requestBackgroundPermissions()
            }

            else -> {
                // No location access granted.
                showSnackBarForExplanation()
            }
        }
    }

    private val backgroundLocationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Check background service location request is approved in onStart() function
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeAuthenticationState()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                (binding.navHostFragment as NavHostFragment).navController.popBackStack()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun observeAuthenticationState() {
        viewModel.authenticationState.observe(this) { authenticationState ->
            when (authenticationState) {
                AuthenticationState.AUTHENTICATED -> {
                    // Signed In
                    Timber.d("observeAuthenticationState AUTHENTICATED")
                }

                else -> {
                    // Signed Out
                    Timber.d("observeAuthenticationState UNAUTHENTICATED")
                    goToAuthenticationScreen()
                }
            }
        }
    }

    private fun goToAuthenticationScreen() {
        val intent = Intent(this, AuthenticationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun showSnackBarForExplanation() {
        Timber.d("showSnackBarForExplanation")
        Snackbar.make(
            binding.root,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", application.packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }

    override fun onStart() {
        super.onStart()
        checkPermissions()
    }

    private fun checkPermissions() {
//        if (viewModel.geofenceIsActive()) return
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundPermissions()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundCoarseLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
        val foregroundFineLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        Timber.d(
            "foregroundAndBackgroundLocationPermissionApproved " +
                    "foregroundCoarseLocationApproved: $foregroundCoarseLocationApproved, " +
                    "foregroundFineLocationApproved: $foregroundFineLocationApproved, " +
                    "backgroundPermissionApproved: $backgroundPermissionApproved"
        )
        return foregroundCoarseLocationApproved && foregroundFineLocationApproved && backgroundPermissionApproved
    }

    /*
 *  Requests ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
 */
    @TargetApi(29)
    private fun requestForegroundPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) return
        Timber.d("Request foreground location permission")
        foregroundLocationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /*
     *  Requests on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29)
    private fun requestBackgroundPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved()) return
        Timber.d("Request background location permission")
        // Open setting.
        backgroundLocationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        )
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        Timber.d("checkDeviceLocationSettingsAndStartGeofence")
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 10000)
            .setWaitForAccurateLocation(false)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this)
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            Timber.d("Error getting location settings resolution: ${exception.message}")
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        this,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.d("Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            Timber.d("addOnCompleteListener isSuccessful: ${it.isSuccessful}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AuthUI.getInstance().signOut(this)
    }

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }
}
