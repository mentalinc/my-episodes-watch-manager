package nz.mentalinc.watcher.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.domain.Episode;

/**
 * Adapter for the expandable episode listing.
 *
 * <p>Groups are immutable value objects and are compared with {@link DiffUtil} so the list is only
 * rebound when the content actually changed. Views are recycled through view holders instead of
 * being recreated for every row.</p>
 */
public class EpisodeExpandableAdapter extends BaseExpandableListAdapter {

    private final LayoutInflater inflater;
    private final List<Group> groups = new ArrayList<>();

    public EpisodeExpandableAdapter(Context context, List<Group> initialGroups) {
        this.inflater = LayoutInflater.from(context);
        if (initialGroups != null) {
            groups.addAll(initialGroups);
        }
    }

    /**
     * Replaces the current content, notifying the list only when the groups actually changed.
     */
    public void submitList(List<Group> newGroups) {
        List<Group> incoming = newGroups == null
                ? Collections.emptyList()
                : new ArrayList<>(newGroups);

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(new GroupDiffCallback(groups, incoming), true);
        ChangeCounter changeCounter = new ChangeCounter();
        diffResult.dispatchUpdatesTo(changeCounter);

        groups.clear();
        groups.addAll(incoming);

        if (changeCounter.changed) {
            notifyDataSetChanged();
        }
    }

    public String getGroupKey(int groupPosition) {
        return groups.get(groupPosition).key;
    }

    public String getGroupTitle(int groupPosition) {
        return groups.get(groupPosition).title;
    }

    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return groups.get(groupPosition).episodes.size();
    }

    @Override
    public Group getGroup(int groupPosition) {
        return groups.get(groupPosition);
    }

    @Override
    public Episode getChild(int groupPosition, int childPosition) {
        return groups.get(groupPosition).episodes.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groups.get(groupPosition).key.hashCode();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return episodeId(getChild(groupPosition, childPosition), childPosition);
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {
        GroupViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.episode_listing_tab_row_group, parent, false);
            holder = new GroupViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (GroupViewHolder) convertView.getTag();
        }

        holder.title.setText(getGroupTitle(groupPosition));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        ChildViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.episode_listing_tab_row_child, parent, false);
            holder = new ChildViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ChildViewHolder) convertView.getTag();
        }

        Episode episode = getChild(groupPosition, childPosition);
        holder.title.setText(episode.getShowName());
        holder.detail.setText("S" + episode.getSeasonString() + "E"
                + episode.getEpisodeString() + " - " + episode.getName());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private static long episodeId(Episode episode, int fallback) {
        if (episode.getMyEpisodeID() != null) {
            return episode.getMyEpisodeID().hashCode();
        }
        return Objects.hash(episode.getShowName(), episode.getSeason(), episode.getEpisode(),
                fallback);
    }

    /**
     * A single group in the expandable list: a stable key, the rendered title and its episodes.
     */
    public static final class Group {
        private final String key;
        private final String title;
        private final List<Episode> episodes;

        public Group(String key, String title, List<Episode> episodes) {
            this.key = key;
            this.title = title;
            // Defensive copy: the source lists are mutable and reused between refreshes.
            this.episodes = episodes == null
                    ? Collections.emptyList()
                    : List.copyOf(episodes);
        }

        public List<Episode> getEpisodes() {
            return episodes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Group group)) {
                return false;
            }
            return Objects.equals(key, group.key)
                    && Objects.equals(title, group.title)
                    && episodesEqual(episodes, group.episodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, title, episodesHash(episodes));
        }
    }

    private static boolean episodesEqual(List<Episode> first, List<Episode> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (!episodeEquals(first.get(i), second.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean episodeEquals(Episode first, Episode second) {
        return Objects.equals(first.getShowName(), second.getShowName())
                && Objects.equals(first.getName(), second.getName())
                && first.getSeason() == second.getSeason()
                && first.getEpisode() == second.getEpisode()
                && first.getAirDate() == second.getAirDate()
                && Objects.equals(first.getMyEpisodeID(), second.getMyEpisodeID());
    }

    private static int episodesHash(List<Episode> episodes) {
        int result = 1;
        for (Episode episode : episodes) {
            result = 31 * result + Objects.hash(episode.getShowName(), episode.getName(),
                    episode.getSeason(), episode.getEpisode(), episode.getAirDate(),
                    episode.getMyEpisodeID());
        }
        return result;
    }

    private static final class GroupDiffCallback extends DiffUtil.Callback {
        private final List<Group> oldGroups;
        private final List<Group> newGroups;

        private GroupDiffCallback(List<Group> oldGroups, List<Group> newGroups) {
            this.oldGroups = oldGroups;
            this.newGroups = newGroups;
        }

        @Override
        public int getOldListSize() {
            return oldGroups.size();
        }

        @Override
        public int getNewListSize() {
            return newGroups.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldGroups.get(oldItemPosition).key,
                    newGroups.get(newItemPosition).key);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldGroups.get(oldItemPosition).equals(newGroups.get(newItemPosition));
        }
    }

    /**
     * Counts the changes reported by {@link DiffUtil} so the adapter can skip a full rebind when
     * nothing changed. {@link BaseExpandableListAdapter} only supports a full refresh, so the
     * individual callbacks cannot be forwarded to it.
     */
    private static final class ChangeCounter implements ListUpdateCallback {
        private boolean changed;

        @Override
        public void onInserted(int position, int count) {
            changed |= count > 0;
        }

        @Override
        public void onRemoved(int position, int count) {
            changed |= count > 0;
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            changed = true;
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
            changed |= count > 0;
        }
    }

    private static final class GroupViewHolder {
        private final TextView title;

        private GroupViewHolder(View itemView) {
            title = itemView.findViewById(R.id.episodeRowTitle);
        }
    }

    private static final class ChildViewHolder {
        private final TextView title;
        private final TextView detail;

        private ChildViewHolder(View itemView) {
            title = itemView.findViewById(R.id.episodeRowChildTitle);
            detail = itemView.findViewById(R.id.episodeRowChildDetail);
        }
    }
}
