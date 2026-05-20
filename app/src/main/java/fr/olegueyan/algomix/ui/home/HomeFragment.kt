package fr.olegueyan.algomix.ui.home

import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.port.LibraryRepository
import fr.olegueyan.algomix.databinding.FragmentHomeBinding
import fr.olegueyan.algomix.domain.cube.MoveParser
import fr.olegueyan.algomix.domain.library.AlgorithmEntry
import fr.olegueyan.algomix.domain.library.AlgorithmId
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.library.Scramble
import fr.olegueyan.algomix.domain.library.ScrambleId
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.ui.components.common.MoveIconRegistry
import fr.olegueyan.algomix.ui.scan.ScanDialogFragment
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
    private var renderedKeyboardCategory: MoveKeyboardCategory? = null
    private var moveLabelColor = 0xFF212121.toInt()
    private val toastHandler = Handler(Looper.getMainLooper())
    private var currentToastView: View? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            openScanDialog()
        } else {
            showToast(getString(R.string.scan_permission_denied), isError = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity() as MainActivity
        sharedCubeViewModel = activity.sharedCubeViewModel
        libraryRepository = activity.appContainer.libraryRepository().getOrNull()
            ?: error("LibraryRepository is not configured")
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
        binding?.cubeView?.keepScreenOn = true
        bindActions()
        bindStaticIcons()
        binding?.cubeView?.onDoubleTapResetListener = {
            binding?.cubeView?.resetRotation(sharedCubeViewModel.computeResetTargetQuaternion())
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedCubeViewModel.uiState.collect { state ->
                    render(state)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedCubeViewModel.animationEvents.collect { event ->
                    binding?.cubeView?.playMove(event.move, event.finalState)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                (requireActivity() as MainActivity).settingsViewModel.uiState.collect { state ->
                    applyHomeColors(state.preferences.appAppearance)
                    val background = state.preferences.appAppearance.backgroundColor()
                    binding?.cubeView?.appearance =
                        CubeThemeAppearanceMapper.map(state.preferences.cubeTheme, background)
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
        dismissCurrentToast()
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
        currentBinding.scanButton.setOnClickListener { requestCameraPermissionAndOpenScan() }
        currentBinding.scrambleButton.setOnClickListener { sharedCubeViewModel.scramble() }
        currentBinding.loadAlgorithmButton.setOnClickListener { showLoadAlgorithmDialog() }
        currentBinding.loadScrambleButton.setOnClickListener { showLoadScrambleDialog() }
        currentBinding.playPreviousButton.setOnClickListener { sharedCubeViewModel.playPrevious() }
        currentBinding.playNextButton.setOnClickListener { sharedCubeViewModel.playNext() }
        currentBinding.playSpeedButton.setOnClickListener { sharedCubeViewModel.cyclePlaybackSpeed() }
        currentBinding.playAutoButton.setOnClickListener { sharedCubeViewModel.toggleAutoPlay() }
        currentBinding.playLoopButton.setOnClickListener { sharedCubeViewModel.toggleLoop() }
        currentBinding.playResetButton.setOnClickListener { sharedCubeViewModel.resetPlayback() }
        currentBinding.lockRotationButton.setOnClickListener { sharedCubeViewModel.toggleRotationLock() }
        currentBinding.resetCubeButton.setOnClickListener { confirmResetCube() }
        currentBinding.editSaveButton.setOnClickListener { showSaveEditingDialog() }
        currentBinding.editUndoButton.setOnClickListener { sharedCubeViewModel.undoEditing() }
        currentBinding.editRedoButton.setOnClickListener { sharedCubeViewModel.redoEditing() }
        currentBinding.editSuppressButton.setOnClickListener { sharedCubeViewModel.suppressLastEditingMove() }
        currentBinding.editDeleteAllButton.setOnClickListener { confirmDeleteAllEditing() }
    }

    private fun bindStaticIcons() {
        val currentBinding = binding ?: return
        currentBinding.modeFreeButton.setIconResource(R.drawable.ic_nav_home)
        currentBinding.modePlayButton.setIconResource(R.drawable.ic_nav_timer)
        currentBinding.modeEditButton.setIconResource(R.drawable.ic_nav_settings)
        currentBinding.scanButton.setIconResource(android.R.drawable.ic_menu_camera)
        currentBinding.scrambleButton.setIconResource(android.R.drawable.ic_popup_sync)
        currentBinding.loadAlgorithmButton.setIconResource(android.R.drawable.ic_menu_upload)
        currentBinding.loadScrambleButton.setIconResource(android.R.drawable.ic_menu_upload)
        currentBinding.playPreviousButton.setIconResource(android.R.drawable.ic_media_previous)
        currentBinding.playNextButton.setIconResource(android.R.drawable.ic_media_next)
        currentBinding.playSpeedButton.setIconResource(android.R.drawable.ic_menu_manage)
        currentBinding.playAutoButton.setIconResource(android.R.drawable.ic_media_play)
        currentBinding.playLoopButton.setIconResource(android.R.drawable.ic_popup_sync)
        currentBinding.playResetButton.setIconResource(android.R.drawable.ic_menu_revert)
        currentBinding.editSaveButton.setIconResource(android.R.drawable.ic_menu_save)
        currentBinding.editUndoButton.setIconResource(android.R.drawable.ic_menu_revert)
        currentBinding.editRedoButton.setIconResource(android.R.drawable.ic_menu_rotate)
        currentBinding.editSuppressButton.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
        currentBinding.editDeleteAllButton.setIconResource(android.R.drawable.ic_menu_delete)
    }

    private fun requestCameraPermissionAndOpenScan() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            openScanDialog()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun openScanDialog() {
        if (parentFragmentManager.findFragmentByTag(ScanDialogFragment.TAG) == null) {
            ScanDialogFragment().show(parentFragmentManager, ScanDialogFragment.TAG)
        }
    }

    private fun render(state: SharedCubeUiState) {
        val currentBinding = binding ?: return
        currentBinding.cubeView.setRotationLocked(state.rotationLocked)
        currentBinding.cubeView.renderCube(state.cubeState)
        currentBinding.lockRotationButton.setIconResource(
            if (state.rotationLocked) R.drawable.ic_cube_lock else R.drawable.ic_cube_lock_open,
        )
        currentBinding.lockRotationButton.contentDescription = getString(
            if (state.rotationLocked) R.string.home_unlock_rotation else R.string.home_lock_rotation,
        )
        currentBinding.modeToggleGroup.checkIfNeeded(state.homeMode.toButtonId())

        val keyboardVisible = state.homeMode == HomeMode.FREE || state.homeMode == HomeMode.EDIT
        val playVisible = state.homeMode == HomeMode.PLAY || state.homeMode == HomeMode.EDIT
        currentBinding.quickActionsGroup.visibility = state.homeMode.quickActionsVisibility()
        currentBinding.keyboardPanel.visibility = keyboardVisible.toVisibility()
        currentBinding.playPanel.visibility = playVisible.toVisibility()
        currentBinding.editActionsPanel.visibility = (state.homeMode == HomeMode.EDIT).toVisibility()

        val feedbackMessage = state.homeUiState.feedbackMessage
        if (feedbackMessage != null) {
            showToast(feedbackMessage, isError = state.homeUiState.feedbackIsError)
            sharedCubeViewModel.consumeFeedback()
        }

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
        currentBinding.moveGrid.removeAllViews()
        category.moves.forEach { token ->
            currentBinding.moveGrid.addView(createMoveButton(token))
        }
    }

    private fun createMoveButton(token: String): View {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val outer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_move_button)
            val pad = (6 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            contentDescription = getString(R.string.accessibility_move_button_format, token)
            setOnClickListener { sharedCubeViewModel.applyMoveToken(token) }
        }
        val iconSize = resources.getDimensionPixelSize(R.dimen.home_move_icon_size)
        val icon = ImageView(ctx).apply {
            setImageResource(MoveIconRegistry.iconForNotation(token))
            imageTintList = null
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        outer.addView(icon, LinearLayout.LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        val label = TextView(ctx).apply {
            text = token
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(moveLabelColor)
            isAllCaps = false
        }
        outer.addView(label, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = (2 * dp).toInt() })
        outer.layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(MOVE_BUTTON_MARGIN, MOVE_BUTTON_MARGIN, MOVE_BUTTON_MARGIN, MOVE_BUTTON_MARGIN)
        }
        return outer
    }

    private fun confirmResetCube() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.home_reset_cube_title)
            .setMessage(R.string.home_reset_cube_message)
            .setNegativeButton(R.string.home_reset_cube_cancel, null)
            .setPositiveButton(R.string.home_reset_cube_confirm) { _, _ ->
                binding?.cubeView?.resetRotation(sharedCubeViewModel.computeResetTargetQuaternion())
                sharedCubeViewModel.resetCurrentCubeToSolved()
            }
            .show()
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
                showToast("Aucun algorithme disponible", isError = false)
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
                        showToast("Sélection requise", isError = false)
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
                showToast("Aucun mélange disponible", isError = false)
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
                        showToast("Sélection requise", isError = false)
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
            showToast("Séquence invalide", isError = true)
            return
        }
        if (sharedCubeViewModel.uiState.value.homeMode == HomeMode.EDIT) {
            sharedCubeViewModel.loadEditingSequence(parsedSequence)
        } else {
            sharedCubeViewModel.loadPlaybackSequence(parsedSequence)
            sharedCubeViewModel.setHomeMode(HomeMode.PLAY)
        }
        sharedCubeViewModel.showFeedback("$name chargé")
    }

    private fun showSaveEditingDialog() {
        val sequence = sharedCubeViewModel.uiState.value.editingSession.sequence.normalizedNotation
        if (sequence.isBlank()) {
            showToast("Séquence vide", isError = false)
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
                setText("Édition ${System.currentTimeMillis()}")
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
                    saveScramble(collection, "Édition ${System.currentTimeMillis()}", sequence)
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
                showToast("Algorithme sauvegardé")
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
        showToast("Mélange sauvegardé")
    }

    private fun showToast(message: String, isError: Boolean = false) {
        val root = binding?.root as? ViewGroup ?: return
        dismissCurrentToast()
        val bgColor = if (isError) 0xFFC62828.toInt() else 0xFF2E7D32.toInt()
        val toastView = TextView(requireContext()).apply {
            text = message
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(bgColor)
            val pad = (12 * resources.displayMetrics.density).toInt()
            val padH = (16 * resources.displayMetrics.density).toInt()
            setPadding(padH, pad, padH, pad)
            textSize = 14f
            elevation = 8f * resources.displayMetrics.density
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.TOP
            val margin = (16 * resources.displayMetrics.density).toInt()
            setMargins(margin, margin, margin, 0)
        }
        root.addView(toastView, params)
        currentToastView = toastView
        toastView.alpha = 0f
        ObjectAnimator.ofFloat(toastView, "alpha", 0f, 1f).apply {
            duration = 200
            start()
        }
        toastHandler.postDelayed({
            if (currentToastView === toastView) {
                ObjectAnimator.ofFloat(toastView, "alpha", 1f, 0f).apply {
                    duration = 300
                    start()
                }
                toastHandler.postDelayed({ root.removeView(toastView) }, 300)
                currentToastView = null
            }
        }, TOAST_DURATION_MS)
    }

    private fun dismissCurrentToast() {
        toastHandler.removeCallbacksAndMessages(null)
        currentToastView?.let { (binding?.root as? ViewGroup)?.removeView(it) }
        currentToastView = null
    }

    private fun applyHomeColors(appearance: AppAppearance) {
        val currentBinding = binding ?: return
        val background = appearance.backgroundColor()
        val surface = if (appearance == AppAppearance.DARK) DARK_SURFACE else LIGHT_SURFACE
        val inputSurface = if (appearance == AppAppearance.DARK) DARK_INPUT_SURFACE else LIGHT_INPUT_SURFACE
        val muted = if (appearance == AppAppearance.DARK) DARK_MUTED else LIGHT_MUTED
        val title = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_ORANGE
        val newLabelColor = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_BODY
        if (newLabelColor != moveLabelColor) {
            moveLabelColor = newLabelColor
            renderedKeyboardCategory = null
        }
        currentBinding.root.setBackgroundColor(background)
        currentBinding.cubeStage.setBackgroundColor(background)
        currentBinding.playPanel.setBackgroundColor(surface)
        currentBinding.sequenceText.setBackgroundColor(inputSurface)
        currentBinding.appTitle.setTextColor(title)
        currentBinding.sequenceText.setTextColor(if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_TITLE)
        currentBinding.progressLabel.setTextColor(muted)
    }

    private fun Int.toHomeMode(): HomeMode =
        when (this) {
            R.id.modePlayButton -> HomeMode.PLAY
            R.id.modeEditButton -> HomeMode.EDIT
            else -> HomeMode.FREE
        }

    private fun HomeMode.toButtonId(): Int =
        when (this) {
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
        (this == HomeMode.FREE).toVisibility()

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

    private fun AppAppearance.backgroundColor(): Int =
        if (this == AppAppearance.DARK) DARK_BACKGROUND else LIGHT_BACKGROUND

    private fun com.google.android.material.button.MaterialButtonToggleGroup.checkIfNeeded(
        buttonId: Int,
    ) {
        if (checkedButtonId != buttonId) {
            check(buttonId)
        }
    }

    companion object {
        private const val MOVE_BUTTON_MARGIN = 3
        private const val NO_SELECTION = -1
        private const val TOAST_DURATION_MS = 2500L
        private const val LIGHT_BACKGROUND = 0xFFF4F1EA.toInt()
        private const val LIGHT_SURFACE = 0xFFFDF8F0.toInt()
        private const val LIGHT_INPUT_SURFACE = 0xFFF7FBFF.toInt()
        private const val LIGHT_TITLE = 0xFF1A1A1A.toInt()
        private const val LIGHT_BODY = 0xFF212121.toInt()
        private const val LIGHT_ORANGE = 0xFFE65100.toInt()
        private const val LIGHT_MUTED = 0xFF4D5B75.toInt()
        private const val DARK_BACKGROUND = 0xFF0D1117.toInt()
        private const val DARK_SURFACE = 0xFF161B22.toInt()
        private const val DARK_INPUT_SURFACE = 0xFF21262D.toInt()
        private const val DARK_TEXT = 0xFFE6EDF3.toInt()
        private const val DARK_MUTED = 0xFF8B949E.toInt()
    }

    private fun newId(): String = UUID.randomUUID().toString()
}
