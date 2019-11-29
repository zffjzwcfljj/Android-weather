package com.example.weather10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.textclassifier.TextLinks;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.weather10.gson.Forecast;
import com.example.weather10.gson.Weather;
import com.example.weather10.service.AutoUpdateService;
import com.example.weather10.util.HttpUtil;
import com.example.weather10.util.Utility;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button navButton;

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView bingPicImg;

    private PendingIntent pendingIntent;
    private AlarmManager alarmManager;
    public String str,str1 = "!",wendu;
    public boolean flag = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >=21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS} , 1);

        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.api_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if (weatherString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh(){
                requestWeather(mWeatherId);
            }
        });
        String bingPic = prefs.getString("bing_pic",null);
        if (bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        navButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


//        MyReceiver myReceiver = new MyReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("repeating");
//        registerReceiver(myReceiver,filter);
//
//        Intent intent = new Intent();
//        intent.setAction("repeating");
//        intent.putExtra("broadcastmsg",str);


//        PendingIntent sender= PendingIntent.getBroadcast(this, 0, intent, 0);
//        long firstime = SystemClock.elapsedRealtime();
//        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
//        //24小时一个周期，不停的发送广播
//        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstime, 1 * 60 * 60 * 24 * 1000, sender);
//        sendBroadcast(intent);


    }


    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        } else{
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });

            }
        });
        loadBingPic();
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor =PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        wendu = weather.now.temperature;
        String weatherInfo = weather.now.more.info;


        int res = 0;

        if(weatherInfo.equals("晴")){
            res = R.drawable.qing;
        } else if(weatherInfo.equals("小雨")){
            res = R.drawable.xiaoyu;
        }else if(weatherInfo.equals("中雨")){
            res = R.drawable.zhongyu;
        } else if(weatherInfo.equals("大雨")){
            res = R.drawable.dayu;
        }else if(weatherInfo.equals("雷阵雨")){
            res = R.drawable.leizhenyu;
        }else if(weatherInfo.equals("阴")){
            res = R.drawable.yin;
        }else if(weatherInfo.equals("多云")){
            res = R.drawable.duoyun;
        }else if(weatherInfo.equals("阵雨")) {
            res = R.drawable.zhenyu;
        }


        if (res != 0) {
            ((ImageView)findViewById(R.id.weather_image)).setImageResource(res);
        }

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dataText = (TextView) view.findViewById(R.id.data_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dataText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
//            if(flag){
//                str = "今日天气是" + weatherInfo + " 温度是" + degree;
//                if (Integer.parseInt(wendu) <= 0)
//                    str1 = "气温零下，注意保暖";
//                if (weatherInfo.equals("小雨") || weatherInfo.equals("中雨") || weatherInfo.equals("大雨"))
//                    str1 = "今日下雨，出门记得带伞";
//                if (weatherInfo.equals("雷阵雨"))
//                    str1 = "今日有雷阵雨，出门别忘带伞哦~";
//                if (weatherInfo.equals("晴"))
//                    str1 = "今日有太阳，出门记得带太阳伞哦~";
//                str = str + " "+ str1;
//                flag = false;
//            }
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

    }

}
