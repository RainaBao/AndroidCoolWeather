package com.example.myandroidv3;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myandroidv3.db.City;
import com.example.myandroidv3.db.County;
import com.example.myandroidv3.db.Province;
import com.example.myandroidv3.util.HttpUtil;
import com.example.myandroidv3.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /*省列表*/
    private List<Province> provinceList;
    /*市列表*/
    private List<City> cityList;
    /*县列表*/
    private List<County> countyList;
    /*选中的省份*/
    private Province selectedProvince;
    /*选中的城市*/
    private City selectedCity;
    /*当前选中的级别*/
    private int currentLevel;
//1.onCreateView() 方法中先是获取到了一些控件的实例，
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        return super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        //适配器
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
       return view;
    }
    //2.onActivityCreated() 方法中给ListView和Button设置了点击事件
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(i);
                    //查询市
                    queryCities();
                }
                else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(i);
                    //查询县
                    queryCounties();
                } else if(currentLevel==LEVEL_COUNTY){
                    String weatherId = countyList.get(i).getWeatherId();
                    //判断出该碎片是否在MainActivity中，即是否属于MainActivity类
                    if(getActivity() instanceof MainActivity){
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity){
                        //判断出该碎片是否在WeatherActivity中
                        WeatherActivity activity =(WeatherActivity)getActivity();
                        //关掉滑动菜单
                        activity.drawerLayout.closeDrawers();
                        //显示下拉刷新进度条
                        activity.swipeRefresh.setRefreshing(true);
                        //请求新城市的天气信息
                        activity.requestWeather(weatherId);

                    }

                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentLevel==LEVEL_COUNTY){
                    //查询市
                    queryCities();
                }
                else if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });
        //也就是从这里开始加载省级数据的
        queryProvinces();
    }
    //查询所有省，先从数据库查询，如果没有的话再去服务器查询
    private void queryProvinces(){
        //首先会将头布局的标题设置成中国，
        titleText.setText("中国");
        //将返回按钮隐藏起来， 因为省级列表已经不能再返回了
        backButton.setVisibility(View.GONE);
        //调用LitePal的查询接口来从数据库中读取省级数据
        provinceList = DataSupport.findAll(Province.class);
        //如果读取到了就直接将数据显示到界面上
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            //没有读取到就调用queryFromServer() 方法来从服务器上查询数据
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            //调用ArrayAdapter类的adapter实例的notifyDataSetChanged() 方法通知数据发生了变化，
            adapter.notifyDataSetChanged();
            //将光标移动到最开始的位置？
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }

    }
    //根据传入的地址和类型用queryFromServer() 方法来从服务器上查询数据
    private void queryFromServer(String address,final String type){
        showProgressDialog();
        Log.d("chooseAreaFragment", "queryFromServer: type:"+type+" address:"+address);
        //调用HttpUtil的sendOkHttpRequest()方法来向服务器发送请求
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.d("chooseAreaFragment", "onFailure: "+call+"IOException:"+e);
                //通过runOnUiThread回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_LONG).show();
                    }
                });
            }
            //响应的数据会回调到onResponse() 方法中
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    //去调用Utility的handleProvincesResponse() 方法来解析和处理服务器返回的数据， 并存储到数据库
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                //牵扯到了UI操作， 因此必须要在主线程中调用
                //借助了runOnUiThread() 方法来实现从子线程切换到主线程
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                //调用queryProvinces() 方法来重新加载省级数据，
                                queryProvinces();
                            } else if ("city".equals(type)){
                                queryCities();

                            } else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });

        //调用queryProvinces() 就直接将数据显示到界面上

    }
    //显示进度对话框
    private void showProgressDialog(){
    if(progressDialog == null){
        progressDialog =  new ProgressDialog(getActivity());
        progressDialog.setMessage("正在加载中");
        progressDialog.setCanceledOnTouchOutside(false);
    }
    progressDialog.show();
    }
    //关闭进度对话框
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
