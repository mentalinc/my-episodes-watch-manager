package nz.mentalinc.episodeWatcher.activities;


import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;

import nz.mentalinc.episodeWatcher.EpisodeListingFrag;
import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.ShowDetailFrag;
import nz.mentalinc.episodeWatcher.constants.ActivityConstants;
import nz.mentalinc.episodeWatcher.enums.EpisodeType;
import nz.mentalinc.episodeWatcher.pager.ViewPager2Adapter;

public class ShowHomeTabActivity extends AppCompatActivity  {

    private static final String LOG_TAG = ShowHomeTabActivity.class.getSimpleName();
    private AppBarConfiguration appBarConfiguration;
    ViewPager2 viewPager2;
    TabLayout tabLayout;
    ViewPager2Adapter viewPager2Adapter;

    private static EpisodeType episodesType;
    private String showMyEpisodeID;

   // String tabNames[] = {"Show Overview","Episode Summary","Episodes to Watch","Episodes to Acquire","Episodes Coming"};
   String tabNames[] = {"Show Overview","Episodes to Watch","Episodes to Acquire","Episodes Coming"};

  // String[] tabNames = {"Episodes to Watch","Episodes to Acquire","Episodes Coming"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_home_tab);

        viewPager2 = findViewById(R.id.showHomeViewPager2);
        viewPager2.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        viewPager2.setUserInputEnabled(true);
        tabLayout = findViewById(R.id.tabLayoutShowHome);

        setViewPagerAdapter();

        //TODO this is being called twice I think when the tab is clicked.

        //tab names are defined in the show_home_tab.xml file
        new TabLayoutMediator(
                tabLayout,
                viewPager2,
                true,
                new TabLayoutMediator.TabConfigurationStrategy() {
                    @Override
                    public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                        tab.setText(tabNames[position]);
                        Log.w(LOG_TAG, "Tab names: " + tab.getText().toString());
                    }
                }
                ).attach();



    }

    public void setViewPagerAdapter() {
        viewPager2Adapter = new ViewPager2Adapter(this);
        ArrayList<Fragment> fragmentList = new ArrayList<>(); //creates an ArrayList of Fragments

        Bundle data = this.getIntent().getExtras();
        episodesType = (EpisodeType) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE);
        showMyEpisodeID = (String) data.getSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID);
        String Title = data.getString("Title");

        Log.w(LOG_TAG, "setViewPagerAdapter: Called");



        Fragment showDetailOverview = new ShowDetailFrag();
        Bundle BundleInfoShowDetail = new Bundle();
        BundleInfoShowDetail.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, episodesType);
        BundleInfoShowDetail.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID ,showMyEpisodeID);
        BundleInfoShowDetail.putString("Title",Title);
        showDetailOverview.setArguments(BundleInfoShowDetail);
        fragmentList.add(showDetailOverview);

        //fragmentList.add(new NextEpisodeToWatchFrag()); //Need to Build a "detailed" episode Frag



       //add the info for shows to watch.
        Fragment episodesWatchListing = new EpisodeListingFrag();
        Bundle BundleInfoWatch = new Bundle();
        BundleInfoWatch.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_WATCH);
        BundleInfoWatch.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID ,showMyEpisodeID);
        BundleInfoWatch.putString("Title",Title);
        episodesWatchListing.setArguments(BundleInfoWatch);
        fragmentList.add(episodesWatchListing);

        //add the info for shows to acquire
        Fragment episodesAcquireListing = new EpisodeListingFrag();
        Bundle BundleInfoAcquire = new Bundle();
        BundleInfoAcquire.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_TO_ACQUIRE);
        BundleInfoAcquire.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID ,showMyEpisodeID);
        BundleInfoAcquire.putString("Title",Title);
        episodesAcquireListing.setArguments(BundleInfoAcquire);
        fragmentList.add(episodesAcquireListing);

        //add the info for shows coming
        Fragment episodesComingListing = new EpisodeListingFrag();
        Bundle BundleInfoComing = new Bundle();
        BundleInfoComing.putSerializable(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, EpisodeType.EPISODES_COMING);
        BundleInfoComing.putString(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID ,showMyEpisodeID);
        BundleInfoComing.putString("Title",Title);
        episodesComingListing.setArguments(BundleInfoComing);
        fragmentList.add(episodesComingListing);

        viewPager2Adapter.setData(fragmentList); //sets the data for the adapter

        viewPager2.setAdapter(viewPager2Adapter);
/*
        int tabToOpen = 0;
        //TODO update the ID when adding show overview and next episode tabs,
        if(episodesType.equals(EpisodeType.EPISODES_TO_WATCH)){
            tabToOpen = 1;
        }else if(episodesType.equals(EpisodeType.EPISODES_TO_ACQUIRE)){
            tabToOpen = 2;
        }
        else if(episodesType.equals(EpisodeType.EPISODES_COMING)){
            tabToOpen = 3;
        }
        viewPager2.setCurrentItem(tabToOpen,true);
*/
    }
}