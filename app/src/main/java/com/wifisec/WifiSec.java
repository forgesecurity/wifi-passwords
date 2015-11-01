package com.wifisec;

public class WifiSec {
    public String wifi_title;
    public String wifi_bssid;
    public int wifi_security;

    public WifiSec(String wifi_title, String wifi_bssid, int wifi_security){
        this.wifi_title = wifi_title;
        this.wifi_security = wifi_security;
        this.wifi_bssid = wifi_title;
    }

    public String getssid(){
        return this.wifi_title;
    }

    public String getbssid(){
        return this.wifi_bssid;
    }
}