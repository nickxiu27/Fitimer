package com.nickxiu.fitimer;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class FitTimerImpl extends Fragment implements BaseTimerInterface  {
    private static final String TAG = "FitTimerImpl";
    private static final int UNIT = 60;

    private ViewGroup viewGroup;
    private TextView minuteTextView;
    private TextView secondTextView;

    private boolean isRunning = false;
    private int totalTimeSeconds = 5;
    private int currentTextViewSeconds = 5;
    private Timer timer;

    // The starting point of a running segment.
    private long startingTimestamp = 0;
    // If pauses happened, this is the actual starting point, summing up previous running segments.
    private long cumulativeStartingTime = 0;
    // This keeps track of the total time of all previous time-running segments.
    private long cumulativeElapsedTime = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewGroup = (ViewGroup) inflater.inflate(
                R.layout.fit_timer_view, container, false);
        minuteTextView = viewGroup.findViewById(R.id.minute_text_view);
        secondTextView = viewGroup.findViewById(R.id.second_text_view);

        setEventTrackers();
        return viewGroup;
    }

    @Override
    public void start() {
        Log.i(TAG, "start");
        isRunning = true;
        timer = new Timer();
        startingTimestamp = System.currentTimeMillis();
        cumulativeStartingTime = startingTimestamp - cumulativeElapsedTime;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if ((System.currentTimeMillis() - cumulativeStartingTime) / 1000 > totalTimeSeconds - currentTextViewSeconds) {
                    currentTextViewSeconds--;
                    if (currentTextViewSeconds <= 0) {
                        endTimer();
                    }
                    setTextView();
                }
            }
        }, 0, 50);
    }

    @Override
    public void pause() {
        Log.i(TAG, "pause");
        isRunning = false;
        cumulativeElapsedTime += System.currentTimeMillis() - startingTimestamp;
        timer.cancel();
    }

    @Override
    public void reset() {
        Log.i(TAG, "reset");
        isRunning = false;
        timer.cancel();
        totalTimeSeconds = 100;
        currentTextViewSeconds = 100;
        cumulativeStartingTime = 0;
        cumulativeElapsedTime = 0;
        setTextView();
    }

    private void endTimer() {
        Log.i(TAG, "end");
        isRunning = false;
        timer.cancel();
    }

    private void setTextView() {
        secondTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds % UNIT));
        minuteTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds / UNIT % UNIT));
    }

    private void setEventTrackers() {
        viewGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    pause();
                } else {
                    start();
                }
            }
        });

        viewGroup.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                reset();
                return true;
            }
        });
    }
}