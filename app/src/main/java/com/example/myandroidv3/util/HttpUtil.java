package com.example.myandroidv3.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    //注册一个回调来处理服务器响应
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);
    }

}
