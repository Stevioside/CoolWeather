package com.coolweather.android;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.KeepInNum;
import com.coolweather.android.db.Province;
import com.coolweather.android.db.SearchKeep;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;
import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.security.auth.login.LoginException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    public DrawerLayout drawerLayout;
    private Button navButton;

    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;

    private TextView comfortText;
    private TextView sportText;

    private TextView keepIn;
    private TextView keep;

    private List<SearchKeep> searchKeeps;

    private AutoCompleteTextView editText;
    private TextView search;
    private TextView refresh;
    private TextView time_text;

    private TextView hum_text;
    ArrayAdapter<String> adapter;
    String[] str;
    List<String> searchList = new ArrayList<>();;
    private boolean keepFlag = false;

    private List<KeepInNum> keepInNumList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        //初始化各个控件
        keep = findViewById(R.id.keep);
        editText = findViewById(R.id.editText);
        search = findViewById(R.id.search);
        weatherLayout = (ScrollView)findViewById(R.id.weather_layout);
        titleCity = (TextView)findViewById(R.id.title_city);
        time_text = findViewById(R.id.time_text);
        hum_text = findViewById(R.id.hum_text);
        degreeText = (TextView)findViewById(R.id.degree_text);
        weatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);
        comfortText = (TextView)findViewById(R.id.comfort_text);
        sportText = (TextView)findViewById(R.id.sport_text);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        refresh = findViewById(R.id.refresh);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestWeather(mWeatherId,false);
            }
        });
        adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_activated_1,searchList);
        editText.setAdapter(adapter);
        editText.setDropDownAnchor(R.id.editText);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        keepIn = findViewById(R.id.keepIn);
        keepIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPopupWindowDialog(view);
            }
        });

        swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        String weatherString = prefs.getString("weather",null);
//        if(weatherString!=null){
//            //有缓存时直接解析数据
//            Weather weather = Utility.handleWeatherResponse(weatherString);
//            mWeatherId = weather.basic.weatherId;
//            showWeatherInfo(weather);
//        }else{
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
//            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId,false);
//        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId,false);
            }
        });
        keep.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                keepFlag = !keepFlag;
                if(keepFlag){
                    KeepInNum keepInNum = new KeepInNum();
                    keepInNum.setCityName(titleCity.getText().toString());
                    keepInNum.setNum(mWeatherId);
                    keepInNum.save();
                }else{
                    DataSupport.deleteAll(KeepInNum.class,"num = ?",mWeatherId);
                }
                queryKeep();
            }
        });
        keepInNumList = new ArrayList<>();
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestWeather(editText.getText().toString(),true);
                queryKeep();
            }
        });
        queryKeep();
    }

    //根据天气id请求城市天气信息
    public void requestWeather(final String weatherId, final boolean flag){
        Log.i("weatherId",weatherId);
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=8bc4e62241894d7f8b26384ba1b6da60";
//        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=7504c02355ad4d5b9d81f5e5ec164889";
        //String weatherUrl = "https://free-api.heweather.net/v5/weather?city="+weatherId+"&key=7504c02355ad4d5b9d81f5e5ec164889";
//        String weatherUrl = "https://free-api.heweather.net/v5/weather?city=CN101190407&key=7504c02355ad4d5b9d81f5e5ec164889";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }

                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("response",response.toString());
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                Log.i("success-------------------","getWeather");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null &&"ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                            queryKeep();
                            if (flag){
                                SearchKeep searchKeep = new SearchKeep();
                                searchKeep.setNum(weatherId);
                                searchKeep.save();
                            }
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });

            }
        });

    }

    private void queryKeep(){
        keepInNumList = DataSupport.findAll(KeepInNum.class);
        searchKeeps = DataSupport.findAll(SearchKeep.class);
        searchList.clear();
        Collections.reverse(searchKeeps);
        int size = searchKeeps.size()>=3?3:searchKeeps.size()==0?0:searchKeeps.size();
        Log.i("maxsize",size+"  "+searchKeeps.size());
        for(int i=0;i<size;i++){
            searchList.add(searchKeeps.get(i).getNum());
        }
        Collections.reverse(searchList);
        Log.i("size",searchList.size()+"");
        adapter.notifyDataSetChanged();
        list2Array();
    }

    //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather){
        String cityName = weather.basic.cityName;
        String degree = weather.now.temperature+"℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(weather.basic.parent_city+" · "+cityName);
        hum_text.setText("湿度："+weather.now.hum);
        SimpleDateFormat s = new SimpleDateFormat("HH:mm:ss");
        time_text.setText("更新时间： "+s.format(new Date()));
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for(Forecast forecast:weather.forecastList){
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
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

        String comfort = "舒适度"+weather.suggestion.comfort.info;
        String sport = "运动建议"+weather.suggestion.sport.info;
        comfortText.setText(comfort);

        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    /**
     * @param view 设置参考组件
     * */
    public void listPopupWindowDialog(View view) {
        queryKeep();
        final ListPopupWindow listPopupWindow = new ListPopupWindow(this);
        if(keepInNumList.size()>0){
            list2Array();
            ArrayAdapter listPopupWindowAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_activated_1,str);

            // 添加适配器
            listPopupWindow.setAdapter(listPopupWindowAdapter);
            // 设置弹窗的大小
            listPopupWindow.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
            listPopupWindow.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
            //设置参考组件
            listPopupWindow.setAnchorView(view);
            // 设置背景
            //listPopupWindow.setBackgroundDrawable();
            //模态框，设置为true响应物理键
            listPopupWindow.setModal(true);
            listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Toast.makeText(WeatherActivity.this,str[i]+"is selected",Toast.LENGTH_SHORT).show();
                    requestWeather(keepInNumList.get(i).getNum(),false);
                    listPopupWindow.dismiss();
                }
            });
            listPopupWindow.show();
        }else{
            Toast.makeText(WeatherActivity.this,"请先关注城市",Toast.LENGTH_SHORT).show();
        }
    }

    public void list2Array(){
        boolean keepFlag = false;
        keepInNumList = DataSupport.findAll(KeepInNum.class);
        List<String> temps = new ArrayList<>();
        for(KeepInNum num:keepInNumList){
            temps.add(num.getCityName());
            Log.i("find",num.getCityName()+"  "+titleCity.getText().toString());
            if(num.getCityName().equals(titleCity.getText().toString())){
                keepFlag = true;
            }
        }
        str = new String[keepInNumList.size()];
        temps.toArray(str);
        buttonStatus(keepFlag);
    }

    public void buttonStatus(boolean keepFlag){
        if(keepFlag){
            keep.setText("已关注");
            keep.setBackground(getResources().getDrawable(R.drawable.shape_keeped));
        }else{
            keep.setText("切换城市");
            keep.setBackground(getResources().getDrawable(R.drawable.shape_keep));
        }
    }
}
