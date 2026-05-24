package nz.mentalinc.episodeWatcher.activities;

import static nz.mentalinc.episodeWatcher.constants.MyEpisodeConstants.CONTEXT;

import android.content.Context;
import android.text.util.Linkify;
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

import java.util.List;

import nz.mentalinc.episodeWatcher.R;
import nz.mentalinc.episodeWatcher.constants.MyEpisodeConstants;
import nz.mentalinc.episodeWatcher.database.AppDatabase;
import nz.mentalinc.episodeWatcher.database.SeriesDAO;
import nz.mentalinc.episodeWatcher.domain.Episode;
import nz.mentalinc.episodeWatcher.domain.Show;
import nz.mentalinc.episodeWatcher.service.EpisodeRuntime;


public class ShowDetailAdapter extends ListAdapter<Show, ShowDetailAdapter.ViewHolder> {

    private static final String LOG_TAG = ShowDetailAdapter.class.getSimpleName();
    private List<Show> showsList;
    Context context = CONTEXT.getApplicationContext();


    public ShowDetailAdapter(List<Show> shows) {
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
            return (oldItem.getShowName().equals(newItem.getShowName()) && oldItem.getMyEpisodeID().equals(newItem.getMyEpisodeID()));
        }
    };


    public class ViewHolder extends RecyclerView.ViewHolder { //implements View.OnClickListener{

        public TextView seriesnameView;
        public TextView tvMazeShowWebsite;
        public TextView episodetime;
        public TextView textViewShowsRemaining;
        public TextView officialShowDetailWebsite;
        public TextView tvMazeShowDetailSummary;

        public ImageView showposter;


        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            seriesnameView = itemView.findViewById(R.id.ShowName);
            tvMazeShowWebsite = itemView.findViewById(R.id.tvMazeShowWebsite);
            episodetime = itemView.findViewById(R.id.showDetailRuntime);
            textViewShowsRemaining = itemView.findViewById(R.id.textViewShowsRemaining);
            officialShowDetailWebsite = itemView.findViewById(R.id.officialShowDetailWebsite);
            tvMazeShowDetailSummary = itemView.findViewById(R.id.tvMazeShowDetailSummary);


            showposter = itemView.findViewById(R.id.showDetailposter);

        }
    }


    @Override
    public ShowDetailAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.w(LOG_TAG, "Item_show_detail from ShowDetail Adapater");
        View showView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_show_detail, parent, false);
        ViewHolder viewHolder = new ViewHolder(showView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ShowDetailAdapter.ViewHolder holder, int position) {

        //TODO Add in all the other bits required to populate the show tile thing
        Show show = getItem(position);
        showsList.add(show);


        try {

            // Set item views based on your views and data model
            TextView showName = holder.seriesnameView;
            String displayName = show.getShowName();
            if (displayName.startsWith("Error mins - ")) {
                displayName = displayName.substring("Error mins - ".length());
            }
            showName.setText(displayName);
            /* nextEpisode = holder.TextViewShowListNextEpisode;
            //need to build the show array up with episodes attached to the show before adding complex data.
            String seasonNumber = show.getFirstEpisode().getSeasonString();
            String episodeNumber = show.getFirstEpisode().getEpisodeString();
            String episodeName = show.getFirstEpisode().getName();
            String episodeFullNumbering = "S" + seasonNumber + "E" + episodeNumber + " " + episodeName;
            nextEpisode.setText(episodeFullNumbering);*/

            //TextView episodeAirTime = holder.episodeAirDateTime;
            // episodeAirTime.setText(DateFormat.getDateInstance().format(show.getFirstEpisode().getAirDate()));


            TextView textViewShowsRemaining = holder.textViewShowsRemaining;
            String episodesRemaining;

           /* Date today = Calendar.getInstance().getTime();
            if(show.getFirstEpisode().getAirDate().after(today)){
                episodesRemaining = show.getNumberEpisodes() + " episodes coming";
            }else{
                episodesRemaining = show.getNumberEpisodes() + " episodes remaining";
            }

            textViewShowsRemaining.setText(episodesRemaining);
            */


            Episode nextEpisodeToWatch = show.getFirstEpisode();
            String myepisodeID = nextEpisodeToWatch.getMyEpisodeID();

            AppDatabase database = AppDatabase.getInstance(nz.mentalinc.episodeWatcher.activities.HomeActivity.getContext().getApplicationContext());

            SeriesDAO seriesDAO = database.getSeriesDAO();
            EpisodeRuntime showRuntime = seriesDAO.getEpisodeRuntimeWithMyEpsId(myepisodeID);


            TextView textViewShowDetail = holder.tvMazeShowDetailSummary;
            textViewShowDetail.setText(showRuntime.getShowSummary());

            TextView textViewShowDetailWebsite = holder.officialShowDetailWebsite;
            textViewShowDetailWebsite.setText(showRuntime.getOfficialSite());
            Linkify.addLinks(textViewShowDetailWebsite, Linkify.WEB_URLS);

            TextView textViewTvMazeWebsite = holder.tvMazeShowWebsite;
            textViewTvMazeWebsite.setText(showRuntime.getShowURL());
            Linkify.addLinks(textViewTvMazeWebsite, Linkify.WEB_URLS);


            TextView textViewShowsRunTime = holder.episodetime;
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

        } catch (NullPointerException e) {
            String message = "Problem reading runtime for " + show.getShowName();
            Log.e(LOG_TAG, message);
        }
    }
}
