package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.CardType
import com.example.data.NfcCard
import com.example.data.TagCategories
import com.example.ui.components.DigitalCardItem
import com.example.ui.theme.MyApplicationTheme
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
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleCard = NfcCard(
      uidHex = "04:A1:B2:C3:D4:E5:80",
      name = "Tech Hub Master Key",
      category = TagCategories.DEFAULT_CATEGORY,
      description = "Server room and executive office",
      facilityName = "Apex Tower",
      cardType = CardType.ACCESS_BADGE,
      facilityCode = "104",
      cardNumber = "88421"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DigitalCardItem(card = sampleCard, onClick = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
