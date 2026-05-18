package fr.olegueyan.algomix.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.databinding.FragmentTimerBinding
import fr.olegueyan.algomix.ui.home.AppContainerOwner
import fr.olegueyan.algomix.ui.home.SingleViewModelFactory
import fr.olegueyan.algomix.ui.state.TimerDisplayEntry
import fr.olegueyan.algomix.ui.state.TimerRunState
import fr.olegueyan.algomix.ui.state.TimerUiState
import fr.olegueyan.algomix.ui.viewmodel.TimerViewModel
import kotlinx.coroutines.launch

class TimerFragment : Fragment() {
    private var binding: FragmentTimerBinding? = null
    private lateinit var viewModel: TimerViewModel

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
    }

    override fun onDestroyView() {
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
        currentBinding.timerFeedback.text = state.feedbackMessage.orEmpty()
        currentBinding.timerFeedback.visibility = (state.feedbackMessage != null).toVisibility()
        currentBinding.timerFeedback.setTextColor(if (state.isError) ERROR_COLOR else INFO_COLOR)
        renderHistory(state.history)
    }

    private fun renderHistory(entries: List<TimerDisplayEntry>) {
        val currentBinding = binding ?: return
        currentBinding.historyContainer.removeAllViews()
        if (entries.isEmpty()) {
            currentBinding.historyContainer.addView(bodyText(getString(R.string.timer_history_empty)))
            return
        }
        entries.forEach { entry ->
            currentBinding.historyContainer.addView(historyRow(entry))
        }
    }

    private fun historyRow(entry: TimerDisplayEntry): View =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(HISTORY_ROW_PADDING, HISTORY_ROW_PADDING, HISTORY_ROW_PADDING, HISTORY_ROW_PADDING)
            setBackgroundColor(HISTORY_ROW_COLOR)
            addView(titleText(entry.durationLabel))
            addView(bodyText(entry.solvedAtLabel))
        }

    private fun titleText(text: String): TextView =
        TextView(requireContext()).apply {
            this.text = text
            textSize = TITLE_TEXT_SIZE
            setTextColor(TITLE_COLOR)
        }

    private fun bodyText(text: String): TextView =
        TextView(requireContext()).apply {
            this.text = text
            textSize = BODY_TEXT_SIZE
            setTextColor(BODY_COLOR)
        }

    private fun Boolean.toVisibility(): Int =
        if (this) View.VISIBLE else View.GONE

    companion object {
        private const val HISTORY_ROW_PADDING = 10
        private const val TITLE_TEXT_SIZE = 18f
        private const val BODY_TEXT_SIZE = 13f
        private const val TITLE_COLOR = 0xFF003A5D.toInt()
        private const val BODY_COLOR = 0xFF4D5B75.toInt()
        private const val INFO_COLOR = 0xFF8A3B00.toInt()
        private const val ERROR_COLOR = 0xFFB00020.toInt()
        private const val HISTORY_ROW_COLOR = 0xFFF7FBFF.toInt()
    }
}
