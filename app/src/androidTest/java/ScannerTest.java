import android.os.Environment;
import android.test.AndroidTestCase;

import java.util.concurrent.ExecutionException;

import co.arcs.groove.basking.MediaScanner;

public class ScannerTest extends AndroidTestCase {

    public void testScan() throws InterruptedException, ExecutionException {
        new MediaScanner(getContext(), Environment.getExternalStorageDirectory()).scan().get();
    }
}
