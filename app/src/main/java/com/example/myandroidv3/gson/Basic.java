package com.example.myandroidv3.gson;

import com.google.gson.annotations.SerializedName;

public class Basic {
    //SerializedName注释的方式让JSON字段与java字段之间建立映射关系
    @SerializedName("city")
    public String cityName;
    @SerializedName("id")
    public String weatherId;
    public Update update;
    public class Update{
        @SerializedName("loc")
        public String updateTime;
    }


}
