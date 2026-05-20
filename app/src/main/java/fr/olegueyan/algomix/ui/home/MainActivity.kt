package fr.olegueyan.algomix.ui.home

import android.content.res.ColorStateList
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
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.di.AppContainer
import fr.olegueyan.algomix.databinding.ActivityMainBinding
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.infrastructure.di.AndroidAppContainerFactory
import fr.olegueyan.algomix.ui.library.LibraryFragment
import fr.olegueyan.algomix.ui.settings.SettingsFragment
import fr.olegueyan.algomix.ui.state.MainRoute
import fr.olegueyan.algomix.ui.timer.TimerFragment
import fr.olegueyan.algomix.ui.theme.AlgomixPalettes
import fr.olegueyan.algomix.ui.viewmodel.SettingsViewModel
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import kotlinx.coroutines.launch

/** Hosts the app shell and owns the activity-scoped shared cube state. */
class MainActivity : FragmentActivity(), AppContainerOwner {
    private lateinit var binding: ActivityMainBinding
    private var renderingRoute = false

    override val appContainer: AppContainer by lazy {
        AndroidAppContainerFactory.create(this)
    }

    val sharedCubeViewModel: SharedCubeViewModel by lazy {
        val repository = appContainer.cubeSessionRepository().getOrNull()
            ?: error("CubeSessionRepository is not configured")
        val settingsRepository = appContainer.settingsRepository().getOrNull()
            ?: error("SettingsRepository is not configured")
        ViewModelProvider(
            this,
            SharedCubeViewModel.Factory(repository, settingsRepository, appContainer.clockProvider),
        )[SharedCubeViewModel::class.java]
    }

    val settingsViewModel: SettingsViewModel by lazy {
        val settingsRepository = appContainer.settingsRepository().getOrNull()
            ?: error("SettingsRepository is not configured")
        val cubeSessionRepository = appContainer.cubeSessionRepository().getOrNull()
            ?: error("CubeSessionRepository is not configured")
        ViewModelProvider(
            this,
            SingleViewModelFactory {
                SettingsViewModel(
                    settingsRepository = settingsRepository,
                    cubeSessionRepository = cubeSessionRepository,
                    cloudAuthGateway = appContainer.cloudAuthGateway().getOrNull(),
                    cloudSyncGateway = appContainer.cloudSyncGateway().getOrNull(),
                )
            },
        )[SettingsViewModel::class.java]
    }

    /** Initializes the edge-to-edge shell and bottom navigation. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.paddingLeft,
                binding.bottomNavigation.paddingTop,
                binding.bottomNavigation.paddingRight,
                systemBars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }

        binding.mainPager.adapter = MainPagerAdapter(this)
        binding.mainPager.offscreenPageLimit = MainRoute.entries.size
        binding.mainPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (!renderingRoute) {
                        sharedCubeViewModel.setRoute(MainRoute.entries[position])
                    }
                }
            },
        )
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val route = item.itemId.toMainRoute()
            sharedCubeViewModel.setRoute(route)
            true
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedCubeViewModel.uiState.collect { state ->
                    renderRoute(state.activeRoute)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.uiState.collect { state ->
                    applyShellColors(state.preferences.appAppearance)
                }
            }
        }

        if (savedInstanceState == null) {
            renderRoute(sharedCubeViewModel.uiState.value.activeRoute)
        }
    }

    private fun renderRoute(route: MainRoute) {
        renderingRoute = true
        try {
            if (binding.mainPager.currentItem != route.ordinal) {
                binding.mainPager.setCurrentItem(route.ordinal, true)
            }
            if (binding.bottomNavigation.selectedItemId != route.toMenuItemId()) {
                binding.bottomNavigation.selectedItemId = route.toMenuItemId()
            }
        } finally {
            renderingRoute = false
        }
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

    private class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = MainRoute.entries.size

        override fun createFragment(position: Int): Fragment =
            when (MainRoute.entries[position]) {
                MainRoute.HOME -> HomeFragment()
                MainRoute.LIBRARY -> LibraryFragment()
                MainRoute.TIMER -> TimerFragment()
                MainRoute.SETTINGS -> SettingsFragment()
            }
    }

    private fun applyShellColors(appearance: AppAppearance) {
        val palette = AlgomixPalettes.from(appearance)
        binding.main.setBackgroundColor(palette.background)
        binding.bottomNavigation.setBackgroundColor(palette.surface)
        val itemColors = ColorStateList.valueOf(palette.accent)
        binding.bottomNavigation.itemIconTintList = itemColors
        binding.bottomNavigation.itemTextColor = itemColors
    }
}
