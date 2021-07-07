package com.nickxiu.fitimer;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;

public class FitTimerImpl extends Fragment implements BaseTimerInterface {
    private static final String TAG = "FitTimerImpl";
    private static final int UNIT = 60;

    private Context context;
    private ViewGroup viewGroup;
    private TextView minuteTextView;
    private TextView secondTextView;
    private TextView titleTextView;
    private TextView startTextView;

    private boolean isSetup = false;
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

    // Variables for swipe up/down events during set-up.
    private float startingY = 0.0f;
    private int setupWorkMinutes;
    private int setupRestMinutes;

    public void setContext(Context context) {
        this.context = context;
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

        linkRunningPageEventTrackers();
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
                    if (currentTextViewSeconds <= 0) {
                        endTimer();
                    } else {
                        currentTextViewSeconds--;
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
        totalTimeSeconds = Integer.parseInt((String) minuteTextView.getText())*UNIT;
        currentTextViewSeconds = Integer.parseInt((String) minuteTextView.getText())*UNIT;
        cumulativeStartingTime = 0;
        cumulativeElapsedTime = 0;
        setTextView();
    }

    private void endTimer() {
        Log.i(TAG, "end");
        isRunning = false;
        timer.cancel();
    }

    // TODO: move the numbers properly during the animation.
    // TODO: maybe refactor.
    private void animateToRunning() {
        unlinkSetupEventTrackers();
        titleTextView.setVisibility(View.GONE);
        startTextView.setVisibility(View.GONE);

        int colorFrom = ContextCompat.getColor(this.context, R.color.colorSetup);
        int colorTo = ContextCompat.getColor(this.context, R.color.colorPrimary);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(500);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                minuteTextView.setTextColor((int) animator.getAnimatedValue());
                secondTextView.setTextColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();

        minuteTextView.animate().scaleX(1f).scaleY(1f).setDuration(500).start();
        secondTextView.animate().scaleX(1f).scaleY(1f).translationYBy(400f).setDuration(500).start();

        isSetup = false;

        linkRunningPageEventTrackers();
        start();
    }

    // TODO: move the numbers properly during the animation.
    // TODO: maybe refactor.
    private void animateToSetup() {
        unlinkRunningPageEventTrackers();

        int colorFrom = ContextCompat.getColor(this.context, R.color.colorPrimary);
        int colorTo = ContextCompat.getColor(this.context, R.color.colorSetup);
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(500);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                minuteTextView.setTextColor((int) animator.getAnimatedValue());
                secondTextView.setTextColor((int) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();

        minuteTextView.animate().scaleX(0.5f).scaleY(0.5f).setDuration(500).start();
        secondTextView.animate().scaleX(0.5f).scaleY(0.5f).translationYBy(-400f).setDuration(500).start();
        titleTextView.setVisibility(View.VISIBLE);
        startTextView.setVisibility(View.VISIBLE);

        linkSetupPageEventTrackers();

        isSetup = true;
    }

    private void setTextView() {
        secondTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds % UNIT));
        minuteTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds / UNIT % UNIT));
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