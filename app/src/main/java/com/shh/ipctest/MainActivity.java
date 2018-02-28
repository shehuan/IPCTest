package com.shh.ipctest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void aidl(View view) {
        startActivity(new Intent(MainActivity.this, AIDLActivity.class));
    }

    public void messenger(View view) {
        startActivity(new Intent(MainActivity.this, MessengerActivity.class));
    }
}
