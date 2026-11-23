package com.example.shoes_store;

import android.app.Application;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.FirestoreStoreHelper;

/**
 * Application class khởi tạo SharedPreferences và đồng bộ Cloud Firestore khi mở app.
 */
public class ShoesStoreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppPrefsHelper.init(this);
        FirestoreStoreHelper.getInstance().init(this);
    }
}
