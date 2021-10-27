package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderData: ReminderDataItem
    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isEmpty() ||
            grantResults[RemindersListFragment.LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == RemindersListFragment.REQUEST_LOCATION_PERMISSION &&
                    grantResults[RemindersListFragment.BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Log.v(TAG, "Permissions not granted")
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation, Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Displays App settings screen.
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(resolve: Boolean = true) {
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
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null, 0, 0, 0, null
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                showPermissionSnackBar(binding.root)
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofenceForReminder()
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder() {
        if (this::reminderData.isInitialized) {
            val currentGeofenceData = reminderData


            val geofence = Geofence.Builder()
                .setRequestId(currentGeofenceData.id)
                .setCircularRegion(
                    currentGeofenceData.latitude!!,
                    currentGeofenceData.longitude!!,
                    GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()


            val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
            intent.action = ACTION_GEOFENCE_EVENT

            val geofencePendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            geofencingClient.removeGeofences(geofencePendingIntent)?.run {
                addOnCompleteListener {
                    geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                        addOnSuccessListener {
                            // save reminder to local db
                            _viewModel.validateAndSaveReminder(reminderData)
                            Log.e("Add Geofence", geofence.requestId)

                        }
                        addOnFailureListener {
                            Toast.makeText(
                                context,
                                "Failed to add location!!! Try again later!",
                                Toast.LENGTH_LONG
                            ).show()

                            if ((it.message != null)) {
                                Log.w(TAG, it.message!!)
                            }
                        }
                    }
                }
            }

            // I marked this call and I tested the previous one
            // was I wrong?

            //geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
            //    addOnSuccessListener {
            //        Log.d(TAG, "Location added!!!")
            //        // save reminder to local db
            //        _viewModel.validateAndSaveReminder(reminderData)
            //    }
            //    addOnFailureListener {
            //        Toast.makeText(
            //            context,
            //            "Failed to add location!!! Try again later!",
            //            Toast.LENGTH_LONG
            //        ).show()
            //    }
            //}
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            // TODO: (Ok) use the user entered reminder details to:
            // 1) add a geofencing request
            reminderData = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )
            val canSave = _viewModel.validateEnteredData(
                reminderData
            )
            // 2) save the reminder to the local db
            if (canSave) {
                // _viewModel.saveReminder(reminderData)
                checkPermissionsAndStartGeofencing()
            }
            //if (canSave) {
            //    val geofence = Geofence.Builder()
            //        .setRequestId(id.toString())
            //        .setCircularRegion(
            //            latitude!!,
            //            longitude!!,
            //            100f
            //        )
            //        .setExpirationDuration(Geofence.NEVER_EXPIRE)
            //        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            //        .build()
            //
            //    val geofencingRequest = GeofencingRequest.Builder()
            //        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            //        .addGeofence(geofence)
            //        .build()
            //
            //    if (hasAllLocationPermissions())  {
            //        if (ActivityCompat.checkSelfPermission(
            //                requireActivity(),
            //                Manifest.permission.ACCESS_FINE_LOCATION
            //            ) == PackageManager.PERMISSION_GRANTED
            //        ) {
            //            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
            //                .run {
            //                    addOnFailureListener {
            //                        Log.v("Geofence", "Failed adding..." + it.message)
            //                    }
            //                    addOnSuccessListener {
            //                        _viewModel.validateAndSaveReminder(
            //                            ReminderDataItem(
            //                                title,
            //                                description,
            //                                location,
            //                                latitude,
            //                                longitude,
            //                                geofence.requestId
            //                            )
            //                        )
            //                        Log.v("Geofence", "Added successfully...")
            //                    }
            //                }
            //        }
            //
            //    } else {
            //        showPermissionSnackBar(binding.root)
            //    }
            //}
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
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
        return foregroundLocationApproved && backgroundPermissionApproved

    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "LocationReminder.action.ACTION_GEOFENCE_EVENT"
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
        private const val TAG = "SaveReminderFragment"
        private const val GEOFENCE_RADIUS_IN_METERS = 500f
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 1
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 2
    }
}

