package fr.olegueyan.algomix.ui.library

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.port.PdfExporter
import fr.olegueyan.algomix.databinding.FragmentLibraryBinding
import fr.olegueyan.algomix.domain.library.AlgorithmEntry
import fr.olegueyan.algomix.domain.library.AlgorithmSheet
import fr.olegueyan.algomix.domain.library.LibraryCollection
import fr.olegueyan.algomix.domain.library.Scramble
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.ui.home.AppContainerOwner
import fr.olegueyan.algomix.ui.home.MainActivity
import fr.olegueyan.algomix.ui.home.SingleViewModelFactory
import fr.olegueyan.algomix.ui.state.LibraryCollectionSection
import fr.olegueyan.algomix.ui.state.LibraryItemType
import fr.olegueyan.algomix.ui.state.LibraryScreen
import fr.olegueyan.algomix.ui.state.LibraryUiState
import fr.olegueyan.algomix.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.launch
import java.io.File

@Suppress("TooManyFunctions")
class LibraryFragment : Fragment() {
    private var binding: FragmentLibraryBinding? = null
    private lateinit var viewModel: LibraryViewModel
    private lateinit var pdfExporter: PdfExporter
    private var currentAppearance = AppAppearance.LIGHT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (requireActivity() as AppContainerOwner).appContainer
        val repository = container.libraryRepository().getOrNull()
            ?: error("LibraryRepository is not configured")
        pdfExporter = container.pdfExporter().getOrNull()
            ?: error("PdfExporter is not configured")
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
        val fragmentBinding = FragmentLibraryBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindActions()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                (requireActivity() as MainActivity).settingsViewModel.uiState.collect { state ->
                    applyLibraryColors(state.preferences.appAppearance)
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun bindActions() {
        val currentBinding = binding ?: return
        currentBinding.searchInput.addTextChangedListener(
            SimpleTextWatcher { viewModel.setQuery(it.toString()) },
        )
        currentBinding.typeFilterGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.setItemType(checkedId.toItemType())
            }
        }
        currentBinding.createCollectionButton.setOnClickListener { showCreateCollectionDialog() }
        currentBinding.createSheetButton.setOnClickListener { showCreateSheetDialog() }
        currentBinding.createScrambleButton.setOnClickListener { viewModel.showScrambleCreate() }
        currentBinding.backToOverviewButton.setOnClickListener { viewModel.showOverview() }
        currentBinding.importAlgorithmButton.setOnClickListener { showImportAlgorithmDialog() }
        currentBinding.exportPdfButton.setOnClickListener { exportSelectedSheet() }
        currentBinding.sheetTagsButton.setOnClickListener { showSheetTagsDialog() }
        currentBinding.loadScrambleCodeButton.setOnClickListener { showScrambleCodeDialog() }
        currentBinding.generateScrambleButton.setOnClickListener { viewModel.generateDraftScramble() }
        currentBinding.saveScrambleButton.setOnClickListener { showSaveDraftScrambleDialog() }
    }

    private fun render(state: LibraryUiState) {
        val currentBinding = binding ?: return
        currentBinding.libraryTitle.text = when (state.screen) {
            LibraryScreen.SHEET_PREVIEW -> state.selectedSheet?.name ?: getString(R.string.library_title)
            LibraryScreen.SCRAMBLE_CREATE -> getString(R.string.library_create_scramble)
            LibraryScreen.OVERVIEW -> getString(R.string.library_title)
        }
        currentBinding.overviewActions.visibility = (state.screen == LibraryScreen.OVERVIEW).toVisibility()
        currentBinding.searchPanel.visibility = (state.screen == LibraryScreen.OVERVIEW).toVisibility()
        currentBinding.overviewContainer.visibility = (state.screen == LibraryScreen.OVERVIEW).toVisibility()
        currentBinding.sheetPanel.visibility = (state.screen == LibraryScreen.SHEET_PREVIEW).toVisibility()
        currentBinding.scrambleCreatePanel.visibility = (state.screen == LibraryScreen.SCRAMBLE_CREATE).toVisibility()
        currentBinding.typeFilterGroup.checkIfNeeded(state.filterState.itemType.toButtonId())
        val feedbackMessage = state.feedback?.message ?: getString(R.string.library_loading).takeIf { state.isLoading }
        currentBinding.libraryFeedback.text = feedbackMessage.orEmpty()
        currentBinding.libraryFeedback.visibility = (feedbackMessage != null).toVisibility()
        currentBinding.libraryFeedback.setTextColor(if (state.feedback?.isError == true) ERROR_COLOR else INFO_COLOR)
        currentBinding.scrambleCodeText.text = state.draftScrambleSequence.ifBlank {
            getString(R.string.library_empty)
        }
        renderTagFilters(state)
        renderOverview(state)
        renderSelectedSheet(state)
    }

    private fun renderTagFilters(state: LibraryUiState) {
        val currentBinding = binding ?: return
        currentBinding.tagFilterContainer.removeAllViews()
        state.tags.forEach { tag ->
            currentBinding.tagFilterContainer.addView(
                chipButton(
                    label = tag.name,
                    selected = tag.id in state.filterState.selectedTagIds,
                    onClick = { viewModel.toggleTagFilter(tag.id) },
                ),
            )
        }
    }

    private fun renderOverview(state: LibraryUiState) {
        val currentBinding = binding ?: return
        currentBinding.overviewContainer.removeAllViews()
        if (state.isLoading) {
            currentBinding.overviewContainer.addView(bodyText(getString(R.string.library_loading)))
            return
        }
        if (state.visibleSections.isEmpty()) {
            currentBinding.overviewContainer.addView(bodyText(getString(R.string.library_empty)))
            return
        }
        state.visibleSections.forEach { section ->
            currentBinding.overviewContainer.addView(collectionView(section))
        }
    }

    private fun collectionView(section: LibraryCollectionSection): View {
        val layout = verticalPanel()
        val collection = section.collection
        layout.addView(titleText(collection.name))
        layout.addView(
            bodyText("${section.sheets.size} fiches - ${section.scrambles.size} melanges"),
        )
        layout.addView(rowButton(R.string.library_rename) { showRenameCollectionDialog(collection) })
        layout.addView(
            rowButton(R.string.library_delete) {
                confirmDelete { viewModel.deleteCollection(collection.id) }
            },
        )
        section.sheets.forEach { sheet -> layout.addView(sheetRow(sheet)) }
        section.scrambles.forEach { scramble -> layout.addView(scrambleRow(scramble)) }
        return layout
    }

    private fun sheetRow(sheet: AlgorithmSheet): View {
        val layout = verticalPanel(compact = true)
        layout.addView(titleText("Fiche: ${sheet.name}"))
        layout.setOnClickListener { viewModel.showSheet(sheet.id) }
        layout.addView(rowButton(R.string.library_rename) { showRenameSheetDialog(sheet) })
        layout.addView(rowButton(R.string.library_tags) { showSheetTagsDialog(sheet) })
        layout.addView(rowButton(R.string.library_delete) { confirmDelete { viewModel.deleteSheet(sheet.id) } })
        return layout
    }

    private fun scrambleRow(scramble: Scramble): View {
        val layout = verticalPanel(compact = true)
        layout.addView(titleText("Mélange: ${scramble.name}"))
        layout.addView(bodyText(scramble.sequence))
        layout.addView(rowButton(R.string.library_rename) { showRenameScrambleDialog(scramble) })
        layout.addView(rowButton(R.string.library_tags) { showScrambleTagsDialog(scramble) })
        layout.addView(rowButton(R.string.library_delete) { confirmDelete { viewModel.deleteScramble(scramble.id) } })
        return layout
    }

    private fun renderSelectedSheet(state: LibraryUiState) {
        val currentBinding = binding ?: return
        currentBinding.algorithmContainer.removeAllViews()
        currentBinding.algorithmContainer.addView(titleText(getString(R.string.library_algorithms_title)))
        if (state.selectedSheetAlgorithms.isEmpty()) {
            currentBinding.algorithmContainer.addView(bodyText(getString(R.string.library_empty)))
            return
        }
        state.selectedSheetAlgorithms.forEach { algorithm ->
            currentBinding.algorithmContainer.addView(algorithmView(algorithm))
        }
    }

    private fun algorithmView(algorithm: AlgorithmEntry): View {
        val layout = verticalPanel(compact = true)
        layout.addView(titleText(algorithm.name))
        layout.addView(bodyText(algorithm.sequence))
        return layout
    }

    private fun showCreateCollectionDialog() {
        showNameDialog(
            titleResId = R.string.library_create_collection,
            positiveResId = R.string.library_create,
        ) { viewModel.createCollection(it) }
    }

    private fun showCreateSheetDialog() {
        val state = viewModel.uiState.value
        val collection = state.collections.firstOrNull()
        if (collection == null) {
            showCreateCollectionDialog()
            return
        }
        showNameDialog(R.string.library_create_sheet, R.string.library_create) {
            viewModel.createSheet(collection.id, it)
        }
    }

    private fun showRenameCollectionDialog(collection: LibraryCollection) {
        showNameDialog(R.string.library_rename, R.string.library_rename, collection.name) {
            viewModel.renameCollection(collection, it)
        }
    }

    private fun showRenameSheetDialog(sheet: AlgorithmSheet) {
        showNameDialog(R.string.library_rename, R.string.library_rename, sheet.name) {
            viewModel.renameSheet(sheet, it)
        }
    }

    private fun showRenameScrambleDialog(scramble: Scramble) {
        showNameDialog(R.string.library_rename, R.string.library_rename, scramble.name) {
            viewModel.renameScramble(scramble, it)
        }
    }

    private fun showImportAlgorithmDialog() {
        val sheetId = viewModel.uiState.value.selectedSheetId ?: return
        val nameInput = input(hintResId = R.string.library_name_hint)
        val sequenceInput = input(hintResId = R.string.library_sequence_hint)
        val container = dialogForm(nameInput, sequenceInput)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_import_title)
            .setView(container)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_save) { _, _ ->
                viewModel.importAlgorithm(sheetId, nameInput.text.toString(), sequenceInput.text.toString())
            }
            .show()
    }

    private fun showSheetTagsDialog(sheet: AlgorithmSheet? = viewModel.uiState.value.selectedSheet) {
        val target = sheet ?: return
        showTagsDialog { tags -> viewModel.setSheetTags(target.id, tags) }
    }

    private fun showScrambleTagsDialog(scramble: Scramble) {
        showTagsDialog { tags -> viewModel.setScrambleTags(scramble.id, tags) }
    }

    private fun showTagsDialog(onSave: (List<String>) -> Unit) {
        val input = input(hintResId = R.string.library_tags_hint)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_manage_tags_title)
            .setView(input)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_save) { _, _ ->
                onSave(input.text.toString().split(","))
            }
            .show()
    }

    private fun showScrambleCodeDialog() {
        val input = input(hintResId = R.string.library_sequence_hint)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_load_code)
            .setView(input)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_save) { _, _ ->
                viewModel.setDraftScrambleSequence(input.text.toString())
            }
            .show()
    }

    private fun showSaveDraftScrambleDialog() {
        val collection = viewModel.uiState.value.collections.firstOrNull() ?: return showCreateCollectionDialog()
        showNameDialog(R.string.library_save_scramble, R.string.library_save) {
            viewModel.saveDraftScramble(collection.id, it)
        }
    }

    private fun exportSelectedSheet() {
        val sheetId = viewModel.uiState.value.selectedSheetId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            pdfExporter.exportSheet(sheetId).fold(
                onSuccess = { sharePdf(File(it.localFilePath)) },
                onFailure = { /* ViewModel owns visible library feedback for data operations. */ },
            )
        }
    }

    private fun sharePdf(file: File) {
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.library_share_pdf_title)))
    }

    private fun showNameDialog(
        titleResId: Int,
        positiveResId: Int,
        initialValue: String = "",
        onSave: (String) -> Unit,
    ) {
        val input = input(hintResId = R.string.library_name_hint, value = initialValue)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleResId)
            .setView(input)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(positiveResId) { _, _ -> onSave(input.text.toString()) }
            .show()
    }

    private fun confirmDelete(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.library_delete_confirm_title)
            .setMessage(R.string.library_delete_confirm_message)
            .setNegativeButton(R.string.library_cancel, null)
            .setPositiveButton(R.string.library_delete) { _, _ -> onConfirm() }
            .show()
    }

    private fun input(hintResId: Int, value: String = ""): EditText =
        EditText(requireContext()).apply {
            hint = getString(hintResId)
            setText(value)
            minHeight = MIN_INPUT_HEIGHT
        }

    private fun dialogForm(vararg views: View): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(DIALOG_PADDING, 0, DIALOG_PADDING, 0)
            views.forEach { addView(it) }
        }

    private fun applyLibraryColors(appearance: AppAppearance) {
        val currentBinding = binding ?: return
        currentAppearance = appearance
        val background = if (appearance == AppAppearance.DARK) DARK_BACKGROUND else LIGHT_BACKGROUND
        val titleColor = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_ORANGE
        currentBinding.libraryRoot.setBackgroundColor(background)
        currentBinding.libraryTitle.setTextColor(titleColor)
    }

    private fun verticalPanel(compact: Boolean = false): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING)
            setBackgroundColor(
                if (currentAppearance == AppAppearance.DARK) {
                    if (compact) DARK_COMPACT_PANEL else DARK_PANEL
                } else {
                    if (compact) COMPACT_PANEL_COLOR else PANEL_COLOR
                },
            )
        }

    private fun titleText(text: String): TextView =
        TextView(requireContext()).apply {
            this.text = text
            textSize = TITLE_TEXT_SIZE
            setTextColor(if (currentAppearance == AppAppearance.DARK) DARK_TEXT else LIGHT_TITLE)
        }

    private fun bodyText(text: String): TextView =
        TextView(requireContext()).apply {
            this.text = text
            textSize = BODY_TEXT_SIZE
            setTextColor(if (currentAppearance == AppAppearance.DARK) DARK_MUTED else BODY_COLOR)
        }

    private fun rowButton(labelResId: Int, onClick: () -> Unit): MaterialButton =
        MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle,
        ).apply {
            text = getString(labelResId)
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun chipButton(label: String, selected: Boolean, onClick: () -> Unit): MaterialButton =
        MaterialButton(
            requireContext(),
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle,
        ).apply {
            text = label
            isAllCaps = false
            isSelected = selected
            setOnClickListener { onClick() }
        }

    private fun Int.toItemType(): LibraryItemType =
        when (this) {
            R.id.filterSheetsButton -> LibraryItemType.SHEETS
            R.id.filterScramblesButton -> LibraryItemType.SCRAMBLES
            else -> LibraryItemType.ALL
        }

    private fun LibraryItemType.toButtonId(): Int =
        when (this) {
            LibraryItemType.ALL -> R.id.filterAllButton
            LibraryItemType.SHEETS -> R.id.filterSheetsButton
            LibraryItemType.SCRAMBLES -> R.id.filterScramblesButton
        }

    private fun com.google.android.material.button.MaterialButtonToggleGroup.checkIfNeeded(buttonId: Int) {
        if (checkedButtonId != buttonId) {
            check(buttonId)
        }
    }

    private fun Boolean.toVisibility(): Int =
        if (this) View.VISIBLE else View.GONE

    private class SimpleTextWatcher(
        private val onChanged: (CharSequence) -> Unit,
    ) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s ?: "")
        }

        override fun afterTextChanged(s: Editable?) = Unit
    }

    companion object {
        private const val MIN_INPUT_HEIGHT = 48
        private const val DIALOG_PADDING = 24
        private const val PANEL_PADDING = 12
        private const val TITLE_TEXT_SIZE = 17f
        private const val BODY_TEXT_SIZE = 13f
        private const val LIGHT_BACKGROUND = 0xFFF4F1EA.toInt()
        private const val LIGHT_ORANGE = 0xFFE65100.toInt()
        private const val LIGHT_TITLE = 0xFF000000.toInt()
        private const val BODY_COLOR = 0xFF4D5B75.toInt()
        private const val INFO_COLOR = 0xFF8A3B00.toInt()
        private const val ERROR_COLOR = 0xFFB00020.toInt()
        private const val PANEL_COLOR = 0xFFFFFDF8.toInt()
        private const val COMPACT_PANEL_COLOR = 0xFFF7FBFF.toInt()
        private const val DARK_BACKGROUND = 0xFF0D1117.toInt()
        private const val DARK_PANEL = 0xFF161B22.toInt()
        private const val DARK_COMPACT_PANEL = 0xFF21262D.toInt()
        private const val DARK_TEXT = 0xFFE6EDF3.toInt()
        private const val DARK_MUTED = 0xFF8B949E.toInt()
    }
}
