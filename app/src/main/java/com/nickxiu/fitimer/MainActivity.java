package com.nickxiu.fitimer;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements LandingPage.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int MAX_PAGES = 3;

    private TimerFragmentPagerAdapter adapter;
    private ViewPager viewPager;
    private Toast toast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setWindowManagerFlags();
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setOffscreenPageLimit(MAX_PAGES);

        // Create an adapter that
        // knows which fragment should
        // be shown on each page
        adapter = new TimerFragmentPagerAdapter(getSupportFragmentManager());

        // Set the adapter onto
        // the view pager
        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                Log.i(TAG, String.format("get page: %d", position));
                toast.setText(adapter.getPageTitle(position));
                toast.show();
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        Log.i(TAG, "onAttachFragment");
        if (fragment instanceof LandingPage) {
            LandingPage landingPage = (LandingPage) fragment;
            landingPage.setOnClickListener(this);
        }
        if (fragment instanceof FitTimerImpl) {
            FitTimerImpl fitTimerImpl = (FitTimerImpl) fragment;
        }
    }

    private void setWindowManagerFlags() {
        // Use the background of the app behind the status bar and nav bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Override
    public void createTimer() {
        Log.i(TAG, "createTimer");
        if (!adapter.hasTimer()) {
            adapter.initializeTimer();
        }
        viewPager.setCurrentItem(adapter.getTimerIndex());
    }

    @Override
    public void createFitTimer() {
        Log.i(TAG, "createFitTimer");
        if (!adapter.hasFitTimer()) {
            adapter.initializeFitTimer();
        }
        viewPager.setCurrentItem(adapter.getFitTimerIndex());
    }
}
