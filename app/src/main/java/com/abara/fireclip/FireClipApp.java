package com.abara.fireclip;

import android.app.Application;

import io.realm.DynamicRealm;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmMigration;

/**
 * Created by abara on 09/09/16.
 */

/*
* FireClip application class.
* */
public class FireClipApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RealmConfiguration configuration = new RealmConfiguration.Builder(getApplicationContext())
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
    }
}
