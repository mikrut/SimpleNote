package ru.bmstu.iu6.simplenote.activities.login;

import android.support.annotation.Nullable;

/**
 * Created by Михаил on 19.01.2017.
 */

public interface IEncryptedPasswordRepository {

    @Nullable
    byte[] getEncryptedPassword();

    void storeEncryptedPassword(@Nullable byte[] encryptedPassword);

    boolean containsEncryptedPassword();

}
