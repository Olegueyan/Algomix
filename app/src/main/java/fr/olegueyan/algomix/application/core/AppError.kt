package fr.olegueyan.algomix.application.core

enum class AppErrorType(val defaultMessage: String) {
    VALIDATION("Validation failed"),
    NOT_FOUND("Resource not found"),
    UNAUTHORIZED("Unauthorized operation"),
    NETWORK("Network operation failed"),
    CONFLICT("Conflicting data"),
    STORAGE("Storage operation failed"),
    UNKNOWN("Unexpected error"),
}

sealed class AppError(
    open val detail: String? = null,
    open val cause: Throwable? = null,
) {
    abstract val type: AppErrorType

    val message: String
        get() = detail ?: type.defaultMessage

    data class Validation(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.VALIDATION
    }

    data class NotFound(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.NOT_FOUND
    }

    data class Unauthorized(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.UNAUTHORIZED
    }

    data class Network(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.NETWORK
    }

    data class Conflict(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.CONFLICT
    }

    data class Storage(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.STORAGE
    }

    data class Unknown(
        override val detail: String? = null,
        override val cause: Throwable? = null,
    ) : AppError(detail, cause) {
        override val type = AppErrorType.UNKNOWN
    }
}
