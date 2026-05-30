package nz.mentalinc.watcher.activities;

import static nz.mentalinc.watcher.constants.MyEpisodeConstants.CONTEXT;

import android.content.Context;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.domain.Show;
import nz.mentalinc.watcher.service.EpisodeRuntime;

public class ShowAdapter extends ListAdapter<Show, ShowAdapter.ViewHolder> {
    private static final String LOG_TAG = ShowAdapter.class.getSimpleName();
    private List<Show> showsList;
    Context context = CONTEXT.getApplicationContext();
    private final Map<String, EpisodeRuntime> runtimeMap;

    public ShowAdapter(List<Show> shows, Map<String, EpisodeRuntime> runtimeMap) {
        super(DIFF_CALLBACK);
        this.showsList = shows != null ? new ArrayList<>(shows) : new ArrayList<>();
        this.runtimeMap = runtimeMap;
        submitList(this.showsList);
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
            return (oldItem.getShowName().equals(newItem.getShowName()) && oldItem.getMyEpisodeID().equals(newItem.getMyEpisodeID()));
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
            String seasonNumber = show.getFirstEpisode().getSeasonString();
            String episodeNumber = show.getFirstEpisode().getEpisodeString();
            String episodeName = show.getFirstEpisode().getName();
            String episodeFullNumbering = "S" + seasonNumber + "E" + episodeNumber + " " + episodeName;
            nextEpisode.setText(episodeFullNumbering);

            TextView episodeAirTime = holder.episodetime;
            episodeAirTime.setText(DateFormat.getDateInstance().format(new Date(show.getFirstEpisode().getAirDate())));

            TextView textViewShowsRemaining = holder.textViewShowsRemaining;
            String episodesRemaining;

            Date today = Calendar.getInstance().getTime();
            if (new Date(show.getFirstEpisode().getAirDate()).before(today)) {
                episodesRemaining = show.getNumberEpisodes() + " episodes remaining";
            } else {
                episodesRemaining = show.getNumberEpisodes() + " episodes coming";
            }

            textViewShowsRemaining.setText(episodesRemaining);

            TextView textViewShowsRunTime = holder.textViewShowsRunTime;

            Episode nextEpisodeToWatch = show.getFirstEpisode();
            String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

            EpisodeRuntime showRuntime = runtimeMap.get(myepisodeID);
            if (showRuntime != null) {
                String showRuntimeText = showRuntime.getShowRuntime() + " Mins";
                textViewShowsRunTime.setText(showRuntimeText);

                String showImageURL = showRuntime.getShowImageURL();
                ImageView showPoster = holder.showposter;

                RequestOptions requestOptions = new RequestOptions();
                requestOptions.placeholder(R.drawable.placeholder);
                requestOptions.error(R.drawable.error);

                Glide.with(holder.showposter)
                        .load(showImageURL)
                        .apply(requestOptions)
                        .into(showPoster);
            }

        } catch (NullPointerException e) {
            String message = "Problem reading runtime for " + show.getShowName();
            Log.e(LOG_TAG, message);
        }
    }
}