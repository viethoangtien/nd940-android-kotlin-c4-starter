package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.toReminderDTO
import com.udacity.project4.locationreminders.data.local.FakeRemindersLocalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin() //stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            single<ReminderDataSource> { FakeRemindersLocalRepository() }
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun tearDown() {
        stopKoin() // Stop Koin after tests
    }

    @Test
    fun clickReminder_navigateToDescriptionFragment() = runTest {
        val reminder1 = ReminderDataItem(
            title = "TITLE1",
            description = "DESCRIPTION1",
            location = "Pham Van Bach",
            latitude = 21.026988562137088,
            longitude = 105.78886761961745
        )
        val reminder2 = ReminderDataItem(
            title = "TITLE2",
            description = "DESCRIPTION2",
            location = "Pham Van Bach",
            latitude = 21.026988562137088,
            longitude = 105.78886761961745
        )
        repository.saveReminder(reminder1.toReminderDTO())
        repository.saveReminder(reminder2.toReminderDTO())

        // GIVEN - On the list screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment { fragment ->
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        onView(withId(R.id.reminderssRecyclerView))
            .perform(
                RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                    hasDescendant(withText("TITLE2")), click()
                )
            )

        verify(navController).navigate(
            ReminderListFragmentDirections.actionReminderListFragmentToReminderDescriptionActivity(
                reminder2
            )
        )
    }

    @Test
    fun getListReminders_displayedNoData() = runTest {
        // WHEN - List fragment launched to display reminder
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - Show no data
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun getListReminders_displayedRecyclerView() = runTest {
        val reminder1 = ReminderDataItem(
            title = "TITLE1",
            description = "DESCRIPTION1",
            location = "Pham Van Bach",
            latitude = 21.026988562137088,
            longitude = 105.78886761961745
        )
        val reminder2 = ReminderDataItem(
            title = "TITLE2",
            description = "DESCRIPTION2",
            location = "Pham Van Bach",
            latitude = 21.026988562137088,
            longitude = 105.78886761961745
        )
        repository.saveReminder(reminder1.toReminderDTO())
        repository.saveReminder(reminder2.toReminderDTO())

        // WHEN - List fragment launched to display reminder
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - Show no data
        onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(hasMinimumChildCount(2)))
    }

    @Test
    fun retrievesReminder_getError() = runTest {
        // GIVEN - set error flag for repository
        (repository as FakeRemindersLocalRepository).setReturnError(true)

        // WHEN  - List fragment launched to display reminder.
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)

        // THEN - Show snack bar with error.
        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Test exception")))
    }


}