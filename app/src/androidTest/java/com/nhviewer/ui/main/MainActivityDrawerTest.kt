package com.nhviewer.ui.main

import androidx.core.view.GravityCompat
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.init
import androidx.test.espresso.intent.Intents.release
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nhviewer.R
import com.nhviewer.ui.search.SearchActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityDrawerTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun setupIntents() {
        init()
    }

    @After
    fun tearDownIntents() {
        release()
    }

    @Test
    fun homeScreenContainer_isVisible_onLaunch() {
        onView(withId(R.id.drawerLayout)).check(matches(isDisplayed()))
        onView(withId(R.id.menuButton)).check(matches(isDisplayed()))
    }

    @Test
    fun drawer_openAndClickHome_keepsHomeVisible() {
        onView(withId(R.id.drawerLayout)).check(matches(isClosed(GravityCompat.START)))

        onView(withId(R.id.drawerLayout)).perform(open())
        onView(withId(R.id.drawerLayout)).check(matches(isOpen(GravityCompat.START)))

        onView(withId(R.id.drawerHomeButton)).perform(click())
        onView(withId(R.id.drawerLayout)).check(matches(isClosed(GravityCompat.START)))
        onView(withId(R.id.menuButton)).check(matches(isDisplayed()))
    }

    @Test
    fun drawer_openAndClickPopular_keepsHomeVisible() {
        onView(withId(R.id.drawerLayout)).perform(open())
        onView(withId(R.id.drawerLayout)).check(matches(isOpen(GravityCompat.START)))

        onView(withId(R.id.drawerPopularButton)).perform(click())

        onView(withId(R.id.drawerLayout)).check(matches(isClosed(GravityCompat.START)))
        onView(withId(R.id.menuButton)).check(matches(isDisplayed()))
    }

    @Test
    fun drawer_openAndClickSearch_launchesSearchActivity() {
        onView(withId(R.id.drawerLayout)).perform(open())
        onView(withId(R.id.drawerLayout)).check(matches(isOpen(GravityCompat.START)))

        onView(withId(R.id.drawerSearchButton)).perform(click())

        intended(hasComponent(SearchActivity::class.java.name))
    }
}
