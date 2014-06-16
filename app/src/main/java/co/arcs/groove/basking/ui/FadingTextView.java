package co.arcs.groove.basking.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

public class FadingTextView extends FrameLayout {

    private TextView view1;
    private TextView view2;
    private boolean showingFirst;
    private int animationDuration;

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

        this.view1 = new TextView(context, attrs, defStyle);
        this.view2 = new TextView(context, attrs, defStyle);
        view2.setAlpha(0.0f);
        addView(view1, lp);
        addView(view2, lp);
    }

    public void setText(CharSequence text) {
        TextView toHide = showingFirst ? view1 : view2;
        TextView toShow = showingFirst ? view2 : view1;
        toShow.setText(text);
        toShow.animate().alpha(1.0f);
        toHide.animate().alpha(0.0f).setDuration(animationDuration).start();
        showingFirst = !showingFirst;
    }

    public void setText(int id) {
        setText(getContext().getResources().getString(id));
    }
}
