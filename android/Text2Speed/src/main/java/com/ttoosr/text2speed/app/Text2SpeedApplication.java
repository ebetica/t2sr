package com.ttoosr.text2speed.app;

import android.app.Application;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import pro.dbro.openspritz.OpenSpritzApplication;

/**
 * Created by soltanmm on 4/12/14.
 */
public class Text2SpeedApplication extends Application implements OpenSpritzApplication {private Bus mBus;

    @Override
    public void onCreate() {
        super.onCreate();
        this.mBus = new Bus(ThreadEnforcer.ANY);
    }
    public Bus getBus() {
        return this.mBus;
    }
}
