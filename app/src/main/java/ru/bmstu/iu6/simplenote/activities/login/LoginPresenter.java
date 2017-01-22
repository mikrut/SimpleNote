package ru.bmstu.iu6.simplenote.activities.login;

import android.os.Build;
import android.support.annotation.NonNull;

import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import ru.bmstu.iu6.simplenote.activities.security_utils.IEncryptedPasswordRepository;
import ru.bmstu.iu6.simplenote.activities.security_utils.PasswordEncryptor;

/**
 * Created by Михаил on 19.01.2017.
 */

public class LoginPresenter implements LoginContract.Presenter {
    @NonNull
    private final LoginContract.View view;
    @NonNull
    private final IEncryptedPasswordRepository repository;
    @NonNull
    private final PasswordEncryptor encryptor;

    public LoginPresenter(@NonNull LoginContract.View view,
                          @NonNull IEncryptedPasswordRepository repository,
                          @NonNull PasswordEncryptor encryptor) {
        this.view = view;
        this.repository = repository;
        this.encryptor = encryptor;

        view.setPresenter(this);
    }

    private boolean authWithPassword(@NonNull String password) {
        boolean valid = view.checkPassword(password);
        if(valid) {
            byte[] bytes = encryptor.encrypt(password);
            repository.storeEncryptedPassword(bytes);
            view.openMainActivity();
        }
        return valid;
    }

    @Override
    public void login(@NonNull String password) {
        if (password.length() != 4) {
            view.displayPasswordErrorMessage("Length should be exactly 4 symbols");
            return;
        }

        if (!authWithPassword(password)) {
            view.displayPasswordErrorMessage("Invalid password");
        }
    }

    @Override
    public void fingerprintAuth(@NonNull Cipher resultCipher) {
        view.displayFingerprintStatus(LoginContract.STATUS_OK, "Fingerprint recognized");
        try {
            byte[] encryptedPassword = repository.getEncryptedPassword();
            if (encryptedPassword == null) {
                view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                        "First time enter PIN manually.\n" +
                        "Fingerprint auth is unavailable.");
                return;
            }

            String password = encryptor.decrypt(encryptedPassword, resultCipher);
            if (!authWithPassword(password)) {
                view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                        "Invalid password in storage.\n" +
                        "Enter PIN.");
            }
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        view.displayFingerprintStatus(LoginContract.STATUS_FAIL, helpString);
    }

    @Override
    public void handleAuthenticationError(int errMsgId, CharSequence errString) {
        view.displayFingerprintStatus(LoginContract.STATUS_FAIL, errString);
    }

    @Override
    public void handleAuthenticationFailed() {
        view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                "Fingerprint not recognized.\n" +
                "Try again.");
    }

    @Override
    public void start() {
 //       if (!authWithPassword(NotesDBOpenHelper.DATABASE_DEFAULT_PASSWORD)) {
            if (!view.fingerprintHardwareDetected()) {
                view.displayFingerprintStatus(LoginContract.STATUS_FAIL, "No fingerprint hardware found");
                // view.hideFingerprintStuff();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    if (!encryptor.isKeyReady()) {
                        view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                                "Keystore error.\n" +
                                        "Fingerprint auth is unavailable.");
                    } else if (!repository.containsEncryptedPassword()) {
                        view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                                "First time enter PIN manually.\n" +
                                        "Fingerprint auth is unavailable.");
                    } else {
                        view.executeFingerprintAuth(encryptor.getDecryptCipher());
                    }
                } catch (InvalidKeyException | UnrecoverableKeyException |
                        KeyStoreException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            } else {
                // TODO: report API is too low to use fingerprint auth
                view.hideFingerprintStuff();
            }
 //       }
    }
}
