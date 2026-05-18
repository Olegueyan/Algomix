package fr.olegueyan.algomix.ui.home

import android.os.Looper
import android.view.View
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.library.LibraryFragment
import fr.olegueyan.algomix.ui.settings.SettingsFragment
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.timer.TimerFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
class MainActivityNavigationTest {
    @Test
    fun displaysHomeAtLaunch() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.supportFragmentManager.executePendingTransactions()

        assertTrue(activity.currentFragment() is HomeFragment)
        assertEquals(R.id.navigation_home, activity.bottomNavigation().selectedItemId)
    }

    @Test
    fun bottomNavigationSwitchesBetweenPlaceholderSections() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        activity.bottomNavigation().selectedItemId = R.id.navigation_library
        activity.supportFragmentManager.executePendingTransactions()
        assertTrue(activity.currentFragment() is LibraryFragment)
        assertEquals(MainRoute.LIBRARY, activity.sharedCubeViewModel.uiState.value.activeRoute)

        activity.bottomNavigation().selectedItemId = R.id.navigation_timer
        activity.supportFragmentManager.executePendingTransactions()
        assertTrue(activity.currentFragment() is TimerFragment)

        activity.bottomNavigation().selectedItemId = R.id.navigation_settings
        activity.supportFragmentManager.executePendingTransactions()
        assertTrue(activity.currentFragment() is SettingsFragment)
    }

    @Test
    fun libraryDisplaysOverview() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        activity.bottomNavigation().selectedItemId = R.id.navigation_library
        activity.supportFragmentManager.executePendingTransactions()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            activity.getString(R.string.library_title),
            activity.findViewById<TextView>(R.id.libraryTitle).text,
        )
    }

    @Test
    fun timerDisplaysRealLayoutAndStartButtonChangesState() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        activity.bottomNavigation().selectedItemId = R.id.navigation_timer
        activity.supportFragmentManager.executePendingTransactions()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(activity.getString(R.string.timer_title), activity.findViewById<TextView>(R.id.timerTitle).text)

        activity.findViewById<MaterialButton>(R.id.startPauseButton).performClick()

        assertEquals(
            activity.getString(R.string.timer_pause),
            activity.findViewById<MaterialButton>(R.id.startPauseButton).text,
        )
        activity.findViewById<MaterialButton>(R.id.startPauseButton).performClick()
    }

    @Test
    fun settingsDisplaysRealLayoutAndCloudFeedback() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        activity.bottomNavigation().selectedItemId = R.id.navigation_settings
        activity.supportFragmentManager.executePendingTransactions()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            activity.getString(R.string.settings_title),
            activity.findViewById<TextView>(R.id.settingsTitle).text,
        )

        activity.findViewById<MaterialButton>(R.id.recoverCloudButton).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertEquals(
            "Connexion cloud requise",
            activity.findViewById<TextView>(R.id.settingsFeedback).text,
        )
    }

    @Test
    fun settingsDialogsOpenFromCloudActions() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        activity.bottomNavigation().selectedItemId = R.id.navigation_settings
        activity.supportFragmentManager.executePendingTransactions()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        activity.findViewById<MaterialButton>(R.id.signInButton).performClick()
        assertTrue(ShadowDialog.getLatestDialog().isShowing)
        ShadowDialog.getLatestDialog().dismiss()

        activity.findViewById<MaterialButton>(R.id.createAccountButton).performClick()
        assertTrue(ShadowDialog.getLatestDialog().isShowing)
    }

    @Test
    fun returningHomeKeepsActivityScopedSharedViewModel() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val sharedViewModel = activity.sharedCubeViewModel
        val initialCube = sharedViewModel.uiState.value.cubeState

        activity.bottomNavigation().selectedItemId = R.id.navigation_library
        activity.supportFragmentManager.executePendingTransactions()
        activity.bottomNavigation().selectedItemId = R.id.navigation_home
        activity.supportFragmentManager.executePendingTransactions()

        assertTrue(activity.currentFragment() is HomeFragment)
        assertSame(sharedViewModel, activity.sharedCubeViewModel)
        assertEquals(initialCube, activity.sharedCubeViewModel.uiState.value.cubeState)
    }

    @Test
    fun homeStartsInVisualizationMode() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()

        activity.supportFragmentManager.executePendingTransactions()

        assertEquals(HomeMode.VISUALIZATION, activity.sharedCubeViewModel.uiState.value.homeMode)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.quickActionsGroup).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.keyboardPanel).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.playPanel).visibility)
    }

    @Test
    fun homeModeButtonsSwitchVisiblePanels() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        activity.supportFragmentManager.executePendingTransactions()

        activity.findViewById<MaterialButton>(R.id.modeFreeButton).performClick()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(HomeMode.FREE, activity.sharedCubeViewModel.uiState.value.homeMode)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.keyboardPanel).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.playPanel).visibility)

        activity.findViewById<MaterialButton>(R.id.modePlayButton).performClick()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.playPanel).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.keyboardPanel).visibility)

        activity.findViewById<MaterialButton>(R.id.modeEditButton).performClick()
        org.robolectric.Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.playPanel).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.keyboardPanel).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.editActionsPanel).visibility)
    }

    private fun MainActivity.currentFragment() =
        supportFragmentManager.findFragmentById(R.id.mainFragmentContainer)

    private fun MainActivity.bottomNavigation(): BottomNavigationView =
        findViewById(R.id.bottomNavigation)
}
