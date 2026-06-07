package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.state.AppViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun testViewModelInitialization() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(context, kotlinx.coroutines.GlobalScope)
    val repository = AppRepository(database)
    val factory = AppViewModel.Factory(context, repository)
    val viewModel = factory.create(AppViewModel::class.java)
    assertNotNull(viewModel)
  }
}
