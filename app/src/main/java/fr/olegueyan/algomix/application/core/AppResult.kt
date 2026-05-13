package fr.olegueyan.algomix.application.core

sealed interface AppResult<out T> {
    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure

    data class Success<T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>

    fun <R> map(transform: (T) -> R): AppResult<R> =
        when (this) {
            is Success -> success(transform(value))
            is Failure -> this
        }

    fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (AppError) -> R,
    ): R =
        when (this) {
            is Success -> onSuccess(value)
            is Failure -> onFailure(error)
        }

    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            is Failure -> null
        }

    fun errorOrNull(): AppError? =
        when (this) {
            is Success -> null
            is Failure -> error
        }

    companion object {
        fun <T> success(value: T): AppResult<T> = Success(value)

        fun failure(error: AppError): AppResult<Nothing> = Failure(error)
    }
}
