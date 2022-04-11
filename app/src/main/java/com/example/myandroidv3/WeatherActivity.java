package com.example.myandroidv3;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.myandroidv3.gson.Forecast;
import com.example.myandroidv3.gson.Weather;
import com.example.myandroidv3.service.AutoUpdateService;
import com.example.myandroidv3.util.HttpUtil;
import com.example.myandroidv3.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
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
    public SwipeRefreshLayout swipeRefresh;
    //mWeatherId 变量，
    //用于记录城市的天气id，
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button navButton;
    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        // 初始化各控件
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        bingPicImg = (ImageView)findViewById(R.id.bin_pic_img);
        swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        //调用setColorSchemeResources()
        //方法来设置下拉刷新进度条的颜色
        swipeRefresh.setColorSchemeColors(com.google.android.material.R.color.design_default_color_primary);
        //抽屉菜单 滑动菜单
        drawerLayout =(DrawerLayout)findViewById(R.id.drawer_layout);
        navButton = (Button)findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开滑动菜单
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences
                (this);
        String weatherString = prefs.getString("weather", null);
        String binPic = prefs.getString("bing_pic",null);
        if (weatherString != null) {
        // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
        // 无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        //设置一个下拉刷新的监听器
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //去调用requestWeather()方法请求天气信息
                requestWeather(mWeatherId);
            }
        });

        if(binPic!=null){
            Glide.with(this).load(binPic).into(bingPicImg);
        }
        else {
            //下载图片
            loadBingPic();
        }
    }

    //根据天气id请求城市天气信息
   public void requestWeather(final String weatherId){
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+weatherId+"&key=bc0418b57b2d4918819d3974ac1285d9";
       HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
           @Override
           public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        //请求结束后调用SwipeRefreshLayout的setRefreshing() 方法并传入false ， 用于表示刷新事件结束， 并隐藏刷新进度条
                        swipeRefresh.setRefreshing(false);
                    }
                });

           }

           @Override
           public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
               final String responseText = response.body().string();
               //将返回的JSON数据转换成Weather 对象
               final Weather weather = Utility.handleWeatherResponse(responseText);
               //将当前线程切换到主线程
               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       if(weather!=null && "ok".equals(weather.status)){
                           SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                           editor.putString("weather",responseText);
                           editor.apply();
                           mWeatherId = weather.basic.weatherId;
                           showWeatherInfo(weather);
                       } else{
                           Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                       }
                       //请求结束后调用SwipeRefreshLayout的setRefreshing() 方法并传入false ， 用于表示刷新事件结束， 并隐藏刷新进度条
                       swipeRefresh.setRefreshing(false);
                   }
               });
           }

       });
    //每次请求天气信息的时候会同步刷新背景图片
      loadBingPic();
   }

  //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        weatherInfoText.setText(weatherInfo);
        degreeText.setText(degree);
        forecastLayout.removeAllViews();
        for(Forecast forecast :weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度： " + weather.suggestion.comfort.info;
        String carWash = "洗车指数： " + weather.suggestion.carWash.info;
        String sport = "运动建议： " + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        //激活AutoUpdateService服务
        /*这样只要一旦选中了某个城市并成功更新天气之后， AutoUpdateService就会一直在后台运行， 并保证每8小时更新一次天气。*/
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);


    }

    //加载每日一图
    private void loadBingPic(){
        //HttpUtil.sendOkHttpRequest() 方法获取到必应背景图的链接
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String bingPic = response.body().string();
                //将这个链接缓存到SharedPreferences当中
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                //将当前线程切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //将当前线程切换到主线程
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }


}
