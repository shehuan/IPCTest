package com.shh.ipctest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MessengerActivity extends AppCompatActivity {
    private static final String TAG = "MessengerActivity";

    public static final int MESSAGE_FROM_CLIENT = 1;
    public static final int MESSAGE_FROM_SERVICE = 2;

    private Messenger mServiceMessenger;

    private Messenger mClientMessenger = new Messenger(new MessengerHandler());

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mServiceMessenger = new Messenger(service);
            Message message = Message.obtain();
            message.what = MESSAGE_FROM_CLIENT;
            Bundle bundle = new Bundle();
            bundle.putString("msg", "how are you?");
            message.setData(bundle);
            // 传递服务端回复客户端时需要使用的Messenger
            message.replyTo = mClientMessenger;
            try {
                mServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        Intent intent = new Intent(MessengerActivity.this, MessengerService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MessengerActivity.MESSAGE_FROM_SERVICE:
                    Log.e(TAG, "receive message from service:" + msg.getData().getString("msg"));
                    break;
            }
        }
    }
}
