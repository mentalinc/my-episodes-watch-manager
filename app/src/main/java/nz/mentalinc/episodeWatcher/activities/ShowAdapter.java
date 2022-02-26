package nz.mentalinc.episodeWatcher.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.domain.Show;

public class ShowAdapter extends ListAdapter<Show, ShowAdapter.ViewHolder> {

    private List<Show> showsList;



    public ShowAdapter(List<Show> shows) {
        super(DIFF_CALLBACK);
    }


    public void addMoreShows(List<Show> newShows) {
        showsList.addAll(newShows);
        submitList(showsList); // DiffUtil takes care of the check
    }

    public static final DiffUtil.ItemCallback<Show> DIFF_CALLBACK = new DiffUtil.ItemCallback<Show>() {
                @Override
                public boolean areItemsTheSame(Show oldItem, Show newItem) {
                    return oldItem.getShowName().equals(newItem.getShowName());
                }
                @Override
                public boolean areContentsTheSame(Show oldItem, Show newItem) {
                    return (oldItem.getShowName().equals( newItem.getShowName()) && oldItem.getMyEpisodeID().equals(newItem.getMyEpisodeID()));
                }
            };

    public class ViewHolder extends RecyclerView.ViewHolder {
        //
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView seriesnameView;
        public TextView TextViewShowListNextEpisode;


        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            seriesnameView = (TextView) itemView.findViewById(R.id.seriesname);
            TextViewShowListNextEpisode = (TextView) itemView.findViewById(R.id.TextViewShowListNextEpisode);
        }
    }

    @Override
    public ShowAdapter.ViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View showView = inflater.inflate(R.layout.item_show, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(showView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        //TODO Add in all the other bits required to populate the show tile thing

        // Get the data model based on position
        Show show = getItem(position);

        // Set item views based on your views and data model
        TextView showName = holder.seriesnameView;
        showName.setText(show.getShowName());
        TextView nextEpisode = holder.TextViewShowListNextEpisode;
        //need to build the show array up with eppisodes attached to the show before adding complex data.
        nextEpisode.setText(show.getMyEpisodeID());

    }
}
