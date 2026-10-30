package nz.mentalinc.watcher.preferences;

import android.content.Context;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;

/**
 * An {@link EditTextPreference} that renders any URL in its summary as a clickable link.
 */
public class LinkSummaryEditTextPreference extends EditTextPreference {

    public LinkSummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public LinkSummaryEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LinkSummaryEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LinkSummaryEditTextPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        TextView summaryView = (TextView) holder.findViewById(android.R.id.summary);
        if (summaryView == null) {
            return;
        }

        CharSequence summary = getSummary();
        if (summary != null) {
            SpannableString linkedSummary = new SpannableString(summary);
            Linkify.addLinks(linkedSummary, Linkify.WEB_URLS);
            summaryView.setText(linkedSummary);
        }

        // The default preference layout caps summaries at four lines, which can hide the URL.
        summaryView.setMaxLines(Integer.MAX_VALUE);
        summaryView.setMovementMethod(LinkMovementMethod.getInstance());
        summaryView.setLinksClickable(true);
    }
}
