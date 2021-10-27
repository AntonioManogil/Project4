package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class RemindersLocalRepositoryTest {

    //TODO: (Ok) Add testing implementation to the RemindersLocalRepository.kt
    private lateinit var remindersDatabase: RemindersDatabase
    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var mockReminder: ReminderDTO

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    // Using an memory database in order to be destroyed when the
    // process ends.
    fun initRemindersDatabaseAndRepository() = runBlocking{
        remindersDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        remindersLocalRepository = RemindersLocalRepository(remindersDatabase.reminderDao(), Dispatchers.Main)

        // Get a mock ReminderDTO row.
        mockReminder = mockReminderRow()

        // I save reminder before.
        // The Test functions need the mockReminder to be saved on the contrary they will test false
        remindersLocalRepository.saveReminder(mockReminder)
    }

    @After
    // Close database
    fun closeRemindersDatabase() {
        remindersDatabase.close()
    }

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
    fun getReminders() = runBlocking {
        // Get all reminders from remindersLocalRepository
        val mockRemindersFromRepository = remindersLocalRepository.getReminders()

        // Verify Result.Success
        assertThat(mockRemindersFromRepository is Result.Success, `is`(true))

        mockRemindersFromRepository as Result.Success

        // We have to retrieve 1 row from remindersLocalRepository
        assertThat(mockRemindersFromRepository.data.size,  `is` (1))

        compareReminderDTO(mockRemindersFromRepository.data[0])
    }

    @Test
    fun getReminder() = runBlocking {
        // Get all reminders from remindersLocalRepository
        val mockRemindersFromRepository = remindersLocalRepository.getReminder(mockReminder.id)

        // Verify Result.Success
        assertThat(mockRemindersFromRepository is Result.Success, `is`(true))

        mockRemindersFromRepository as Result.Success

        compareReminderDTO(mockRemindersFromRepository.data)
    }

    @Test
    fun deleteAllReminders() = runBlocking{

        remindersLocalRepository.deleteAllReminders()

        val mockReminderFromRepository = remindersLocalRepository.getReminder(mockReminder.id)

        assertThat(mockReminderFromRepository is Result.Error, `is`(true))
        mockReminderFromRepository as Result.Error
        assertThat(mockReminderFromRepository.message, `is`("Reminder not found!"))
    }

    // Compare mockReminder to the file retrieve from repository
    private fun compareReminderDTO(mockReminderFromRepository: ReminderDTO?) = runBlocking {
        assertThat<ReminderDTO>(mockReminderFromRepository as ReminderDTO,
            CoreMatchers.notNullValue()
        )
        assertThat(mockReminderFromRepository.id, `is`(mockReminder.id))
        assertThat(mockReminderFromRepository.title, `is`(mockReminder.title))
        assertThat(mockReminderFromRepository.description, `is`(mockReminder.description))
        assertThat(mockReminderFromRepository.latitude, `is`(mockReminder.latitude))
        assertThat(mockReminderFromRepository.longitude, `is`(mockReminder.longitude))
        assertThat(mockReminderFromRepository.location, `is`(mockReminder.location))
    }

}