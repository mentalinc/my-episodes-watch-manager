package nz.mentalinc.episodeWatcher;


import com.google.android.material.color.DynamicColors;

import nz.mentalinc.episodeWatcher.guice.Application;

public class MyEpisodes extends Application {

    public class MyApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
