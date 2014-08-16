package co.arcs.groove.basking.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class FadingTextView extends FrameLayout {

    private TextView view1;
    private TextView view2;
    private boolean showingFirst;
    private int animationDuration;
    private Interpolator animationInterpolator;

    public FadingTextView(Context context) {
        this(context, null);
    }

    public FadingTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public FadingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        FrameLayout.LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        this.showingFirst = true;
        this.animationDuration = context.getResources()
                .getInteger(android.R.integer.config_mediumAnimTime);
        this.animationInterpolator = new AccelerateDecelerateInterpolator();

        this.view1 = new TextView(context, attrs, defStyle);
        this.view2 = new TextView(context, attrs, defStyle);
        view2.setAlpha(0.0f);
        addView(view1, lp);
        addView(view2, lp);
    }

    public void setText(CharSequence text) {
        TextView toHide = showingFirst ? view1 : view2;
        TextView toShow = showingFirst ? view2 : view1;

        toHide.setScaleX(1.0f);
        toHide.setScaleY(1.0f);
        toHide.animate()
                .alpha(0.0f)
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setInterpolator(animationInterpolator)
                .setDuration(animationDuration)
                .start();

        toShow.setText(text);
        toShow.setScaleX(0.0f);
        toShow.setScaleY(0.0f);
        toShow.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setInterpolator(animationInterpolator)
                .setDuration(animationDuration)
                .start();
        showingFirst = !showingFirst;
    }

    public void setText(int id) {
        setText(getContext().getResources().getString(id));
    }
}
