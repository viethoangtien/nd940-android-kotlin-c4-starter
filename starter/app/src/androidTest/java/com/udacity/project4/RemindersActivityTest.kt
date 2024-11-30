package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.toReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.Test


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest : KoinTest {
    // Extended Koin Test - embed autoclose @after method to close Koin after every test
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext, get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext, get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)

    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun saveReminder() = runBlocking {
        val reminder = ReminderDataItem(
            title = "TITLE",
            description = "DESCRIPTION",
            location = "Pham Van Bach",
            latitude = 21.026988562137088,
            longitude = 105.78886761961745
        )
        repository.saveReminder(reminder.toReminderDTO())

        // Start up Tasks screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Interact with the activity
        activityScenario.onActivity { activity ->
            // Perform actions on the activity
            dataBindingIdlingResource.monitorActivity(activityScenario)

            onView(withId(R.id.reminderssRecyclerView)).check(matches(isDisplayed()))
            onView(withId(R.id.reminderssRecyclerView)).check(matches(hasMinimumChildCount(1)))

            onView(withId(R.id.addReminderFAB)).perform(click())
            onView(withId(R.id.reminderTitle)).perform(replaceText("NEW TITLE"))
            onView(withId(R.id.reminderDescription)).perform(replaceText("NEW DESCRIPTION"))
            onView(withId(R.id.saveReminder)).perform(click())

            onView(withText("NEW TITLE")).check(matches(isDisplayed()))
            onView(withId(R.id.reminderssRecyclerView)).check(matches(hasMinimumChildCount(2)))
            onView(withText(R.string.reminder_saved)).inRoot(
                withDecorView(
                    not(
                        activity.window.decorView
                    )
                )
            ).check(matches(isDisplayed()))

        }
        activityScenario.close()
    }


}
