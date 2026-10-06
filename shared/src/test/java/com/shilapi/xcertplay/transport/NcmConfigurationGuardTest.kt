package com.shilapi.xcertplay.transport

import java.io.IOException
import org.junit.Test

class NcmConfigurationGuardTest {
    @Test fun sameActiveConfigurationIsPreserved() { NcmConfigurationGuard.requireActive(2, 2) }
    @Test(expected = IOException::class) fun changedConfigurationCannotResetClaimedUsbMux() { NcmConfigurationGuard.requireActive(1, 2) }
    @Test(expected = IOException::class) fun unknownConfigurationIsNotReportedReady() { NcmConfigurationGuard.requireActive(null, 2) }
}
