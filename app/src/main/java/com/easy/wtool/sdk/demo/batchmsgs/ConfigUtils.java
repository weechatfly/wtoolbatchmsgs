package com.easy.wtool.sdk.demo.batchmsgs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by TK on 2017-02-06.
 */

public class ConfigUtils {
    public static final String KEY_APPID = "AppId";
    public static final String KEY_AUTHCODE = "AuthCode";
    public static final String KEY_WXID = "WxId";
    private String PROFILE_NAME = "wtoolsdkdemorobot";
    private Context context;

    public ConfigUtils(Context context) {
        this.context = context;
        PROFILE_NAME = context.getPackageName();
    }

    /**
     * 保存参数
     * @param key  参数名
     * @param value   参数值
     */
    public void save(String section,String key, String value) {
        //获得SharedPreferences对象
        SharedPreferences preferences = context.getSharedPreferences(PROFILE_NAME+"."+section, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }
    public void save(String key, String value) {
        save("params",key,value);
    }
    /**
     * 获取参数
     * @return
     */
    public String get(String section,String key,String defvalue) {

        SharedPreferences preferences = context.getSharedPreferences(PROFILE_NAME+"."+section, Context.MODE_PRIVATE);
        return preferences.getString(key, defvalue);
    }
    public String get(String key,String defvalue){
        return get("params",key,defvalue);
    }



}
