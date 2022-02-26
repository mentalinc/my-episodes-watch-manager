package nz.mentalinc.episodeWatcher;


import nz.mentalinc.episodeWatcher.guice.Application;
import com.google.android.material.color.DynamicColors;

public class MyEpisodes extends Application {

    public class MyApplication extends Application {
        @Override
        public void onCreate() {
            super.onCreate();
            DynamicColors.applyToActivitiesIfAvailable(this);
        }
    }
}
