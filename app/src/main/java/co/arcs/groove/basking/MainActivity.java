package co.arcs.groove.basking;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.jfeinstein.jazzyviewpager.JazzyViewPager;
import com.jfeinstein.jazzyviewpager.JazzyViewPager.TransitionEffect;

import java.io.File;

import co.arcs.android.fileselector.FileSelectorActivity;
import co.arcs.groove.basking.pref.PreferenceFragment;
import co.arcs.groove.basking.pref.SyncDirPreference;

public class MainActivity extends Activity implements SyncDirPreference.Listener {

    private SectionPagerAdapter sectionPagerAdapter;
    private JazzyViewPager viewPager;

    private static final int REQUEST_SYNC_DIR = 1;
    private SyncDirPreference syncDirPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = (JazzyViewPager) findViewById(R.id.viewPager);
        viewPager.setTransitionEffect(TransitionEffect.RotateDown);

        sectionPagerAdapter = new SectionPagerAdapter(getFragmentManager(), viewPager);
        viewPager.setAdapter(sectionPagerAdapter);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() != 0) {
            viewPager.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SYNC_DIR && resultCode == Activity.RESULT_OK) {
            File file = (File) data.getSerializableExtra(FileSelectorActivity.EXTRA_PICKED_FILE);
            syncDirPreference.onDirectorySelected(file);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClickSyncDirPreference(SyncDirPreference preference) {
        this.syncDirPreference = preference;
        Intent i = new Intent(this, FileSelectorActivity.class);
        i.putExtra(FileSelectorActivity.EXTRA_STR_SELECTION_TYPE,
                FileSelectorActivity.TYPE_DIRECTORY);
        startActivityForResult(i, REQUEST_SYNC_DIR);
    }

    private static class SectionPagerAdapter extends FragmentPagerAdapter {

        private final JazzyViewPager viewPager;

        public SectionPagerAdapter(FragmentManager fm, JazzyViewPager viewPager) {
            super(fm);
            this.viewPager = viewPager;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Object obj = super.instantiateItem(container, position);
            viewPager.setObjectForPosition(obj, position);
            return obj;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new MainFragment();
            } else if (position == 1) {
                return new PreferenceFragment();
            } else {
                return null;
            }
        }
    }
}
