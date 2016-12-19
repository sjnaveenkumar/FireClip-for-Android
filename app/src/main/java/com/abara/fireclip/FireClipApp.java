package com.abara.fireclip;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

/**
 * FireClip application class.
 * <p>
 * Created by abara on 09/09/16.
 */
public class FireClipApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .name("clipdata.realm")
                .schemaVersion(1)
                .migration(new RealmMigration() {
                    @Override
                    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
                        // No migration required for now!
                    }
                })
                .build();
        Realm.setDefaultConfiguration(configuration);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
