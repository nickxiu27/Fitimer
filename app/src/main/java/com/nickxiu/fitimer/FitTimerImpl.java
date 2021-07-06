package com.nickxiu.fitimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Timer;
import java.util.TimerTask;

public class FitTimerImpl extends Fragment implements BaseTimerInterface  {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(
                R.layout.fit_timer_view, container, false);
    }

    @Override
    public void start() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void reset() {
    }
}