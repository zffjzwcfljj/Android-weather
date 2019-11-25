package com.example.weather10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class AlarmReceiver extends BroadcastReceiver {
    public String str ;

        @Override
    public void onReceive(Context context, Intent intent) {
       // Toast.makeText(context,str, Toast.LENGTH_LONG).show();

    }

//    public AlarmReceiver(String str){
//        this.str = str;
//    }
}