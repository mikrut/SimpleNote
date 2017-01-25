package ru.bmstu.iu6.simplenote.activities.settings.custom;

import android.content.Context;
import android.os.Build;
import android.preference.DialogPreference;
import android.security.keystore.KeyProperties;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.security_utils.SharedPreferencesEncryptedPasswordRepository;
import ru.bmstu.iu6.simplenote.activities.security_utils.PasswordEncryptor;
import ru.bmstu.iu6.simplenote.data.database.NotesDAO;

/**
 * Created by Михаил on 21.01.2017.
 */

public class PasswordPreference extends DialogPreference {
    private EditText editPassword;
    private EditText editPasswordConfirm;

    public PasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_password);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        editPassword = (EditText) view.findViewById(R.id.edit_password);
        editPasswordConfirm = (EditText) view.findViewById(R.id.edit_password_confirm);

        editPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String password = editPassword.getText().toString();
                final String confirmation = editable.toString();
                if (confirmation.length() == 4 && !confirmation.equals(password)) {
                    // FIXME: use string resources
                    editPasswordConfirm.setError("Passwords do not match");
                }
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        final String password = editPassword.getText().toString();
        final String passwordConfirm = editPasswordConfirm.getText().toString();

        if (positiveResult && password.equals(passwordConfirm)) {
            NotesDAO notesDAO = NotesDAO.getInstance(getContext().getApplicationContext());
            if (notesDAO != null) {
                notesDAO.changePassword(password);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                KeyStore keyStore = null;
                KeyPairGenerator keyPairGenerator = null;
                try {
                    keyStore = KeyStore.getInstance("AndroidKeyStore");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
                    } else {
                        keyPairGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
                    }
                } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyStoreException ignore) {
                    ignore.printStackTrace();
                }

                PasswordEncryptor encryptor = new PasswordEncryptor(keyStore, keyPairGenerator);
                if (encryptor.isKeyReady()) {
                    SharedPreferencesEncryptedPasswordRepository repository = new SharedPreferencesEncryptedPasswordRepository(getSharedPreferences());

                    byte[] encryptedPassword = encryptor.encrypt(password);
                    repository.storeEncryptedPassword(encryptedPassword);
                }
            }
        }
    }
}
