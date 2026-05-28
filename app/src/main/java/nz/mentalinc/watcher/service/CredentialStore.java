package nz.mentalinc.watcher.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class CredentialStore {
    private static final String FILE_NAME = "secure_credentials";
    private static final String KEY_PASSWORD = "UserPassword";
    private static final String KEY_STORE_ALIAS = "credential_master_key";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static SharedPreferences prefs;

    private static SharedPreferences getInstance(Context context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
        }
        return prefs;
    }

    private static SecretKey getOrCreateKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            if (keyStore.containsAlias(KEY_STORE_ALIAS)) {
                return (SecretKey) keyStore.getKey(KEY_STORE_ALIAS, null);
            }

            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE);
            keyGenerator.init(new KeyGenParameterSpec.Builder(
                    KEY_STORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes("GCM")
                    .setEncryptionPaddings("NoPadding")
                    .setKeySize(256)
                    .build());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Android Keystore key", e);
        }
    }

    private static String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt password", e);
        }
    }

    private static String decrypt(String encryptedData) {
        try {
            byte[] combined = Base64.decode(encryptedData, Base64.NO_WRAP);
            byte[] iv = new byte[12];
            byte[] ciphertext = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static void savePassword(Context context, String password) {
        String encrypted = encrypt(password);
        getInstance(context).edit().putString(KEY_PASSWORD, encrypted).apply();
    }

    public static String getPassword(Context context) {
        String encrypted = getInstance(context).getString(KEY_PASSWORD, null);
        if (encrypted == null) return null;
        return decrypt(encrypted);
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
