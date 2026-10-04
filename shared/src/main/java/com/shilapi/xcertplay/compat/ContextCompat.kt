package com.shilapi.xcertplay.compat

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build

/**
 * Permission and signing lookups that moved or changed shape after Android 4.3.
 */
object ContextCompat {

    /**
     * Context.checkSelfPermission is API 23. The pre-23 equivalent for the app's own permissions is
     * checkCallingOrSelfPermission, which returns the same PERMISSION_GRANTED/DENIED answer.
     */
    fun checkSelfPermission(context: Context, permission: String): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(permission)
        } else {
            @Suppress("DEPRECATION")
            context.checkCallingOrSelfPermission(permission)
        }

    /**
     * Reads the install signatures. PackageInfo.signingInfo is API 28; older units expose
     * signatures[], and on API 28+ that array is empty for some install paths so signingInfo is
     * preferred where available.
     */
    fun signatures(context: Context, packageName: String): Array<Signature> {
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES,
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return emptyArray()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo
            if (signing != null && signing.hasMultipleSigners()) {
                return signing.apkContentsSigners ?: emptyArray()
            }
            return signing?.signingCertificateHistory ?: emptyArray()
        }
        @Suppress("DEPRECATION")
        return info.signatures ?: emptyArray()
    }

    /** PackageInfo.signingInfo is API 28; older units use the deprecated signatures array. */
    fun hasSigningInfo(info: PackageInfo): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.signingInfo != null
}
