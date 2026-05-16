package fr.olegueyan.algomix.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.olegueyan.algomix.databinding.FragmentPlaceholderBinding
import fr.olegueyan.algomix.ui.home.AppContainerOwner
import fr.olegueyan.algomix.ui.home.SingleViewModelFactory
import fr.olegueyan.algomix.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch

class LibraryFragment : Fragment() {
    private var binding: FragmentPlaceholderBinding? = null
    private lateinit var viewModel: LibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (requireActivity() as AppContainerOwner)
            .appContainer
            .libraryRepository()
            .getOrNull()
            ?: error("LibraryRepository is not configured")
        viewModel = ViewModelProvider(
            this,
            SingleViewModelFactory { LibraryViewModel(repository) },
        )[LibraryViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val fragmentBinding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding?.placeholderTitle?.setText(state.titleResId)
                    binding?.placeholderBody?.setText(state.bodyResId)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
