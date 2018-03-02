package com.shh.ipctest;

import com.shh.ipctest.Book;
import com.shh.ipctest.IOnNewBookArrivedListener;

interface ILibraryManager{
    List<Book> getNewBookList();
    //in 代表参数为输入类型的
    void donateBook(in Book book);

    void register(IOnNewBookArrivedListener listener);

    void unregister(IOnNewBookArrivedListener listener);
}