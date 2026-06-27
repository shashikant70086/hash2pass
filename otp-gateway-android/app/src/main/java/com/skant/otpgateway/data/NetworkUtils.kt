package com.skant.otpgateway.data

import java.net.NetworkInterface

object NetworkUtils {
    /** First non-loopback IPv4 (usually the WiFi LAN address). */
    fun lanIpv4(): String? {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { addr ->
                    addr.hostAddress?.contains(':') == false &&
                        !addr.isLoopbackAddress && !addr.isLinkLocalAddress
                }
                ?.hostAddress
        }.getOrNull()
    }
}
