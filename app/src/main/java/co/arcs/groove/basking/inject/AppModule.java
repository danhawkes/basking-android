package co.arcs.groove.basking.inject;

import android.app.Application;
import android.content.Context;

import java.lang.annotation.Retention;

import javax.inject.Qualifier;
import javax.inject.Singleton;

import co.arcs.groove.basking.App;
import co.arcs.groove.basking.BackgroundSyncScheduler;
import co.arcs.groove.basking.pref.AppPreferences;
import co.arcs.groove.basking.ui.SyncFragment;
import dagger.Module;
import dagger.Provides;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Module(injects = {App.class, BackgroundSyncScheduler.class, AppPreferences.class, SyncFragment.class})
public class AppModule {

    @Qualifier
    @Retention(RUNTIME)
    public @interface ApplicationContext {
    }

    private final Application application;

    public AppModule(App application) {
        this.application = application;
    }

    @Provides
    @Singleton
    @ApplicationContext
    Context provideApplicationContext() {
        return application;
    }

    @Provides
    @Singleton
    AppPreferences provideAppPreferences() {
        return new AppPreferences(application);
    }
}
