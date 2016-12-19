package com.abara.fireclip;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
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

import com.abara.fireclip.fragment.HistoryFragment;
import com.abara.fireclip.fragment.HomeFragment;
import com.abara.fireclip.fragment.PinsFragment;
import com.abara.fireclip.service.ClipboardService;
import com.abara.fireclip.util.AndroidUtils;
import com.abara.fireclip.util.FireClipUtils;
import com.abara.fireclip.util.HistoryClip;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.Realm;

/**
 * Activity to show Pins, Home and Recent history.
 * <p>
 * Created by abara on 24/09/16.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigation;
    private Toolbar toolbar;

    private SharedPreferences preferences;
    private Fragment currentFragment;

    private AppCompatTextView drawerDisplayName, drawerEmail, drawerNoPhotoText;
    private CircleImageView drawerUserPhoto;
    private FirebaseUser clipUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        preferences = AndroidUtils.getPreference(this);
        clipUser = FirebaseAuth.getInstance().getCurrentUser();

        setupBottomNavigation();
        initNavigationDrawer();

        drawerDisplayName = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_displayname);
        drawerEmail = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_email);
        drawerNoPhotoText = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_no_photo);
        drawerUserPhoto = (CircleImageView) navigationView.getHeaderView(0).findViewById(R.id.drawer_main_user_photo);

        drawerDisplayName.setText(clipUser.getDisplayName());
        drawerEmail.setText(clipUser.getEmail());

        if (clipUser.getPhotoUrl() == null) {
            drawerNoPhotoText.setText(clipUser.getDisplayName().toUpperCase().substring(0, 1));
            drawerUserPhoto.setVisibility(View.GONE);
        } else {
            Glide.with(this).load(clipUser.getPhotoUrl()).fitCenter().into(drawerUserPhoto);
            drawerNoPhotoText.setVisibility(View.GONE);
        }

        AndroidUtils.askStoragePermission(this);

    }

    /**
     * Setup navigation drawer.
     */
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

    /**
     * Inform configuration changes to the navigation drawer.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    /**
     * Handle logout and drawer ham icon.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_signout:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.dialog_signout_title);
                builder.setMessage(R.string.dialog_signout_message);
                builder.setPositiveButton(R.string.action_singout, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        signOut();
                    }
                });
                builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
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

    /**
     * Sign out, delete local history.
     */
    private void signOut() {

        FireClipUtils.unSubscribe();
        stopService(new Intent(this, ClipboardService.class));
        FirebaseAuth.getInstance().signOut();

        Realm realm = Realm.getDefaultInstance();
        realm.removeAllChangeListeners();
        realm.beginTransaction();
        realm.delete(HistoryClip.class);
        realm.commitTransaction();
        realm.close();

        preferences.edit().clear().apply();

        Intent intent = new Intent(MainActivity.this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        Toast.makeText(this, "You are signed out!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Setup the bottom navigation.
     */
    private void setupBottomNavigation() {

        bottomNavigation = (BottomNavigationView) findViewById(R.id.main_bottom_nav);
        MenuItem homeMenuItem = bottomNavigation.getMenu().getItem(1);
        homeMenuItem.setChecked(true);

        bottomNavigation.setOnNavigationItemSelectedListener(this);
        onNavigationItemSelected(homeMenuItem);

    }

    /**
     * Close drawer (if opened) and then navigate back.
     **/
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView);
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Create options menu.
     **/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Set navigation item selections.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                drawerLayout.closeDrawer(navigationView);
                break;
            case R.id.nav_action_tell:
                Intent inviteIntent = new Intent();
                inviteIntent.setAction(Intent.ACTION_SEND);
                inviteIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.tell_friend_message));
                inviteIntent.setType("text/plain");
                if (inviteIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(inviteIntent, getResources().getString(R.string.tell_friend_title_chooser)));
                    drawerLayout.closeDrawer(navigationView);
                    Toast.makeText(this, "Share with...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No apps to share with!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.nav_action_feedback:
                drawerLayout.closeDrawer(navigationView);
                Intent feedbackIntent = new Intent(MainActivity.this, FeedbackActivity.class);
                startActivity(feedbackIntent);
                break;
            /**
             * Bottom navigation items
             */
            case R.id.action_bottom_nav_pin:
                if (!(currentFragment instanceof PinsFragment)) {
                    currentFragment = new PinsFragment();
                    showFragment();
                    /*bottomNavigation.getMenu().getItem(0).setChecked(true);
                    bottomNavigation.getMenu().getItem(1).setChecked(false);
                    bottomNavigation.getMenu().getItem(2).setChecked(false);*/
                }
                break;
            case R.id.action_bottom_nav_home:
                if (!(currentFragment instanceof HomeFragment)) {
                    currentFragment = new HomeFragment();
                    showFragment();
                    /*bottomNavigation.getMenu().getItem(1).setChecked(true);
                    bottomNavigation.getMenu().getItem(0).setChecked(false);
                    bottomNavigation.getMenu().getItem(2).setChecked(false);*/
                }
                break;
            case R.id.action_bottom_nav_history:
                if (!(currentFragment instanceof HistoryFragment)) {
                    currentFragment = new HistoryFragment();
                    showFragment();
                    /*bottomNavigation.getMenu().getItem(2).setChecked(true);
                    bottomNavigation.getMenu().getItem(1).setChecked(false);
                    bottomNavigation.getMenu().getItem(0).setChecked(false);*/
                }
                break;
        }
        return true;
    }

    /**
     * Show fragment.
     */
    private void showFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.anim_slide_in_bottom, R.anim.anim_disappear)
                .replace(R.id.main_content, currentFragment)
                .commit();
    }

}
