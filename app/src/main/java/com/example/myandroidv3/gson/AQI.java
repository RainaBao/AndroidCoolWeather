package com.example.myandroidv3.gson;

import com.google.gson.annotations.SerializedName;

public class AQI {
    public AQICity city;
    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
