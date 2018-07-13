package database.example.com.instantshare;

import android.app.Application;
import android.content.Context;

/**
 * * ============================================================================
 * * Copyright (C) 2018 W3 Engineers Ltd - All Rights Reserved.
 * * Unauthorized copying of this file, via any medium is strictly prohibited
 * * Proprietary and confidential
 * * ----------------------------------------------------------------------------
 * * Created by: Mimo Saha on [13-Jul-2018 at 11:45 AM].
 * * Email: mimosaha@w3engineers.com
 * * ----------------------------------------------------------------------------
 * * Project: InstantShare.
 * * Code Responsibility: <Purpose of code>
 * * ----------------------------------------------------------------------------
 * * Edited by :
 * * --> <First Editor> on [13-Jul-2018 at 11:45 AM].
 * * --> <Second Editor> on [13-Jul-2018 at 11:45 AM].
 * * ----------------------------------------------------------------------------
 * * Reviewed by :
 * * --> <First Reviewer> on [13-Jul-2018 at 11:45 AM].
 * * --> <Second Reviewer> on [13-Jul-2018 at 11:45 AM].
 * * ============================================================================
 **/
public class InstantShareApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();
        PreferenceHelper.init(context);
        WebUpdater.getWebUpdater().initContext(context);
    }

}
