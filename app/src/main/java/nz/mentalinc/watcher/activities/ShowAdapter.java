package nz.mentalinc.watcher.activities;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.utils.DateUtil;

public class ShowAdapter extends ListAdapter<Show, ShowAdapter.ViewHolder> {
    private static final String LOG_TAG = ShowAdapter.class.getSimpleName();
    private static final RequestOptions POSTER_REQUEST_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error);
    private final Map<String, EpisodeRuntime> runtimeMap;

    public ShowAdapter(List<Show> shows, Map<String, EpisodeRuntime> runtimeMap) {
        super(DIFF_CALLBACK);
        this.runtimeMap = runtimeMap;
        submitList(shows != null ? new ArrayList<>(shows) : new ArrayList<>());
    }

    public void addMoreShows(List<Show> newShows) {
        if (newShows == null || newShows.isEmpty()) {
            return;
        }
        // ListAdapter short-circuits submitList when the same instance is passed in, so build a
        // new list instead of mutating the one currently held by the adapter.
        List<Show> updatedShows = new ArrayList<>(getCurrentList());
        updatedShows.addAll(newShows);
        submitList(updatedShows);
    }

    public static final DiffUtil.ItemCallback<Show> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(Show oldItem, Show newItem) {
            return Objects.equals(oldItem.getShowName(), newItem.getShowName())
                    && Objects.equals(oldItem.getMyEpisodeID(), newItem.getMyEpisodeID());
        }

        @Override
        public boolean areContentsTheSame(Show oldItem, Show newItem) {
            if (!areItemsTheSame(oldItem, newItem)
                    || oldItem.getNumberEpisodes() != newItem.getNumberEpisodes()) {
                return false;
            }
            Episode oldEpisode = oldItem.getFirstEpisode();
            Episode newEpisode = newItem.getFirstEpisode();
            return Objects.equals(oldEpisode.getName(), newEpisode.getName())
                    && oldEpisode.getAirDate() == newEpisode.getAirDate()
                    && Objects.equals(oldItem.getRunTime(), newItem.getRunTime());
        }
    };

    public class ViewHolder extends RecyclerView.ViewHolder { //implements View.OnClickListener{
        //
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView seriesnameView;
        public TextView TextViewShowListNextEpisode;
        public TextView episodetime;
        public TextView textViewShowsRemaining;
        public TextView textViewShowsRunTime;
        public ImageView setWatchedButton;
        public ImageView showposter;


        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            seriesnameView = itemView.findViewById(R.id.seriesname);
            TextViewShowListNextEpisode = itemView.findViewById(R.id.TextViewShowListNextEpisode);
            episodetime = itemView.findViewById(R.id.episodetime);
            textViewShowsRemaining = itemView.findViewById(R.id.textViewShowsRemaining);
            textViewShowsRunTime = itemView.findViewById(R.id.textViewShowsRunTime);
            setWatchedButton = itemView.findViewById(R.id.imageViewShowsSetWatched);

            showposter = itemView.findViewById(R.id.showposter);

        }
    }


    @Override
    public ShowAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View showView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_show, parent, false);
        ViewHolder viewHolder = new ViewHolder(showView);
        return viewHolder;
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        Show show = getItem(position);

        try {

            // Set item views based on your views and data model
            TextView showName = holder.seriesnameView;
            String displayName = show.getShowName();
            if (displayName.startsWith("Error mins - ")) {
                displayName = displayName.substring("Error mins - ".length());
            }
            showName.setText(displayName);
            TextView nextEpisode = holder.TextViewShowListNextEpisode;
            //need to build the show array up with episodes attached to the show before adding complex data.
            Episode firstEpisode = show.getFirstEpisode();
            String seasonNumber = firstEpisode.getSeasonString();
            String episodeNumber = firstEpisode.getEpisodeString();
            String episodeName = firstEpisode.getName();
            String episodeFullNumbering = "S" + seasonNumber + "E" + episodeNumber + " " + episodeName;
            nextEpisode.setText(episodeFullNumbering);

            TextView episodeAirTime = holder.episodetime;
            episodeAirTime.setText(DateUtil.formatAirDateMedium(firstEpisode.getAirDate(),
                    holder.itemView.getContext().getString(R.string.unknownAirDate)));

            TextView textViewShowsRemaining = holder.textViewShowsRemaining;
            String episodesRemaining;

            LocalDate airDate = DateUtil.toLocalDate(firstEpisode.getAirDate());
            if (airDate != null && airDate.isBefore(LocalDate.now())) {
                episodesRemaining = show.getNumberEpisodes() + " episodes remaining";
            } else {
                episodesRemaining = show.getNumberEpisodes() + " episodes coming";
            }

            textViewShowsRemaining.setText(episodesRemaining);

            TextView textViewShowsRunTime = holder.textViewShowsRunTime;

            String myepisodeID = firstEpisode.getMyEpisodeID();

            EpisodeRuntime showRuntime = runtimeMap.get(myepisodeID);
            if (showRuntime != null) {
                String showRuntimeText = showRuntime.getShowRuntime() + " Mins";
                textViewShowsRunTime.setText(showRuntimeText);

                String showImageURL = showRuntime.getShowImageURL();
                ImageView showPoster = holder.showposter;

                Glide.with(holder.showposter)
                        .load(showImageURL)
                        .apply(POSTER_REQUEST_OPTIONS)
                        .into(showPoster);
            }

        } catch (NullPointerException e) {
            String message = "Problem reading runtime for " + show.getShowName();
            Log.e(LOG_TAG, message);
        }
    }
}
