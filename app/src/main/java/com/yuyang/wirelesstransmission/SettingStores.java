package com.yuyang.wirelesstransmission;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Created by vista on 2017/11/25.
 */

public class SettingStores {
    private String portSignalDefault,portFileDefault,timeOut;
    private String shUri;
    private Context context;

    private static String[] settings={"def_sig_port","def_file_port","time_out","def_dir"};
    private static  SettingStores settingStores;
    public static SettingStores getSetingStores(Context c) {
        settingStores=new SettingStores(c);
        return settingStores;
    }

    public SettingStores(Context context){
        this.context=context;
        reading_saving();
    }


    private void reading_saving(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        portSignalDefault=sp.getString(settings[0],"11080");
        portFileDefault=sp.getString(settings[1],"11081");
        timeOut=sp.getString(settings[2],"10000");
        shUri=sp.getString(settings[3],null);
    }

    public void saving(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(settings[0],portSignalDefault);
        editor.putString(settings[1],portFileDefault);
        editor.putString(settings[2],timeOut);
        editor.putString(settings[3],shUri);
        editor.commit();
    }

    private void clearSave(){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();
        Toast.makeText(context,"history cleared",Toast.LENGTH_SHORT).show();
    }

    public String getPortSignalDefault() {
        return portSignalDefault;
    }

    public int getDefaultSignalPort(){
        return Integer.parseInt(portSignalDefault);
    }

    public void setPortSignalDefault(String portSignalDefault) {
        this.portSignalDefault = portSignalDefault;
    }

    public String getPortFileDefault() {
        return portFileDefault;
    }

    public void setPortFileDefault(String portFileDefault) {
        this.portFileDefault = portFileDefault;
    }
    public int getDefaultFilePort(){
        return Integer.parseInt(portFileDefault);
    }

    public String getTimeOut() {
        return timeOut;
    }

    public int getTime_Out(){
        return Integer.parseInt(timeOut);
    }

    public void setTimeOut(String timeOut) {
        this.timeOut = timeOut;
    }


    public String getShUri() {
        return shUri;
    }

    public void setShUri(String shUri) {
        this.shUri = shUri;
    }


    public static String[] getSettings() {
        return settings;
    }

    public static void setSettings(String[] settings) {
        SettingStores.settings = settings;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
