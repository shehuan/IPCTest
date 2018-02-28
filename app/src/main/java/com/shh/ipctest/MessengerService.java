package com.shh.ipctest;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MessengerService extends Service {
    private static final String TAG = "MessengerService";

    private Messenger mServiceMessenger = new Messenger(new MessengerHandler());

    public MessengerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mServiceMessenger.getBinder();
    }

    private static class MessengerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MessengerActivity.MESSAGE_FROM_CLIENT:
                    Log.e(TAG, "receive message from client:" + msg.getData().getString("msg"));

                    Messenger clientMessenger = msg.replyTo;
                    Message message = Message.obtain();
                    message.what = MessengerActivity.MESSAGE_FROM_SERVICE;
                    Bundle bundle = new Bundle();
                    bundle.putString("msg", "I am fine,thank you!");
                    message.setData(bundle);
                    try {
                        clientMessenger.send(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }
}
