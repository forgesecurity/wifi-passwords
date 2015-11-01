package com.wifisec;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class RequeteWifi extends AsyncTask<Void, Void, Void> {

    String result = "";
    String url = "";
    Context context = null;
    WifiManager wifi;
    WifiScanReceiver wifireceiver;
    List<ScanResult> wifiScanList;
    double location_x;
    double location_y;

    private class WifiScanReceiver extends BroadcastReceiver {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void onReceive(Context c, Intent intent) {

            if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo networkInfo =
                            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if(networkInfo.isConnected()) {
                        // Wifi is connected
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                            String mBssid = intent.getStringExtra(WifiManager.EXTRA_BSSID);
                        }
                    }
                } else if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                NetworkInfo networkInfo =
                        intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI &&
                        !networkInfo.isConnected()) {
                    // Wifi is disconnected
                }
            }
            else if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

                wifiScanList = wifi.getScanResults();
                FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(context);
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                for (int i = 0; i < wifiScanList.size(); i++) {

                    if (!(((wifiScanList.get(i)).SSID).isEmpty())) {
                        ContentValues values = new ContentValues();
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_TITLE, ((wifiScanList.get(i)).SSID));
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_BSSID, ((wifiScanList.get(i)).BSSID));
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_LASTSCAN, ((wifiScanList.get(i)).timestamp));

                        int security = check_security(wifiScanList.get(i).capabilities);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_SECURITY, security);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_ROBUSTNESS, security);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_PASSWORD, "null");
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_COORDINATES_X, location_x);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_COORDINATES_Y, location_y);
                        long newRowId = db.insert(FeedReaderContract.FeedEntry.TABLE_NAME_WIFI, "null", values);
                    }
                }
            }
        }
    }

    public boolean connect_wifi(Context tmpcontext, String bssid, String ssid, String password) {

        if (!wifi.isWifiEnabled()) {
            wifi.setWifiEnabled(true);
        }

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.preSharedKey = "\"" + password + "\"";

            int ret = this.wifi.addNetwork(conf);
            boolean returne = this.wifi.enableNetwork(ret, true);
            this.wifi.saveConfiguration();
            this.wifi.reconnect();


            if (ret > 0) {
                FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(tmpcontext);
                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_ROBUSTNESS, 0);
                values.put(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_PASSWORD, password);
                long newRowId = db.update(FeedReaderContract.FeedEntry.TABLE_NAME_WIFI, values, FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_BSSID + "=\"" + bssid + "\"", null);

                return true;
            }

        return false;
    }

    private int check_security(String capabilities)
    {
        if (capabilities.toLowerCase().contains("WEP".toLowerCase())
                || (capabilities.toLowerCase().contains("WPA2".toLowerCase()))
                    || (capabilities.toLowerCase().contains("WPA".toLowerCase())))
            return 1;

        return 0;
    }

    public RequeteWifi(Context context)
    {
        this.context = context;

        this.wifi = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.wifireceiver = new WifiScanReceiver();

        IntentFilter filter1 = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter1.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        this.context.registerReceiver(wifireceiver, filter1);
        IntentFilter filter2 = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter2.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.context.registerReceiver(wifireceiver, filter2);
        IntentFilter filter3 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter3.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.context.registerReceiver(wifireceiver, filter3);
    }

    public boolean clearWifi() {
        if (!disconnectAP()) {
            return false;
        }
        // Disable Wifi
        if (!this.wifi.setWifiEnabled(false)) {
            return false;
        }
        // Wait for the actions to be completed
        try {
            Thread.sleep(5*1000);
        } catch (InterruptedException e) {}
        return true;
    }

    public boolean disconnectAP() {
        if (this.wifi.isWifiEnabled()) {
            //remove the current network Id
            WifiInfo curWifi = this.wifi.getConnectionInfo();
            if (curWifi == null) {
                return false;
            }
            int curNetworkId = curWifi.getNetworkId();
            this.wifi.removeNetwork(curNetworkId);
            this.wifi.saveConfiguration();

            // remove other saved networks
            List<WifiConfiguration> netConfList = this.wifi.getConfiguredNetworks();
            if (netConfList != null) {
                for (int i = 0; i < netConfList.size(); i++) {
                    WifiConfiguration conf = new WifiConfiguration();
                    conf = netConfList.get(i);
                    this.wifi.removeNetwork(conf.networkId);
                }
            }
        }
        this.wifi.saveConfiguration();
        return true;
    }

    @Override
    protected Void doInBackground(Void... params) {
        String locationProvider = LocationManager.NETWORK_PROVIDER;
        LocationManager locationManager = (LocationManager) this.context.getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);

        this.location_x = lastKnownLocation.getLatitude();
        this.location_y = lastKnownLocation.getLongitude();

        this.wifi.startScan();

        return null;
    }

    protected void onPostExecute()
    {

    }
}