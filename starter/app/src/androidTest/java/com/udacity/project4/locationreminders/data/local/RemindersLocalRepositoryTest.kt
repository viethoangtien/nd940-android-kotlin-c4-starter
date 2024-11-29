package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.isError
import com.udacity.project4.locationreminders.data.dto.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var remindersLocalRepository: RemindersLocalRepository
    private lateinit var database: RemindersDatabase

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // Using an in-memory database for testing, because it doesn't survive killing the process.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        remindersLocalRepository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveReminder_retrievesReminder() = runTest {
        // GIVEN - A new reminder saved in the database.
        val newReminder = ReminderDTO(
            title = "FHN",
            description = "My Working Location",
            location = "Pham Van Bach",
            latitude = 21.026988562137088,
            longitude = 105.78886761961745
        )
        remindersLocalRepository.saveReminder(newReminder)

        // WHEN  - Reminder retrieved by ID.
        val result = remindersLocalRepository.getReminder(newReminder.id)

        // THEN - Same reminder is returned.
        assertThat(result.isSuccess, Matchers.`is`(true))
        result as Result.Success
        assertThat(result.data.title, Matchers.`is`("FHN"))
        assertThat(result.data.description, Matchers.`is`("My Working Location"))
        assertThat(result.data.location, Matchers.`is`("Pham Van Bach"))
        assertThat(result.data.latitude, Matchers.`is`(21.026988562137088))
        assertThat(result.data.longitude, Matchers.`is`(105.78886761961745))
    }

    @Test
    fun retrievesReminder_notFoundResult() = runTest {
        // WHEN  - Reminder retrieved by ID.
        val result = remindersLocalRepository.getReminder(UUID.randomUUID().toString())

        // THEN - Same reminder is returned.
        assertThat(result.isError, Matchers.`is`(true))
    }

}