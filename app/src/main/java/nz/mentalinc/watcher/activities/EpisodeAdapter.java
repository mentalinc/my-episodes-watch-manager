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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.service.EpisodeRuntime;

public class EpisodeAdapter extends ListAdapter<Episode, EpisodeAdapter.ViewHolder> {
    private static final String LOG_TAG = EpisodeAdapter.class.getSimpleName();
    private List<Episode> episodeList;
    private final Map<String, EpisodeRuntime> runtimeMap;

    public EpisodeAdapter(List<Episode> episodes, Map<String, EpisodeRuntime> runtimeMap) {
        super(DIFF_CALLBACK);
        this.episodeList = episodes != null ? new ArrayList<>(episodes) : new ArrayList<>();
        this.runtimeMap = runtimeMap;
    }


    public void addMoreEpisodes(List<Episode> newEpisodes) {
        episodeList.addAll(newEpisodes);
        submitList(episodeList); // DiffUtil takes care of the check
    }

    public static final DiffUtil.ItemCallback<Episode> DIFF_CALLBACK = new DiffUtil.ItemCallback<Episode>() {
        @Override
        public boolean areItemsTheSame(Episode oldItem, Episode newItem) {
            return oldItem.getShowName().equals(newItem.getShowName());
        }

        @Override
        public boolean areContentsTheSame(Episode oldItem, Episode newItem) {
            return (oldItem.getShowName().equals(newItem.getShowName()) && oldItem.getMyEpisodeID().equals(newItem.getMyEpisodeID()));
        }
    };


    public class ViewHolder extends RecyclerView.ViewHolder { //implements View.OnLongClickListener{
        //
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView showName;
        public TextView episodetime;
        public TextView episodeNumber;
        public TextView episodeName;
        public TextView episodeRuntime;
        public ImageView showposter;


        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            showName = itemView.findViewById(R.id.textViewActivityShow);
            episodetime = itemView.findViewById(R.id.textViewActivityInfo);
            episodeNumber = itemView.findViewById(R.id.textViewActivityEpisode);
            episodeName = itemView.findViewById(R.id.textViewActivityEpisode);
            episodeRuntime = itemView.findViewById(R.id.textViewActivityRunTime);

            showposter = itemView.findViewById(R.id.imageViewActivityPoster);
        }
    }


    @Override
    public EpisodeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View episodeView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        EpisodeAdapter.ViewHolder viewHolder = new EpisodeAdapter.ViewHolder(episodeView);
        return viewHolder;

    }

    @Override
    public void onBindViewHolder(EpisodeAdapter.ViewHolder holder, int position) {

        //TODO Add in all the other bits required to populate the show tile thing
        Episode episode = getItem(position);

        Log.e(LOG_TAG, "Episode Bindholder: " + episode.getName());

        try {

            // Set item views based on your views and data model
            TextView showName = holder.showName;
            String displayName = episode.getShowName();
            if (displayName != null && displayName.startsWith("Error mins - ")) {
                displayName = displayName.substring("Error mins - ".length());
            }
            showName.setText(displayName);
            TextView episodeName = holder.episodeName;
            //need to build the show array up with episodes attached to the show before adding complex data.
            String seasonNumber = episode.getSeasonString();
            String episodeNumber = episode.getEpisodeString();
            String episodeNameFull = episode.getName();
            String episodeFullNumbering = "S" + seasonNumber + "E" + episodeNumber + " " + episodeNameFull;

            episodeName.setText(episodeFullNumbering);

            TextView episodeAirTime = holder.episodetime;
            episodeAirTime.setText(DateFormat.getDateInstance().format(episode.getAirDate()));

            TextView textViewEpisodeShowsRunTime = holder.episodeRuntime;
            String myepisodeID = episode.getMyEpisodeID();
            EpisodeRuntime showRuntime = runtimeMap.get(myepisodeID);
            if (showRuntime == null) {
                String message = "Problem reading runtime for " + episode.getShowName();
                Log.e(LOG_TAG, message);
                return;
            }

            String showRuntimeText = showRuntime.getShowRuntime() + " Mins";
            textViewEpisodeShowsRunTime.setText(showRuntimeText);

            String showImageURL = showRuntime.getShowImageURL();
            ImageView showPoster = holder.showposter;

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.placeholder);
            requestOptions.error(R.drawable.error);

            Glide.with(holder.showposter)
                    .load(showImageURL)
                    .placeholder(R.drawable.placeholder)
                    .into(showPoster);

        } catch (NullPointerException e) {
            String message = "Problem reading runtime for " + episode.getShowName();
            Log.e(LOG_TAG, message);
        }
    }
}
