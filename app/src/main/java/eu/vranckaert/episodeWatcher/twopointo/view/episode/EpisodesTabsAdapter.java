package eu.vranckaert.episodeWatcher.twopointo.view.episode;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import eu.vranckaert.android.viewholder.AbstractViewHolder;
import eu.vranckaert.episodeWatcher.R;
import eu.vranckaert.episodeWatcher.domain.Episode;
import eu.vranckaert.episodeWatcher.enums.EpisodeType;
import eu.vranckaert.episodeWatcher.twopointo.view.episode.EpisodesListAdapter.EpisodesListListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 04/11/15
 * Time: 07:58
 *
 * @author Dirk Vranckaert
 */
public class EpisodesTabsAdapter extends PagerAdapter {
    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final EpisodesListListener mListener;

    private List<Episode> mEpisodesToWatch = new ArrayList<>();
    private List<Episode> mEpisodesToAcquire = new ArrayList<>();

    private EpisodesListView mEpisodesToWatchView;
    private EpisodesListView mEpisodesToAcquireView;

    public EpisodesTabsAdapter(Context context, EpisodesListListener listener) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mListener = listener;
    }

    public void setEpisodesToWatch(List<Episode> episodes) {
        mEpisodesToWatch.clear();
        mEpisodesToWatch.addAll(episodes);
        if (mEpisodesToWatchView != null) {
            mEpisodesToWatchView.setEpisodes(mEpisodesToWatch);
        }
        notifyDataSetChanged();
    }

    public void setEpisodesToAcquire(List<Episode> episodes) {
        mEpisodesToAcquire.clear();
        mEpisodesToAcquire.addAll(episodes);
        if (mEpisodesToAcquireView != null) {
            mEpisodesToAcquireView.setEpisodes(mEpisodesToAcquire);
        }
        notifyDataSetChanged();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return mContext.getString(R.string.watchhome, mEpisodesToWatch.size());
        } else {
            return mContext.getString(R.string.acquirehome, mEpisodesToAcquire.size());
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        EpisodesListView view;
        if (position == 0) {
            if (mEpisodesToWatchView == null) {
                mEpisodesToWatchView = new EpisodesListView(mLayoutInflater, container, EpisodeType.EPISODES_TO_WATCH, mListener);
                mEpisodesToWatchView.setEpisodes(mEpisodesToWatch);
            }
            view = mEpisodesToWatchView;
        } else {
            if (mEpisodesToAcquireView == null) {
                mEpisodesToAcquireView = new EpisodesListView(mLayoutInflater, container, EpisodeType.EPISODES_TO_ACQUIRE, mListener);
                mEpisodesToAcquireView.setEpisodes(mEpisodesToAcquire);
            }
            view = mEpisodesToAcquireView;
        }

        if (view != null) {
            container.addView(view.getView(), position);
        }
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(((AbstractViewHolder) object).getView());
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.getTag() != null && view.getTag().equals(object);
    }

    public void onPageChanged(int position) {
        if (position == 0) {
            mEpisodesToAcquireView.cancelContextualActionbar();
        } else {
            mEpisodesToWatchView.cancelContextualActionbar();
        }
    }

    public void onEpisodesMarkedAcquired(List<Episode> episodes) {
        mEpisodesToAcquire.removeAll(episodes);
        mEpisodesToWatch.addAll(episodes);
        notifyDataSetChanged();

        mEpisodesToAcquireView.removeAllEpisodes(episodes);
        mEpisodesToWatchView.addAllEpisodes(episodes);
    }

    public void onEpisodesMarkedWatched(List<Episode> episodes) {
        mEpisodesToAcquire.removeAll(episodes);
        mEpisodesToWatch.removeAll(episodes);
        notifyDataSetChanged();

        mEpisodesToAcquireView.removeAllEpisodes(episodes);
        mEpisodesToWatchView.removeAllEpisodes(episodes);
    }

    public void onEpisodesNotMarkedAcquired(List<Episode> episodes) {
        mEpisodesToAcquire.addAll(episodes);
        notifyDataSetChanged();
        mEpisodesToAcquireView.addAllEpisodes(episodes);
    }

    public void onEpisodesNotMarkedWatched(List<Episode> episodes) {
        mEpisodesToWatch.addAll(episodes);
        notifyDataSetChanged();
        mEpisodesToWatchView.addAllEpisodes(episodes);
    }

    public RecyclerView getListFor(int page) {
        if (page == 0 && mEpisodesToWatchView != null) {
            return mEpisodesToWatchView.getList();
        } else if (page == 1 && mEpisodesToAcquireView != null) {
            return mEpisodesToAcquireView.getList();
        }
        return null;
    }
}
