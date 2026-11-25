package com.example.shoes_store;

import android.app.Application;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.SessionSyncHelper;
import com.example.shoes_store.module.cart.CartRepository;
import com.example.shoes_store.module.home.HomeRepository;
import com.example.shoes_store.module.notification.NotificationRepository;
import com.example.shoes_store.module.order.OrderRepository;
import com.example.shoes_store.module.product.ProductRepository;
import com.example.shoes_store.module.profile.ProfileRepository;
import com.example.shoes_store.module.voucher.VoucherRepository;
import com.example.shoes_store.module.wishlist.WishlistRepository;

/**
 * Application class khởi tạo SharedPreferences và đăng ký vòng đời đồng bộ Cloud Firestore.
 */
public class ShoesStoreApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppPrefsHelper.init(this);
        SessionSyncHelper sessionSync = SessionSyncHelper.getInstance();
        sessionSync.register(HomeRepository.getInstance());
        sessionSync.register(ProductRepository.getInstance());
        sessionSync.register(VoucherRepository.getInstance());
        sessionSync.register(ProfileRepository.getInstance());
        sessionSync.register(WishlistRepository.getInstance());
        sessionSync.register(CartRepository.getInstance());
        sessionSync.register(NotificationRepository.getInstance());
        sessionSync.register(OrderRepository.getInstance());
        sessionSync.init(this);
    }
}
