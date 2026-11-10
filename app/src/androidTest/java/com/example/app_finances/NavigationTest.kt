package com.example.app_finances

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun string(id: Int) = compose.activity.getString(id)

    @Test
    fun abreNoInicioComSaldo() {
        compose.onNodeWithText(string(R.string.home_total_balance)).assertIsDisplayed()
    }

    @Test
    fun navegaEntreAsAbas() {
        compose.onAllNodesWithText(string(R.string.nav_cards)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.cards_empty_title)).assertIsDisplayed()

        compose.onAllNodesWithText(string(R.string.nav_more)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.more_recurring)).assertIsDisplayed()

        compose.onAllNodesWithText(string(R.string.nav_transactions)).onFirst().performClick()
        compose.onNodeWithText(string(R.string.transactions_empty_title)).assertIsDisplayed()
    }
}
