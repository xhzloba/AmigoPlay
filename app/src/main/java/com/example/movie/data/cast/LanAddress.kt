package com.example.movie.data.cast

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.Inet4Address

@Suppress("DEPRECATION")
internal fun lanIpv4(context: Context): String? {
    val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return null
    return manager.allNetworks.firstNotNullOfOrNull { network ->
        val capabilities = manager.getNetworkCapabilities(network) ?: return@firstNotNullOfOrNull null
        val local = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        if (!local) return@firstNotNullOfOrNull null
        manager.getLinkProperties(network)?.linkAddresses
            ?.asSequence()
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }
}

internal fun runningOnEmulator(): Boolean {
    val hardware = Build.HARDWARE
    val product = Build.PRODUCT
    return hardware == "ranchu" || hardware == "goldfish" || product.startsWith("sdk_gphone")
}
