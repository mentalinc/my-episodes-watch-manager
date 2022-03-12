package nz.mentalinc.episodeWatcher.pager;



import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.List;



public class ViewPager2Adapter extends FragmentStateAdapter {
    private static final String LOG_TAG = ViewPager2Adapter.class.getSimpleName();
    private List<Fragment> fragments; //variable holds the fragments the ViewPager2 allows us to swipe to.


    public ViewPager2Adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.w(LOG_TAG, "createFrag position: " + position);
        Log.w(LOG_TAG, "Fragments array size: " + fragments.size());
        return this.fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return this.fragments.size();
    }


    public void setData(List<Fragment> fragments) {
        this.fragments = fragments;
    }
}
