package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import timber.log.Timber

class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        GeofenceBroadcastReceiver.getBroadcast(requireContext())
    }

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
            "foregroundLocationPermissionRequest callback " + "foregroundCoarseLocationApproved: $foregroundCoarseLocationApproved, " + "foregroundFineLocationApproved: $foregroundFineLocationApproved"
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
        checkPermissionsBeforeAddGeofence()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        initViews()
        observeLiveData()
    }

    private fun observeLiveData() {
        _viewModel.addGeofenceLiveData.observe(viewLifecycleOwner) { remainerData ->
            remainerData?.let {
                addGeofence(remainerData)
            }
        }
    }

    private fun initViews() {
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            val directions =
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            // Check permission at first
            checkPermissionsBeforeAddGeofence()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminderDataItem: ReminderDataItem) {
        Timber.d("addGeofence start")
        val geofence = Geofence.Builder().setRequestId(reminderDataItem.id).setCircularRegion(
            reminderDataItem.latitude!!,
            reminderDataItem.longitude!!,
            GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
        ).setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER).build()
        val geofencingRequest =
            GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence).build()
        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Timber.d("Add Geofence successfully with id: ${geofence.requestId}")
            }
            addOnFailureListener {
                Timber.e("Add Geofence error: ${it.message}")
            }
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundCoarseLocationApproved =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        val foregroundFineLocationApproved =
            (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ))
        val backgroundPermissionApproved = if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            true
        }
        Timber.d(
            "foregroundAndBackgroundLocationPermissionApproved foregroundCoarseLocationApproved: $foregroundCoarseLocationApproved, foregroundFineLocationApproved: $foregroundFineLocationApproved, backgroundPermissionApproved: $backgroundPermissionApproved"
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
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
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

    private fun showSnackBarForExplanation() {
        Timber.d("showSnackBarForExplanation")
        Snackbar.make(
            binding.root, R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.settings) {
            startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", requireActivity().application.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.show()
    }

    /*
 *  Uses the Location Client to check the current state of location settings, and gives the user
 *  the opportunity to turn on location services within our app.
 */
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        Timber.d("checkDeviceLocationSettingsAndStartGeofence")
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 10000)
            .setWaitForAccurateLocation(false).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            Timber.d("Error getting location settings resolution: ${exception.message}")
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.d("Error getting location settings resolution: ${sendEx.message}")
                }
            } else {
                Snackbar.make(
                    binding.root, R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            Timber.d("addOnCompleteListener isSuccessful: ${it.isSuccessful}")
            if (it.isSuccessful) {
                _viewModel.validateAndSaveReminder()
            }
        }
    }

    private fun checkPermissionsBeforeAddGeofence() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettings()
        } else {
            requestForegroundPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            runBlocking {
                delay(1000)
                checkDeviceLocationSettings(false)
            }
        }
    }

    companion object {
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}