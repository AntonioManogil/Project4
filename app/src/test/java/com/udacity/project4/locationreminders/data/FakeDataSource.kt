package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import java.lang.Exception

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(var reminders: MutableList<ReminderDTO>? = mutableListOf()) : ReminderDataSource {

    //TODO: (Ok) Create a fake data source to act as a double to the real data source
    private var shouldReturnError = false
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if(shouldReturnError){
            return Result.Error("Reminders not found")
        }else {
            reminders.let {
                return Result.Success(ArrayList(reminders))
            }
            return Result.Error("Reminders not found")
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        //TODO: (Ok) "save the reminder"

        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        //TODO (Ok) "return the reminder with the id"
        if(shouldReturnError){
            return Result.Error("Reminders not found")
        }else {

            val reminder = reminders?.find { it.id == id }
            return if (reminder != null) {
                Result.Success(reminder)
            } else {
                Result.Error("Reminder not found")
            }
        }
    }
    override suspend fun deleteAllReminders() {
        //TODO (Ok) "delete all the reminders"
        reminders?.clear()
    }

    fun setShouldReturnError(shouldReturn: Boolean) {
        this.shouldReturnError = shouldReturn
    }
}