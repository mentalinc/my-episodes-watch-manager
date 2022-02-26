package nz.mentalinc.episodeWatcher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;



public class ShowListingFrag extends Fragment {


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.recycle_view_shows, container, false);
        return rootView;
    }
   /*
    override fun onCreateView(

    ): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        emptyView = view.findViewById(R.id.emptyViewList)
        ViewTools.setVectorDrawableTop(emptyView, R.drawable.ic_list_white_24dp)
        return view
    }*/
}
