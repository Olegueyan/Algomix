package fr.olegueyan.algomix.ui.timer

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.databinding.FragmentTimerBinding
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.ui.home.AppContainerOwner
import fr.olegueyan.algomix.ui.home.MainActivity
import fr.olegueyan.algomix.ui.home.SingleViewModelFactory
import fr.olegueyan.algomix.ui.state.TimerDisplayEntry
import fr.olegueyan.algomix.ui.state.TimerRunState
import fr.olegueyan.algomix.ui.state.TimerUiState
import fr.olegueyan.algomix.ui.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

class TimerFragment : Fragment() {
    private var binding: FragmentTimerBinding? = null
    private lateinit var viewModel: TimerViewModel
    private var currentAppearance = AppAppearance.LIGHT
    private val toastHandler = Handler(Looper.getMainLooper())
    private var currentToastView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (requireActivity() as AppContainerOwner).appContainer
        val repository = container.timerRepository().getOrNull()
            ?: error("TimerRepository is not configured")
        viewModel = ViewModelProvider(
            this,
            SingleViewModelFactory {
                TimerViewModel(
                    timerRepository = repository,
                    clockProvider = container.clockProvider,
                )
            },
        )[TimerViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val fragmentBinding = FragmentTimerBinding.inflate(inflater, container, false)
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
                    applyTimerColors(state.preferences.appAppearance)
                }
            }
        }
    }

    override fun onDestroyView() {
        toastHandler.removeCallbacksAndMessages(null)
        currentToastView = null
        binding = null
        super.onDestroyView()
    }

    private fun bindActions() {
        val currentBinding = binding ?: return
        currentBinding.startPauseButton.setOnClickListener { viewModel.startOrPause() }
        currentBinding.resetButton.setOnClickListener { viewModel.reset() }
        currentBinding.saveTimeButton.setOnClickListener { viewModel.saveTime() }
    }

    private fun render(state: TimerUiState) {
        val currentBinding = binding ?: return
        currentBinding.timerDisplay.text = state.durationLabel
        currentBinding.startPauseButton.text = when (state.runState) {
            TimerRunState.IDLE -> getString(R.string.timer_start)
            TimerRunState.RUNNING -> getString(R.string.timer_pause)
            TimerRunState.PAUSED -> getString(R.string.timer_resume)
        }
        val feedbackMessage = state.feedbackMessage ?: getString(R.string.timer_loading).takeIf { state.isLoading }
        if (feedbackMessage != null) {
            showTimerToast(feedbackMessage, state.isError)
            viewModel.consumeFeedback()
        }
        renderHistory(state)
    }

    private fun renderHistory(state: TimerUiState) {
        val currentBinding = binding ?: return
        currentBinding.historyContainer.removeAllViews()
        if (state.isLoading) {
            currentBinding.historyContainer.addView(bodyText(getString(R.string.timer_loading)))
            return
        }
        if (state.history.isEmpty()) {
            currentBinding.historyContainer.addView(bodyText(getString(R.string.timer_history_empty)))
            return
        }
        state.history.forEach { entry ->
            currentBinding.historyContainer.addView(historyRow(entry))
        }
    }

    private fun historyRow(entry: TimerDisplayEntry): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val dp = resources.displayMetrics.density
            val pad = (10 * dp).toInt()
            val margin = (0 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            setBackgroundResource(R.drawable.bg_history_row)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = (4 * dp).toInt() }
            layoutParams = lp
            addView(titleText(entry.durationLabel))
            addView(bodyText(entry.solvedAtLabel))
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
            setTextColor(if (currentAppearance == AppAppearance.DARK) DARK_MUTED else LIGHT_MUTED)
        }

    private fun applyTimerColors(appearance: AppAppearance) {
        val currentBinding = binding ?: return
        currentAppearance = appearance
        val background = if (appearance == AppAppearance.DARK) DARK_BACKGROUND else LIGHT_BACKGROUND
        val titleColor = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_ORANGE
        val displayColor = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_TITLE
        val sectionTitle = if (appearance == AppAppearance.DARK) DARK_TEXT else 0xFF000000.toInt()
        currentBinding.timerRoot.setBackgroundColor(background)
        currentBinding.timerTitle.setTextColor(titleColor)
        currentBinding.timerDisplay.setTextColor(displayColor)
        currentBinding.historyTitle.setTextColor(sectionTitle)
    }

    private fun showTimerToast(message: String, isError: Boolean) {
        val rootView = (view as? FrameLayout) ?: return
        currentToastView?.let { rootView.removeView(it) }
        toastHandler.removeCallbacksAndMessages(null)

        val bgColor = if (isError) TOAST_ERROR else TOAST_SUCCESS
        val toast = TextView(requireContext()).apply {
            text = message
            setTextColor(Color.WHITE)
            setBackgroundColor(bgColor)
            val h = (20 * resources.displayMetrics.density).toInt()
            val v = (10 * resources.displayMetrics.density).toInt()
            setPadding(h, v, h, v)
            textSize = 13f
            alpha = 0f
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP,
        )
        rootView.addView(toast, params)
        currentToastView = toast

        ObjectAnimator.ofFloat(toast, "alpha", 0f, 1f).apply { duration = 200; start() }
        toastHandler.postDelayed({
            ObjectAnimator.ofFloat(toast, "alpha", 1f, 0f).apply { duration = 300; start() }
            toastHandler.postDelayed({
                (toast.parent as? FrameLayout)?.removeView(toast)
                if (currentToastView === toast) currentToastView = null
            }, 300)
        }, 2500)
    }

    private fun Boolean.toVisibility(): Int = if (this) View.VISIBLE else View.GONE

    companion object {
        private const val TITLE_TEXT_SIZE = 18f
        private const val BODY_TEXT_SIZE = 13f
        private const val LIGHT_BACKGROUND = 0xFFF4F1EA.toInt()
        private const val LIGHT_ORANGE = 0xFFE65100.toInt()
        private const val LIGHT_TITLE = 0xFF1A1A1A.toInt()
        private const val LIGHT_MUTED = 0xFF4D5B75.toInt()
        private const val DARK_BACKGROUND = 0xFF0D1117.toInt()
        private const val DARK_TEXT = 0xFFE6EDF3.toInt()
        private const val DARK_MUTED = 0xFF8B949E.toInt()
        private const val TOAST_SUCCESS = 0xFF2E7D32.toInt()
        private const val TOAST_ERROR = 0xFFC62828.toInt()
    }
}
