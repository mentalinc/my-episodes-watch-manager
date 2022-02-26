package nz.mentalinc.episodeWatcher.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.domain.Show;

public class ShowListingActivity extends Activity {

    List<Show> shows;

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycle_view_shows);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String themeSetting = sharedPref.getString("ThemeSetting", "0");
        switch (themeSetting) {
            case "0":
                setTheme(R.style.ThemeDayNight);
                break;
            case "1":
                setTheme(R.style.ThemeLight);
                break;
            case "2":
                setTheme(R.style.ThemeDark);
                break;
        }

        RecyclerView rvShows = (RecyclerView) findViewById(R.id.recyclerViewListItems);

        // Initialize Show fake data
        //TODO this is where where the ACTUAL data needs to be added from the .xml files.
        shows = Show.createShowsList(20);


        // Create adapter passing in the sample user data
        ShowAdapter adapter = new ShowAdapter(shows);

        adapter.submitList(shows);
        adapter.notifyDataSetChanged();
        // Attach the adapter to the recyclerview to populate items
        rvShows.setAdapter(adapter);
        // Set layout manager to position the items
        rvShows.setLayoutManager(new LinearLayoutManager(this));
        // That's all!



        // need to work out what i need to do to add many of the item_show files and work with them to add content.
        //probably need to get shows from the .watch.xml to start wth(only 2) then try with aquie to test a few more???
        // or need to do something with the recyclerViewListItems to add things to it some how?
        // might need some sort of adapter? to do soemthing with it?



/*
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.layout.fragment_list, new ShowListingFrag())
                .commit();
*/
/*
        androidx.appcompat.view.menu.ActionMenuItemView appBarHome = findViewById(R.id.home);
        appBarHome.setOnClickListener(v -> {

            //Log.w(LOG_TAG, "logout button clicked.");
            finish();
        });
*/
    }
}
