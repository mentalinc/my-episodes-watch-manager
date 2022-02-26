package nz.mentalinc.episodeWatcher.guice;

import android.util.Log;

import roboguice.config.AbstractAndroidModule;

class Module extends AbstractAndroidModule {
    private static final String LOG_TAG = Module.class.getSimpleName();

    @Override
    protected void configure() {
        Log.i(LOG_TAG, "Configuring module " + getClass().getSimpleName());
        Log.i(LOG_TAG, "DAO's and services are now bound!");
    }


}
