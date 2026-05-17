package fr.olegueyan.algomix.ui.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.databinding.FragmentHomeBinding
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MoveKeyboardCategory
import fr.olegueyan.algomix.ui.state.SharedCubeUiState
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private lateinit var sharedCubeViewModel: SharedCubeViewModel
    private var renderedKeyboardCategory: MoveKeyboardCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity() as MainActivity
        sharedCubeViewModel = activity.sharedCubeViewModel
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
        bindActions()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedCubeViewModel.uiState.collect { state ->
                    render(state)
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
        renderedKeyboardCategory = null
        binding = null
        super.onDestroyView()
    }

    private fun bindActions() {
        val currentBinding = binding ?: return
        currentBinding.modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sharedCubeViewModel.setHomeMode(checkedId.toHomeMode())
            }
        }
        currentBinding.keyboardCategoryToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                sharedCubeViewModel.setKeyboardCategory(checkedId.toKeyboardCategory())
            }
        }
        currentBinding.scanButton.setOnClickListener { sharedCubeViewModel.requestScan() }
        currentBinding.scrambleButton.setOnClickListener { sharedCubeViewModel.scramble() }
        currentBinding.loadAlgorithmButton.setOnClickListener { sharedCubeViewModel.requestLoadAlgorithm() }
        currentBinding.loadScrambleButton.setOnClickListener { sharedCubeViewModel.requestLoadScramble() }
        currentBinding.playPreviousButton.setOnClickListener { sharedCubeViewModel.playPrevious() }
        currentBinding.playNextButton.setOnClickListener { sharedCubeViewModel.playNext() }
        currentBinding.playSpeedButton.setOnClickListener { sharedCubeViewModel.cyclePlaybackSpeed() }
        currentBinding.playAutoButton.setOnClickListener { sharedCubeViewModel.toggleAutoPlay() }
        currentBinding.playLoopButton.setOnClickListener { sharedCubeViewModel.toggleLoop() }
        currentBinding.playResetButton.setOnClickListener { sharedCubeViewModel.resetPlayback() }
        currentBinding.editSaveButton.setOnClickListener { sharedCubeViewModel.requestSaveEditing() }
        currentBinding.editUndoButton.setOnClickListener { sharedCubeViewModel.undoEditing() }
        currentBinding.editRedoButton.setOnClickListener { sharedCubeViewModel.redoEditing() }
        currentBinding.editSuppressButton.setOnClickListener { sharedCubeViewModel.suppressLastEditingMove() }
        currentBinding.editDeleteAllButton.setOnClickListener { confirmDeleteAllEditing() }
    }

    private fun render(state: SharedCubeUiState) {
        val currentBinding = binding ?: return
        currentBinding.cubeView.renderCube(state.cubeState)
        currentBinding.modeToggleGroup.checkIfNeeded(state.homeMode.toButtonId())

        val keyboardVisible = state.homeMode == HomeMode.FREE || state.homeMode == HomeMode.EDIT
        val playVisible = state.homeMode == HomeMode.PLAY || state.homeMode == HomeMode.EDIT
        currentBinding.quickActionsGroup.visibility = state.homeMode.quickActionsVisibility()
        currentBinding.keyboardPanel.visibility = keyboardVisible.toVisibility()
        currentBinding.playPanel.visibility = playVisible.toVisibility()
        currentBinding.editActionsPanel.visibility = (state.homeMode == HomeMode.EDIT).toVisibility()

        currentBinding.feedbackText.text = state.homeUiState.feedbackMessage.orEmpty()
        currentBinding.feedbackText.visibility = (state.homeUiState.feedbackMessage != null).toVisibility()

        currentBinding.keyboardCategoryToggleGroup.checkIfNeeded(
            state.homeUiState.keyboardCategory.toButtonId(),
        )
        renderMoveKeyboard(state.homeUiState.keyboardCategory)
        renderSequence(state)
        renderPlaybackControls(state)
    }

    private fun renderMoveKeyboard(category: MoveKeyboardCategory) {
        if (renderedKeyboardCategory == category) {
            return
        }
        val currentBinding = binding ?: return
        renderedKeyboardCategory = category
        currentBinding.keyboardTitle.text = category.label
        currentBinding.moveGrid.removeAllViews()
        category.moves.forEach { token ->
            currentBinding.moveGrid.addView(createMoveButton(token))
        }
    }

    private fun createMoveButton(token: String): MaterialButton {
        val button = MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle,
        )
        button.text = token
        button.gravity = Gravity.CENTER
        button.isAllCaps = false
        button.setOnClickListener { sharedCubeViewModel.applyMoveToken(token) }
        button.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(MOVE_BUTTON_MARGIN, MOVE_BUTTON_MARGIN, MOVE_BUTTON_MARGIN, MOVE_BUTTON_MARGIN)
        }
        return button
    }

    private fun renderSequence(state: SharedCubeUiState) {
        val currentBinding = binding ?: return
        val notation = when (state.homeMode) {
            HomeMode.EDIT -> state.editingSession.sequence.normalizedNotation
            HomeMode.PLAY -> state.playbackState.sequence.normalizedNotation
            else -> state.homeUiState.freeSequenceNotation
        }
        currentBinding.sequenceText.text = notation.ifBlank {
            getString(R.string.home_empty_sequence)
        }
    }

    private fun renderPlaybackControls(state: SharedCubeUiState) {
        val currentBinding = binding ?: return
        val total = state.playbackState.sequence.moves.size
        val index = state.playbackState.currentIndex.coerceIn(0, total)
        currentBinding.progressLabel.text = getString(R.string.home_progress_format, index, total)
        currentBinding.playbackProgress.max = total.coerceAtLeast(1)
        currentBinding.playbackProgress.progress = index
        currentBinding.playSpeedButton.text = getString(
            R.string.home_speed_format,
            state.playbackState.speedMultiplier.toSpeedLabel(),
        )
        currentBinding.playAutoButton.text = state.homeUiState.autoPlayEnabled.toToggleText(
            getString(R.string.home_auto),
        )
        currentBinding.playLoopButton.text = state.playbackState.loop.toToggleText(
            getString(R.string.home_loop),
        )
    }

    private fun confirmDeleteAllEditing() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.home_delete_all_title)
            .setMessage(R.string.home_delete_all_message)
            .setNegativeButton(R.string.home_delete_all_cancel, null)
            .setPositiveButton(R.string.home_delete_all_confirm) { _, _ ->
                sharedCubeViewModel.deleteAllEditing()
            }
            .show()
    }

    private fun Int.toHomeMode(): HomeMode =
        when (this) {
            R.id.modeFreeButton -> HomeMode.FREE
            R.id.modePlayButton -> HomeMode.PLAY
            R.id.modeEditButton -> HomeMode.EDIT
            else -> HomeMode.VISUALIZATION
        }

    private fun HomeMode.toButtonId(): Int =
        when (this) {
            HomeMode.VISUALIZATION -> R.id.modeVisualizationButton
            HomeMode.FREE -> R.id.modeFreeButton
            HomeMode.PLAY -> R.id.modePlayButton
            HomeMode.EDIT -> R.id.modeEditButton
        }

    private fun Int.toKeyboardCategory(): MoveKeyboardCategory =
        when (this) {
            R.id.categoryCubeRotationsButton -> MoveKeyboardCategory.CUBE_ROTATIONS
            R.id.categorySliceMovesButton -> MoveKeyboardCategory.SLICE_MOVES
            R.id.categoryWideMovesButton -> MoveKeyboardCategory.WIDE_MOVES
            else -> MoveKeyboardCategory.FACE_TURNS
        }

    private fun MoveKeyboardCategory.toButtonId(): Int =
        when (this) {
            MoveKeyboardCategory.CUBE_ROTATIONS -> R.id.categoryCubeRotationsButton
            MoveKeyboardCategory.FACE_TURNS -> R.id.categoryFaceTurnsButton
            MoveKeyboardCategory.SLICE_MOVES -> R.id.categorySliceMovesButton
            MoveKeyboardCategory.WIDE_MOVES -> R.id.categoryWideMovesButton
        }

    private fun HomeMode.quickActionsVisibility(): Int =
        (this == HomeMode.VISUALIZATION || this == HomeMode.FREE).toVisibility()

    private fun Boolean.toVisibility(): Int =
        if (this) View.VISIBLE else View.GONE

    private fun Float.toSpeedLabel(): String =
        when (this) {
            1f -> "1"
            1.5f -> "1.5"
            2f -> "2"
            else -> toString()
        }

    private fun Boolean.toToggleText(label: String): String =
        if (this) {
            getString(R.string.home_toggle_on_format, label)
        } else {
            getString(R.string.home_toggle_off_format, label)
        }

    private fun com.google.android.material.button.MaterialButtonToggleGroup.checkIfNeeded(
        buttonId: Int,
    ) {
        if (checkedButtonId != buttonId) {
            check(buttonId)
        }
    }

    companion object {
        private const val MOVE_BUTTON_MARGIN = 4
    }
}
