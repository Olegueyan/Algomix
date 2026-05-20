package fr.olegueyan.algomix.ui.state

data class HomeUiState(
    val keyboardCategory: MoveKeyboardCategory = MoveKeyboardCategory.FACE_TURNS,
    val freeSequenceNotation: String = "",
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
    val autoPlayEnabled: Boolean = false,
)
