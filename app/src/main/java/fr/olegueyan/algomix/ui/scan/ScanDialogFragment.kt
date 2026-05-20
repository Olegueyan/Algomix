package fr.olegueyan.algomix.ui.scan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.application.core.AppResult
import fr.olegueyan.algomix.application.port.CubeScanner
import fr.olegueyan.algomix.databinding.FragmentScanDialogBinding
import fr.olegueyan.algomix.domain.cube.CubeState
import fr.olegueyan.algomix.domain.cube.FaceColor
import fr.olegueyan.algomix.domain.scan.ScanFaceDraft
import fr.olegueyan.algomix.domain.scan.ScanFaceletAssembler
import fr.olegueyan.algomix.domain.scan.ScanSessionDraft
import fr.olegueyan.algomix.infrastructure.scan.CameraXCubeScanner
import fr.olegueyan.algomix.ui.home.MainActivity
import kotlinx.coroutines.launch

class ScanDialogFragment : DialogFragment() {
    private var binding: FragmentScanDialogBinding? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var scanner: CubeScanner
    private lateinit var cameraScanner: CameraXCubeScanner
    private var session = ScanSessionDraft()
    private var currentDraft: ScanFaceDraft? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = requireActivity() as MainActivity
        val scannerResult = activity.appContainer.cubeScanner()
        scanner = scannerResult.getOrNull() ?: error("CubeScanner is not configured")
        cameraScanner = scanner as? CameraXCubeScanner ?: error("Camera scanner is not configured")
        cameraScanner.cubeTheme = activity.settingsViewModel.uiState.value.preferences.cubeTheme
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val fragmentBinding = FragmentScanDialogBinding.inflate(inflater, container, false)
        binding = fragmentBinding
        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindActions()
        render()
        viewLifecycleOwner.lifecycleScope.launch {
            scanner.startSession().fold(
                onSuccess = { startedSession -> session = startedSession },
                onFailure = { error -> showFeedback(error.message) },
            )
            render()
        }
        startCamera()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onDestroyView() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        binding = null
        super.onDestroyView()
    }

    private fun bindActions() {
        val currentBinding = binding ?: return
        currentBinding.scanCloseButton.setOnClickListener { dismiss() }
        currentBinding.scanCaptureButton.setOnClickListener { captureCurrentFace() }
        currentBinding.scanRecaptureButton.setOnClickListener {
            currentDraft = null
            showFeedback(null)
            render()
        }
        currentBinding.scanValidateButton.setOnClickListener { validateCurrentStep() }
    }

    private fun startCamera() {
        val context = context ?: return
        val currentBinding = binding ?: return
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder().build().also { previewUseCase ->
                        previewUseCase.setSurfaceProvider(currentBinding.scanPreview.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                                proxy.close()
                            }
                        }
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: RuntimeException) {
                    showFeedback(getString(R.string.scan_camera_unavailable))
                } catch (_: IllegalStateException) {
                    showFeedback(getString(R.string.scan_camera_unavailable))
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    private fun captureCurrentFace() {
        val currentBinding = binding ?: return
        val bitmap = currentBinding.scanPreview.bitmap
        if (bitmap == null) {
            showFeedback(getString(R.string.scan_preview_unavailable))
            return
        }
        currentDraft = cameraScanner.extractFaceFromBitmap(bitmap, session.currentFaceIndex)
        render()
    }

    private fun validateCurrentStep() {
        val draft = currentDraft
        if (draft == null) {
            showFeedback(getString(R.string.scan_capture_required))
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = scanner.saveFace(session, draft)) {
                is AppResult.Success -> {
                    session = result.value
                    currentDraft = null
                    if (session.isComplete) {
                        applyCompletedScan()
                    } else {
                        showFeedback(null)
                        render()
                    }
                }
                is AppResult.Failure -> showFeedback(result.error.message)
            }
        }
    }

    private suspend fun applyCompletedScan() {
        when (val validation = scanner.validateSession(session)) {
            is AppResult.Success -> {
                val faceletCube = ScanFaceletAssembler.assemble(session).faceletCube
                if (faceletCube == null) {
                    showFeedback(getString(R.string.scan_incomplete))
                    return
                }
                (requireActivity() as MainActivity).sharedCubeViewModel.applyScannedCube(
                    CubeState.fromFaceletCube(faceletCube),
                )
                dismiss()
            }
            is AppResult.Failure -> {
                session = session.copy(validationErrors = listOf(validation.error.message))
                showFeedback(validation.error.message)
                render()
            }
        }
    }

    private fun render() {
        val currentBinding = binding ?: return
        val displayFaceIndex = session.completedFaceCount.coerceAtMost(FACE_COUNT - 1)
        currentBinding.scanProgressText.text = getString(
            R.string.scan_progress_format,
            displayFaceIndex + 1,
            session.currentFace.name,
        )
        renderStickerPreview(currentDraft ?: session.face(session.currentFaceIndex))
        currentBinding.scanCubePreview.render(session, currentDraft)
    }

    private fun renderStickerPreview(faceDraft: ScanFaceDraft?) {
        val currentBinding = binding ?: return
        currentBinding.scanStickerPreview.removeAllViews()
        repeat(ScanFaceDraft.STICKER_COUNT) { index ->
            val color = faceDraft?.stickers?.getOrNull(index)
            currentBinding.scanStickerPreview.addView(createStickerView(index, color))
        }
    }

    private fun createStickerView(index: Int, color: FaceColor?): View {
        val size = resources.getDimensionPixelSize(R.dimen.scan_sticker_size)
        return View(requireContext()).apply {
            setBackgroundColor(color?.toAndroidColor() ?: EMPTY_STICKER_COLOR)
            contentDescription = getString(R.string.accessibility_scan_sticker_format, index + 1)
            setOnClickListener {
                val draft = currentDraft ?: return@setOnClickListener
                showColorPicker(index, draft)
            }
            layoutParams = GridLayout.LayoutParams().apply {
                width = size
                height = size
                setMargins(STICKER_MARGIN, STICKER_MARGIN, STICKER_MARGIN, STICKER_MARGIN)
            }
        }
    }

    private fun showColorPicker(index: Int, draft: ScanFaceDraft) {
        val labels = FaceColor.entries.map { color -> color.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.scan_color_picker_title)
            .setItems(labels) { _, selectedIndex ->
                val nextStickers = draft.stickers.toMutableList()
                nextStickers[index] = FaceColor.entries[selectedIndex]
                currentDraft = draft.copy(stickers = nextStickers)
                render()
            }
            .show()
    }

    private fun showFeedback(message: String?) {
        if (message != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun FaceColor.toAndroidColor(): Int =
        android.graphics.Color.rgb(
            (r * RGB_MAX).toInt().coerceIn(0, RGB_MAX),
            (g * RGB_MAX).toInt().coerceIn(0, RGB_MAX),
            (b * RGB_MAX).toInt().coerceIn(0, RGB_MAX),
        )

    companion object {
        const val TAG = "ScanDialogFragment"
        private const val FACE_COUNT = 6
        private const val RGB_MAX = 255
        private const val EMPTY_STICKER_COLOR = 0xFF2A2F38.toInt()
        private const val STICKER_MARGIN = 4
    }
}
