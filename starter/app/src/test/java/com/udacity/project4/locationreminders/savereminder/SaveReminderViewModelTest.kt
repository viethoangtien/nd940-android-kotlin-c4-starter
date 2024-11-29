package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.toReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    // Use a fake repository to be injected into the view model.
    private lateinit var remindersLocalRepository: FakeDataSource

    @Before
    fun setUp() {
        remindersLocalRepository = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(
            app = ApplicationProvider.getApplicationContext(),
            dataSource = remindersLocalRepository
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun saveReminders_saveSuccessfully() = runTest {
        saveReminderViewModel.reminderTitle.value = "TITLE1"
        saveReminderViewModel.reminderDescription.value = "DESCRIPTION1"
        saveReminderViewModel.reminderSelectedLocationStr.value = "Pham Van Bach"
        saveReminderViewModel.latitude.value = 21.026988562137088
        saveReminderViewModel.longitude.value = 105.78886761961745

        // Main dispatcher will not run coroutines eagerly for this test
        Dispatchers.setMain(StandardTestDispatcher())

        saveReminderViewModel.validateAndSaveReminder()

        // Then assert that the progress indicator is shown.
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutine actions
        // Wait until coroutine in saveReminder complete
        advanceUntilIdle()

        // Then empty and error are true (which triggers an error message to be shown).
        assertThat(saveReminderViewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(saveReminderViewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
    }

    @Test
    fun validateEnteredData_titleIsNullOrEmpty() = runTest {
        saveReminderViewModel.validateAndSaveReminder()

        assertThat(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }


}