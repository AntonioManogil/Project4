package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RemindersDaoTest {

    //TODO: (Ok) Add testing implementation to the RemindersDao.kt
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var mockReminder: ReminderDTO

    // Executes in order: Before, Test, After.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    // Using an memory database
    fun initRemindersDatabase() = runBlockingTest{
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        // Get a mock ReminderDTO row.
        mockReminder = mockReminderRow()

        // I save reminder before.
        // The Test functions need the mockReminder to be saved on the contrary they will test false
        remindersDatabase.reminderDao().saveReminder(mockReminder)
    }

    @After
    // Close database
    fun closeRemindersDatabase() = remindersDatabase.close()

    // Get a mock ReminderDTO row
    private fun mockReminderRow(): ReminderDTO {
        return ReminderDTO(
            title = "mock",
            description = "mock description",
            location = "my work location",
            latitude = 41.44449744049808,
            longitude = 2.1830127781324538)
    }

    @Test
    fun getReminders() = runBlockingTest {
        val mockRemindersFromDatabase = remindersDatabase.reminderDao().getReminders()
        // We have to retrieve 1 row from remindersDatabase
        assertThat(mockRemindersFromDatabase.size, `is` (1))
        compareReminderDTO(mockRemindersFromDatabase[0])
    }

    @Test
    fun getReminderById() = runBlockingTest {
        val mockReminderFromDatabase = remindersDatabase.reminderDao().getReminderById(mockReminder.id)
        compareReminderDTO(mockReminderFromDatabase)
    }

    @Test
    fun deleteAllReminders() = runBlockingTest {
        // There is 1 row
        getReminders()
        remindersDatabase.reminderDao().deleteAllReminders()

        // After deleting all rows the size is 0
        val mockRemindersFromDatabase = remindersDatabase.reminderDao().getReminders()
        assertThat(mockRemindersFromDatabase.size, `is` (0))
    }

    private fun compareReminderDTO(mockReminderFromDatabase: ReminderDTO?) = runBlockingTest {
        assertThat<ReminderDTO>(mockReminderFromDatabase as ReminderDTO, notNullValue())
        assertThat(mockReminderFromDatabase.id, `is`(mockReminder.id))
        assertThat(mockReminderFromDatabase.title, `is`(mockReminder.title))
        assertThat(mockReminderFromDatabase.description, `is`(mockReminder.description))
        assertThat(mockReminderFromDatabase.latitude, `is`(mockReminder.latitude))
        assertThat(mockReminderFromDatabase.longitude, `is`(mockReminder.longitude))
        assertThat(mockReminderFromDatabase.location, `is`(mockReminder.location))
    }
}