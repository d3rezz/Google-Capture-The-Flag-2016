package com.example.francis.illsolver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by francis on 4/30/16.
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "MyReceiver";
    @Override
    public void onReceive(Context context, Intent intent ) {
        String message = intent.getStringExtra("msg");
        Log.d(TAG, "Got a broadcast");
        Log.d(TAG, "Content: "+message);
    }
}
