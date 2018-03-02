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

import static android.os.Binder.getCallingUid;

public class LibraryManagerService extends Service {

    private static final String TAG = "LibraryManagerService";

    // CopyOnWriteArrayList 支持并发读写
    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();
    // 系统提供的专门用于删除跨进程 listener 的类
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();
    // AtomicBoolean 支持并发读写
    private AtomicBoolean mIsServiceDestroy = new AtomicBoolean(false);

    private Binder mBinder = new ILibraryManager.Stub() {

        @Override
        public List<Book> getNewBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void donateBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void register(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.register(listener);
            Log.e(TAG, "register success");
        }

        @Override
        public void unregister(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.unregister(listener);
            Log.e(TAG, "unregister success");
        }

        /**
         * 客户端调用服务端方法时校验，客户端和服务端是否是同一个应用都可以验证
         */
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            if (!passBindCheck()) {
                Log.e(TAG, "bind denied");
                return false;
            }

            return super.onTransact(code, data, reply, flags);
        }
    };

    public LibraryManagerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // 客户端绑定服务端时校验
        // 如果客户端和服务端是两个应用，则无法在onBind中完成校验，需要在onTransact中完成
//        if (!passBindCheck()) {
//            Log.e(TAG, "bind denied");
//            return null;
//        }

        return mBinder;
    }

    /**
     * 进行客户端的连接校验
     *
     * @return
     */
    private boolean passBindCheck() {
        // 客户端是否已申请了指定权限
        int check = checkCallingOrSelfPermission("com.shh.ipctest.permission.ACCESS_LIBRARY_SERVICE");
        if (check == PackageManager.PERMISSION_DENIED) {
            return false;
        }

        // 检验客户端包名
        String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
        if (packages != null && packages.length > 0 && !packages[0].startsWith("com.shh")) {
            return false;
        }

        return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBookList.add(new Book("book0"));
        mBookList.add(new Book("book1"));
        // 在子线程中每隔3秒创建一本新书，并通知所有已注册的客户端
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 如果服务还没终止
                while (!mIsServiceDestroy.get()) {
                    try {
                        Thread.sleep(3 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Book book = new Book("book" + mBookList.size());
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
