package nz.mentalinc.watcher.service;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.security.GeneralSecurityException;

public class CredentialStore {
    private static final String FILE_NAME = "secure_credentials";
    private static final String KEY_PASSWORD = "UserPassword";

    private static SharedPreferences prefs;

    private static SharedPreferences getInstance(Context context) {
        if (prefs == null) {
            try {
                String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
                prefs = EncryptedSharedPreferences.create(
                        FILE_NAME,
                        masterKeyAlias,
                        context,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | java.io.IOException e) {
                throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
            }
        }
        return prefs;
    }

    public static void savePassword(Context context, String password) {
        getInstance(context).edit().putString(KEY_PASSWORD, password).apply();
    }

    public static String getPassword(Context context) {
        return getInstance(context).getString(KEY_PASSWORD, null);
    }

    public static boolean hasPassword(Context context) {
        return getPassword(context) != null;
    }

    public static void removePassword(Context context) {
        getInstance(context).edit().remove(KEY_PASSWORD).apply();
    }

    public static void clear(Context context) {
        getInstance(context).edit().clear().apply();
    }
}
