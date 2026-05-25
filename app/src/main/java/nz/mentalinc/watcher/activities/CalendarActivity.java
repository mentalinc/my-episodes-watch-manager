package nz.mentalinc.watcher.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.NumberPicker;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import nz.mentalinc.watcher.R;
import nz.mentalinc.watcher.constants.ActivityConstants;
import nz.mentalinc.watcher.controllers.EpisodesController;
import nz.mentalinc.watcher.domain.Episode;
import nz.mentalinc.watcher.enums.EpisodeType;

public class CalendarActivity extends AppCompatActivity {

    private List<CalendarEpisode> allEpisodes = new ArrayList<>();
    private CalendarEpisodeAdapter episodeAdapter;
    private CalendarDayAdapter dayAdapter;
    private TextView emptyText;
    private TextView monthLabel;
    private Calendar currentCalendar = Calendar.getInstance();
    private Calendar selectedDate = Calendar.getInstance();
    private Set<String> episodeDateSet = new HashSet<>();
    private Set<String> watchDateSet = new HashSet<>();
    private Set<String> acquireDateSet = new HashSet<>();
    private Set<String> comingDateSet = new HashSet<>();

    private boolean filterWatchEnabled = true;
    private boolean filterAcquireEnabled = true;
    private boolean filterComingEnabled = true;

    private final NavigationBarView.OnItemSelectedListener navigationItemSelectedListener =
            new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int id = item.getItemId();
                    if (id == R.id.barHome) {
                        finish();
                        return true;
                    } else if (id == R.id.barWatch) {
                        Intent intent = new Intent(getApplicationContext(), ShowListingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE,
                                EpisodeType.EPISODES_TO_WATCH);
                        startActivity(intent);
                        return true;
                    } else if (id == R.id.barAcquire) {
                        Intent intent = new Intent(getApplicationContext(), ShowListingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE,
                                EpisodeType.EPISODES_TO_ACQUIRE);
                        startActivity(intent);
                        return true;
                    } else if (id == R.id.barComing) {
                        Intent intent = new Intent(getApplicationContext(), ShowListingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        intent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE,
                                EpisodeType.EPISODES_COMING);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String themeSetting = prefs.getString("ThemeSetting", "0");
        switch (themeSetting) {
            case "0":
                setTheme(R.style.ThemeDayNight);
                break;
            case "1":
                setTheme(R.style.ThemeLight);
                break;
            case "2":
                setTheme(R.style.ThemeDark);
                break;
        }

        setContentView(R.layout.activity_calendar);

        MaterialToolbar toolbar = findViewById(R.id.topAppBarCalendar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        monthLabel = findViewById(R.id.currentMonthLabel);
        monthLabel.setOnClickListener(v -> showMonthYearPicker());

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY)
                        && Math.abs(diffX) > SWIPE_THRESHOLD
                        && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        currentCalendar.add(Calendar.MONTH, -1);
                    } else {
                        currentCalendar.add(Calendar.MONTH, 1);
                    }
                    refreshCalendarGrid();
                    return true;
                }
                return false;
            }
        });
        monthLabel.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        emptyText = findViewById(R.id.emptyCalendarText);

        RecyclerView calendarGrid = findViewById(R.id.calendarGrid);
        calendarGrid.setLayoutManager(new GridLayoutManager(this, 7));
        dayAdapter = new CalendarDayAdapter();
        calendarGrid.setAdapter(dayAdapter);

        RecyclerView episodeList = findViewById(R.id.calendarEpisodeList);
        episodeList.setLayoutManager(new LinearLayoutManager(this));
        episodeAdapter = new CalendarEpisodeAdapter();
        episodeList.setAdapter(episodeAdapter);

        MaterialButton prevBtn = findViewById(R.id.btnPrevMonth);
        MaterialButton nextBtn = findViewById(R.id.btnNextMonth);
        MaterialButton todayBtn = findViewById(R.id.btnToday);
        prevBtn.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            refreshCalendarGrid();
        });
        nextBtn.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            refreshCalendarGrid();
        });
        todayBtn.setOnClickListener(v -> {
            currentCalendar = Calendar.getInstance();
            selectedDate = Calendar.getInstance();
            refreshCalendarGrid();
        });

        MaterialButton filterWatch = findViewById(R.id.filterWatch);
        MaterialButton filterAcquire = findViewById(R.id.filterAcquire);
        MaterialButton filterComing = findViewById(R.id.filterComing);
        filterWatch.setTag(true);
        filterAcquire.setTag(true);
        filterComing.setTag(true);
        View.OnClickListener filterListener = view -> {
            MaterialButton btn = (MaterialButton) view;
            boolean enabled = !(boolean) btn.getTag();
            btn.setTag(enabled);
            if (enabled) {
                btn.setAlpha(1.0f);
            } else {
                btn.setAlpha(0.4f);
            }
            if (view.getId() == R.id.filterWatch) filterWatchEnabled = enabled;
            else if (view.getId() == R.id.filterAcquire) filterAcquireEnabled = enabled;
            else filterComingEnabled = enabled;
            filterEpisodesForDate(selectedDate.getTime());
        };
        filterWatch.setOnClickListener(filterListener);
        filterAcquire.setOnClickListener(filterListener);
        filterComing.setOnClickListener(filterListener);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationCalendar);
        bottomNav.setOnItemSelectedListener(navigationItemSelectedListener);

        loadAllEpisodes();
        refreshCalendarGrid();
    }

    private void loadAllEpisodes() {
        allEpisodes.clear();
        episodeDateSet.clear();
        watchDateSet.clear();
        acquireDateSet.clear();
        comingDateSet.clear();
        EpisodesController controller = EpisodesController.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Episode ep : controller.getEpisodes(EpisodeType.EPISODES_TO_WATCH)) {
            allEpisodes.add(new CalendarEpisode(ep, EpisodeType.EPISODES_TO_WATCH));
            if (ep.getAirDate() != null) {
                String dateStr = sdf.format(ep.getAirDate());
                episodeDateSet.add(dateStr);
                watchDateSet.add(dateStr);
            }
        }
        for (Episode ep : controller.getEpisodes(EpisodeType.EPISODES_TO_ACQUIRE)) {
            allEpisodes.add(new CalendarEpisode(ep, EpisodeType.EPISODES_TO_ACQUIRE));
            if (ep.getAirDate() != null) {
                String dateStr = sdf.format(ep.getAirDate());
                episodeDateSet.add(dateStr);
                acquireDateSet.add(dateStr);
            }
        }
        for (Episode ep : controller.getEpisodes(EpisodeType.EPISODES_COMING)) {
            allEpisodes.add(new CalendarEpisode(ep, EpisodeType.EPISODES_COMING));
            if (ep.getAirDate() != null) {
                String dateStr = sdf.format(ep.getAirDate());
                episodeDateSet.add(dateStr);
                comingDateSet.add(dateStr);
            }
        }
    }

    private void refreshCalendarGrid() {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH);

        Calendar temp = Calendar.getInstance();
        temp.set(year, month, 1);
        int firstDayOfWeek = temp.get(Calendar.DAY_OF_WEEK) - 1;
        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH);
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        Map<String, Integer> dayCounts = new HashMap<>();
        for (CalendarEpisode ce : allEpisodes) {
            if (ce.episode.getAirDate() != null) {
                String dateStr = sdf.format(ce.episode.getAirDate());
                dayCounts.merge(dateStr, 1, Integer::sum);
            }
        }

        int monthTotal = 0;

        List<CalendarDay> days = new ArrayList<>();
        for (int i = 0; i < firstDayOfWeek; i++) {
            days.add(new CalendarDay(0, false, false, false, false, 0, false));
        }
        for (int day = 1; day <= daysInMonth; day++) {
            temp.set(year, month, day);
            String dateStr = sdf.format(temp.getTime());
            boolean hasWatch = watchDateSet.contains(dateStr);
            boolean hasAcquire = acquireDateSet.contains(dateStr);
            boolean hasComing = comingDateSet.contains(dateStr);
            int dayCount = dayCounts.getOrDefault(dateStr, 0);
            monthTotal += dayCount;
            boolean isToday = year == todayYear && month == todayMonth && day == todayDay;
            boolean isSelected = selectedDate.get(Calendar.YEAR) == year
                    && selectedDate.get(Calendar.MONTH) == month
                    && selectedDate.get(Calendar.DAY_OF_MONTH) == day;
            days.add(new CalendarDay(day, hasWatch, hasAcquire, hasComing, isToday, dayCount, isSelected));
        }

        String baseMonth = new SimpleDateFormat("MMMM yyyy", Locale.US).format(currentCalendar.getTime());
        monthLabel.setText(baseMonth + " (" + monthTotal + ")");

        dayAdapter.submitList(days);

        if (selectedDate.get(Calendar.MONTH) == month && selectedDate.get(Calendar.YEAR) == year) {
            filterEpisodesForDate(selectedDate.getTime());
        } else {
            Calendar firstOfMonth = Calendar.getInstance();
            firstOfMonth.set(year, month, 1);
            selectedDate.set(year, month, 1);
            filterEpisodesForDate(firstOfMonth.getTime());
        }
    }

    private void filterEpisodesForDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String targetDate = sdf.format(date);

        List<CalendarEpisode> filtered = new ArrayList<>();
        for (CalendarEpisode ce : allEpisodes) {
            if (ce.episode.getAirDate() != null) {
                String episodeDate = sdf.format(ce.episode.getAirDate());
                if (episodeDate.equals(targetDate)) {
                    switch (ce.type) {
                        case EPISODES_TO_WATCH:
                            if (filterWatchEnabled) filtered.add(ce);
                            break;
                        case EPISODES_TO_ACQUIRE:
                            if (filterAcquireEnabled) filtered.add(ce);
                            break;
                        case EPISODES_COMING:
                            if (filterComingEnabled) filtered.add(ce);
                            break;
                    }
                }
            }
        }

        episodeAdapter.submitList(filtered);

        if (filtered.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
        } else {
            emptyText.setVisibility(View.GONE);
        }
    }

    private void showMonthYearPicker() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_month_year_picker, null);
        NumberPicker monthPicker = dialogView.findViewById(R.id.monthPicker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.yearPicker);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        });
        monthPicker.setValue(currentCalendar.get(Calendar.MONTH));

        yearPicker.setMinValue(1970);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(currentCalendar.get(Calendar.YEAR));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.selectMonthYear)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (d, w) -> {
                    currentCalendar.set(yearPicker.getValue(), monthPicker.getValue(), 1);
                    refreshCalendarGrid();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class CalendarDay {
        final int day;
        final boolean hasWatch;
        final boolean hasAcquire;
        final boolean hasComing;
        final boolean isToday;
        final int totalCount;
        boolean isSelected;

        CalendarDay(int day, boolean hasWatch, boolean hasAcquire, boolean hasComing, boolean isToday, int totalCount, boolean isSelected) {
            this.day = day;
            this.hasWatch = hasWatch;
            this.hasAcquire = hasAcquire;
            this.hasComing = hasComing;
            this.isToday = isToday;
            this.totalCount = totalCount;
            this.isSelected = isSelected;
        }
    }

    private class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.ViewHolder> {
        private List<CalendarDay> items = new ArrayList<>();

        void submitList(List<CalendarDay> list) {
            items = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarDay cd = items.get(position);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CalendarActivity.this);
            String themeSetting = prefs.getString("ThemeSetting", "0");
            boolean isDarkTheme;
            switch (themeSetting) {
                case "2":
                    isDarkTheme = true;
                    break;
                case "1":
                    isDarkTheme = false;
                    break;
                default:
                    int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                    isDarkTheme = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
                    break;
            }

            int primaryColor = ContextCompat.getColor(CalendarActivity.this, R.color.colorPrimary);
            int accentColor = ContextCompat.getColor(CalendarActivity.this, R.color.colorAccent);
            int textColorPrimary = ContextCompat.getColor(CalendarActivity.this,
                    isDarkTheme ? R.color.on_text_white : R.color.on_text_black);
            int textColorWhite = ContextCompat.getColor(CalendarActivity.this, R.color.on_text_white);
            int watchColor = ContextCompat.getColor(CalendarActivity.this, R.color.calendarWatchColor);
            int acquireColor = ContextCompat.getColor(CalendarActivity.this, R.color.calendarAcquireColor);
            int comingColor = ContextCompat.getColor(CalendarActivity.this, R.color.calendarComingColor);

            if (cd.day == 0) {
                holder.dayText.setText("");
                holder.itemView.setBackground(null);
                holder.itemView.setOnClickListener(null);
                holder.dayText.setTextColor(textColorPrimary);
                holder.dotWatch.setVisibility(View.GONE);
                holder.dotAcquire.setVisibility(View.GONE);
                holder.dotComing.setVisibility(View.GONE);
                return;
            }

            holder.dayText.setText(String.valueOf(cd.day));

            holder.dotWatch.setVisibility(cd.hasWatch ? View.VISIBLE : View.INVISIBLE);
            holder.dotAcquire.setVisibility(cd.hasAcquire ? View.VISIBLE : View.INVISIBLE);
            holder.dotComing.setVisibility(cd.hasComing ? View.VISIBLE : View.INVISIBLE);
            holder.dotWatch.getBackground().setTint(watchColor);
            holder.dotAcquire.getBackground().setTint(acquireColor);
            holder.dotComing.getBackground().setTint(comingColor);

            if (cd.totalCount > 0) {
                holder.dayCountBadge.setVisibility(View.VISIBLE);
                holder.dayCountBadge.setText(String.valueOf(cd.totalCount));
            } else {
                holder.dayCountBadge.setVisibility(View.GONE);
            }

            if (cd.isSelected) {
                GradientDrawable filled = new GradientDrawable();
                filled.setShape(GradientDrawable.RECTANGLE);
                filled.setColor(primaryColor);
                filled.setCornerRadius(24);
                holder.itemView.setBackground(filled);
                holder.dayText.setTextColor(textColorWhite);
            } else if (cd.isToday) {
                GradientDrawable filled = new GradientDrawable();
                filled.setShape(GradientDrawable.RECTANGLE);
                filled.setColor(accentColor);
                filled.setCornerRadius(24);
                holder.itemView.setBackground(filled);
                holder.dayText.setTextColor(textColorWhite);
            } else {
                holder.itemView.setBackground(null);
                holder.dayText.setTextColor(textColorPrimary);
            }

            holder.itemView.setOnClickListener(v -> {
                int prevSelectedPos = -1;
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).isSelected) {
                        prevSelectedPos = i;
                        items.get(i).isSelected = false;
                        break;
                    }
                }
                cd.isSelected = true;
                notifyItemChanged(position);
                if (prevSelectedPos >= 0) {
                    notifyItemChanged(prevSelectedPos);
                }

                int year = currentCalendar.get(Calendar.YEAR);
                int month = currentCalendar.get(Calendar.MONTH);
                selectedDate.set(year, month, cd.day);
                filterEpisodesForDate(selectedDate.getTime());
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView dayText;
            final View dotWatch;
            final View dotAcquire;
            final View dotComing;
            final TextView dayCountBadge;

            ViewHolder(View itemView) {
                super(itemView);
                dayText = itemView.findViewById(R.id.calendarDayText);
                dotWatch = itemView.findViewById(R.id.dotWatch);
                dotAcquire = itemView.findViewById(R.id.dotAcquire);
                dotComing = itemView.findViewById(R.id.dotComing);
                dayCountBadge = itemView.findViewById(R.id.dayCountBadge);
            }
        }
    }

    private static class CalendarEpisode {
        final Episode episode;
        final EpisodeType type;

        CalendarEpisode(Episode episode, EpisodeType type) {
            this.episode = episode;
            this.type = type;
        }
    }

    private static class CalendarEpisodeAdapter extends RecyclerView.Adapter<CalendarEpisodeAdapter.ViewHolder> {
        private List<CalendarEpisode> items = new ArrayList<>();

        void submitList(List<CalendarEpisode> list) {
            items = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_episode, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CalendarEpisode ce = items.get(position);
            Episode ep = ce.episode;

            holder.showName.setText(ep.getShowName());
            holder.episodeInfo.setText(String.format(Locale.US, "S%02dE%02d - %s",
                    ep.getSeason(), ep.getEpisode(), ep.getName()));

            String typeLabel;
            int colorRes;
            switch (ce.type) {
                case EPISODES_TO_WATCH:
                    typeLabel = holder.itemView.getContext().getString(R.string.watchLabel);
                    colorRes = R.color.calendarWatchColor;
                    break;
                case EPISODES_TO_ACQUIRE:
                    typeLabel = holder.itemView.getContext().getString(R.string.acquireLabel);
                    colorRes = R.color.calendarAcquireColor;
                    break;
                case EPISODES_COMING:
                default:
                    typeLabel = holder.itemView.getContext().getString(R.string.comingLabel);
                    colorRes = R.color.calendarComingColor;
                    break;
            }
            holder.episodeType.setText(typeLabel);
            holder.typeIndicator.setBackgroundColor(
                    holder.itemView.getContext().getColor(colorRes));

            holder.itemView.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, EpisodeDetailsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE, ep);
                intent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_SHOW_MYEPISODE_ID, ep.getMyEpisodeID());
                intent.putExtra(ActivityConstants.EXTRA_BUNDLE_VAR_EPISODE_TYPE, ce.type);
                intent.putExtra("Title", ep.getShowName());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final View typeIndicator;
            final TextView showName;
            final TextView episodeInfo;
            final TextView episodeType;

            ViewHolder(View itemView) {
                super(itemView);
                typeIndicator = itemView.findViewById(R.id.typeIndicator);
                showName = itemView.findViewById(R.id.calendarEpisodeShowName);
                episodeInfo = itemView.findViewById(R.id.calendarEpisodeInfo);
                episodeType = itemView.findViewById(R.id.calendarEpisodeType);
            }
        }
    }
}
