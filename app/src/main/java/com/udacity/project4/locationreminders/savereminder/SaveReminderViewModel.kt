package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val locationSelected = MutableLiveData<Boolean>()

    val _reminderTitle = MutableLiveData<String?>()
    val reminderTitle: LiveData<String?> get() = _reminderTitle
    val _reminderDescription = MutableLiveData<String?>()
    val reminderDescription: LiveData<String?> get() = _reminderDescription
    private val _reminderSelectedLocationStr = MutableLiveData<String?>()
    val reminderSelectedLocationStr: LiveData<String?> get() = _reminderSelectedLocationStr
    private val _selectedPOI = MutableLiveData<PointOfInterest?>()
    val selectedPOI: LiveData<PointOfInterest?> get() = _selectedPOI
    private val _latitude = MutableLiveData<Double?>()
    val latitude : LiveData<Double?> get() = _latitude
    private val _longitude = MutableLiveData<Double?>()
    val longitude : LiveData<Double?> get() = _longitude


    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        _reminderTitle.value = null
        _reminderDescription.value = null
        _reminderSelectedLocationStr.value = null
        _selectedPOI.value = null
        _latitude.value = null
        _longitude.value = null
    }
    fun setSelectedPOI(value: PointOfInterest) {
        _selectedPOI.value = value
    }
    fun setReminderSelectedLocationStr(value: String) {
        _reminderSelectedLocationStr.value = value
    }
    fun setLatitude(value: Double) {
        _latitude.value = value
    }
    fun setLongitude(value: Double) {
        _longitude.value = value
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude
                )
            )
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }
}