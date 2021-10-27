package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(maxSdk = Build.VERSION_CODES.P)
@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {
    //TODO: (Ok) provide testing to the SaveReminderView and its live data objects
    @get: Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val fakeDataSource = FakeDataSource()

    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Before
    fun createRepository(){
        stopKoin()
        saveReminderViewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    private fun getReminder(): ReminderDataItem {
        return ReminderDataItem(
            title = "mock",
            description = "mock description",
            location = "my work location",
            latitude = 41.44449744049808,
            longitude = 2.1830127781324538
)
    }

    @Test
    fun check_loading(){
        val reminderDataItem = getReminder()
        mainCoroutineRule.pauseDispatcher()
        saveReminderViewModel.saveReminder(reminderDataItem)
        var showLoading = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(true))
        mainCoroutineRule.resumeDispatcher()
        showLoading = saveReminderViewModel.showLoading.getOrAwaitValue()
        assertThat(showLoading, `is`(false))
    }

    @Test
    fun check_toast(){
        val reminderDataItem = getReminder()
        saveReminderViewModel.saveReminder(reminderDataItem)
        val showToast = saveReminderViewModel.showToast.getOrAwaitValue()
        assertThat(showToast, `is`("Reminder Saved !"))
    }

    @Test
    fun check_navigation(){
        val reminderDataItem = getReminder()
        saveReminderViewModel.saveReminder(reminderDataItem)
        val navigate = saveReminderViewModel.navigationCommand.getOrAwaitValue()
        navigate as NavigationCommand
        assertThat(navigate, instanceOf(NavigationCommand.Back::class.java))
    }

    @Test
    fun shouldReturnError_noTitle() {
        val reminderDataItem = ReminderDataItem(
            title = null,
            description = "mock description",
            location = "my work location",
            latitude = 41.44449744049808,
            longitude = 2.1830127781324538        )
        assertThat(saveReminderViewModel.validateEnteredData(reminderDataItem), `is`(false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun shouldReturnError_noLocation() {
        val reminderDataItem = ReminderDataItem(
            title = "mock",
            description = "mock description",
            location = null,
            latitude = 41.44449744049808,
            longitude = 2.1830127781324538
        )
        assertThat(saveReminderViewModel.validateEnteredData(reminderDataItem), `is`(false))
        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }
}