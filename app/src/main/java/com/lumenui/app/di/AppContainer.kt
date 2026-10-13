package com.lumenui.app.di

import android.content.Context
import com.lumenui.app.data.BackendRepository
import com.lumenui.app.data.ConnectivityProbe
import com.lumenui.app.data.DnsLeakProbe
import com.lumenui.app.data.HttpConnectivityProbe
import com.lumenui.app.data.HttpIpReputationApi
import com.lumenui.app.data.IpReputationApi
import com.lumenui.app.data.MihomoApi
import com.lumenui.app.data.SettingsRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val prefs = context.getSharedPreferences("lumen_prefs", Context.MODE_PRIVATE)

    val backends = BackendRepository(prefs)
    val settings = SettingsRepository(prefs)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    val mihomoApi = MihomoApi(httpClient)
    val connectivity: ConnectivityProbe = HttpConnectivityProbe(httpClient)
    val ipReputation: IpReputationApi = HttpIpReputationApi(httpClient)
    val dnsLeak = DnsLeakProbe(httpClient)
}
