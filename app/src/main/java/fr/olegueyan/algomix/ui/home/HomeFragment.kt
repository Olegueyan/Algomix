package fr.olegueyan.algomix.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.olegueyan.algomix.databinding.FragmentHomeBinding
import fr.olegueyan.algomix.ui.viewmodel.HomeViewModel
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private lateinit var sharedCubeViewModel: SharedCubeViewModel

    @Suppress("unused")
    private lateinit var homeViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity() as MainActivity
        sharedCubeViewModel = activity.sharedCubeViewModel
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val fragmentBinding = FragmentHomeBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedCubeViewModel.uiState.collect { state ->
                    binding?.cubeView?.renderCube(state.cubeState)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.cubeView?.onResume()
    }

    override fun onPause() {
        binding?.cubeView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
