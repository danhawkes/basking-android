package co.arcs.groove.basking;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.io.File;

public class MediaScanner {

    private static final String[] MIME_TYPES = new String[]{"audio/mpeg"};

    private final Context context;
    private final File path;

    public MediaScanner(Context context, File path) {
        this.context = context;
        this.path = path;
    }

    public ListenableFuture<Void> scan() {

        final SettableFuture<Void> future = SettableFuture.create();

        MediaScannerConnection.scanFile(context,
                new String[]{path.getAbsolutePath()},
                MIME_TYPES,
                new MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {
                    }

                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        if (uri != null) {
                            future.set(null);
                        } else {
                            future.setException(new Exception(
                                    "Media scanner failed to scan " + path));
                        }
                    }
                });

        return future;
    }
}
