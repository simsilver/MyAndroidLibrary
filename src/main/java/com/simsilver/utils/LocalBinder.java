package com.simsilver.utils;

import android.app.Service;
import android.os.Binder;


public class LocalBinder<T extends Service> extends Binder {
    private T mSelf = null;

    public LocalBinder(T instance) {
        mSelf = instance;
    }

    public T getService() {
        return mSelf;
    }
}