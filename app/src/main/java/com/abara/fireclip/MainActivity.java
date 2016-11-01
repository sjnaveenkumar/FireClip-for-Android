package com.abara.fireclip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
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

import com.abara.fireclip.fragment.FavouritesFragment;
import com.abara.fireclip.fragment.HistoryFragment;
import com.abara.fireclip.fragment.HomeFragment;
import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.HistoryClip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import io.realm.Realm;

/*
* MainActivity having favourites, home and history.
* */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;

    private SharedPreferences preferences;
    private Fragment currentFragment;

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

        setupBottomNavigation();
        initNavigationDrawer();

        drawerDisplayName = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_displayname);
        drawerEmail = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_email);

        drawerDisplayName.setText(clipUser.getDisplayName());
        drawerEmail.setText(clipUser.getEmail());

    }

    /*
    * Setup navigation drawer.
    * */
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

    /*
    * Set option item selections.
    * */
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
        }
        return drawerToggle.onOptionsItemSelected(item);
    }

    /*
    * Signout, delete local history.
    * */
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

    /*
    * Setup the bottom navigation.
    * */
    private void setupBottomNavigation() {

        bottomNavigation = (BottomNavigationView) findViewById(R.id.main_bottom_nav);
        MenuItem homeMenuItem = bottomNavigation.getMenu().getItem(1);
        homeMenuItem.setChecked(true);
        bottomNavigation.getMenu().getItem(0).setChecked(false);
        bottomNavigation.getMenu().getItem(2).setChecked(false);

        bottomNavigation.setOnNavigationItemSelectedListener(this);
        onNavigationItemSelected(homeMenuItem);

    }

    /*
    * Close drawer and then navigate back.
    * */
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

    /*
    * Set navigation item selections.
    * */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ic_nav_action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                drawerLayout.closeDrawer(navigationView);
                break;
            case R.id.ic_nav_action_tell:
                Intent inviteIntent = new Intent();
                inviteIntent.setAction(Intent.ACTION_SEND);
                inviteIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.tell_friend_message));
                inviteIntent.setType("text/plain");
                startActivity(Intent.createChooser(inviteIntent, getResources().getString(R.string.tell_friend_title_chooser)));
                drawerLayout.closeDrawer(navigationView);
                Toast.makeText(this, "Sharing with...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.ic_nav_action_feedback:
                drawerLayout.closeDrawer(navigationView);
                Intent feedbackIntent = new Intent(MainActivity.this, FeedbackActivity.class);
                startActivity(feedbackIntent);
                break;
            // Bottom navigation items
            case R.id.action_bottom_nav_fav:
                if (!(currentFragment instanceof FavouritesFragment)) {
                    currentFragment = new FavouritesFragment();
                    showFragment();
                }
                break;
            case R.id.action_bottom_nav_home:
                if (!(currentFragment instanceof HomeFragment)) {
                    currentFragment = new HomeFragment();
                    showFragment();
                }
                break;
            case R.id.action_bottom_nav_history:
                if (!(currentFragment instanceof HistoryFragment)) {
                    currentFragment = new HistoryFragment();
                    showFragment();
                }
                break;
        }
        return true;
    }

    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_bottom, R.anim.disappear)
                .replace(R.id.main_content, currentFragment)
                .commit();
    }

}
