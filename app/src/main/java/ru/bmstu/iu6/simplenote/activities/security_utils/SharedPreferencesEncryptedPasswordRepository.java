package ru.bmstu.iu6.simplenote.activities.security_utils;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

/**
 * Created by Михаил on 19.01.2017.
 */

public class SharedPreferencesEncryptedPasswordRepository implements IEncryptedPasswordRepository {
    private static final String ENCRYPTED_PASSWORD_ALIAS =
            "security_password";
    private final @NonNull SharedPreferences sharedPreferences;

    public SharedPreferencesEncryptedPasswordRepository(@NonNull SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Nullable
    @Override
    public byte[] getEncryptedPassword() {
        String password = sharedPreferences.getString(ENCRYPTED_PASSWORD_ALIAS, null);
        if (password == null)
            return  null;
        try {
            return Base64.decode(password, Base64.DEFAULT);
        } catch (IllegalArgumentException ex) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(ENCRYPTED_PASSWORD_ALIAS);
            editor.apply();
            return null;
        }
    }

    @Override
    public void storeEncryptedPassword(@Nullable byte[] encryptedPassword) {
        String password = Base64.encodeToString(encryptedPassword, Base64.DEFAULT);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ENCRYPTED_PASSWORD_ALIAS, password);
        editor.apply();
    }

    @Override
    public boolean containsEncryptedPassword() {
        return sharedPreferences.contains(ENCRYPTED_PASSWORD_ALIAS);
    }
}
