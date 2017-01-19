package com.simsilver.utils;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;

import static com.simsilver.utils.Common.BIND_ACTION_LOCAL_BINDER;
import static com.simsilver.utils.Common.BIND_ACTION_MESSENGER;
import static com.simsilver.utils.Common.LOCAL_MESSAGE_ACTION;
import static com.simsilver.utils.Common.LOCAL_MESSAGE_DATA;

/**
 *
 */

public abstract class ExampleService extends Service implements MsgHandler {

    public static final int _MSG_BASE_START_ = 0x3000;

    private Messenger mMessenger = null;
    private StaticHandler<ExampleService> mHandler = null;
    private LocalBroadcastManager mLocalBroadcastManager = null;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new StaticHandler<>(this);
        mMessenger = new Messenger(mHandler);
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            action = "";
        }
        switch (action) {
            case BIND_ACTION_LOCAL_BINDER:
                return new LocalBinder<>(this);
            case BIND_ACTION_MESSENGER:
                return mMessenger.getBinder();
            default:
                // 如果继承其他Service类，需返回
                // super.onBind(intent);
                return null;
        }
    }

    protected void sendSelfMsg(Message msg, long delayMills) {
        mHandler.sendMessageDelayed(msg, delayMills);
    }

    protected void sendLocalMessage(Bundle bundle) {
        Intent intent = new Intent(LOCAL_MESSAGE_ACTION);
        intent.putExtra(LOCAL_MESSAGE_DATA, bundle);
        if (mLocalBroadcastManager == null) {
            mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        }
        mLocalBroadcastManager.sendBroadcast(intent);
    }

    protected Handler getHandler() {
        return mHandler;
    }
}
