package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.MainAppContainer
import com.example.ui.state.AppViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testMainAppContainerRendering() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    val database = AppDatabase.getDatabase(context, kotlinx.coroutines.GlobalScope)
    val repository = AppRepository(database)
    val factory = AppViewModel.Factory(context, repository)
    val viewModel = factory.create(AppViewModel::class.java)

    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
        MainAppContainer(viewModel = viewModel)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/main_app_container.png")
  }
}
