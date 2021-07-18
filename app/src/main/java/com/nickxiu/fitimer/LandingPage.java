package com.nickxiu.fitimer;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class LandingPage extends Fragment {
    private static final String TAG = "LandingPage";

    private ViewGroup viewGroup;
    private OnClickListener callback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewGroup = (ViewGroup) inflater.inflate(
                R.layout.landing_page_view, container, false);
        setEventTrackers();
        return viewGroup;
    }

    // Provide several buttons to create timers.
    // Tapping on certain timer will create one, and jump to it if it's not yet created.
    // Otherwise go to the existing timer directly.
    private void setEventTrackers() {
        Log.i(TAG, "setEventTrackers");

        viewGroup.findViewById(R.id.landing_page_timer_button).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "on click landing page timer button");
                    callback.createTimer();
                }
        });

        viewGroup.findViewById(R.id.landing_page_fit_timer_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG, "on click landing page fit timer button");
                        callback.createFitTimer();
                    }
                });
    }

    public void setOnClickListener(OnClickListener callback) {
        Log.i(TAG, "setOnClickListener");
        this.callback = callback;
    }

    public interface OnClickListener {
        void createTimer();
        void createFitTimer();
    }
}
