package com.wifisec;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class HandleWifi {

    private Context context;
    private RequeteWifi reqWifi;

    public HandleWifi(Context context)
    {
        this.context = context;
        this.reqWifi = new RequeteWifi(this.context);
    }

    private void deleteWifis() {
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(FeedReaderContract.FeedEntry.TABLE_NAME_WIFI, null, null);
    }

    private void majWifis() {
        this.reqWifi.execute();
    }

    public boolean scanWifis(String ssid, String bssid)
    {
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_PASSWORDS_TITLE
        };

        String sortOrder = FeedReaderContract.FeedEntry._ID + " DESC";

        Cursor c = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME_PASSWORDS,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );


        while (c.moveToNext()) {
            long itemId = c.getLong(c.getColumnIndexOrThrow(FeedReaderContract.FeedEntry._ID));
            String password = c.getString(c.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_PASSWORDS_TITLE));

            if(this.reqWifi.connect_wifi(this.context, bssid, ssid, password))
                return true;
        }

        return false;
    }

    public ArrayList<WifiSec> getWifis() {

        ArrayList<WifiSec> items = new ArrayList<>();
        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(this.context);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_TITLE,
                FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_BSSID,
                FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_SECURITY
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                FeedReaderContract.FeedEntry._ID + " DESC";

        Cursor c = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME_WIFI,  // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );

        if(c.getCount() == 0)
        {
            this.majWifis();

            c = db.query(
                    FeedReaderContract.FeedEntry.TABLE_NAME_WIFI,  // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        }

        while (c.moveToNext()) {
            long itemId = c.getLong(c.getColumnIndexOrThrow(FeedReaderContract.FeedEntry._ID));
            String title = c.getString(c.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_TITLE));
            String bssid = c.getString(c.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_BSSID));
            int security = c.getInt(c.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_NAME_WIFI_SECURITY));

            items.add(new WifiSec(title, bssid, security));
        }

        return items;
    }
}
