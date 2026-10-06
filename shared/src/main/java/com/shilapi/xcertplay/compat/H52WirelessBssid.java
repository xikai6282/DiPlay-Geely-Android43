package com.shilapi.xcertplay.compat;

import android.util.Log;

/** Optional six-byte BSSID; invalid or synthetic non-MAC identifiers are omitted. */
public final class H52WirelessBssid {
    public static byte[] parse(String identifier) {
        if (identifier == null || !identifier.matches("(?i)[0-9a-f]{2}(:[0-9a-f]{2}){5}"))
            return null;
        String[] parts = identifier.split(":");
        byte[] result = new byte[6];
        int combined = 0;
        for (int i = 0; i < 6; i++) {
            result[i] = (byte) Integer.parseInt(parts[i], 16);
            combined |= result[i] & 255;
        }
        if (combined == 0 || (result[0] & 1) != 0) return null;
        Log.i("xcertplay-usb", "P12 Wi-Fi configuration includes six-byte interface BSSID");
        return result;
    }
}
