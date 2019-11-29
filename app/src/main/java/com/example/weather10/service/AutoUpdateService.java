package com.example.weather10.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;

import com.example.weather10.MyReceiver;
import com.example.weather10.gson.Weather;
import com.example.weather10.util.HttpUtil;
import com.example.weather10.util.Utility;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public String wendu,str,str1="!";

    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent,int flags,int startId){
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() +anHour;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this,0,i,0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent,flags,startId);
    }

    /**
     * 更新天气信息
     */
    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if (weatherString != null){
            //有缓存时直接解析天气数据
           final Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;

            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId;
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String updateTime = weather.basic.update.updateTime.split(" ")[1];
                    String cityName = weather.basic.cityName;
                    String degree = weather.now.temperature + "℃";
                    wendu = weather.now.temperature;
                    String weatherInfo = weather.now.more.info;
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if (weather != null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();

                            str = cityName + " 今日天气是" + weatherInfo + "，温度是" + degree;
                            if (Integer.parseInt(wendu) <= 0)
                                str1 = "气温零下，注意保暖";
                            if (weatherInfo.equals("小雨") || weatherInfo.equals("中雨") || weatherInfo.equals("大雨"))
                                str1 = "今日下雨，出门记得带伞";
                            if (weatherInfo.equals("雷阵雨"))
                                str1 = "今日有雷阵雨，出门别忘带伞哦~";
                            if (weatherInfo.equals("晴"))
                                str1 = "今日有太阳，出门记得带太阳伞哦~";
                            str = str + " "+ str1;
                            str1 = "!";

                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("smsto:5554"));
                        intent.putExtra("sms_body",str);

                        String content = str;//短信内容
                        String phone = "5554";//电话号码
                        SmsManager sm = SmsManager.getDefault();
                        List<String> sms = sm.divideMessage(content);
                        for (String smslist :sms)
                            sm.sendTextMessage(phone,null,smslist,null,null);

                        // 广播写法一
                        Intent intent1 = new Intent();
                        intent1.setAction("repeating");
                        intent1.putExtra("broadcastmsg",str);

                        MyReceiver myReceiver = new MyReceiver();
                        IntentFilter filter = new IntentFilter();
                        filter.addAction("repeating");
                        registerReceiver(myReceiver,filter);
                        sendBroadcast(intent1);

                        // 广播方法二
//                        PendingIntent sender= PendingIntent.getBroadcast(AutoUpdateService.this, 0, intent1, 0);
//                        long firstime = SystemClock.elapsedRealtime();
//                        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
//                        //24小时一个周期，不停的发送广播
//                        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstime, 1 * 60 * 60 * 24 * 1000, sender);
//                        sendBroadcast(intent1);

                        editor.putString("weather",responseText);
                        editor.apply();
                    }
                }
            });
        }
    }

    /**
     * 更新必应每日一图
     */
    private void updateBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }


}
