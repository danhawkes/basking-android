package co.arcs.groove.basking;

import org.acra.ACRA;
import org.acra.annotation.ReportsCrashes;

import co.arcs.groove.basking.inject.AppModule;
import co.arcs.groove.basking.pref.AppPreferences;
import co.arcs.groove.basking.pref.AppPreferences.Keys;
import dagger.ObjectGraph;

@ReportsCrashes(
        formKey = "",
        formUri = "https://arcs.cloudant.com/acra-basking/_design/acra-storage/_update/report",
        excludeMatchingSharedPreferencesKeys = {Keys.STR_USERNAME, Keys.STR_PASSWORD},
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = "bosequiestanieropningeri",
        formUriBasicAuthPassword = "p2XDLB6jNdGrAfQSwhWb2MGl")
public class App extends android.app.Application {

    private ObjectGraph graph;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
        ACRA.getErrorReporter().setEnabled(!BuildConfig.DEBUG);

        this.graph = ObjectGraph.create(new AppModule(this));

        // Initialise default preferences, and re-schedule alarms
        graph.get(AppPreferences.class).initDefaults();
        graph.get(BackgroundSyncScheduler.class).init();
    }

    public void inject(Object object) {
        graph.inject(object);
    }
}
