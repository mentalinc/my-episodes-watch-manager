package nz.mentalinc.watcher.activities;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.service.EpisodeRuntime;
import nz.mentalinc.watcher.utils.DateUtil;

public class EpisodeAdapter extends ListAdapter<Episode, EpisodeAdapter.ViewHolder> {
    public interface OnEpisodeMarkListener {
        void onMarkWatched(Episode episode);
        void onMarkAcquired(Episode episode);
        void onMarkUnacquired(Episode episode);
    }

    private static final String LOG_TAG = EpisodeAdapter.class.getSimpleName();
    private static final RequestOptions POSTER_REQUEST_OPTIONS = new RequestOptions()
            .placeholder(R.drawable.placeholder)
            .error(R.drawable.error);
    private final Map<String, EpisodeRuntime> runtimeMap;
    private OnEpisodeMarkListener markListener;
    private boolean markControlsVisible = true;
    private boolean acquiredCheckedByDefault = false;
    private boolean comingMode = false;

    public EpisodeAdapter(List<Episode> episodes, Map<String, EpisodeRuntime> runtimeMap) {
        super(DIFF_CALLBACK);
        this.runtimeMap = runtimeMap;
        submitList(episodes != null ? new ArrayList<>(episodes) : new ArrayList<>());
    }

    public void setOnEpisodeMarkListener(OnEpisodeMarkListener listener) {
        this.markListener = listener;
    }

    public void setMarkControlsVisible(boolean visible) {
        this.markControlsVisible = visible;
    }

    public void setAcquiredCheckedByDefault(boolean checked) {
        this.acquiredCheckedByDefault = checked;
    }

    public void setComingMode(boolean coming) {
        this.comingMode = coming;
    }

    private boolean isToday(long airDateMillis) {
        if (airDateMillis <= 0) return false;
        java.util.Calendar c1 = java.util.Calendar.getInstance();
        c1.setTimeInMillis(airDateMillis);
        java.util.Calendar c2 = java.util.Calendar.getInstance();
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                && c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR);
    }


    public void addMoreEpisodes(List<Episode> newEpisodes) {
        if (newEpisodes == null || newEpisodes.isEmpty()) {
            return;
        }
        // ListAdapter short-circuits submitList when the same instance is passed in, so build a
        // new list instead of mutating the one currently held by the adapter.
        List<Episode> updatedEpisodes = new ArrayList<>(getCurrentList());
        updatedEpisodes.addAll(newEpisodes);
        submitList(updatedEpisodes);
    }

    public static final DiffUtil.ItemCallback<Episode> DIFF_CALLBACK = new DiffUtil.ItemCallback<Episode>() {
        @Override
        public boolean areItemsTheSame(Episode oldItem, Episode newItem) {
            return Objects.equals(oldItem.getShowName(), newItem.getShowName())
                    && Objects.equals(oldItem.getMyEpisodeID(), newItem.getMyEpisodeID())
                    && oldItem.getSeason() == newItem.getSeason()
                    && oldItem.getEpisode() == newItem.getEpisode();
        }

        @Override
        public boolean areContentsTheSame(Episode oldItem, Episode newItem) {
            return areItemsTheSame(oldItem, newItem)
                    && Objects.equals(oldItem.getName(), newItem.getName())
                    && oldItem.getAirDate() == newItem.getAirDate();
        }
    };


    public class ViewHolder extends RecyclerView.ViewHolder { //implements View.OnLongClickListener{
        //
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        private TextView showName;
        private TextView episodetime;
        private TextView episodeNumber;
        private TextView episodeName;
        private TextView episodeRuntime;
        private ImageView showposter;
        private MaterialCheckBox checkboxWatched;
        private MaterialCheckBox checkboxAcquired;


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
            checkboxWatched = itemView.findViewById(R.id.checkboxWatched);
            checkboxAcquired = itemView.findViewById(R.id.checkboxAcquired);
        }
    }


    @Override
    public EpisodeAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View episodeView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        return new EpisodeAdapter.ViewHolder(episodeView);
    }

    @Override
    public void onBindViewHolder(EpisodeAdapter.ViewHolder holder, int position) {

        Episode episode = getItem(position);

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
            episodeAirTime.setText(DateUtil.formatAirDateMedium(episode.getAirDate(),
                    holder.itemView.getContext().getString(R.string.unknownAirDate)));

            TextView textViewEpisodeShowsRunTime = holder.episodeRuntime;
            String myepisodeID = episode.getMyEpisodeID();
            EpisodeRuntime showRuntime = runtimeMap.get(myepisodeID);
            if (showRuntime != null) {
                String showRuntimeText = showRuntime.getShowRuntime() + " Mins";
                textViewEpisodeShowsRunTime.setText(showRuntimeText);

                String showImageURL = showRuntime.getShowImageURL();
                ImageView showPoster = holder.showposter;

                Glide.with(holder.showposter)
                        .load(showImageURL)
                        .apply(POSTER_REQUEST_OPTIONS)
                        .into(showPoster);
            }

        } catch (NullPointerException e) {
            String message = "Problem reading runtime for " + episode.getShowName();
            Log.e(LOG_TAG, message);
        }

        // Per-episode mark controls.
        boolean showControls = markControlsVisible && !(comingMode && !isToday(episode.getAirDate()));
        int vis = showControls ? View.VISIBLE : View.GONE;
        if (holder.checkboxWatched != null) holder.checkboxWatched.setVisibility(vis);
        if (holder.checkboxAcquired != null) holder.checkboxAcquired.setVisibility(vis);
        if (showControls && holder.checkboxWatched != null) {
            holder.checkboxWatched.setClickable(true);
            holder.checkboxWatched.setFocusable(true);
            holder.checkboxWatched.setOnCheckedChangeListener(null);
            holder.checkboxWatched.setChecked(false);
            holder.checkboxWatched.setOnCheckedChangeListener((buttonView, isChecked) -> {
                android.widget.Toast.makeText(buttonView.getContext(), "Watched changed: " + isChecked, android.widget.Toast.LENGTH_SHORT).show();
                if (isChecked && markListener != null) {
                    markListener.onMarkWatched(episode);
                }
            });
        }
        if (holder.checkboxAcquired != null) {
            holder.checkboxAcquired.setClickable(true);
            holder.checkboxAcquired.setFocusable(true);
            holder.checkboxAcquired.setOnCheckedChangeListener(null);
            holder.checkboxAcquired.setChecked(acquiredCheckedByDefault);
            holder.checkboxAcquired.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (markListener != null) {
                    if (isChecked) {
                        markListener.onMarkAcquired(episode);
                    } else {
                        markListener.onMarkUnacquired(episode);
                    }
                }
            });
        }
    }
}
