package com.nickxiu.fitimer;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;

public class FitTimerImpl extends Fragment implements BaseTimerInterface {
    private static final String TAG = "FitTimerImpl";
    private static final int UNIT = 60;
    private static final int FAST_FORWARD_RATIO = 1;

    private SoundPool soundPool;
    private int workStartSound;
    private int restStartSound;

    private ViewGroup viewGroup;
    private View setupView;
    private View runningView;
    private TextView workoutSetupMinuteTextView;
    private TextView workoutSetupSecondTextView;
    private TextView restSetupMinuteTextView;
    private TextView restSetupSecondTextView;
    private TextView runningMinuteTextView;
    private TextView runningSecondTextView;
    private TextView startTextView;

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
    private int setupWorkSeconds;
    private int setupRestMinutes;
    private int setupRestSeconds;

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
        setupView = viewGroup.findViewById(R.id.fit_timer_view_setup);
        runningView = viewGroup.findViewById(R.id.fit_timer_view_running);
        workoutSetupMinuteTextView = viewGroup.findViewById(R.id.workout_text_view_setup_min);
        workoutSetupSecondTextView = viewGroup.findViewById(R.id.workout_text_view_setup_sec);
        restSetupMinuteTextView = viewGroup.findViewById(R.id.rest_text_view_setup_min);
        restSetupSecondTextView = viewGroup.findViewById(R.id.rest_text_view_setup_sec);
        runningMinuteTextView = viewGroup.findViewById(R.id.minute_text_view_running);
        runningSecondTextView = viewGroup.findViewById(R.id.second_text_view_running);
        startTextView = viewGroup.findViewById(R.id.start_button_text_view);

        soundPool = new SoundPool.Builder().setMaxStreams(2).build();
        workStartSound = soundPool.load(getActivity(), R.raw.work_timer_start_sound, 1);
        restStartSound = soundPool.load(getActivity(), R.raw.rest_timer_start_sound, 1);

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
                if ((System.currentTimeMillis() - cumulativeStartingTime) / 1000 * FAST_FORWARD_RATIO > (isWorkTimer ? workTimeSeconds : restTimeSeconds) - currentTextViewSeconds) {
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
        workTimeSeconds = Integer.parseInt((String) workoutSetupMinuteTextView.getText()) * UNIT + Integer.parseInt((String) workoutSetupSecondTextView.getText());
        restTimeSeconds = Integer.parseInt((String) restSetupMinuteTextView.getText()) * UNIT + Integer.parseInt((String) restSetupSecondTextView.getText());
        cumulativeStartingTime = 0;
        cumulativeElapsedTime = 0;

        if (workTimeSeconds > 0) {
            setWorkTimer();
        } else {
            setRestTimer();
        }
    }

    private void setWorkTimer() {
        setTextColor(true);
        soundPool.play(workStartSound, 1, 1, 0, 0, 1);
        currentTextViewSeconds = workTimeSeconds;
        setTextView();
        isWorkTimer = true;
    }

    private void setRestTimer() {
        setTextColor(false);
        soundPool.play(restStartSound, 1, 1, 0, 0, 1);
        currentTextViewSeconds = restTimeSeconds;
        setTextView();
        isWorkTimer = false;
    }

    private void swtichTimer() {
        pause();
        if ((isWorkTimer && restTimeSeconds > 0) || workTimeSeconds == 0) {
            setRestTimer();
        } else {
            setWorkTimer();
        }
        cumulativeStartingTime = 0;
        cumulativeElapsedTime = 0;
        start();
    }

    private void animateToRunning() {
        unlinkSetupEventTrackers();

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setInterpolator(new DecelerateInterpolator(1f));
        animatorSet.setDuration(500);

        TextView fromView, toView, minuteShuttleView, secondShuttleView;

        FrameLayout rootView = viewGroup.findViewById(R.id.fit_timer_view);

        // Set up minute digits animation.
        fromView = viewGroup.findViewById(R.id.workout_text_view_setup_sec);
        toView = viewGroup.findViewById(R.id.minute_text_view_running);
        minuteShuttleView = viewGroup.findViewById(R.id.minute_shuttle);

        Rect fromRect = new Rect();
        Rect toRect = new Rect();
        fromView.getGlobalVisibleRect(fromRect);
        toView.getGlobalVisibleRect(toRect);

        animatorSet = getViewToViewScalingAnimator(animatorSet, rootView, minuteShuttleView, fromRect, toRect);

        // Set up second digits animation.
        fromView = viewGroup.findViewById(R.id.rest_text_view_setup_sec);
        toView = viewGroup.findViewById(R.id.second_text_view_running);
        secondShuttleView = viewGroup.findViewById(R.id.second_shuttle);

        fromRect = new Rect();
        toRect = new Rect();
        fromView.getGlobalVisibleRect(fromRect);
        toView.getGlobalVisibleRect(toRect);

        animatorSet = getViewToViewScalingAnimator(animatorSet, rootView, secondShuttleView, fromRect, toRect);

        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                minuteShuttleView.setText(workoutSetupMinuteTextView.getText());
                secondShuttleView.setText(workoutSetupSecondTextView.getText());
                setupView.setVisibility(View.GONE);
                runningView.setVisibility(View.GONE);
                minuteShuttleView.setVisibility(View.VISIBLE);
                secondShuttleView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                minuteShuttleView.setVisibility(View.GONE);
                secondShuttleView.setVisibility(View.GONE);
                runningView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                onAnimationEnd(animation);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                onAnimationStart(animation);
            }
        });
        animatorSet.start();

        isSetup = false;

        linkRunningPageEventTrackers();
        start();
    }

    public AnimatorSet getViewToViewScalingAnimator(AnimatorSet animatorSet,
                                                    final FrameLayout parentView,
                                                    final View viewToAnimate,
                                                    final Rect fromViewRect,
                                                    final Rect toViewRect) {
        // get all coordinates at once
        final Rect parentViewRect = new Rect(), viewToAnimateRect = new Rect();
        parentView.getGlobalVisibleRect(parentViewRect);
        viewToAnimate.getGlobalVisibleRect(viewToAnimateRect);

        viewToAnimate.setScaleX(1f);
        viewToAnimate.setScaleY(1f);

        // rescaling of the object on X-axis
        final ValueAnimator valueAnimatorWidth = ValueAnimator.ofInt(fromViewRect.width(), toViewRect.width());
        valueAnimatorWidth.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Get animated width value update
                int newWidth = (int) valueAnimatorWidth.getAnimatedValue();
                // Get and update LayoutParams of the animated view
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) viewToAnimate.getLayoutParams();
                lp.width = newWidth;
                viewToAnimate.setLayoutParams(lp);
            }
        });

        // rescaling of the object on Y-axis
        final ValueAnimator valueAnimatorHeight = ValueAnimator.ofInt(fromViewRect.height(), toViewRect.height());
        valueAnimatorHeight.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                // Get animated width value update
                int newHeight = (int) valueAnimatorHeight.getAnimatedValue();
                // Get and update LayoutParams of the animated view
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) viewToAnimate.getLayoutParams();
                lp.height = newHeight;
                viewToAnimate.setLayoutParams(lp);
            }
        });

        // moving of the object on X-axis
        ObjectAnimator translateAnimatorX = ObjectAnimator.ofFloat(viewToAnimate, "X", fromViewRect.left - parentViewRect.left, toViewRect.left - parentViewRect.left);

        // moving of the object on Y-axis
        ObjectAnimator translateAnimatorY = ObjectAnimator.ofFloat(viewToAnimate, "Y", fromViewRect.top - parentViewRect.top, toViewRect.top - parentViewRect.top);

        animatorSet.playTogether(valueAnimatorWidth, valueAnimatorHeight, translateAnimatorX, translateAnimatorY);

        return animatorSet;
    }

    private void animateToSetup() {
        unlinkRunningPageEventTrackers();
        setupView.setVisibility(View.VISIBLE);
        runningView.setVisibility(View.GONE);

        linkSetupPageEventTrackers();

        isSetup = true;
    }

    private void setTextView() {
        // View changes must run on UI thread.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                runningSecondTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds % UNIT));
                runningMinuteTextView.setText(String.format(Locale.US, "%02d", currentTextViewSeconds / UNIT % UNIT));
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
    private boolean isValidSeconds(int sec) {
        return sec >= 0 && sec <= 59;
    }

    // TODO: may refactor.
    private void linkSetupPageEventTrackers() {
        workoutSetupMinuteTextView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case ACTION_DOWN:
                                startingY = event.getY();
                                setupWorkMinutes = Integer.parseInt((String) workoutSetupMinuteTextView.getText());
                                break;
                            case ACTION_MOVE:
                                int diff = (int) (startingY - event.getY()) / 100;
                                if (isValidMinutes(setupWorkMinutes + diff)) {
                                    workoutSetupMinuteTextView.setText(String.format("%02d", setupWorkMinutes + diff));
                                }
                                break;
                        }
                        return true;
                    }
                }
        );
        workoutSetupSecondTextView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case ACTION_DOWN:
                                startingY = event.getY();
                                setupWorkSeconds = Integer.parseInt((String) workoutSetupSecondTextView.getText());
                                break;
                            case ACTION_MOVE:
                                int diff = (int) (startingY - event.getY()) / 100;
                                if (isValidSeconds(setupWorkSeconds + diff)) {
                                    workoutSetupSecondTextView.setText(String.format("%02d", setupWorkSeconds + diff));
                                }
                                break;
                        }
                        return true;
                    }
                }
        );

        restSetupMinuteTextView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case ACTION_DOWN:
                                startingY = event.getY();
                                setupRestMinutes = Integer.parseInt((String) restSetupMinuteTextView.getText());
                                break;
                            case ACTION_MOVE:
                                int diff = (int) (startingY - event.getY()) / 100;
                                if (isValidMinutes(setupRestMinutes + diff)) {
                                    restSetupMinuteTextView.setText(String.format("%02d", setupRestMinutes + diff));
                                }
                                break;
                        }
                        return true;
                    }
                }
        );
        restSetupSecondTextView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getAction()) {
                            case ACTION_DOWN:
                                startingY = event.getY();
                                setupRestSeconds = Integer.parseInt((String) restSetupSecondTextView.getText());
                                break;
                            case ACTION_MOVE:
                                int diff = (int) (startingY - event.getY()) / 100;
                                if (isValidSeconds(setupRestSeconds + diff)) {
                                    restSetupSecondTextView.setText(String.format("%02d", setupRestSeconds + diff));
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

    private void setTextColor(boolean isWorkTimer) {
        if (isWorkTimer) {
            int workTimerColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
            runningMinuteTextView.setTextColor(workTimerColor);
            runningSecondTextView.setTextColor(workTimerColor);
        } else {
            int restTimerColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
            runningMinuteTextView.setTextColor(restTimerColor);
            runningSecondTextView.setTextColor(restTimerColor);
        }
    }

    private void unlinkSetupEventTrackers() {
        workoutSetupMinuteTextView.setOnTouchListener(null);
        workoutSetupSecondTextView.setOnTouchListener(null);
        restSetupMinuteTextView.setOnTouchListener(null);
        restSetupSecondTextView.setOnTouchListener(null);
    }
}