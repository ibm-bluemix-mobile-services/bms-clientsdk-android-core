/*
 *     Copyright 2015 IBM Corp.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.ibm.mobilefirstplatform.clientsdk.android.analytics.api.internal;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Implements the android life cycle callbacks to be registered with the application.
 *
 * Implemented as a singleton so that application callbacks can only be registered once.
 */
public class MFPAnalyticsActivityLifecycleListener implements Application.ActivityLifecycleCallbacks{
    private static MFPAnalyticsActivityLifecycleListener instance;

    public static void init(Application app) {
        if (instance == null) {
            instance = new MFPAnalyticsActivityLifecycleListener();
            app.registerActivityLifecycleCallbacks(instance);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        //WLLifecycleHelper.getInstance().onPause();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        //WLLifecycleHelper.getInstance().onResume();
    }

    // we do not currently instrument any other lifecycle callbacks
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}
}
