package com.abara.fireclip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.abara.fireclip.adapter.MainViewPagerAdapter;
import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.HistoryClip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.realm.Realm;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private SharedPreferences preferences;

    private AppCompatTextView drawerDisplayName, drawerEmail;
    private FirebaseUser clipUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle("FireClip (Beta)");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        clipUser = FirebaseAuth.getInstance().getCurrentUser();

        setupViewPager();
        initNavigationDrawer();

        drawerDisplayName = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_displayname);
        drawerEmail = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_email);

        drawerDisplayName.setText(clipUser.getDisplayName());
        drawerEmail.setText(clipUser.getEmail());

    }

    private void initNavigationDrawer() {

        drawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.main_navigation_view);

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout,
                R.string.open_navigation_drawer, R.string.close_navigation_drawer) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };

        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });

        drawerToggle.setDrawerIndicatorEnabled(true);
        drawerLayout.addDrawerListener(drawerToggle);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return false;
            }
        });
        navigationView.setNavigationItemSelectedListener(this);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ic_action_logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.title_signout_dialog);
                builder.setMessage(R.string.message_signout_dialog);
                builder.setPositiveButton("Signout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        signOut();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return true;
            case R.id.ic_action_feedback:
                Intent feedbackIntent = new Intent(MainActivity.this, FeedbackActivity.class);
                startActivity(feedbackIntent);
                return true;
        }
        return drawerToggle.onOptionsItemSelected(item);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();

        Realm realm = Realm.getDefaultInstance();
        realm.removeAllChangeListeners();
        realm.beginTransaction();
        realm.delete(HistoryClip.class);
        realm.commitTransaction();
        realm.close();

        preferences.edit().clear().commit();

        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        stopService(new Intent(this, ClipboardService.class));
        finish();
        Toast.makeText(this, "You are signed out!", Toast.LENGTH_SHORT).show();
    }

    private void setupViewPager() {

        ViewPager viewPager = (ViewPager) findViewById(R.id.main_content);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.main_tabs);

        MainViewPagerAdapter adapter = new MainViewPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_content_fav);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_content_home);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_content_history);
        tabLayout.getTabAt(1).select();

    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ic_nav_action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                drawerLayout.closeDrawer(navigationView);
                break;
        }
        return true;
    }
}
