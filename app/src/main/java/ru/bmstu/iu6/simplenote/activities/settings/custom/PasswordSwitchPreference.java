package ru.bmstu.iu6.simplenote.activities.settings.custom;

import android.content.Context;
import android.os.Build;
import android.preference.SwitchPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;

import ru.bmstu.iu6.simplenote.data.database.NotesDAO;

/**
 * Created by Михаил on 25.01.2017.
 */

public class PasswordSwitchPreference extends SwitchPreference {
    public PasswordSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PasswordSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PasswordSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PasswordSwitchPreference(Context context) {
        super(context);
    }

    @Override
    public void setChecked(boolean checked) {
        final Context appContext = getContext().getApplicationContext();
        if (!NotesDAO.isPasswordDefault(appContext)) {
            if (!checked) {
                NotesDAO notesDAO = NotesDAO.getInstance(appContext);
                if (notesDAO != null) {
                    notesDAO.resetPassword();
                }
            }
            super.setChecked(checked);
        }
    }


}
