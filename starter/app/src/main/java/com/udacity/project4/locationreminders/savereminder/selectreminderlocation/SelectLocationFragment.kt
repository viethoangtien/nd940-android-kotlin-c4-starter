package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    private var selectedPOI: PointOfInterest? = null // To hold the currently selected POI
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        onLocationSelected()
        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setMapStyle(map)
        setPoiClick(map)
        checkPermissionToEnableMap()
    }

    private fun checkPermissionToEnableMap() {
        val foregroundCoarseLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
        val foregroundFineLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        Timber.d(
            "checkPermissionToEnableMap " +
                    "foregroundCoarseLocationApproved: $foregroundCoarseLocationApproved, " +
                    "foregroundFineLocationApproved: $foregroundFineLocationApproved"
        )
        if (foregroundCoarseLocationApproved && foregroundFineLocationApproved) {
            map.isMyLocationEnabled = true
        }
    }

    private fun onLocationSelected() {
        binding.textSave.setOnClickListener {
            _viewModel.setSelectedPOI(pointOfInterest = selectedPOI)
            findNavController().popBackStack()
        }
    }

    private fun setMapStyle(googleMap: GoogleMap) {
        runCatching {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Timber.d("Style parsing failed.")
            }
        }.getOrElse {
            Timber.d("Can't find style. Error: $it")
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            clearAllMarkers()
            val poiMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            poiMarker?.showInfoWindow()
            selectedPOI = poi
        }
    }

    // Function to clear all markers
    private fun clearAllMarkers() {
        map.clear() // This will remove all markers from the map
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundCoarseLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
        val foregroundFineLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
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

}