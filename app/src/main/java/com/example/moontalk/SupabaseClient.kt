package com.example.moontalk

import android.R
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.HttpTimeout
import io.ktor.websocket.WebSocketDeflateExtension.Companion.install
import kotlinx.serialization.Serializable



object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://nfzhklakrgnjleuacrzw.supabase.co",
        supabaseKey = "sb_publishable_GeKHaHJcNLVGEWfcdBPi0w_xruxPlT0"
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    val database = client.postgrest
}