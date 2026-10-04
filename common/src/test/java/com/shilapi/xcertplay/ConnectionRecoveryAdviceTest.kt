package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test

class ConnectionRecoveryAdviceTest {
    @Test fun missingHotspotConfigurationHasSetupAdvice() {
        assertTrue(ConnectionRecoveryAdvice.forFailure("manualHotspotSsid is required in manual hotspot mode").chinese.contains("读取车机热点信息"))
    }
    @Test fun credentialsInMismatchAreNeverEchoed() {
        val text = ConnectionRecoveryAdvice.forFailure("Manual hotspot SSID does not match the active local AP configuration: 'private-network'")
        assertTrue(text.chinese.contains("读取车机热点信息"))
        assertFalse(text.toString().contains("private-network"))
    }
    @Test fun unknownTimeoutDoesNotBecomeBluetoothOrPasswordDiagnosis() {
        val text = ConnectionRecoveryAdvice.forFailure("Unknown socket timed out")
        assertTrue(text.chinese.contains("原因尚未确认"))
        assertFalse(text.chinese.contains("密码错误"))
    }
    @Test fun initializedFlagFailureDoesNotRecommendRepeatedInitialization() {
        val text = ConnectionRecoveryAdvice.forFailure("H52 ANW previous initialization failed or timed out; restart vehicle Bluetooth before retrying")
        assertTrue(text.chinese.contains("重启"))
        assertFalse(text.chinese.contains("原生 Android"))
    }
    @Test fun pairedPhoneIsNotReportedAsSuccessfulSpp() {
        val text = ConnectionRecoveryAdvice.forFailure("H52 ANW connection not confirmed before timeout or cancellation")
        assertTrue(text.chinese.contains("不代表 SPP 已建立"))
    }
    @Test fun nativeFailureWithFactoryEnabledDoesNotTellUserToEnableNativeBluetooth() {
        val text = ConnectionRecoveryAdvice.forFailure("Bluetooth is not enabled", true)
        assertTrue(text.chinese.contains("实际连接模式"))
        assertFalse(text.chinese.contains("设置开启"))
    }
    @Test fun hotspotOffDistinguishesStationFromAp() {
        assertTrue(ConnectionRecoveryAdvice.forFailure("The car hotspot is off. Turn it on in the car settings and connect again.").chinese.contains("客户端开启不等于热点开启"))
    }
    @Test fun explicitPermissionDenialHasConcreteAdvice() {
        assertTrue(ConnectionRecoveryAdvice.forFailure("java.lang.SecurityException: Permission denial").chinese.contains("应用权限"))
    }
}
