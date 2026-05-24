package nz.mentalinc.watcher;


import com.google.android.material.color.DynamicColors;

import nz.mentalinc.watcher.guice.Application;

public class MyEpisodes extends Application {

    public class MyApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
