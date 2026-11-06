package com.tv.screenguard;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

/** Shared native styling for the child-mode settings and parent password page. */
final class ChildStyle {
    static final int PAPER = 0xff101d22;
    static final int INK = 0xfff7faf2;
    static final int MUTED = 0xffdbe4dd;
    static final int LINE = 0x70d8f0dd;
    static final int GREEN = 0xffd8fa9a;
    static final int FOCUS = 0xffdbff9c;

    static final int PANEL = 0xcb19262b;
    private ChildStyle() {}

    static float settingsScale(Context context) {
        if (!(context instanceof ChildSettingsActivity)) return 1f;
        // TV boxes commonly report 240dpi for 1080p (720dp tall). Match the same
        // visual proportions as a 540dp TV viewport without changing system density.
        return Math.max(1f, Math.min(1.4f,
                context.getResources().getConfiguration().screenHeightDp / 540f));
    }

    static float sp(Context context, float value) { return value * settingsScale(context); }

    static int dp(Context context, int value) {
        return Math.round(value * settingsScale(context) * context.getResources().getDisplayMetrics().density);
    }

    static TextView text(Context context, CharSequence label, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(label);
        text.setTextColor(color);
        text.setTextSize(sp(context, size));
        text.setFontFeatureSettings("kern");
        if (bold) text.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return text;
    }

    static GradientDrawable shape(Context context, int fill, int edge, int radius, int stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(context, radius));
        if (stroke != 0) shape.setStroke(dp(context, stroke), edge);
        return shape;
    }

    static LayerDrawable raised(Context context, int fill, int edge, int radius) {
        return new LayerDrawable(new android.graphics.drawable.Drawable[]{shape(context, fill, edge, radius, 1)});
    }

    static Button button(Context context, String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(sp(context, 14));
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setTextColor(INK);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(context, 8), 0, dp(context, 8), dp(context, 2));
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, shape(context, 0xff3c503d, FOCUS, 11, 3));
        states.addState(new int[]{android.R.attr.state_selected}, shape(context, 0xff314d40, 0xffa3c48e, 11, 1));
        states.addState(new int[]{android.R.attr.state_pressed}, shape(context, 0xff35463e, FOCUS, 11, 1));
        states.addState(new int[0], shape(context, primary ? 0xff244b3d : 0xff2b383c, 0xff647875, 11, 1));
        button.setBackground(states);
        button.setStateListAnimator(null);
        return button;
    }

    /** Persistent page-wide primary action with a high-contrast remote focus ring. */
    static Button protectionButton(Context context) {
        Button button = button(context, "", true);
        button.setTextSize(sp(context, 19));
        button.setLetterSpacing(0.025f);
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_focused},
                shape(context, 0xff3c503d, FOCUS, 14, 3));
        background.addState(new int[]{android.R.attr.state_pressed},
                shape(context, 0xff35463e, FOCUS, 14, 2));
        background.addState(new int[0], shape(context, 0xff244b3d, 0xffa3c48e, 14, 2));
        button.setBackground(background);
        button.setTextColor(INK);
        return button;
    }

    static LinearLayout page(Context context) {
        LinearLayout page = new LinearLayout(context);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{PAPER, 0xff20332f}));
        page.setPadding(dp(context, 36), dp(context, 20), dp(context, 36), dp(context, 14));
        return page;
    }

    static void header(LinearLayout page) {
        Context context = page.getContext();
        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(context);
        logo.setImageDrawable(context.getApplicationInfo().loadIcon(context.getPackageManager()));
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setContentDescription(context.getString(R.string.app_name));
        header.addView(logo, new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)));
        TextView title = text(context, context.getString(R.string.child_lock_brand), 17, INK, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, dp(context, 32), 1);
        titleParams.setMarginStart(dp(context, 10));
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, titleParams);
        header.addView(text(context, context.getString(R.string.child_lock_parent_settings), 11, MUTED, true));
        page.addView(header);
        android.view.View rule = new android.view.View(context);
        rule.setBackgroundColor(LINE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(context, 1));
        params.topMargin = dp(context, 13);
        page.addView(rule, params);
    }
}
