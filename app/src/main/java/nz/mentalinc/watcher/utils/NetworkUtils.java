package nz.mentalinc.watcher.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Connectivity helpers that never perform network I/O, so they are safe to call on the UI thread.
 */
public final class NetworkUtils {
    private NetworkUtils() {
    }

    /**
     * Returns whether the device currently has a network with internet capability.
     *
     * <p>This is a local check only: it does not prove that a remote host is reachable, but it
     * never blocks the calling thread the way an HTTP probe does.</p>
     */
    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
