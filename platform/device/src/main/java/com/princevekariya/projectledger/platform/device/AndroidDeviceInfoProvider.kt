package com.princevekariya.projectledger.platform.device

import android.os.Build

data class DeviceInfo(
    val operatingSystem: String,
    val releaseVersion: String,
    val sdkLevel: Int,
) {
    val displayValue: String
        get() = "$operatingSystem $releaseVersion (API $sdkLevel)"
}

interface DeviceInfoProvider {
    fun getDeviceInfo(): DeviceInfo
}

class AndroidDeviceInfoProvider : DeviceInfoProvider {
    override fun getDeviceInfo(): DeviceInfo = DeviceInfo(
        operatingSystem = "Android",
        releaseVersion = Build.VERSION.RELEASE,
        sdkLevel = Build.VERSION.SDK_INT,
    )
}
