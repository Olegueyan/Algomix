package fr.olegueyan.algomix.ui.home

import com.google.android.material.bottomnavigation.BottomNavigationView
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.ui.library.LibraryFragment
import fr.olegueyan.algomix.ui.settings.SettingsFragment
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.timer.TimerFragment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

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

    private fun MainActivity.currentFragment() =
        supportFragmentManager.findFragmentById(R.id.mainFragmentContainer)

    private fun MainActivity.bottomNavigation(): BottomNavigationView =
        findViewById(R.id.bottomNavigation)
}
