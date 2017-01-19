package com.simsilver.utils;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 *
 */

public class StaticHandler<T extends MsgHandler> extends Handler {
    private WeakReference<T> mRef;

    public StaticHandler(T context) {
        mRef = new WeakReference<>(context);
    }

    @Override
    public void handleMessage(Message msg) {
        T context = mRef.get();
        if (context != null) {
            context.process(msg);
        }
    }

}
