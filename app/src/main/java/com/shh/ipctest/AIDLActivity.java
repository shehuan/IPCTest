package com.shh.ipctest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

public class AIDLActivity extends AppCompatActivity {
    private static final String TAG = "AIDLActivity";

    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;

    private ILibraryManager mBookManager;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.e(TAG, "new book:" + msg.obj);
                    break;
            }
            return true;
        }
    });

    private IOnNewBookArrivedListener listener = new IOnNewBookArrivedListener.Stub() {

        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED, book).sendToTarget();
        }
    };

    // Binder意外终止后，重新绑定服务
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if (mBookManager != null) {
                mBookManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
                mBookManager = null;

                bindNewService();
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ILibraryManager bookManager = ILibraryManager.Stub.asInterface(service);
            mBookManager = bookManager;
            try {
                mBookManager.asBinder().linkToDeath(mDeathRecipient, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                List<Book> books = bookManager.getNewBookList();
                Log.e(TAG, "books:" + books.toString());
                bookManager.donateBook(new Book("book" + books.size()));
                List<Book> books2 = bookManager.getNewBookList();
                Log.e(TAG, "books:" + books2.toString());

                // 注册通知
                bookManager.registerListener(listener);

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
        setContentView(R.layout.activity_aidl);

        bindNewService();
    }

    private void bindNewService() {
        Intent intent = new Intent(AIDLActivity.this, LibraryManagerService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // 如果客户端是另一个应用，需要隐式绑定服务
//        Intent intent = new Intent();
//        intent.setAction("android.intent.action.LibraryManagerService");
//        intent.setPackage("com.shh.ipctest");
//        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        if (mBookManager != null && mBookManager.asBinder().isBinderAlive()) {
            try {
                // 取消注册
                mBookManager.unregisterListener(listener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }
}
