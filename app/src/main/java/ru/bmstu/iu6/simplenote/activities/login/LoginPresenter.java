package ru.bmstu.iu6.simplenote.activities.login;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import ru.bmstu.iu6.simplenote.data.database.NotesDBOpenHelper;

/**
 * Created by Михаил on 19.01.2017.
 */

public class LoginPresenter implements LoginContract.Presenter {
    @NonNull
    private final LoginContract.View view;
    @NonNull
    private Cipher cipher;
    @NonNull
    private final IEncryptedPasswordRepository repository;
    @NonNull
    private final KeyStore keyStore;
    @NonNull
    private final KeyPairGenerator keyPairGenerator;

    private static final String KEY_ALIAS =
            LoginPresenter.class.getCanonicalName() + ".KEY_ALIAS";

    public LoginPresenter(@NonNull LoginContract.View view,
                          @NonNull IEncryptedPasswordRepository repository,
                          @NonNull KeyStore keyStore,
                          @NonNull KeyPairGenerator keyPairGenerator) {
        this.view = view;
        this.repository = repository;
        this.keyStore = keyStore;
        this.keyPairGenerator = keyPairGenerator;

        try {
            keyStore.load(null);
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            e.printStackTrace();
        }
        // TODO: check cipher suite
        try {
            cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException ignore) {
            // Exception is not likely to occur, so...
            //noinspection ConstantConditions
            cipher = null;
            ignore.printStackTrace();
        }

        view.setPresenter(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean isKeyReady() {
        try {
            return keyStore.containsAlias(KEY_ALIAS) || generateNewKey();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean generateNewKey() {
        try {
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT);
            KeyGenParameterSpec spec = builder
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setUserAuthenticationRequired(true)
                    .build();

            keyPairGenerator.initialize(spec);
            keyPairGenerator.generateKeyPair();
            return true;
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean authWithPassword(@NonNull String password) {
        boolean valid = view.checkPassword(password);
        if(valid) {
            try {
                PublicKey key = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
                PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm()).generatePublic(new X509EncodedKeySpec(key.getEncoded()));
                OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
                cipher.init(Cipher.ENCRYPT_MODE, unrestricted, spec);
                byte[] bytes = cipher.doFinal(password.getBytes());
                repository.storeEncryptedPassword(bytes);
            } catch (KeyStoreException | NoSuchAlgorithmException | InvalidKeyException |
                    InvalidAlgorithmParameterException | InvalidKeySpecException |
                    IllegalBlockSizeException | BadPaddingException ignore) {
                ignore.printStackTrace();
            }
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

    private void deleteInvalidKey() {
        try {
            keyStore.deleteEntry(KEY_ALIAS);
        } catch (KeyStoreException e) {
            e.printStackTrace();
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

            String password = new String(cipher.doFinal(encryptedPassword));
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
                    if (!isKeyReady()) {
                        view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                                "Keystore error.\n" +
                                        "Fingerprint auth is unavailable.");
                    } else if (!repository.containsEncryptedPassword()) {
                        view.displayFingerprintStatus(LoginContract.STATUS_FAIL,
                                "First time enter PIN manually.\n" +
                                        "Fingerprint auth is unavailable.");
                    } else {
                        PrivateKey key = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
                        cipher.init(Cipher.DECRYPT_MODE, key);
                        view.executeFingerprintAuth(cipher);
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
