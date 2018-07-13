package database.example.com.instantshare;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * * ============================================================================
 * * Copyright (C) 2018 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Mimo Saha on [13-Jul-2018 at 11:23 AM].
 * * Email: mimosaha@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: InstantShare.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [13-Jul-2018 at 11:23 AM].
 * * --> <Second Editor> on [13-Jul-2018 at 11:23 AM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [13-Jul-2018 at 11:23 AM].
 * * --> <Second Reviewer> on [13-Jul-2018 at 11:23 AM].
 * * ============================================================================
 **/
public class PreferenceHelper {

    private static SharedPreferences preferences;
    private static final String MIME_TYPE_KEY = "MIME_TYPE_KEY";

    public static void init(Context context) {
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    public static boolean writeMimeType(String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(MIME_TYPE_KEY, value);
        return editor.commit();
    }

    public static String readMimeType() {
        return preferences.getString(MIME_TYPE_KEY, ".mp3");
    }
}
