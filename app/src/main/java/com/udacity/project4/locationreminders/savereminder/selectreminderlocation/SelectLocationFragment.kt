package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.reminderslist.RemindersListFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.EspressoIdlingResource.wrapEspressoIdlingResource
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.lang.Exception
import java.util.*

private const val TAG = "SetLocationFragment"

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    private lateinit var map: GoogleMap


    //Use Koin to get the view model of the SaveReminder
    override val _viewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocation = MutableLiveData<Location>()
    private lateinit var selectedPointOfInterest: PointOfInterest
    private lateinit var selectedMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)

        //TODO: (Ok) add the map setup implementation
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //TODO: (Ok) zoom to the user location after taking his permission
        myLocation.observe(viewLifecycleOwner) { location ->
            latLngZoomMarker(location.latitude, location.longitude)
        }
        //TODO: (Ok ->  onMapReady) add style to the map
        //TODO: put a marker to location that the user selected


        //TODO: call this function after the user confirms on the selected location
        binding.saveButton.setOnClickListener {
            onLocationSelected()
        }
        return binding.root
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RemindersListFragment.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[RemindersListFragment.LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_GRANTED
            ) {
                map.isMyLocationEnabled = true
                checkDeviceLocationSettingsAndSetMyLocation()
            }else{
                Toast.makeText(context, getString(R.string.permission_denied_explanation), Toast.LENGTH_LONG).show()
           }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SaveReminderFragment.REQUEST_TURN_DEVICE_LOCATION_ON) {
            if(resultCode != 0) {
                map.isMyLocationEnabled = true
                Thread.sleep(1000)
                checkDeviceLocationSettingsAndSetMyLocation()
            }
        }
    }


    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        if (this::selectedPointOfInterest.isInitialized) {
            _viewModel.setSelectedPOI(selectedPointOfInterest)
            _viewModel.setReminderSelectedLocationStr(selectedPointOfInterest.name)
            _viewModel.setLatitude(selectedPointOfInterest.latLng.latitude)
            _viewModel.setLongitude(selectedPointOfInterest.latLng.longitude)
        }
        findNavController().popBackStack()
    }

    //override fun onRequestPermissionsResult(
    //    requestCode: Int,
    //    permissions: Array<out String>,
    //    grantResults: IntArray
    //) {
    //    if (requestCode == REQUEST_LOCATION_PERMISSION) {
    //        if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
    //            enableMyLocation()
    //        }
    //    }
    //}

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        //TODO: (Ok) Change the map type based on the user's selection.
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

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap


        val latitude = 41.58915981771976
        val longitude = 2.207358698077319

        latLngZoomMarker(latitude, longitude)
        setMapStyle(map)
        setPoiClick(map)
        if (hasBaseLocationPermissions()) {
            map.isMyLocationEnabled = true
            checkDeviceLocationSettingsAndSetMyLocation()
        } else {
            requestForegroundLocationPermissions()
        }
    }

    private fun checkDeviceLocationSettingsAndSetMyLocation(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        SaveReminderFragment.REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                setMyLocation()
            }
        }
    }

    private fun latLngZoomMarker(latitude: Double, longitude: Double) {
        val zoomLevel = 15f
        // Add a marker in my home and move the camera
        val homeLatLng = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))
        map.addMarker(MarkerOptions().position(homeLatLng))
        setMapLongClick(map)
    }

    private fun setMapLongClick(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            if (this::selectedMarker.isInitialized) {
                selectedMarker.remove()
            }

            val snippet = String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )

            selectedPointOfInterest = PointOfInterest(latLng, snippet, snippet)

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.reminder_location))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            selectedMarker.showInfoWindow()
            wrapEspressoIdlingResource {
                _viewModel.locationSelected.postValue(true)
            }
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    context, R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setMyLocation() {
        // I just
        val zoomLevel = 18f
        map.moveCamera(CameraUpdateFactory.zoomBy(zoomLevel))


        fusedLocationClient.lastLocation?.addOnSuccessListener {
            try {
                val snippet = String.format(
                    Locale.getDefault(),
                    getString(R.string.lat_long_snippet),
                    it.latitude,
                    it.longitude
                )
                if(it.latitude != null && it.latitude != null) {
                    val myLatLng = LatLng(it.latitude, it.longitude)
                    selectedPointOfInterest =
                        PointOfInterest(myLatLng, snippet, "My Current Location")

                    selectedMarker = map.addMarker(
                        MarkerOptions()
                            .position(myLatLng)
                            .title(getString(R.string.reminder_location))
                            .snippet(snippet)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatLng, zoomLevel))
                }
                selectedMarker.showInfoWindow()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set location")
            }
        }
    }

    private fun setPoiClick(map: GoogleMap) {

        map.setOnPoiClickListener { poi ->
            if (this::selectedMarker.isInitialized) {
                selectedMarker.remove()
            }

            selectedMarker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )

            selectedPointOfInterest = poi

            selectedMarker.showInfoWindow()
            wrapEspressoIdlingResource {
                _viewModel.locationSelected.postValue(true)
            }
        }
    }
}
