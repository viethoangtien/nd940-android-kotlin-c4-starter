package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.toReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var remindersLocalRepository: FakeDataSource

    @Before
    fun setUp() {
        remindersLocalRepository = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = remindersLocalRepository
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun loadReminders_loading() = runTest {
        // Main dispatcher will not run coroutines eagerly for this test
        Dispatchers.setMain(StandardTestDispatcher())

        // Load the reminders in the view model.
        remindersListViewModel.loadReminders()

        // Then assert that the progress indicator is shown.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutine actions
        // Wait until coroutine in remindersListViewModel.loadReminders() complete
        advanceUntilIdle()

        // Then assert that the progress indicator is hidden.
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadRemindersWhenRemindersAreUnavailable_callErrorToDisplay() {
        // Make the repository return errors.
        remindersLocalRepository.setReturnError(true)

        remindersListViewModel.loadReminders()

        // Then empty and error are true (which triggers an error message to be shown).
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
    }

    @Test
    fun loadReminders_getSuccessfully() = runTest {
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
        remindersLocalRepository.saveReminder(reminder1.toReminderDTO())
        remindersLocalRepository.saveReminder(reminder2.toReminderDTO())

        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is`(2))
    }

}