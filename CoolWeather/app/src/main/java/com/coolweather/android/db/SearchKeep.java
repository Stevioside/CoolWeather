package com.coolweather.android.db;

import org.litepal.crud.DataSupport;

public class SearchKeep extends DataSupport {
    private String num;

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}
