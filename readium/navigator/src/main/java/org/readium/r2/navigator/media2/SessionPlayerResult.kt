package org.readium.r2.navigator.media2

import org.readium.r2.shared.util.Try

internal typealias SessionPlayerResult = Try<Unit, SessionPlayerException>

internal class SessionPlayerException(val error: SessionPlayerError) : Exception()

internal enum class SessionPlayerError{
    BAD_VALUE,
    INVALID_STATE,
    IO,
    NOT_SUPPORTED,
    PERMISSION_DENIED,
    SESSION_AUTHENTICATION_EXPIRED,
    SESSION_CONCURRENT_STREAM_LIMIT,
    SESSION_DISCONNECTED,
    SESSION_NOT_AVAILABLE_IN_REGION,
    SESSION_PARENTAL_CONTROL_RESTRICTED,
    SESSION_PREMIUM_ACCOUNT_REQUIRED,
    ERROR_SESSION_SETUP_REQUIRED,
    SESSION_SKIP_LIMIT_REACHED,
    UNKNOWN,
    INFO_SKIPPED;

    companion object {

        fun fromSessionResultCode(resultCode: Int): SessionPlayerError {
            require(resultCode != 0)
            return when(resultCode) {
                -3 -> BAD_VALUE
                -2 -> INVALID_STATE
                -5 -> IO
                -6 -> NOT_SUPPORTED
                -4 -> PERMISSION_DENIED
                -102 -> SESSION_AUTHENTICATION_EXPIRED
                -104 -> SESSION_CONCURRENT_STREAM_LIMIT
                -100 -> SESSION_DISCONNECTED
                -106 -> SESSION_NOT_AVAILABLE_IN_REGION
                -105 -> SESSION_PARENTAL_CONTROL_RESTRICTED
                -103 -> SESSION_PREMIUM_ACCOUNT_REQUIRED
                -108 -> ERROR_SESSION_SETUP_REQUIRED
                -107 -> SESSION_SKIP_LIMIT_REACHED
                1 -> INFO_SKIPPED
                else -> UNKNOWN // -1
            }
        }
    }
}
