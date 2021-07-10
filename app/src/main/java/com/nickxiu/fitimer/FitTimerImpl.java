package com.nickxiu.fitimer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;

public class FitTimerImpl extends Fragment implements BaseTimerInterface {
    private static final String TAG = "FitTimerImpl";
    private static final int UNIT = 60;
    private static final int FAST_FORWARD_RATIO = 10;

    private Context context;
    private ViewGroup viewGroup;
    private TextView minuteTextView;
    private TextView secondTextView;
    private TextView titleTextView;
    private TextView startTextView;
    private TextView workTextView;
    private TextView restTextView;

    private boolean isSetup = true;
    private boolean isRunning = false;
    private boolean isWorkTimer = false;
    private int workTimeSeconds = 0;
    private int restTimeSeconds = 0;
    private int currentTextViewSeconds = 0;
    private Timer timer;

    // The starting point of a running segment.
    private long startingTimestamp = 0;
    // If pauses happened, this is the actual starting point, summing up previous running segments.
    private long cumulativeStartingTime = 0;
    // This keeps track of the total time of all previous time-running segments.
    private long cumulativeElapsedTime = 0;

    // Variables for swipe up/down events during set-up.
    private float startingY = 0.0f;
    private int setupWorkMinutes;
    private int setupRestMinutes;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        viewGroup = (ViewGroup) inflater.inflate(
                R.layout.fit_timer_view, container, false);
        minuteTextView = viewGroup.findViewById(R.id.minute_text_view);
        secondTextView = viewGroup.findViewById(R.id.second_text_view);
        titleTextView = viewGroup.findViewById(R.id.title_text_view);
        startTextView = viewGroup.findViewById(R.id.start_button_text_view);
        workTextView = viewGroup.findViewById(R.id.work_text_view);
        restTextView = viewGroup.findViewById(R.id.rest_text_view);

        linkSetupPageEventTrackers();
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
                if ((System.currentTimeMillis() - cumulativeStartingTime) / 1000 * FAST_FORWARD_RATIO > workTimeSeconds - currentTextViewSeconds) {
                    if (currentTextViewSeconds <= 0) {
                        swtichTimer();
                    } else {
                        currentTextViewSeconds--;
                    }
                }
                setTextView();
            }
        }, 0, 50);
    }

    @Override
    public void pause() {
        Log.i(TAG, "pause");
        isRunning = false;
        cumulativeElapsedTime += System.currentTimeMillis() - startingTimestamp;
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void reset() {
        Log.i(TAG, "reset");
        isRunning = false;
        workTimeSeconds = Integer.parseInt((String) minuteTextView.getText()) * UNIT;
        restTimeSeconds = Integer.parseInt((String) secondTextView.getText()) * UNIT;
        cumulativeStartingTime = 0;
        cumulativeElapsedTime = 0;

        setWorkTimer();
    }

    private void setWorkTimer() {
        currentTextViewSeconds = workTimeSeconds;
        setTextView();
        isWorkTimer = true;
    }

    private void setRestTimer() {
        currentTextViewSeconds = restTimeSeconds;
        setTextView();
        isWorkTimer = false;
    }

    private void swtichTimer() {
        pause();
        if (isWorkTimer) {
            setRestTimer();
        } else {
            setWorkTimer();
        }
        start();
    }

    // TODO: make the x and y movements by dp rather than px.
    // TODO: maybe refactor.
    private void animateToRunning() {
        unlinkSetupEventTrackers();
        titleTextView.setVisibility(View.GONE);
        startTextView.setVisibility(View.GONE);
        workTextView.setVisibility(View.GONE);
        restTextView.setVisibility(View.GONE);

        minuteTextView.animate().scaleX(2f).scaleY(2f).translationXBy(-250f).translationYBy(-125f).setDuration(500).start();
        secondTextView.animate().scaleX(2f).scaleY(2f).translationXBy(-250f).translationYBy(210f).setDuration(500).setListener(null).start();

        isSetup = false;

        linkRunningPageEventTrackers();
        start();
    }

    // TODO: make the x and y movements by dp rather than px.
    // TODO: maybe refactor.
    private void animateToSetup() {
        unlinkRunningPageEventTrackers();

        minuteTextView.animate().scaleX(1f).scaleY(1f).translationXBy(250f).translationYBy(125f).start();
        secondTextView.animate().scaleX(1f).scaleY(1f).translationXBy(250f).translationYBy(-210f).setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        titleTextView.setVisibility(View.VISIBLE);
                        startTextView.setVisibility(View.VISIBLE);
                        workTextView.setVisibility(View.VISIBLE);
                        restTextView.setVisibility(View.VISIBLE);
                    }
                }).start();


        linkSetupPageEventTrackers();

        isSetup = true;
    }

    private void setTextView() {
        // View changes must run on UI thread.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                secondTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds % UNIT));
                minuteTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds / UNIT % UNIT));
            }
        });
    }

    private void linkRunningPageEventTrackers() {
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
                if (isSetup) {
                    return false;
                } else {
                    if (isRunning) {
                        pause();
                    }
                    animateToSetup();
                }
                return true;
            }
        });
    }

    private void unlinkRunningPageEventTrackers() {
        viewGroup.setOnClickListener(null);
        viewGroup.setOnLongClickListener(null);
    }

    private boolean isValidMinutes(int min) {
        return min >= 0 && min <= 99;
    }

    // TODO: may refactor.
    private void linkSetupPageEventTrackers() {
        minuteTextView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case ACTION_DOWN:
                                startingY = event.getY();
                                setupWorkMinutes = Integer.parseInt((String) minuteTextView.getText());
                                break;
                            case ACTION_MOVE:
                                int movingMinutes = (int) (startingY - event.getY()) / 100;
                                if (isValidMinutes(setupWorkMinutes + movingMinutes)) {
                                    minuteTextView.setText(String.format("%02d", setupWorkMinutes + movingMinutes));
                                }
                                break;
                        }
                        return true;
                    }
                }
        );
        secondTextView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case ACTION_DOWN:
                                startingY = event.getY();
                                setupRestMinutes = Integer.parseInt((String) secondTextView.getText());
                                break;
                            case ACTION_MOVE:
                                int movingMinutes = (int) (startingY - event.getY()) / 100;
                                if (isValidMinutes(setupRestMinutes + movingMinutes)) {
                                    secondTextView.setText(String.format("%02d", setupRestMinutes + movingMinutes));
                                }
                                break;
                        }
                        return true;
                    }
                }
        );

        startTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reset();
                animateToRunning();
            }
        });
    }

    private void unlinkSetupEventTrackers() {
        minuteTextView.setOnTouchListener(null);
        secondTextView.setOnTouchListener(null);
    }
}