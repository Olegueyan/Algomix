package fr.olegueyan.algomix.ui.home

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.di.AppContainer
import fr.olegueyan.algomix.databinding.ActivityMainBinding
import fr.olegueyan.algomix.infrastructure.di.AndroidAppContainerFactory
import fr.olegueyan.algomix.ui.library.LibraryFragment
import fr.olegueyan.algomix.ui.settings.SettingsFragment
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.timer.TimerFragment
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import kotlinx.coroutines.launch

/** Hosts the app shell and owns the activity-scoped shared cube state. */
class MainActivity : FragmentActivity(), AppContainerOwner {
    private lateinit var binding: ActivityMainBinding
    private var renderedRoute: MainRoute? = null

    override val appContainer: AppContainer by lazy {
        AndroidAppContainerFactory.create(this)
    }

    val sharedCubeViewModel: SharedCubeViewModel by lazy {
        val repository = appContainer.cubeSessionRepository().getOrNull()
            ?: error("CubeSessionRepository is not configured")
        ViewModelProvider(
            this,
            SharedCubeViewModel.Factory(repository, appContainer.clockProvider),
        )[SharedCubeViewModel::class.java]
    }

    /** Initializes the edge-to-edge shell and bottom navigation. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            sharedCubeViewModel.setRoute(item.itemId.toMainRoute())
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedCubeViewModel.uiState.collect { state ->
                    renderRoute(state.activeRoute)
                    binding.bottomNavigation.selectedItemId = state.activeRoute.toMenuItemId()
                }
            }
        }

        if (savedInstanceState == null) {
            renderRoute(sharedCubeViewModel.uiState.value.activeRoute)
        }
    }

    private fun renderRoute(route: MainRoute) {
        if (renderedRoute == route) {
            return
        }
        renderedRoute = route
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFragmentContainer, route.createFragment())
            .commit()
    }

    private fun Int.toMainRoute(): MainRoute =
        when (this) {
            R.id.navigation_library -> MainRoute.LIBRARY
            R.id.navigation_timer -> MainRoute.TIMER
            R.id.navigation_settings -> MainRoute.SETTINGS
            else -> MainRoute.HOME
        }

    private fun MainRoute.toMenuItemId(): Int =
        when (this) {
            MainRoute.HOME -> R.id.navigation_home
            MainRoute.LIBRARY -> R.id.navigation_library
            MainRoute.TIMER -> R.id.navigation_timer
            MainRoute.SETTINGS -> R.id.navigation_settings
        }

    private fun MainRoute.createFragment(): Fragment =
        when (this) {
            MainRoute.HOME -> HomeFragment()
            MainRoute.LIBRARY -> LibraryFragment()
            MainRoute.TIMER -> TimerFragment()
            MainRoute.SETTINGS -> SettingsFragment()
        }
}
