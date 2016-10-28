package com.abara.fireclip.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.abara.fireclip.fragment.FavouritesFragment;
import com.abara.fireclip.fragment.HistoryFragment;
import com.abara.fireclip.fragment.HomeFragment;

/**
 * Created by abara on 07/09/16.
 */

/*
* Fragment Adapter to populate Fragments for Tabs.
* */

public class MainViewPagerAdapter extends FragmentPagerAdapter {

    private Fragment fragments[] = {new FavouritesFragment(), new HomeFragment(), new HistoryFragment()};

    public MainViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }
}
