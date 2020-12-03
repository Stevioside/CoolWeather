package com.coolweather.android.db;

import org.litepal.crud.DataSupport;

public class KeepInNum extends DataSupport {
    private String num;
    private String cityName;

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}
