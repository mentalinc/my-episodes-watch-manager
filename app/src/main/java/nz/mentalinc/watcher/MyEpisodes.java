package nz.mentalinc.watcher;

import android.app.Application;

import com.google.android.material.color.DynamicColors;

public class MyEpisodes extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
