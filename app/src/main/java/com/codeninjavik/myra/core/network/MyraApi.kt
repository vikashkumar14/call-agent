package com.codeninjavik.myra.core.network

import com.codeninjavik.myra.data.model.*
import retrofit2.http.*

interface MyraApi {

    @GET("forwarding/target")
    suspend fun getForwardingTarget(): ForwardingTargetResponse

    @POST("forwarding/verify")
    suspend fun verifyForwarding(
        @Body request: VerifyForwardingRequest
    ): VerifyForwardingResponse

    @GET("forwarding/verify/{ticket}")
    suspend fun getForwardingVerificationStatus(
        @Path("ticket") ticket: String
    ): ForwardingStatusResponse

    @POST("forwarding/disable")
    suspend fun disableForwarding(
        @Body request: DisableForwardingRequest
    ): MessageResponse
}
