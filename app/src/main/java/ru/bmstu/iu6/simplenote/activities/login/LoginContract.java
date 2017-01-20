package ru.bmstu.iu6.simplenote.activities.login;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

import javax.crypto.Cipher;

import ru.bmstu.iu6.simplenote.mvp.BasePresenter;
import ru.bmstu.iu6.simplenote.mvp.BaseView;

/**
 * Created by Михаил on 19.01.2017.
 */

public interface LoginContract {

    int STATUS_OK = 0;
    int STATUS_FAIL = 1;
    int STATUS_NORMAL = 2;

    interface View extends BaseView<Presenter> {

        // FIXME: use string resources
        void displayPasswordErrorMessage(@NonNull CharSequence errorMessage);

        // FIXME: use string resources
        void displayHelpText(@NonNull CharSequence helpText);

        // FIXME: use string resources
        void displayFingerprintStatus(int status, @NonNull CharSequence message);

        void openMainActivity();

        void executeFingerprintAuth(Cipher cipher);

        void hideFingerprintStuff();

        boolean fingerprintHardwareDetected();

        boolean checkPassword(@NonNull String password);

    }

    interface Presenter extends BasePresenter {

        void login(@NonNull String password);

        void fingerprintAuth(@NonNull Cipher resultCipher);

        void handleAuthenticationHelp(int helpMsgId, CharSequence helpString);

        void handleAuthenticationFailed();

    }

}
