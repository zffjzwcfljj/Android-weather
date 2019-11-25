package com.example.weather10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class AlarmReceiver extends BroadcastReceiver {
    public String str ;

        @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "今日天气情况", Toast.LENGTH_LONG).show();

    }

}