package com.nickxiu.fitimer;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TimerFragmentPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = "TimerFragmentPagerAdapter";

    private List<Fragment> fragments;
    private boolean hasTimer = false;
    private boolean hasFitTimer = false;
    private int timerIndex = -1;
    private int fitTimerIndex = -1;

    public TimerFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
        this.fragments = new ArrayList<>();
        initializeLandingPage();
    }

    // Adds a fragment to the list, and return the index of the new fragment.
    public int addFragment(Fragment fragment) {
        if (fragments.isEmpty()) {
            fragments.add(fragment);
        } else {
            fragments.add(fragments.size()-1/* Always add to the immediate left */, fragment);
        }
        notifyDataSetChanged();
        return fragments.size()-2;
    }

    private void initializeLandingPage() { addFragment(new LandingPage()); }

    public void initializeTimer() {
        timerIndex = addFragment(new TimerImpl());
        hasTimer = true;
    }

    public void initializeFitTimer() {
        fitTimerIndex = addFragment(new FitTimerImpl());
        hasFitTimer = true;
    }

    public boolean hasTimer() {
        return hasTimer;
    }

    public boolean hasFitTimer() {
        return hasFitTimer;
    }

    public int getTimerIndex() {
        return timerIndex;
    }

    public int getFitTimerIndex() {
        return fitTimerIndex;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (fragments.get(position) instanceof LandingPage) {
            return "Add new timer";
        } else if (fragments.get(position) instanceof TimerImpl) {
            return "Timer";
        } else if (fragments.get(position) instanceof FitTimerImpl) {
            return "Fit timer";
        }
        return "";
    }

    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }

    @Override
    public int getItemPosition(@NonNull Object object){
        /*
         * called when the fragments are reordered to get the
         * changes.
         */
        int idx = fragments.indexOf(object);
        return idx < 0 ? POSITION_NONE : idx;
    }

    @Override
    public long getItemId(int position) {
        /*
         * map to a position independent ID, because this
         * adapter reorders fragments
         */
        return System.identityHashCode(fragments.get(position));
    }
}