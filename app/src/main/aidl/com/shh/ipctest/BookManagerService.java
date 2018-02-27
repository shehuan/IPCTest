package com.shh.ipctest;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookManagerService extends Service {

    private static final String TAG = "BookManagerService";

    // CopyOnWriteArrayList 支持并发读写
    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();
    // 系统提供的专门用于删除跨进程 listener 的接口
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();
    // AtomicBoolean 支持并发读写
    private AtomicBoolean mIsServiceDestroy = new AtomicBoolean(false);

    private Binder mBinder = new IBookManager.Stub() {

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.register(listener);
            Log.e(TAG, "register success");
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.unregister(listener);
            Log.e(TAG, "unregister success");
        }


        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int check = checkCallingOrSelfPermission("com.shh.ipctest.permission.ACCESS_BOOK_SERVICE");
            if (check == PackageManager.PERMISSION_DENIED) {
                return false;
            }

            String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
            if (packages != null && packages.length > 0 && !packages[0].startsWith("com.shh")) {
                return false;
            }

            return super.onTransact(code, data, reply, flags);
        }
    };

    public BookManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book(0, "book0"));
        mBookList.add(new Book(1, "book1"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mIsServiceDestroy.get()) {
                    try {
                        Thread.sleep(5 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Book book = new Book(mBookList.size(), "book" + mBookList.size());
                    mBookList.add(book);
                    bookArrivedNotify(book);
                }
            }
        }).start();
    }

    private void bookArrivedNotify(Book book) {
        int n = mListenerList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            IOnNewBookArrivedListener listener = mListenerList.getBroadcastItem(i);
            if (listener != null) {
                try {
                    listener.onNewBookArrived(book);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mListenerList.finishBroadcast();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceDestroy.set(true);
    }
}
