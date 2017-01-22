package ru.bmstu.iu6.simplenote.activities.security_utils;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

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

/**
 * Created by Михаил on 21.01.2017.
 */

public class PasswordEncryptor {
    private static final String KEY_ALIAS =
            PasswordEncryptor.class.getCanonicalName() + ".KEY_ALIAS";

    private final KeyStore keyStore;
    private final KeyPairGenerator keyPairGenerator;
    private Cipher cipher;

    public PasswordEncryptor(@NonNull KeyStore keyStore, @NonNull KeyPairGenerator keyPairGenerator) {
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
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean isKeyReady() {
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

    public byte[] encrypt(@NonNull String password) {
        try {
            PublicKey key = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            PublicKey unrestricted = KeyFactory.getInstance(key.getAlgorithm()).generatePublic(new X509EncodedKeySpec(key.getEncoded()));
            OAEPParameterSpec spec = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, unrestricted, spec);
            return cipher.doFinal(password.getBytes());
        } catch (KeyStoreException | NoSuchAlgorithmException | InvalidKeyException |
                InvalidAlgorithmParameterException | InvalidKeySpecException |
                IllegalBlockSizeException | BadPaddingException ignore) {
            ignore.printStackTrace();
        }
        return null;
    }

    public String decrypt(@NonNull byte[] encryptedPassword, @NonNull Cipher permittedCipher) throws BadPaddingException, IllegalBlockSizeException {
        return new String(cipher.doFinal(encryptedPassword));
    }

    public Cipher getDecryptCipher() throws InvalidKeyException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        PrivateKey key = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher;
    }

    public void deleteInvalidKey() {
        try {
            keyStore.deleteEntry(KEY_ALIAS);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }
}
