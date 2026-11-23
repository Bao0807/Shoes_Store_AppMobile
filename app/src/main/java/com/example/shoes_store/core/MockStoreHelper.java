package com.example.shoes_store.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shoes_store.R;
import com.example.shoes_store.module.cart.CartItemModel;
import com.example.shoes_store.module.home.HomeBannerModel;
import com.example.shoes_store.module.notification.NotificationModel;
import com.example.shoes_store.module.order.OrderModel;
import com.example.shoes_store.module.product.ProductModel;
import com.example.shoes_store.module.product.ProductReviewModel;
import com.example.shoes_store.module.profile.ProfileAddressModel;
import com.example.shoes_store.module.profile.ProfileModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Kho dữ liệu mẫu tập trung (in-memory) phản hồi qua LiveData cho toàn bộ luồng UI-first.
 * Khi kết nối Firebase sau này, từng *Repository sẽ thay thế lời gọi từ lớp này.
 */
public final class MockStoreHelper {

    private static volatile MockStoreHelper instance;

    private final List<ProductModel> products = new ArrayList<>();
    private final List<HomeBannerModel> banners = new ArrayList<>();
    private final Set<String> favoriteIds = new LinkedHashSet<>();
    private final List<CartItemModel> cartItems = new ArrayList<>();
    private final List<NotificationModel> notifications = new ArrayList<>();
    private final List<OrderModel> orders = new ArrayList<>();
    private final List<ProfileAddressModel> addresses = new ArrayList<>();
    private final List<String> recentSearches = new ArrayList<>();
    private ProfileModel profile;

    private final MutableLiveData<List<ProductModel>> productsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Set<String>> favoriteIdsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<CartItemModel>> cartItemsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> cartTotalCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<List<NotificationModel>> notificationsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Integer> unreadNotificationCountLiveData = new MutableLiveData<>(0);
    private final MutableLiveData<List<OrderModel>> ordersLiveData = new MutableLiveData<>();
    private final MutableLiveData<ProfileModel> profileLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ProfileAddressModel>> addressesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<String>> recentSearchesLiveData = new MutableLiveData<>();

    private int orderSequence = 1024;
    private int addressSequence = 3;
    private int notificationSequence = 10;

    private MockStoreHelper() {
        seedProducts();
        seedBanners();
        seedFavorites();
        seedCart();
        seedNotifications();
        seedProfileAndAddresses();
        seedOrders();
        seedRecentSearches();
        publishAll();
    }

    public static MockStoreHelper getInstance() {
        if (instance == null) {
            synchronized (MockStoreHelper.class) {
                if (instance == null) {
                    instance = new MockStoreHelper();
                }
            }
        }
        return instance;
    }

    private void publishAll() {
        productsLiveData.setValue(new ArrayList<>(products));
        publishFavorites();
        publishCart();
        publishNotifications();
        ordersLiveData.setValue(new ArrayList<>(orders));
        profileLiveData.setValue(profile);
        addressesLiveData.setValue(new ArrayList<>(addresses));
        recentSearchesLiveData.setValue(new ArrayList<>(recentSearches));
    }

    private void publishFavorites() {
        favoriteIdsLiveData.setValue(new LinkedHashSet<>(favoriteIds));
    }

    private void publishCart() {
        List<CartItemModel> snapshot = new ArrayList<>();
        int totalQty = 0;
        for (CartItemModel item : cartItems) {
            snapshot.add(item.copy());
            totalQty += item.getQuantity();
        }
        cartItemsLiveData.setValue(snapshot);
        cartTotalCountLiveData.setValue(totalQty);
    }

    private void publishNotifications() {
        notificationsLiveData.setValue(new ArrayList<>(notifications));
        int unread = 0;
        for (NotificationModel n : notifications) {
            if (n.isUnread()) {
                unread++;
            }
        }
        unreadNotificationCountLiveData.setValue(unread);
    }

    private void seedProducts() {
        List<String> standardSizes = Arrays.asList("38", "39", "40", "40.5", "41", "42", "43", "44");

        products.add(new ProductModel(
                "p_novablast_5",
                "Novablast 5",
                "ASICS",
                "Daily",
                3_490_000L,
                3_890_000L,
                4.9,
                128,
                R.drawable.img_shoe_asics_novablast,
                new int[]{R.drawable.img_shoe_asics_novablast, R.drawable.img_shoe_nike_pegasus, R.drawable.img_shoe_hoka_bondi},
                "255g",
                "8 mm",
                "Cao",
                "Road",
                "Đôi giày chạy hằng ngày thế hệ mới với đệm FF Blast Max êm và đàn hồi vượt trội, phù hợp cho cả easy run lẫn long run cuối tuần.",
                Arrays.asList("Trắng / Cam", "Đen / Bạc", "Xanh Navy"),
                Arrays.asList("#FF5A36", "#1F2937", "#1D4ED8"),
                standardSizes,
                new HashSet<>(Collections.singletonList("44")),
                Arrays.asList(
                        new ProductReviewModel("Hoàng Nam", 5.0f, "18/11/2026", "Đệm êm mà vẫn có độ nảy rất sướng, chạy 15km không đau gót."),
                        new ProductReviewModel("Thảo Vy", 4.8f, "12/11/2026", "Form ôm chân vừa vặn, thoáng khí tốt khi chạy buổi trưa.")
                )
        ));

        products.add(new ProductModel(
                "p_alphafly_3",
                "Alphafly 3",
                "Nike",
                "Race",
                7_490_000L,
                0L,
                5.0,
                214,
                R.drawable.img_shoe_nike_alphafly,
                new int[]{R.drawable.img_shoe_nike_alphafly, R.drawable.img_shoe_adidas_adios},
                "218g",
                "8 mm",
                "Carbon / Max",
                "Road",
                "Siêu giày thi đấu marathon tích hợp tấm carbon Flyplate toàn phần và hai túi khí Air Zoom phía trước, tối ưu hoàn trả lực cho ngày race.",
                Arrays.asList("Volt / Trắng", "Cam Prototype", "Đen Carbon"),
                Arrays.asList("#84CC16", "#F97316", "#111827"),
                standardSizes,
                new HashSet<>(Arrays.asList("38", "44")),
                Arrays.asList(
                        new ProductReviewModel("Tuấn Kiệt", 5.0f, "20/11/2026", "Phá PR 21K ngay lần đầu xỏ chân, giữ pace 4:00 cực kỳ nhẹ nhàng."),
                        new ProductReviewModel("Đức Anh", 5.0f, "09/11/2026", "Chuyển tiếp gót - mũi mượt, xứng đáng cho các giải marathon.")
                )
        ));

        products.add(new ProductModel(
                "p_endorphin_4",
                "Endorphin Speed 4",
                "Saucony",
                "Tempo",
                4_290_000L,
                4_790_000L,
                4.8,
                96,
                R.drawable.img_shoe_saucony_endorphin,
                new int[]{R.drawable.img_shoe_saucony_endorphin, R.drawable.img_shoe_asics_novablast},
                "233g",
                "8 mm",
                "Nảy / Nylon",
                "Road",
                "Trang bị tấm nylon có cánh kết hợp bọt PWRRUN PB và công nghệ SpeedRoll, lý tưởng cho các buổi chạy tempo, interval hoặc biến tốc.",
                Arrays.asList("Xanh Cobalt", "Đen / Vàng", "Trắng Đỏ"),
                Arrays.asList("#2563EB", "#F59E0B", "#EF4444"),
                standardSizes,
                new HashSet<>(Collections.singletonList("39")),
                Arrays.asList(
                        new ProductReviewModel("Quốc Bảo", 4.9f, "15/11/2026", "Nhẹ và dễ kiểm soát hơn giày carbon cứng, tập track rất thích."),
                        new ProductReviewModel("Minh Tú", 4.7f, "04/11/2026", "Màu xanh ngoài đời cực đẹp, bám đường tốt kể cả khi sân ướt.")
                )
        ));

        products.add(new ProductModel(
                "p_speedgoat_6",
                "Speedgoat 6",
                "HOKA",
                "Trail",
                3_990_000L,
                0L,
                4.8,
                85,
                R.drawable.img_shoe_hoka_speedgoat,
                new int[]{R.drawable.img_shoe_hoka_speedgoat, R.drawable.img_shoe_hoka_bondi},
                "278g",
                "5 mm",
                "Vừa / Bám",
                "Trail",
                "Giày chạy địa hình với đế ngoài Vibram Megagrip gai 5mm bám đá ướt và bùn lầy xuất sắc, bảo vệ bàn chân trên mọi cung đường núi.",
                Arrays.asList("Rêu / Cam", "Xám Đá", "Xanh Dương"),
                Arrays.asList("#0F766E", "#4B5563", "#0284C7"),
                standardSizes,
                new HashSet<>(Collections.singletonList("43")),
                Arrays.asList(
                        new ProductReviewModel("Văn Hùng", 4.9f, "19/11/2026", "Chạy giải 42km Đà Lạt bám dốc cực chắc, không bị trượt mũi."),
                        new ProductReviewModel("Lan Phương", 4.7f, "10/11/2026", "Upper ôm chân ổn định khi đổ dốc kỹ thuật.")
                )
        ));

        products.add(new ProductModel(
                "p_bondi_9",
                "Bondi 9",
                "HOKA",
                "Recovery",
                4_390_000L,
                4_690_000L,
                4.7,
                112,
                R.drawable.img_shoe_hoka_bondi,
                new int[]{R.drawable.img_shoe_hoka_bondi, R.drawable.img_shoe_brooks_glycerin},
                "297g",
                "5 mm",
                "Siêu êm",
                "Road",
                "Khối đệm EVA dày dặn nhất dòng Road của HOKA giúp hấp thụ chấn động tối đa, giảm áp lực lên khớp gối trong các buổi chạy phục hồi.",
                Arrays.asList("Băng Xanh", "Đen Tuyền", "Xám Khói"),
                Arrays.asList("#38BDF8", "#111827", "#9CA3AF"),
                standardSizes,
                new HashSet<>(Collections.emptySet()),
                Arrays.asList(
                        new ProductReviewModel("Ngọc Mai", 4.8f, "14/11/2026", "Chạy phục hồi sau race hoặc đi bộ nhiều cả ngày cực êm."),
                        new ProductReviewModel("Hải Đăng", 4.6f, "02/11/2026", "Đế dày nhưng lên chân rất vững nhờ khung gót rộng.")
                )
        ));

        products.add(new ProductModel(
                "p_adios_pro_4",
                "Adizero Adios Pro 4",
                "Adidas",
                "Race",
                6_500_000L,
                7_000_000L,
                4.9,
                167,
                R.drawable.img_shoe_adidas_adios,
                new int[]{R.drawable.img_shoe_adidas_adios, R.drawable.img_shoe_nike_alphafly},
                "200g",
                "6 mm",
                "Carbon / Nhẹ",
                "Road",
                "Sử dụng thanh EnergyRods 2.0 mô phỏng xương bàn chân cùng 2 lớp đệm Lightstrike Pro siêu nhẹ, hỗ trợ bứt tốc ở nửa cuối marathon.",
                Arrays.asList("Trắng / Đỏ", "Vàng Chanh", "Đen Bạc"),
                Arrays.asList("#DC2626", "#EAB308", "#1F2937"),
                standardSizes,
                new HashSet<>(Collections.singletonList("40.5")),
                Arrays.asList(
                        new ProductReviewModel("Thành Long", 5.0f, "21/11/2026", "Nhẹ như không, đế Continental bám đường nhựa ướt rất yên tâm."),
                        new ProductReviewModel("Gia Huy", 4.8f, "08/11/2026", "Upper Lightlock ôm gót tốt hơn hẳn đời trước.")
                )
        ));

        products.add(new ProductModel(
                "p_pegasus_41",
                "Pegasus 41",
                "Nike",
                "Daily",
                3_290_000L,
                0L,
                4.7,
                310,
                R.drawable.img_shoe_nike_pegasus,
                new int[]{R.drawable.img_shoe_nike_pegasus, R.drawable.img_shoe_asics_novablast},
                "285g",
                "10 mm",
                "Cân bằng",
                "Road",
                "Biểu tượng giày chạy quốc dân nay nâng cấp bọt ReactX kết hợp hai túi Air Zoom trước sau, bền bỉ hàng trăm km cho người mới lẫn runner lâu năm.",
                Arrays.asList("Đen / Cam", "Trắng Xám", "Xanh Lục"),
                Arrays.asList("#111827", "#D1D5DB", "#16A34A"),
                standardSizes,
                new HashSet<>(Collections.emptySet()),
                Arrays.asList(
                        new ProductReviewModel("Khánh Linh", 4.7f, "17/11/2026", "Đôi giày đầu tiên mình mua để tập 10K, đi tập gym cũng hợp."),
                        new ProductReviewModel("Trung Hiếu", 4.7f, "05/11/2026", "Đế bền thật sự, chạy hơn 400km mà gai đế vẫn còn nguyên.")
                )
        ));

        products.add(new ProductModel(
                "p_glycerin_max",
                "Glycerin Max",
                "Brooks",
                "Recovery",
                4_890_000L,
                0L,
                4.8,
                64,
                R.drawable.img_shoe_brooks_glycerin,
                new int[]{R.drawable.img_shoe_brooks_glycerin, R.drawable.img_shoe_hoka_bondi},
                "298g",
                "6 mm",
                "Max Cushion",
                "Road",
                "Công nghệ bọt DNA Tuned truyền nitơ kép giúp tiếp đất mềm mại ở gót và bật gọn ở mũi chân, bảo vệ đôi chân trong mọi buổi long run.",
                Arrays.asList("Xám / Bạc Hà", "Đen / Xanh", "Trắng Kem"),
                Arrays.asList("#34D399", "#1E293B", "#E5E7EB"),
                standardSizes,
                new HashSet<>(Collections.singletonList("38")),
                Arrays.asList(
                        new ProductReviewModel("Bình An", 4.9f, "16/11/2026", "Cảm giác tiếp đất siêu mượt, rất hợp với runner nặng cân."),
                        new ProductReviewModel("Tấn Phát", 4.7f, "01/11/2026", "Mũi giày thoáng, chạy 20km không bị nóng chân.")
                )
        ));
    }

    private void seedBanners() {
        banners.add(new HomeBannerModel(
                "MARATHON SEASON",
                "Race Day Carbon",
                "Bứt phá kỷ lục cá nhân với bộ sưu tập siêu giày thi đấu",
                "Xem dòng Race",
                "Race",
                R.drawable.img_banner_race
        ));
        banners.add(new HomeBannerModel(
                "DAILY TRAINER",
                "Êm Ái Mỗi Km",
                "Bộ đôi ASICS & Nike đồng hành bền bỉ trên từng cung đường",
                "Xem dòng Daily",
                "Daily",
                R.drawable.img_banner_daily
        ));
    }

    private void seedFavorites() {
        favoriteIds.add("p_novablast_5");
        favoriteIds.add("p_alphafly_3");
    }

    private void seedCart() {
        ProductModel novablast = getProductById("p_novablast_5");
        ProductModel endorphin = getProductById("p_endorphin_4");
        if (novablast != null) {
            cartItems.add(new CartItemModel("c_1", novablast, "42", "Trắng / Cam", 1, true));
        }
        if (endorphin != null) {
            cartItems.add(new CartItemModel("c_2", endorphin, "42", "Xanh Cobalt", 1, true));
        }
    }

    private void seedNotifications() {
        notifications.add(new NotificationModel(
                "n_1",
                "Đơn #RN20261023 đang được giao",
                "Shipper đã nhận đôi Nike Alphafly 3 và dự kiến giao trong hôm nay.",
                "12 phút trước",
                NotificationModel.Type.ORDER,
                true
        ));
        notifications.add(new NotificationModel(
                "n_2",
                "Mã RUN50K giảm 150.000₫ cuối tuần",
                "Áp dụng trực tiếp tại Giỏ hàng cho toàn bộ giày Daily & Tempo.",
                "2 giờ trước",
                NotificationModel.Type.PROMO,
                true
        ));
        notifications.add(new NotificationModel(
                "n_3",
                "Mở bán Adidas Adizero Adios Pro 4",
                "Phối màu Trắng / Đỏ đã có đủ size EU 38 - 44 tại cửa hàng.",
                "Hôm qua",
                NotificationModel.Type.DROP,
                true
        ));
        notifications.add(new NotificationModel(
                "n_4",
                "Đơn #RN20261015 đã hoàn thành",
                "Cảm ơn bạn đã tin tưởng Shoes Store. Chúc bạn tập luyện thật sung!",
                "3 ngày trước",
                NotificationModel.Type.ORDER,
                false
        ));
        notifications.add(new NotificationModel(
                "n_5",
                "Ưu đãi thành viên Runner Club",
                "Miễn phí vệ sinh giày chuyên sâu cho đơn hàng từ 4.000.000₫.",
                "1 tuần trước",
                NotificationModel.Type.PROMO,
                false
        ));
    }

    private void seedProfileAndAddresses() {
        profile = new ProfileModel(
                "Minh Quân",
                "minhquan.runner@gmail.com",
                "0912 345 678",
                "42",
                "Daily & Half Marathon",
                R.drawable.img_avatar_runner
        );
        addresses.add(new ProfileAddressModel(
                "addr_1",
                "Minh Quân",
                "0912 345 678",
                "128 Nguyễn Thị Minh Khai, Phường Võ Thị Sáu, Quận 3, TP.HCM",
                "Nhà riêng",
                true
        ));
        addresses.add(new ProfileAddressModel(
                "addr_2",
                "Minh Quân (Công ty)",
                "0912 345 678",
                "Tầng 8, Tòa nhà E-Town, 364 Cộng Hòa, Tân Bình, TP.HCM",
                "Văn phòng",
                false
        ));
    }

    private void seedOrders() {
        ProductModel alphafly = getProductById("p_alphafly_3");
        ProductModel pegasus = getProductById("p_pegasus_41");
        ProductModel bondi = getProductById("p_bondi_9");

        if (alphafly != null) {
            List<CartItemModel> items = Collections.singletonList(
                    new CartItemModel("oi_1", alphafly, "42", "Volt / Trắng", 1, true)
            );
            orders.add(new OrderModel(
                    "RN20261023",
                    "22/11/2026 · 08:45",
                    OrderModel.Status.SHIPPING,
                    items,
                    "128 Nguyễn Thị Minh Khai, Phường Võ Thị Sáu, Quận 3, TP.HCM",
                    "Minh Quân · 0912 345 678",
                    "Giao nhanh 24h",
                    "Chuyển khoản QR",
                    7_490_000L,
                    0L,
                    0L,
                    7_490_000L
            ));
        }

        if (pegasus != null) {
            List<CartItemModel> items = Collections.singletonList(
                    new CartItemModel("oi_2", pegasus, "42", "Đen / Cam", 1, true)
            );
            orders.add(new OrderModel(
                    "RN20261022",
                    "21/11/2026 · 19:20",
                    OrderModel.Status.PENDING,
                    items,
                    "Tầng 8, Tòa nhà E-Town, 364 Cộng Hòa, Tân Bình, TP.HCM",
                    "Minh Quân · 0912 345 678",
                    "Giao tiêu chuẩn",
                    "Thanh toán khi nhận hàng (COD)",
                    3_290_000L,
                    30_000L,
                    0L,
                    3_320_000L
            ));
        }

        if (bondi != null) {
            List<CartItemModel> items = Collections.singletonList(
                    new CartItemModel("oi_3", bondi, "41", "Băng Xanh", 1, true)
            );
            orders.add(new OrderModel(
                    "RN20261015",
                    "15/11/2026 · 10:15",
                    OrderModel.Status.COMPLETED,
                    items,
                    "128 Nguyễn Thị Minh Khai, Phường Võ Thị Sáu, Quận 3, TP.HCM",
                    "Minh Quân · 0912 345 678",
                    "Giao tiêu chuẩn",
                    "Ví điện tử",
                    4_390_000L,
                    30_000L,
                    150_000L,
                    4_270_000L
            ));
        }
    }

    private void seedRecentSearches() {
        recentSearches.add("Alphafly 3");
        recentSearches.add("HOKA Trail");
        recentSearches.add("ASICS Daily");
        recentSearches.add("Carbon");
    }

    public LiveData<List<ProductModel>> getProductsLiveData() {
        return productsLiveData;
    }

    public List<ProductModel> getAllProducts() {
        return new ArrayList<>(products);
    }

    public List<HomeBannerModel> getBanners() {
        return new ArrayList<>(banners);
    }

    @Nullable
    public ProductModel getProductById(@Nullable String productId) {
        if (productId == null) {
            return null;
        }
        for (ProductModel p : products) {
            if (p.getId().equals(productId)) {
                return p;
            }
        }
        return null;
    }

    public LiveData<Set<String>> getFavoriteIdsLiveData() {
        return favoriteIdsLiveData;
    }

    public boolean isFavorite(@Nullable String productId) {
        return productId != null && favoriteIds.contains(productId);
    }

    public boolean toggleFavorite(@NonNull String productId) {
        boolean added;
        if (favoriteIds.contains(productId)) {
            favoriteIds.remove(productId);
            added = false;
        } else {
            favoriteIds.add(productId);
            added = true;
        }
        publishFavorites();
        return added;
    }

    public LiveData<List<CartItemModel>> getCartItemsLiveData() {
        return cartItemsLiveData;
    }

    public LiveData<Integer> getCartTotalCountLiveData() {
        return cartTotalCountLiveData;
    }

    public void addToCart(@NonNull ProductModel product, @NonNull String size, @NonNull String colorName, int quantity) {
        for (CartItemModel item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())
                    && item.getSize().equals(size)
                    && item.getColorName().equals(colorName)) {
                item.setQuantity(item.getQuantity() + quantity);
                item.setSelected(true);
                publishCart();
                return;
            }
        }
        String newId = "c_" + System.currentTimeMillis() + "_" + cartItems.size();
        cartItems.add(0, new CartItemModel(newId, product, size, colorName, quantity, true));
        publishCart();
    }

    public void updateCartQuantity(@NonNull String cartItemId, int delta) {
        Iterator<CartItemModel> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            CartItemModel item = iterator.next();
            if (item.getId().equals(cartItemId)) {
                int nextQty = item.getQuantity() + delta;
                if (nextQty <= 0) {
                    iterator.remove();
                } else {
                    item.setQuantity(nextQty);
                }
                publishCart();
                return;
            }
        }
    }

    public void toggleCartItemSelected(@NonNull String cartItemId, boolean selected) {
        for (CartItemModel item : cartItems) {
            if (item.getId().equals(cartItemId)) {
                item.setSelected(selected);
                publishCart();
                return;
            }
        }
    }

    public void selectAllCartItems(boolean selected) {
        for (CartItemModel item : cartItems) {
            item.setSelected(selected);
        }
        publishCart();
    }

    public void removeCartItem(@NonNull String cartItemId) {
        Iterator<CartItemModel> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getId().equals(cartItemId)) {
                iterator.remove();
                publishCart();
                return;
            }
        }
    }

    public List<CartItemModel> getSelectedCartItems() {
        List<CartItemModel> selected = new ArrayList<>();
        for (CartItemModel item : cartItems) {
            if (item.isSelected()) {
                selected.add(item.copy());
            }
        }
        return selected;
    }

    public LiveData<List<NotificationModel>> getNotificationsLiveData() {
        return notificationsLiveData;
    }

    public LiveData<Integer> getUnreadNotificationCountLiveData() {
        return unreadNotificationCountLiveData;
    }

    public void markNotificationRead(@NonNull String id) {
        for (NotificationModel item : notifications) {
            if (item.getId().equals(id)) {
                if (item.isUnread()) {
                    item.setUnread(false);
                    publishNotifications();
                }
                return;
            }
        }
    }

    public void markAllNotificationsRead() {
        boolean changed = false;
        for (NotificationModel item : notifications) {
            if (item.isUnread()) {
                item.setUnread(false);
                changed = true;
            }
        }
        if (changed) {
            publishNotifications();
        }
    }

    public LiveData<List<OrderModel>> getOrdersLiveData() {
        return ordersLiveData;
    }

    @Nullable
    public OrderModel getOrderByCode(@Nullable String orderCode) {
        if (orderCode == null) {
            return null;
        }
        for (OrderModel order : orders) {
            if (order.getOrderCode().equals(orderCode)) {
                return order;
            }
        }
        return null;
    }

    public OrderModel placeOrder(
            @NonNull List<CartItemModel> checkoutItems,
            @NonNull ProfileAddressModel address,
            @NonNull String shippingMethod,
            long shippingFee,
            @NonNull String paymentMethod,
            long discount,
            boolean removeFromCart
    ) {
        orderSequence++;
        String code = "RN2026" + orderSequence;
        String now = new SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(new Date());

        List<CartItemModel> orderLines = new ArrayList<>();
        long subtotal = 0L;
        Set<String> purchasedCartIds = new HashSet<>();
        for (CartItemModel item : checkoutItems) {
            orderLines.add(item.copy());
            subtotal += item.getLineTotal();
            purchasedCartIds.add(item.getId());
        }
        long total = Math.max(0L, subtotal + shippingFee - discount);

        OrderModel newOrder = new OrderModel(
                code,
                now,
                OrderModel.Status.PENDING,
                orderLines,
                address.getAddressDetail(),
                address.getReceiverName() + " · " + address.getPhone(),
                shippingMethod,
                paymentMethod,
                subtotal,
                shippingFee,
                discount,
                total
        );
        orders.add(0, newOrder);
        ordersLiveData.setValue(new ArrayList<>(orders));

        if (removeFromCart && !purchasedCartIds.isEmpty()) {
            Iterator<CartItemModel> it = cartItems.iterator();
            while (it.hasNext()) {
                if (purchasedCartIds.contains(it.next().getId())) {
                    it.remove();
                }
            }
            publishCart();
        }

        notificationSequence++;
        notifications.add(0, new NotificationModel(
                "n_" + notificationSequence,
                "Đặt hàng #" + code + " thành công",
                "Đơn hàng trị giá " + CurrencyFormatUtil.formatVnd(total) + " đang chờ xác nhận.",
                "Vừa xong",
                NotificationModel.Type.ORDER,
                true
        ));
        publishNotifications();
        return newOrder;
    }

    public void cancelOrder(@NonNull String orderCode) {
        for (OrderModel order : orders) {
            if (order.getOrderCode().equals(orderCode) && order.getStatus() == OrderModel.Status.PENDING) {
                order.setStatus(OrderModel.Status.CANCELLED);
                ordersLiveData.setValue(new ArrayList<>(orders));
                return;
            }
        }
    }

    public void reorder(@NonNull OrderModel order) {
        for (CartItemModel item : order.getItems()) {
            addToCart(item.getProduct(), item.getSize(), item.getColorName(), item.getQuantity());
        }
    }

    public LiveData<ProfileModel> getProfileLiveData() {
        return profileLiveData;
    }

    public ProfileModel getProfile() {
        return profile;
    }

    public void updateProfile(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String preferredSize,
            @NonNull String runningGoal
    ) {
        profile.setFullName(fullName);
        profile.setPhone(phone);
        profile.setEmail(email);
        profile.setPreferredSize(preferredSize);
        profile.setRunningGoal(runningGoal);
        profileLiveData.setValue(profile);
    }

    public LiveData<List<ProfileAddressModel>> getAddressesLiveData() {
        return addressesLiveData;
    }

    @Nullable
    public ProfileAddressModel getDefaultAddress() {
        for (ProfileAddressModel address : addresses) {
            if (address.isDefaultAddress()) {
                return address;
            }
        }
        return addresses.isEmpty() ? null : addresses.get(0);
    }

    @Nullable
    public ProfileAddressModel findAddressById(@Nullable String id) {
        if (id == null) {
            return null;
        }
        for (ProfileAddressModel address : addresses) {
            if (address.getId().equals(id)) {
                return address;
            }
        }
        return null;
    }

    public void addOrUpdateAddress(
            @Nullable String editId,
            @NonNull String receiverName,
            @NonNull String phone,
            @NonNull String addressDetail,
            @NonNull String label,
            boolean isDefault
    ) {
        if (isDefault) {
            for (ProfileAddressModel existing : addresses) {
                existing.setDefaultAddress(false);
            }
        }
        if (editId != null) {
            ProfileAddressModel target = findAddressById(editId);
            if (target != null) {
                target.setReceiverName(receiverName);
                target.setPhone(phone);
                target.setAddressDetail(addressDetail);
                target.setLabel(label);
                target.setDefaultAddress(isDefault || addresses.size() == 1);
                addressesLiveData.setValue(new ArrayList<>(addresses));
                return;
            }
        }
        addressSequence++;
        boolean shouldDefault = isDefault || addresses.isEmpty();
        addresses.add(0, new ProfileAddressModel(
                "addr_" + addressSequence,
                receiverName,
                phone,
                addressDetail,
                label,
                shouldDefault
        ));
        addressesLiveData.setValue(new ArrayList<>(addresses));
    }

    public void setDefaultAddress(@NonNull String id) {
        for (ProfileAddressModel address : addresses) {
            address.setDefaultAddress(address.getId().equals(id));
        }
        addressesLiveData.setValue(new ArrayList<>(addresses));
    }

    public void deleteAddress(@NonNull String id) {
        Iterator<ProfileAddressModel> iterator = addresses.iterator();
        boolean removedDefault = false;
        while (iterator.hasNext()) {
            ProfileAddressModel address = iterator.next();
            if (address.getId().equals(id)) {
                removedDefault = address.isDefaultAddress();
                iterator.remove();
                break;
            }
        }
        if (removedDefault && !addresses.isEmpty()) {
            addresses.get(0).setDefaultAddress(true);
        }
        addressesLiveData.setValue(new ArrayList<>(addresses));
    }

    public LiveData<List<String>> getRecentSearchesLiveData() {
        return recentSearchesLiveData;
    }

    public void addRecentSearch(@NonNull String query) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        recentSearches.remove(trimmed);
        recentSearches.add(0, trimmed);
        if (recentSearches.size() > 8) {
            recentSearches.remove(recentSearches.size() - 1);
        }
        recentSearchesLiveData.setValue(new ArrayList<>(recentSearches));
    }

    public void removeRecentSearch(@NonNull String query) {
        if (recentSearches.remove(query)) {
            recentSearchesLiveData.setValue(new ArrayList<>(recentSearches));
        }
    }
}
