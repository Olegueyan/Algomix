package fr.olegueyan.algomix.ui.settings

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import fr.olegueyan.algomix.R
import fr.olegueyan.algomix.databinding.FragmentSettingsBinding
import fr.olegueyan.algomix.domain.settings.AppAppearance
import fr.olegueyan.algomix.domain.settings.CubeTheme
import fr.olegueyan.algomix.ui.home.MainActivity
import fr.olegueyan.algomix.ui.state.SettingsUiState
import fr.olegueyan.algomix.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Suppress("TooManyFunctions")
class SettingsFragment : Fragment() {
    private var binding: FragmentSettingsBinding? = null
    private val viewModel: SettingsViewModel get() = (requireActivity() as MainActivity).settingsViewModel
    private var renderingState = false
    private val toastHandler = Handler(Looper.getMainLooper())
    private var currentToastView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val fragmentBinding = FragmentSettingsBinding.inflate(inflater, container, false)
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
        toastHandler.removeCallbacksAndMessages(null)
        currentToastView = null
        binding = null
        super.onDestroyView()
    }

    private fun bindActions() {
        val currentBinding = binding ?: return
        currentBinding.appearanceToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !renderingState) {
                viewModel.setAppAppearance(checkedId.toAppAppearance())
            }
        }
        currentBinding.filledThemeButton.setOnClickListener { viewModel.setCubeTheme(CubeTheme.FILLED) }
        currentBinding.stickerThemeButton.setOnClickListener {
            viewModel.setCubeTheme(CubeTheme.STICKER_ON_BLACK)
        }
        currentBinding.carbonThemeButton.setOnClickListener { viewModel.setCubeTheme(CubeTheme.CARBON) }
        currentBinding.localCubeCacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!renderingState) viewModel.setLocalCubeCacheEnabled(isChecked)
        }
        currentBinding.sessionPersistenceSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!renderingState) viewModel.setSessionPersistenceEnabled(isChecked)
        }
        currentBinding.signInButton.setOnClickListener { showLoginDialog() }
        currentBinding.createAccountButton.setOnClickListener { showCreateAccountDialog() }
        currentBinding.signOutButton.setOnClickListener { viewModel.signOut() }
        currentBinding.syncCloudButton.setOnClickListener { viewModel.syncCloud() }
        currentBinding.purgeCloudButton.setOnClickListener { confirmPurgeCloud() }
        currentBinding.changePasswordButton.setOnClickListener { showChangePasswordDialog() }
    }

    private fun render(state: SettingsUiState) {
        val currentBinding = binding ?: return
        renderingState = true
        try {
            applySettingsColors(state.preferences.appAppearance)
            currentBinding.appearanceToggleGroup.checkIfNeeded(state.preferences.appAppearance.toButtonId())
            currentBinding.localCubeCacheSwitch.setCheckedIfNeeded(state.preferences.localCubeCacheEnabled)
            currentBinding.sessionPersistenceSwitch.setCheckedIfNeeded(state.preferences.sessionPersistenceEnabled)
            renderThemeButtons(state.preferences.cubeTheme)
            renderCloud(state)
        } finally {
            renderingState = false
        }
        val feedbackMessage = state.feedbackMessage
            ?: getString(R.string.settings_loading).takeIf { state.isLoading }
        if (feedbackMessage != null) {
            showSettingsToast(feedbackMessage, state.isError)
            viewModel.consumeFeedback()
        }
    }

    private fun renderThemeButtons(theme: CubeTheme) {
        val currentBinding = binding ?: return
        currentBinding.filledThemeButton.text = getString(
            if (theme == CubeTheme.FILLED) R.string.settings_theme_filled_active else R.string.settings_theme_filled,
        )
        currentBinding.stickerThemeButton.text = getString(
            if (theme == CubeTheme.STICKER_ON_BLACK) R.string.settings_theme_sticker_active else R.string.settings_theme_sticker,
        )
        currentBinding.carbonThemeButton.text = getString(
            if (theme == CubeTheme.CARBON) R.string.settings_theme_carbon_active else R.string.settings_theme_carbon,
        )
        currentBinding.filledThemeButton.isChecked = theme == CubeTheme.FILLED
        currentBinding.stickerThemeButton.isChecked = theme == CubeTheme.STICKER_ON_BLACK
        currentBinding.carbonThemeButton.isChecked = theme == CubeTheme.CARBON
    }

    private fun renderCloud(state: SettingsUiState) {
        val currentBinding = binding ?: return
        val session = state.cloudSession
        currentBinding.cloudStatus.text = if (session == null) {
            getString(R.string.settings_cloud_disconnected)
        } else {
            getString(R.string.settings_cloud_connected_format, session.user.email)
        }
        currentBinding.signInButton.visibility = (!state.isAuthenticated).toVisibility()
        currentBinding.createAccountButton.visibility = (!state.isAuthenticated).toVisibility()
        currentBinding.signOutButton.visibility = state.isAuthenticated.toVisibility()
        currentBinding.authenticatedCloudActions.visibility = state.isAuthenticated.toVisibility()
        currentBinding.syncProgressBar.visibility = state.isSyncing.toVisibility()
        currentBinding.syncProgressLabel.visibility = state.isSyncing.toVisibility()
        currentBinding.syncCloudButton.isEnabled = !state.isSyncing
        currentBinding.purgeCloudButton.isEnabled = !state.isSyncing
        currentBinding.profileName.text = if (session == null) {
            getString(R.string.settings_profile_disconnected)
        } else {
            getString(
                R.string.settings_profile_name_format,
                listOfNotNull(session.user.lastName, session.user.firstName).joinToString(" ").ifBlank { "-" },
            )
        }
        currentBinding.profileEmail.text = if (session == null) "" else {
            getString(R.string.settings_profile_email_format, session.user.email)
        }
    }

    private fun showSettingsToast(message: String, isError: Boolean) {
        val rootView = (view as? FrameLayout) ?: return
        currentToastView?.let { rootView.removeView(it) }
        toastHandler.removeCallbacksAndMessages(null)

        val background = if (isError) TOAST_ERROR else TOAST_SUCCESS
        val toast = TextView(requireContext()).apply {
            text = message
            setTextColor(Color.WHITE)
            setBackgroundColor(background)
            val h = resources.getDimensionPixelSize(R.dimen.settings_dialog_padding)
            val v = resources.getDimensionPixelSize(R.dimen.settings_dialog_spacing) * 2
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

        ObjectAnimator.ofFloat(toast, "alpha", 0f, 1f).apply {
            duration = 200
            start()
        }
        toastHandler.postDelayed({
            ObjectAnimator.ofFloat(toast, "alpha", 1f, 0f).apply {
                duration = 300
                start()
            }
            toastHandler.postDelayed({
                (toast.parent as? FrameLayout)?.removeView(toast)
                if (currentToastView === toast) currentToastView = null
            }, 300)
        }, 2500)
    }

    private fun showLoginDialog() {
        val emailInput = textInput(R.string.settings_email_hint, emailInputType())
        val passwordInput = textInput(R.string.settings_password_hint, passwordInputType())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_login_title)
            .setView(verticalInputs(emailInput, passwordInput))
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_sign_in) { _, _ ->
                viewModel.signIn(emailInput.text.toString(), passwordInput.text.toString())
            }
            .show()
    }

    private fun showCreateAccountDialog() {
        val lastNameInput = textInput(R.string.settings_last_name_hint)
        val firstNameInput = textInput(R.string.settings_first_name_hint)
        val emailInput = textInput(R.string.settings_email_hint, emailInputType())
        val passwordInput = textInput(R.string.settings_password_hint, passwordInputType())
        val confirmationInput = textInput(R.string.settings_confirm_password_hint, passwordInputType())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_create_account_title)
            .setView(verticalInputs(lastNameInput, firstNameInput, emailInput, passwordInput, confirmationInput))
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_create) { _, _ ->
                viewModel.createAccount(
                    lastName = lastNameInput.text.toString(),
                    firstName = firstNameInput.text.toString(),
                    email = emailInput.text.toString(),
                    password = passwordInput.text.toString(),
                    passwordConfirmation = confirmationInput.text.toString(),
                )
            }
            .show()
    }

    private fun showChangePasswordDialog() {
        val currentPasswordInput = textInput(R.string.settings_current_password_hint, passwordInputType())
        val newPasswordInput = textInput(R.string.settings_new_password_hint, passwordInputType())
        val confirmationInput = textInput(R.string.settings_confirm_password_hint, passwordInputType())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_change_password_title)
            .setView(verticalInputs(currentPasswordInput, newPasswordInput, confirmationInput))
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_update) { _, _ ->
                viewModel.changePassword(
                    currentPassword = currentPasswordInput.text.toString(),
                    newPassword = newPasswordInput.text.toString(),
                    confirmation = confirmationInput.text.toString(),
                )
            }
            .show()
    }

    private fun confirmPurgeCloud() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_purge_confirm_title)
            .setMessage(R.string.settings_purge_confirm_message)
            .setNegativeButton(R.string.settings_cancel, null)
            .setPositiveButton(R.string.settings_purge_confirm) { _, _ -> viewModel.purgeCloud() }
            .show()
    }

    private fun textInput(hintResId: Int, inputType: Int = InputType.TYPE_CLASS_TEXT): EditText =
        EditText(requireContext()).apply {
            hint = getString(hintResId)
            this.inputType = inputType
        }

    private fun verticalInputs(vararg inputs: EditText): LinearLayout =
        LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            val padding = resources.getDimensionPixelSize(R.dimen.settings_dialog_padding)
            setPadding(padding, 0, padding, 0)
            inputs.forEachIndexed { index, input ->
                if (index > 0) {
                    input.setTopMargin(resources.getDimensionPixelSize(R.dimen.settings_dialog_spacing))
                }
                addView(input)
            }
        }

    private fun passwordInputType(): Int =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

    private fun emailInputType(): Int =
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

    private fun Int.toAppAppearance(): AppAppearance =
        if (this == R.id.darkModeButton) AppAppearance.DARK else AppAppearance.LIGHT

    private fun applySettingsColors(appearance: AppAppearance) {
        val currentBinding = binding ?: return
        val background = if (appearance == AppAppearance.DARK) DARK_BACKGROUND else LIGHT_BACKGROUND
        val surface = if (appearance == AppAppearance.DARK) DARK_SURFACE else LIGHT_SURFACE
        val title = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_ORANGE
        val cardTitle = if (appearance == AppAppearance.DARK) DARK_TEXT else LIGHT_TITLE
        val muted = if (appearance == AppAppearance.DARK) DARK_MUTED else LIGHT_MUTED
        currentBinding.settingsRoot.setBackgroundColor(background)
        currentBinding.appearanceCard.setBackgroundColor(surface)
        currentBinding.cubeThemeCard.setBackgroundColor(surface)
        currentBinding.persistenceCard.setBackgroundColor(surface)
        currentBinding.cloudCard.setBackgroundColor(surface)
        currentBinding.profileCard.setBackgroundColor(surface)
        currentBinding.settingsTitle.setTextColor(title)
        applyCardTextColors(currentBinding.appearanceCard, cardTitle, muted)
        applyCardTextColors(currentBinding.cubeThemeCard, cardTitle, muted)
        applyCardTextColors(currentBinding.persistenceCard, cardTitle, muted)
        applyCardTextColors(currentBinding.cloudCard, cardTitle, muted)
        applyCardTextColors(currentBinding.profileCard, cardTitle, muted)
        currentBinding.cloudStatus.setTextColor(muted)
        currentBinding.profileName.setTextColor(cardTitle)
        currentBinding.profileEmail.setTextColor(muted)
    }

    private fun applyCardTextColors(view: View, title: Int, muted: Int) {
        when (view) {
            is MaterialButton -> return
            is SwitchMaterial -> return
            is TextView -> view.setTextColor(
                if (view.textSize > resources.getDimension(R.dimen.settings_body_text_threshold)) title else muted,
            )
            is ViewGroup -> repeat(view.childCount) { index ->
                applyCardTextColors(view.getChildAt(index), title, muted)
            }
        }
    }

    private fun AppAppearance.toButtonId(): Int =
        if (this == AppAppearance.DARK) R.id.darkModeButton else R.id.lightModeButton

    private fun Boolean.toVisibility(): Int = if (this) View.VISIBLE else View.GONE

    private fun MaterialButtonToggleGroup.checkIfNeeded(buttonId: Int) {
        if (checkedButtonId != buttonId) check(buttonId)
    }

    private fun SwitchMaterial.setCheckedIfNeeded(checkedValue: Boolean) {
        if (isChecked != checkedValue) isChecked = checkedValue
    }

    private fun View.setTopMargin(margin: Int) {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = margin }
    }

    companion object {
        private const val LIGHT_BACKGROUND = 0xFFF4F1EA.toInt()
        private const val LIGHT_SURFACE = 0xFFFDF8F0.toInt()
        private const val LIGHT_ORANGE = 0xFFE65100.toInt()
        private const val LIGHT_TITLE = 0xFF003A5D.toInt()
        private const val LIGHT_MUTED = 0xFF4D5B75.toInt()
        private const val DARK_BACKGROUND = 0xFF0F172A.toInt()
        private const val DARK_SURFACE = 0xFF1E293B.toInt()
        private const val DARK_TEXT = 0xFFF8FAFC.toInt()
        private const val DARK_MUTED = 0xFFCBD5E1.toInt()
        private const val TOAST_SUCCESS = 0xFF2E7D32.toInt()
        private const val TOAST_ERROR = 0xFFC62828.toInt()
    }
}
