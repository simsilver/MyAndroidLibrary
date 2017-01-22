package com.simsilver.utils;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import static com.simsilver.utils.BaseActivity.SpecViewType.Content;
import static com.simsilver.utils.BaseActivity.SpecViewType.DrawerLayout;
import static com.simsilver.utils.BaseActivity.SpecViewType.Navigation;
import static com.simsilver.utils.BaseActivity.SpecViewType.Toolbar;
import static com.simsilver.utils.Common.BIND_ACTION_LOCAL_BINDER;
import static com.simsilver.utils.Common.BIND_ACTION_MESSENGER;

/**
 *
 */

public abstract class BaseActivity extends AppCompatActivity implements MsgHandler, NavigationView.OnNavigationItemSelectedListener {

    public static final int MSG_BIND_REMOTE_SERVICE_DONE = 0x100;
    public static final int MSG_UNBIND_REMOTE_SERVICE_DONE = MSG_BIND_REMOTE_SERVICE_DONE + 1;
    public static final int MSG_BIND_LOCAL_SERVICE_DONE = MSG_UNBIND_REMOTE_SERVICE_DONE + 1;
    public static final int MSG_UNBIND_LOCAL_SERVICE_DONE = MSG_BIND_LOCAL_SERVICE_DONE + 1;

    public static final int _MSG_BASE_START_ = 0x1000;
    public static final int MSG_LOCAL_BROADCAST = _MSG_BASE_START_ + 1;
    public static final int _REQ_BASE_START_ = 0x2000;

    public static final String OpenDrawer = "Open navigation drawer";
    public static final String CloseDrawer = "Close navigation drawer";

    protected enum SpecViewType {
        Content,
        Toolbar,
        DrawerLayout,
        Navigation
    }

    private String mReqPermission = null;
    private int mReqCode = 0;
    private StaticHandler<BaseActivity> mHandler = null;
    private Messenger mMessenger = null;

    private Messenger mServiceMessenger = null;
    private boolean mLocalServiceBound = false, mRemoteServiceBound = false;

    private ServiceConnection mLocalServiceConnection = null, mRemoteServiceConnection = null;


    private Toolbar mToolbar = null;
    private DrawerLayout mDrawer = null;

    private NavigationView mNavigationView = null;

    private class RemoteMessengerServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            mRemoteServiceBound = true;
            Message msg = Message.obtain();
            msg.what = MSG_BIND_REMOTE_SERVICE_DONE;
            sendSelfMsg(msg, 0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceMessenger = null;
            mRemoteServiceBound = false;
            Message msg = Message.obtain();
            msg.what = MSG_UNBIND_REMOTE_SERVICE_DONE;
            sendSelfMsg(msg, 0);
        }
    }

    private Service mLocalService = null;

    protected Service getLocalService() {
        return mLocalService;
    }

    private class LocalServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            mLocalService = binder.getService();
            mLocalServiceBound = true;
            Message msg = Message.obtain();
            msg.what = MSG_BIND_LOCAL_SERVICE_DONE;
            sendSelfMsg(msg, 0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mLocalService = null;
            mLocalServiceBound = false;
            Message msg = Message.obtain();
            msg.what = MSG_UNBIND_LOCAL_SERVICE_DONE;
            sendSelfMsg(msg, 0);
        }
    }

    private BroadcastReceiver mLocalMessageReceiver = null;

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Common.LOCAL_MESSAGE_ACTION.equals(intent.getAction())) {
                Bundle bundle = intent.getBundleExtra(Common.LOCAL_MESSAGE_DATA);
                Message msg = Message.obtain();
                msg.what = MSG_LOCAL_BROADCAST;
                msg.obj = bundle;
                sendSelfMsg(msg, 0);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new StaticHandler<>(this);
        mMessenger = new Messenger(mHandler);

        setContentView(getSpecViewId(Content));
        findViews();
        initViews();
        int toolbarId = getSpecViewId(Toolbar);
        View toolbar = null;
        if (toolbarId > 0) {
            toolbar = findViewById(toolbarId);
        }
        if (toolbar != null && toolbar instanceof Toolbar) {
            mToolbar = (Toolbar) toolbar;
            setSupportActionBar(mToolbar);
            setupToolbar(getSupportActionBar());
            int drawerId = getSpecViewId(DrawerLayout);
            View drawer = null;
            if (drawerId > 0) {
                drawer = findViewById(drawerId);
            }
            if (drawer != null && drawer instanceof DrawerLayout) {
                mDrawer = (DrawerLayout) drawer;
                ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                        this, mDrawer, mToolbar,
                        R.string.library_navigation_drawer_open,
                        R.string.library_navigation_drawer_close);
                mDrawer.addDrawerListener(toggle);
                toggle.syncState();
            }
            int navigationId = getSpecViewId(Navigation);
            View navigation = null;
            if (navigationId > 0) {
                navigation = findViewById(navigationId);
            }
            if (navigation != null && navigation instanceof NavigationView) {
                mNavigationView = (NavigationView) navigation;
                mNavigationView.setNavigationItemSelectedListener(this);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer != null && mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    protected void closeDrawer() {
        if (mDrawer != null) {
            mDrawer.closeDrawer(GravityCompat.START);
        }
    }

    protected boolean regLocalMsgReceiver() {
        if (mLocalMessageReceiver != null) {
            return false;
        }
        IntentFilter localMessageFilter = new IntentFilter(Common.LOCAL_MESSAGE_ACTION);
        mLocalMessageReceiver = new MyBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalMessageReceiver, localMessageFilter);
        return true;
    }

    protected void unregLocalMsgReceiver() {
        if (mLocalMessageReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocalMessageReceiver);
            mLocalMessageReceiver = null;
        }
    }

    protected boolean bindToRemoteService(Class<? extends Service> service) {
        if (mRemoteServiceBound) {
            return false;
        }
        mRemoteServiceConnection = new RemoteMessengerServiceConnection();
        Intent serviceIntent = new Intent(this, service);
        serviceIntent.setAction(BIND_ACTION_MESSENGER);
        return bindService(serviceIntent, mRemoteServiceConnection, BIND_AUTO_CREATE);
    }

    protected void unbindRemoteService() {
        if (!mRemoteServiceBound) {
            return;
        }
        unbindService(mRemoteServiceConnection);
        mRemoteServiceBound = false;
    }

    protected boolean bindLocalService(Class<? extends Service> service) {
        if (mLocalServiceBound) {
            return false;
        }
        mLocalServiceConnection = new LocalServiceConnection();
        Intent serviceIntent = new Intent(this, service);
        serviceIntent.setAction(BIND_ACTION_LOCAL_BINDER);
        return bindService(serviceIntent, mLocalServiceConnection, BIND_AUTO_CREATE);
    }

    protected void unbindLocalService() {
        if (!mLocalServiceBound) {
            return;
        }
        unbindService(mLocalServiceConnection);
        mLocalServiceBound = false;
    }

    protected void sendRemoteServiceMsg(Message msg) {
        if (!mRemoteServiceBound) {
            Log.e("Activity", "sendRemoteServiceMsg failed for not bind");
            return;
        }
        msg.replyTo = mMessenger;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void makeRequest() {
        ActivityCompat.requestPermissions(this, new String[]{mReqPermission}, mReqCode);
    }

    public boolean checkAndRequestPermission(String permission, String reqMessage, String reqTitle, int reqCode, boolean force) {
        int status = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
        if (status != PackageManager.PERMISSION_GRANTED) {
            mReqPermission = permission;
            mReqCode = reqCode;
            if (force || ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(reqMessage)
                        .setTitle(reqTitle)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int id) {
                                makeRequest();
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                makeRequest();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Message msg = Message.obtain();
        msg.what = mReqCode;
        if (requestCode == mReqCode
                && permissions.length != 0
                && permissions[0].equals(mReqPermission)
                && grantResults.length != 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            msg.arg1 = 1;
        } else {
            msg.arg1 = 0;
        }
        sendSelfMsg(msg, 0);
    }

    protected void sendSelfMsg(Message msg, long delayMills) {
        mHandler.sendMessageDelayed(msg, delayMills);
    }

    protected Handler getHandler() {
        return mHandler;
    }

    protected Messenger getMessenger() {
        return mMessenger;
    }

    protected abstract int getSpecViewId(SpecViewType type);

    protected abstract void findViews();

    protected abstract void initViews();

    protected abstract void setupToolbar(ActionBar toolbar);
}
