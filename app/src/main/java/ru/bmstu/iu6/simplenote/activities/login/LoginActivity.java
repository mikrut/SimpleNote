package ru.bmstu.iu6.simplenote.activities.login;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.keystore.KeyProperties;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.os.CancellationSignal;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Cipher;

import de.hdodenhof.circleimageview.CircleImageView;
import ru.bmstu.iu6.simplenote.R;
import ru.bmstu.iu6.simplenote.activities.notes.NotesActivity;
import ru.bmstu.iu6.simplenote.activities.security_utils.PasswordEncryptor;
import ru.bmstu.iu6.simplenote.activities.security_utils.SharedPreferencesEncryptedPasswordRepository;
import ru.bmstu.iu6.simplenote.data.database.NotesDAO;

import static android.view.View.GONE;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {

    private LoginContract.Presenter presenter;

    private CircleImageView fingerprintIcon;
    private TextView fingerprintText;
    private EditText passwordEdit;
    private Button loginButton;

    private static final int REQUEST_FINGERPRINT_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        fingerprintIcon = (CircleImageView) findViewById(R.id.image_fingerprint_icon);
        fingerprintText = (TextView) findViewById(R.id.text_fingerprint_info);
        passwordEdit = (EditText) findViewById(R.id.edit_pin);
        loginButton = (Button) findViewById(R.id.button_login);

        // TODO: use dependency injection
        SharedPreferencesEncryptedPasswordRepository repository = new SharedPreferencesEncryptedPasswordRepository(PreferenceManager.getDefaultSharedPreferences(this));
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
        new LoginPresenter(this, repository, new PasswordEncryptor(keyStore, keyPairGenerator));

        loginButton.setOnClickListener(view -> {
            presenter.login(passwordEdit.getText().toString());
        });

        passwordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                final String password = editable.toString();
                if (password.length() == 4) {
                    presenter.login(password);
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_FINGERPRINT_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINGERPRINT_PERMISSION) {
            presenter.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean shouldStart = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            shouldStart = true;
        } else if (checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED) {
            shouldStart = true;
        }
        if (shouldStart)
            presenter.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void executeFingerprintAuth(Cipher cipher) {
        FingerprintManager.CryptoObject cryptoObject =
                new FingerprintManager.CryptoObject(cipher);

        if (authenticationCallback == null) {
            authenticationCallback = new MyAuthenticationCallback(presenter);
        } else if (authenticationCallback.unbound()) {
            authenticationCallback.bind(presenter);
        }

        FingerprintManager fingerprintManager = getApplicationContext().getSystemService(FingerprintManager.class);
        if (checkSelfPermission(Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fingerprintManager.authenticate(cryptoObject, new CancellationSignal(), 0, authenticationCallback, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static class MyAuthenticationCallback extends FingerprintManager.AuthenticationCallback {
        private LoginContract.Presenter presenter;

        MyAuthenticationCallback(@Nullable LoginContract.Presenter presenter) {
            this.presenter = presenter;
        }

        void unbind() {
            presenter = null;
        }

        boolean unbound() {
            return presenter == null;
        }

        void bind(@Nullable LoginContract.Presenter presenter) {
            this.presenter = presenter;
        }

        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            if (presenter != null)
                presenter.handleAuthenticationError(errMsgId, errString);
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            if (presenter != null)
                presenter.handleAuthenticationHelp(helpMsgId, helpString);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            Cipher cipher = result.getCryptoObject().getCipher();
            if (presenter != null)
                presenter.fingerprintAuth(cipher);
        }

        @Override
        public void onAuthenticationFailed() {
            if (presenter != null)
                presenter.handleAuthenticationFailed();
        }
    }

    MyAuthenticationCallback authenticationCallback;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void displayFingerprintStatus(int status, @NonNull CharSequence message) {
        @ColorInt final int COLOR_OK = Color.rgb(0x00, 0x96, 0x88);
        @ColorInt final int COLOR_FAIL = Color.rgb(0xF4, 0x51, 0x1E);
        @ColorInt final int COLOR_NORMAL = Color.rgb(0x60, 0x7D, 0x8B);

        @ColorInt int currentColor;
        @DrawableRes int currentImage;
        switch (status) {
            case LoginContract.STATUS_OK:
                currentColor = COLOR_OK;
                currentImage = R.drawable.ic_done_white_24dp;
                break;
            case LoginContract.STATUS_FAIL:
                currentColor = COLOR_FAIL;
                currentImage = R.drawable.ic_exclaimation_white_24dp;
                break;
            case LoginContract.STATUS_NORMAL:
            default:
                currentColor = COLOR_NORMAL;
                currentImage = R.drawable.ic_fingerprint_white_24dp;
        }

        Drawable color = new ColorDrawable(currentColor);
        Drawable image = getDrawable(currentImage);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{color, image});
        fingerprintIcon.setImageDrawable(layerDrawable);


        fingerprintText.setTextColor(currentColor);
        fingerprintText.setText(message);
    }

    @Override
    public void setPresenter(@NonNull LoginContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void displayPasswordErrorMessage(@NonNull CharSequence errorMessage) {
        passwordEdit.setError(errorMessage);
    }

    @Override
    public void openMainActivity() {
        Intent intent = new Intent(this, NotesActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void hideFingerprintStuff() {
        fingerprintIcon.setVisibility(GONE);
        fingerprintText.setVisibility(GONE);
    }

    @Override
    public boolean checkPassword(@NonNull String password) {
        boolean valid = NotesDAO.checkPassword(getApplicationContext(), password);
        if (valid) {
            // instantiate DB accessor with provided valid password
            // for further usage
            NotesDAO.getInstance(getApplicationContext(), password);
        }
        return valid;
    }

    @Override
    public boolean fingerprintHardwareDetected() {
        return FingerprintManagerCompat.from(getApplicationContext()).isHardwareDetected();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            authenticationCallback.unbind();
        }
    }
}
