package com.monteirogames.nutriverse.util

import kotlinx.coroutines.TimeoutCancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AiErrorMapper {

    const val MSG_SERVICE_BUSY = "The AI service is busy right now. Please try again later."
    const val MSG_NETWORK_ERROR = "Check your internet connection and try again."
    const val MSG_QUOTA_EXHAUSTED = "The AI usage limit has been reached. Please try again when access is available."
    const val MSG_SECURITY_CONFIG = "Security verification failed. Please check your device configuration or try again shortly."
    const val MSG_TIMEOUT = "The request timed out. Please try again in a few moments."
    const val MSG_EMPTY_RESPONSE = "The AI returned an empty response. Please try again."
    const val MSG_UNPARSEABLE_RESPONSE = "The AI response could not be processed. Please try again."
    const val MSG_GENERIC_ERROR = "The AI service encountered an unexpected error. Please try again."

    fun mapToUserMessage(throwable: Throwable, isImageAnalysis: Boolean = false): String {
        if (throwable is TimeoutCancellationException) {
            return MSG_TIMEOUT
        }

        if (throwable is UnknownHostException || throwable is ConnectException || throwable is SocketTimeoutException) {
            return MSG_NETWORK_ERROR
        }

        val rawMessage = throwable.message.orEmpty()
        val causeMessage = throwable.cause?.message.orEmpty()
        val combined = "$rawMessage $causeMessage".lowercase()

        return when {
            // 1. Temporary overload, decode preemption, unavailable, high demand
            combined.contains("decode_preempted") ||
            combined.contains("overloaded") ||
            combined.contains("unavailable") ||
            combined.contains("high demand") ||
            combined.contains("too many retries") ||
            combined.contains("service unavailable") ||
            combined.contains("status code: 503") ||
            combined.contains("http 503") ||
            combined.contains("503 service") -> {
                MSG_SERVICE_BUSY
            }

            // 2. Network connectivity failures
            combined.contains("unable to resolve host") ||
            combined.contains("failed to connect") ||
            combined.contains("no address associated") ||
            combined.contains("network error") ||
            combined.contains("networkexception") ||
            combined.contains("connection reset") ||
            combined.contains("sslhandshakeexception") ||
            (throwable is IOException && !combined.contains("json")) -> {
                MSG_NETWORK_ERROR
            }

            // 3. Timeout in message
            combined.contains("timed out") || combined.contains("timeout") -> {
                MSG_TIMEOUT
            }

            // 4. Genuine quota exhaustion
            combined.contains("resource_exhausted") ||
            combined.contains("quota exceeded") ||
            combined.contains("usage limit") ||
            combined.contains("exceeded your current quota") -> {
                MSG_QUOTA_EXHAUSTED
            }

            // 5. Rate limiting (429 without specific quota exhaustion)
            combined.contains("429") || combined.contains("too many requests") || combined.contains("rate limit") -> {
                MSG_SERVICE_BUSY
            }

            // 6. App Check / Security / Configuration
            combined.contains("appcheck") ||
            combined.contains("app check") ||
            combined.contains("attestation") ||
            combined.contains("permission_denied") ||
            combined.contains("403 forbidden") ||
            combined.contains("forbidden") -> {
                MSG_SECURITY_CONFIG
            }

            // 7. Empty response
            combined.contains("empty response") || combined.contains("empty ai analysis") -> {
                MSG_EMPTY_RESPONSE
            }

            // 8. JSON or parsing failure
            combined.contains("json") ||
            combined.contains("malformed") ||
            combined.contains("unparseable") ||
            combined.contains("missing 'days'") ||
            combined.contains("no valid meal days") -> {
                if (isImageAnalysis) {
                    "Could not extract lab data from the image. Please ensure the report is clearly lit and legible, then try again."
                } else {
                    MSG_UNPARSEABLE_RESPONSE
                }
            }

            // 9. Safe fallback without exposing tokens, stack traces, or internal server errors
            else -> {
                MSG_GENERIC_ERROR
            }
        }
    }
}
