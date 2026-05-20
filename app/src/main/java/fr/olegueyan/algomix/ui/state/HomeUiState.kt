package fr.olegueyan.algomix.ui.state

import fr.olegueyan.algomix.domain.cube.CubeState

data class HomeUiState(
    val keyboardCategory: MoveKeyboardCategory = MoveKeyboardCategory.FACE_TURNS,
    val freeSequenceNotation: String = "",
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false,
    val autoPlayEnabled: Boolean = false,
    val categoryCubeStates: Map<MoveKeyboardCategory, CubeState> = emptyMap(),
)
