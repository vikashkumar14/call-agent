package com.codeninjavik.myra.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForwardingTargetResponse(
    val number: String,
    val mode: String,
    val displayLabel: String
)

@JsonClass(generateAdapter = true)
data class VerifyForwardingRequest(
    val number: String, // User's phone number being forwarded
    val mode: String // "unconditional" or "conditional"
)

@JsonClass(generateAdapter = true)
data class VerifyForwardingResponse(
    val ticket: String
)

@JsonClass(generateAdapter = true)
data class ForwardingStatusResponse(
    val status: String, // PENDING, CONFIRMED, NOT_FORWARDED, WRONG_TARGET, TIMED_OUT
    val detail: String
)

@JsonClass(generateAdapter = true)
data class DisableForwardingRequest(
    val number: String
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    val message: String
)
