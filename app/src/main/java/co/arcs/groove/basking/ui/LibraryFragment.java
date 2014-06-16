package co.arcs.groove.basking.ui;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Audio.Playlists;
import android.provider.MediaStore.Audio.Playlists.Members;
import android.provider.MediaStore.Audio.PlaylistsColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;

import butterknife.ButterKnife;
import butterknife.InjectView;
import co.arcs.groove.basking.R;

public class LibraryFragment extends Fragment {

    private static final int LOADER_LIST_ADAPTER = 23580726;

    private CursorAdapter adapter;
    @InjectView(R.id.list) ListView listView;
    @InjectView(R.id.listEmpty) View listEmptyView;

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        adapter = new SongAdapter(getActivity());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onItemClickListener);
        listView.setEmptyView(listEmptyView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_LIST_ADAPTER, null, listAdapterLoaderCallbacks);
    }

    private final OnItemClickListener onItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CursorWrapper cursor = (CursorWrapper) adapter.getItem(position);

            String data = cursor.getString(cursor.getColumnIndex(Members.DATA));
            Uri uri = Uri.fromFile(new File(data));

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri, "audio/*");
            i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            getActivity().startActivity(i);
        }
    };

    private static class SongAdapter extends CursorAdapter {

        private final LayoutInflater inflater;

        public SongAdapter(Context context) {
            super(context, null, 0);
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = inflater.inflate(R.layout.view_library_list_item, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.textView1 = (TextView) view.findViewById(android.R.id.text1);
            holder.textView2 = (TextView) view.findViewById(android.R.id.text2);
            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            String title = cursor.getString(cursor.getColumnIndex(Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndex(Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(Media.ALBUM));
            if ((album.length() <= 2) && (album.equals("-") || album.equals(".") || album.equals(
                    "[]"))) {
                album = "Unknown";
            }
            if (album.contains("grooveshark") || album.contains("Grooveshark")) {
                album = "Unknown";
            }

            ViewHolder holder = (ViewHolder) view.getTag();
            holder.textView1.setText(title);
            holder.textView2.setText(artist + " - " + album);
        }

        static class ViewHolder {

            TextView textView1;
            TextView textView2;
        }
    }

    private final LoaderCallbacks<Cursor> listAdapterLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        private long getPlaylistId() {

            String[] projection = new String[]{BaseColumns._ID};
            String selection = PlaylistsColumns.NAME + " = ?";
            String selectionArgs[] = new String[]{"GS Collection"};

            Cursor query = getActivity().getContentResolver()
                    .query(Playlists.EXTERNAL_CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            null);

            if (query.moveToFirst()) {
                return query.getLong(0);
            } else {
                return -1;
            }
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            long playlistId = getPlaylistId();

            String[] projection = new String[]{Members._ID,
                                               AudioColumns.DATA,
                                               AudioColumns.ARTIST,
                                               AudioColumns.TITLE,
                                               AudioColumns.ALBUM,};

            return new CursorLoader(getActivity(),
                    Playlists.Members.getContentUri("external", playlistId),
                    projection,
                    null,
                    null,
                    Members.PLAY_ORDER);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
            adapter.swapCursor(c);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> arg0) {
            adapter.swapCursor(null);
        }
    };
}
