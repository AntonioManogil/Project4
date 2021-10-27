package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {
    private lateinit var repository: ReminderDataSource

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initRepository() {

        stopKoin()

        /**
         * use Koin Library as a service locator
         */
        val myModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as ReminderDataSource
                )
            }

            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(getApplicationContext()) }
        }

        startKoin {
            androidContext(getApplicationContext())
            modules(listOf(myModule))
        }

        repository = GlobalContext.get().koin.get()


        runBlocking {
            repository.deleteAllReminders()
        }
    }



    //TODO: (Ok) test the navigation of the fragments.
    @Test
    fun clickOnFab_navigatesTo_saveReminderFragment() = runBlockingTest {
        // GIVEN - On the fragment
        val scenario =
            launchFragmentInContainer<RemindersListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - Click on addReminderFAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN
        verify(navController).navigate(RemindersListFragmentDirections.toSaveReminder())
    }

    // TODO: (Ok) test the displayed data on the UI.
    @Test
    fun reminders_DisplayedInUi() {
        // GIVEN - Add reminder (incomplete) task to the DB
        val reminder = ReminderDTO(
            title = "mock",
            description = "mock description",
            location = "my work location",
            latitude = 41.44449744049808,
            longitude = 2.1830127781324538
        )

        runBlocking {
            repository.saveReminder(reminder)
        }

        // WHEN - ReminderListFragment fragment launched to display task
        launchFragmentInContainer<RemindersListFragment>(Bundle.EMPTY, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(
            ViewAssertions.matches(
                CoreMatchers.not(
                    ViewMatchers.isDisplayed()
                )
            )
        )

        onView(ViewMatchers.withText(reminder.title)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(ViewMatchers.withText(reminder.description)).check(
            ViewAssertions.matches(
                ViewMatchers.isDisplayed()
            )
        )
        onView(ViewMatchers.withText(reminder.location)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

    }


    // TODO: (Ok) add testing for the error messages.
    @Test
    fun showNoData() = runBlockingTest {
        launchFragmentInContainer<RemindersListFragment>(Bundle.EMPTY, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}