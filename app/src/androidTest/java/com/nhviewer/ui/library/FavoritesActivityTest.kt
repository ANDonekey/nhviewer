package com.nhviewer.ui.library

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.nhviewer.R
import org.junit.Test

class FavoritesActivityTest {

    @Test
    fun launch_showsFavoritesScreenContainer() {
        ActivityScenario.launch(FavoritesActivity::class.java)

        onView(withId(R.id.titleView)).check(matches(isDisplayed()))
        onView(withId(R.id.recyclerView)).check(matches(isDisplayed()))
    }
}
