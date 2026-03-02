package com.example.moontalk

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseClient {

    val client = createSupabaseClient(
        supabaseUrl = "https://nfzhklakrgnjleuacrzw.supabase.co",
        supabaseKey = "sb_publishable_GeKHaHJcNLVGEWfcdBPi0w_xruxPlT0"
    ) {
        install(Auth)
        install(Postgrest)
    }
    val database = client.postgrest
}