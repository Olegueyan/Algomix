package fr.olegueyan.algomix.ui.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.application.port.SettingsRepository
import fr.olegueyan.algomix.databinding.FragmentHomeBinding
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.library.AlgorithmEntry
import fr.olegueyan.algomix.domain.library.AlgorithmId
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.library.Scramble
import fr.olegueyan.algomix.domain.library.ScrambleId
import fr.olegueyan.algomix.ui.settings.CubeThemeAppearanceMapper
import fr.olegueyan.algomix.ui.state.HomeMode
import fr.olegueyan.algomix.ui.state.MoveKeyboardCategory
import fr.olegueyan.algomix.ui.state.SharedCubeUiState
import fr.olegueyan.algomix.ui.viewmodel.SharedCubeViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Suppress("TooManyFunctions")
class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private lateinit var sharedCubeViewModel: SharedCubeViewModel
    private lateinit var libraryRepository: LibraryRepository
    private lateinit var settingsRepository: SettingsRepository
    private var renderedKeyboardCategory: MoveKeyboardCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity() as MainActivity
        sharedCubeViewModel = activity.sharedCubeViewModel
        libraryRepository = activity.appContainer.libraryRepository().getOrNull()
            ?: error("LibraryRepository is not configured")
        settingsRepository = activity.appContainer.settingsRepository().getOrNull()
            ?: error("SettingsRepository is not configured")
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
        applyCubeTheme()
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
        currentBinding.loadAlgorithmButton.setOnClickListener { showLoadAlgorithmDialog() }
        currentBinding.loadScrambleButton.setOnClickListener { showLoadScrambleDialog() }
        currentBinding.playPreviousButton.setOnClickListener { sharedCubeViewModel.playPrevious() }
        currentBinding.playNextButton.setOnClickListener { sharedCubeViewModel.playNext() }
        currentBinding.playSpeedButton.setOnClickListener { sharedCubeViewModel.cyclePlaybackSpeed() }
        currentBinding.playAutoButton.setOnClickListener { sharedCubeViewModel.toggleAutoPlay() }
        currentBinding.playLoopButton.setOnClickListener { sharedCubeViewModel.toggleLoop() }
        currentBinding.playResetButton.setOnClickListener { sharedCubeViewModel.resetPlayback() }
        currentBinding.editSaveButton.setOnClickListener { showSaveEditingDialog() }
        currentBinding.editUndoButton.setOnClickListener { sharedCubeViewModel.undoEditing() }
        currentBinding.editRedoButton.setOnClickListener { sharedCubeViewModel.redoEditing() }
        currentBinding.editSuppressButton.setOnClickListener { sharedCubeViewModel.suppressLastEditingMove() }
        currentBinding.editDeleteAllButton.setOnClickListener { confirmDeleteAllEditing() }
    }

    private fun applyCubeTheme() {
        viewLifecycleOwner.lifecycleScope.launch {
            val preferences = settingsRepository.loadPreferences().getOrNull() ?: return@launch
            binding?.cubeView?.appearance = CubeThemeAppearanceMapper.map(preferences.cubeTheme)
        }
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

    private fun showLoadAlgorithmDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sheets = libraryRepository.listSheets().getOrNull().orEmpty()
            val algorithms = sheets.flatMap { sheet ->
                libraryRepository.listAlgorithms(sheet.id).getOrNull().orEmpty()
            }
            if (algorithms.isEmpty()) {
                sharedCubeViewModel.showFeedback("Aucun algorithme disponible")
                return@launch
            }
            val labels = algorithms.map { "${it.name} - ${it.sequence}" }.toTypedArray()
            var selectedIndex = NO_SELECTION
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.library_load_title)
                .setSingleChoiceItems(labels, NO_SELECTION) { _, which -> selectedIndex = which }
                .setNegativeButton(R.string.library_cancel, null)
                .setPositiveButton(R.string.home_load_algorithm) { _, _ ->
                    val algorithm = algorithms.getOrNull(selectedIndex)
                    if (algorithm == null) {
                        sharedCubeViewModel.showFeedback("Selection requise")
                    } else {
                        loadSequence(algorithm.name, algorithm.sequence)
                    }
                }
                .show()
        }
    }

    private fun showLoadScrambleDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val scrambles = libraryRepository.listScrambles().getOrNull().orEmpty()
            if (scrambles.isEmpty()) {
                sharedCubeViewModel.showFeedback("Aucun melange disponible")
                return@launch
            }
            val labels = scrambles.map { "${it.name} - ${it.sequence}" }.toTypedArray()
            var selectedIndex = NO_SELECTION
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.library_load_title)
                .setSingleChoiceItems(labels, NO_SELECTION) { _, which -> selectedIndex = which }
                .setNegativeButton(R.string.library_cancel, null)
                .setPositiveButton(R.string.home_load_scramble) { _, _ ->
                    val scramble = scrambles.getOrNull(selectedIndex)
                    if (scramble == null) {
                        sharedCubeViewModel.showFeedback("Selection requise")
                    } else {
                        loadSequence(scramble.name, scramble.sequence)
                    }
                }
                .show()
        }
    }

    private fun loadSequence(name: String, sequence: String) {
        val parsedSequence = try {
            MoveParser.parse(sequence)
        } catch (_: IllegalArgumentException) {
            sharedCubeViewModel.showFeedback("Sequence invalide")
            return
        }
        sharedCubeViewModel.loadPlaybackSequence(parsedSequence)
        sharedCubeViewModel.setHomeMode(HomeMode.PLAY)
        sharedCubeViewModel.showFeedback("$name charge")
    }

    private fun showSaveEditingDialog() {
        val sequence = sharedCubeViewModel.uiState.value.editingSession.sequence.normalizedNotation
        if (sequence.isBlank()) {
            sharedCubeViewModel.showFeedback("Sequence vide")
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val collections = libraryRepository.listCollections().getOrNull().orEmpty()
            if (collections.isEmpty()) {
                showCreateCollectionForSaveDialog(sequence)
                return@launch
            }
            val sheets = libraryRepository.listSheets().getOrNull().orEmpty()
            val choices = (sheets.map { "Fiche: ${it.name}" } + "Nouveau melange").toTypedArray()
            var selectedIndex = if (choices.isNotEmpty()) 0 else NO_SELECTION
            val nameInput = EditText(requireContext()).apply {
                hint = getString(R.string.library_name_hint)
                setText("Edition ${System.currentTimeMillis()}")
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.library_save_edit_title)
                .setView(nameInput)
                .setSingleChoiceItems(choices, selectedIndex) { _, which -> selectedIndex = which }
                .setNegativeButton(R.string.library_cancel, null)
                .setPositiveButton(R.string.library_save) { _, _ ->
                    saveEditingSelection(sequence, nameInput.text.toString(), collections, sheets, selectedIndex)
                }
                .show()
        }
    }

    private fun showCreateCollectionForSaveDialog(sequence: String) {
        val nameInput = EditText(requireContext()).apply { hint = getString(R.string.library_collection_hint) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_create_collection)
            .setView(nameInput)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_create) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val collection = LibraryCollection(
                        id = fr.olegueyan.algomix.domain.library.CollectionId(newId()),
                        name = nameInput.text.toString(),
                    )
                    libraryRepository.saveCollection(collection)
                    saveScramble(collection, "Edition ${System.currentTimeMillis()}", sequence)
                }
            }
            .show()
    }

    private fun saveEditingSelection(
        sequence: String,
        name: String,
        collections: List<LibraryCollection>,
        sheets: List<fr.olegueyan.algomix.domain.library.AlgorithmSheet>,
        selectedIndex: Int,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val selectedSheet = sheets.getOrNull(selectedIndex)
            if (selectedSheet != null) {
                val position = libraryRepository.listAlgorithms(selectedSheet.id).getOrNull().orEmpty().size
                libraryRepository.saveAlgorithm(
                    AlgorithmEntry(
                        id = AlgorithmId(newId()),
                        sheetId = selectedSheet.id,
                        name = name,
                        sequence = sequence,
                        position = position,
                    ),
                )
                sharedCubeViewModel.showFeedback("Algorithme sauvegarde")
            } else {
                saveScramble(collections.first(), name, sequence)
            }
        }
    }

    private suspend fun saveScramble(collection: LibraryCollection, name: String, sequence: String) {
        libraryRepository.saveScramble(
            Scramble(
                id = ScrambleId(newId()),
                collectionId = collection.id,
                name = name,
                sequence = sequence,
            ),
        )
        sharedCubeViewModel.showFeedback("Melange sauvegarde")
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
        private const val NO_SELECTION = -1
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
