# Shoes Store — Ứng Dụng Thương Mại Điện Tử Giày Chạy Bộ (Android)

Ứng dụng di động mua sắm và quản lý cửa hàng giày chạy bộ, giày thể thao được phát triển trên nền tảng **Android (Java)** theo kiến trúc **MVVM 3 tầng** kết hợp **Flat Feature Modules**, tích hợp **Firebase Authentication** và **Cloud Firestore** đồng bộ dữ liệu theo thời gian thực.

---

## 1. Tính năng chính

Hệ thống tự động phân luồng giao diện dựa trên trường `role` (`"users"` hoặc `"admin"`) của tài khoản ngay sau khi đăng nhập.

### Phân hệ Khách hàng (`role: "users"`)
- **Xác thực tài khoản (`auth`)**:
  - Đăng ký tài khoản mới, đăng nhập, khôi phục mật khẩu, đăng xuất và duy trì phiên đăng nhập với Firebase Authentication.
  - Tự động khởi tạo hồ sơ người dùng khi đăng nhập lần đầu (`ProfileRepository`).
- **Trang chủ & Tìm kiếm nâng cao (`home`)**:
  - Hiển thị banner khuyến mãi, danh sách giày chạy bộ theo thời gian thực, hỗ trợ chế độ Khách (Guest-first).
  - Tìm kiếm theo tên/thương hiệu, lưu từ khóa tìm kiếm gần đây, lọc đa tiêu chí theo dòng giày (`Daily`, `Race`, `Tempo`, `Trail`, `Recovery`), thương hiệu, khoảng giá và sắp xếp theo độ phổ biến, giá hoặc đánh giá.
- **Chi tiết sản phẩm & Đánh giá (`product`, `review`)**:
  - Xem bộ sưu tập hình ảnh, màu sắc, thông số kỹ thuật chi tiết (trọng lượng, độ dốc đế, độ êm đệm, bề mặt chạy) và bảng hướng dẫn chọn size (`ProductSizeGuideBottomSheet`).
  - Chọn màu sắc và kích cỡ EU theo tồn kho thực tế từng biến thể (`variants`).
  - Xem danh sách đánh giá theo sản phẩm và gửi đánh giá sao kèm nhận xét (chỉ mở khóa khi khách có đơn hàng `COMPLETED` chứa sản phẩm đó).
- **Danh sách yêu thích (`wishlist`)**:
  - Thêm/xóa sản phẩm yêu thích đồng bộ thời gian thực với tài khoản cá nhân và thêm nhanh vào giỏ.
- **Giỏ hàng & Mã giảm giá (`cart`, `voucher`)**:
  - Quản lý sản phẩm trong giỏ theo từng biến thể (`productId + variantId`), kiểm tra giới hạn tồn kho khi tăng/giảm số lượng.
  - Chọn và áp dụng mã giảm giá (`VoucherBottomSheet`) với kiểm tra tự động giá trị đơn tối thiểu, thời hạn và số lượt sử dụng.
- **Đặt hàng & Thanh toán (`order`, `payment`)**:
  - Hỗ trợ cả **Đặt hàng từ giỏ hàng** và **Mua ngay** trực tiếp từ chi tiết sản phẩm.
  - Xử lý đặt hàng nguyên tử bằng **Firestore Transaction** (`OrderPlaceHelper`): đọc & kiểm tra tồn kho thực tế, kiểm tra voucher, trừ kho theo đúng biến thể, tăng lượt dùng voucher, tạo đơn hàng, tạo bản ghi thanh toán (`COD`, `BANK_TRANSFER`, `E_WALLET`) và gửi thông báo.
  - Theo dõi chi tiết đơn hàng với 4 trạng thái thực tế (`PENDING` — *Chờ xác nhận*, `SHIPPING` — *Đang giao*, `COMPLETED` — *Đã giao*, `CANCELLED` — *Đã hủy*) và trạng thái thanh toán (`UNPAID`, `PAID`, `REFUNDED`, `FAILED`).
  - Hỗ trợ khách hủy đơn khi ở trạng thái `PENDING` (tự động hoàn tồn kho, hoàn lượt dùng voucher, hoàn trạng thái thanh toán qua `OrderTransactionHelper`) hoặc mua lại nhanh từ lịch sử đơn (`OrderReorderHelper`).
- **Thông báo & Hồ sơ cá nhân (`notification`, `profile`)**:
  - Nhận thông báo tự động khi đặt hàng hoặc khi trạng thái đơn hàng thay đổi.
  - Cập nhật thông tin cá nhân, ảnh đại diện (tối ưu decode bất đồng bộ) và quản lý sổ địa chỉ giao hàng (thêm, sửa, xóa, đặt mặc định).

### Phân hệ Quản trị viên (`role: "admin"`)
- **Quản lý đơn hàng**:
  - Theo dõi toàn bộ đơn hàng trên hệ thống theo thời gian thực kèm KPI (Chờ duyệt, Đang giao, Doanh thu hoàn tất), lọc nhanh theo 4 trạng thái (`PENDING`, `SHIPPING`, `COMPLETED`, `CANCELLED`).
  - Thực hiện xử lý đơn một chạm:
    - **Xác nhận & Giao hàng**: `PENDING` → `SHIPPING`
    - **Đã giao hàng**: `SHIPPING` → `COMPLETED` (tự động cập nhật bản ghi thanh toán sang `PAID`)
    - **Hủy đơn**: dùng chung `OrderTransactionHelper` để hoàn tồn kho từng biến thể, hoàn lượt dùng voucher và cập nhật thanh toán sang `REFUNDED` (nếu đã thanh toán).
  - Tự động gửi thông báo vào mục Thông báo của khách hàng tương ứng sau mỗi thao tác.
- **Quản lý sản phẩm (`AdminCatalogHelper`)**:
  - Thêm/sửa sản phẩm, thông số kỹ thuật, màu sắc, kích cỡ và cấu hình tồn kho biến thể theo batch, ẩn/hiện sản phẩm.
- **Quản lý mã giảm giá (Voucher)**:
  - Tạo/sửa mã khuyến mãi, mức giảm, đơn hàng tối thiểu, hạn mức lượt dùng, số ngày hiệu lực và bật/tắt kích hoạt.

---

## 2. Công nghệ & Thư viện sử dụng

| Thành phần | Công nghệ / Phiên bản |
| --- | --- |
| **Ngôn ngữ** | Java 11 |
| **Giao diện (UI)** | Android XML Views, Material Design 3 (`1.14.0`), AppCompat (`1.8.0`), Activity KTX (`1.13.0`), ConstraintLayout (`2.2.2`) |
| **Kiến trúc** | MVVM 3 tầng (`UI` → `ViewModel` + `LiveData` → `Repository` / `Service` → `Mapper` & `Model`) |
| **Backend & Database** | Firebase BOM `35.2.0` (`firebase-auth`, `firebase-firestore`) |
| **Build System** | Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`), AGP `9.5.2`, Google Services `4.6.0` |
| **Android SDK** | `minSdk 24` (Android 7.0) — `targetSdk 37` / `compileSdk 37` |
| **Unit Testing** | JUnit `4.13.2` (**42** bài kiểm thử đơn vị trên **13** file test) |

---

## 3. Kiến trúc & Cấu trúc thư mục

Dự án tổ chức theo kiến trúc **Flat Feature Modules** (phẳng tuyệt đối trong `core/` và từng `module/<tên_tính_năng>/`):

```text
app/src/main/java/com/example/shoes_store/
├── MainActivity.java                      # Khung điều hướng Floating Bottom Navigation 5 tab
├── MainViewModel.java                     # ViewModel cho MainActivity (badge, phiên đăng nhập, định tuyến Admin)
├── ShoesStoreApplication.java             # Đăng ký 8 Repository vào SessionSyncHelper khi khởi động
├── core/                                  # Hạ tầng & tiện ích dùng chung (0 import module.*)
│   ├── AppPrefsHelper.java
│   ├── AvatarImageHelper.java
│   ├── BaseActivity.java
│   ├── BaseFirestoreRepository.java       # Quản lý Public listeners vs User listeners theo phiên
│   ├── BaseListAdapter.java
│   ├── CategoryStyleHelper.java
│   ├── CurrencyFormatUtil.java
│   ├── DateFormatUtil.java
│   ├── DialogHelper.java
│   ├── FirebaseProviderHelper.java
│   ├── FirestoreReadUtil.java
│   ├── InputValidatorUtil.java
│   ├── RemoteImageHelper.java
│   ├── ResourceCallback.java
│   ├── SessionSyncCallback.java
│   ├── SessionSyncHelper.java
│   ├── StatusBadgeHelper.java
│   ├── UiInsetHelper.java
│   └── UserRoleHelper.java
└── module/                                # 12 module tính năng phẳng
    ├── admin/                             # AdminCatalogHelper, AdminDashboardActivity, AdminOrderAdapter,
    │                                      # AdminProductAdapter, AdminProductDialog, AdminRepository,
    │                                      # AdminService, AdminViewModel, AdminVoucherAdapter, AdminVoucherDialog
    ├── auth/                              # AuthLoginActivity, AuthRegisterActivity, AuthRepository,
    │                                      # AuthValidator, AuthViewModel
    ├── cart/                              # CartAdapter, CartFragment, CartItemModel, CartMapper,
    │                                      # CartRepository, CartService, CartViewModel
    ├── home/                              # HomeBannerAdapter, HomeBannerMapper, HomeBannerModel,
    │                                      # HomeFilterBottomSheet, HomeFilterState, HomeFragment,
    │                                      # HomeRepository, HomeSearchActivity, HomeSearchService,
    │                                      # HomeShoeAdapter, HomeViewModel
    ├── notification/                      # NotificationAdapter, NotificationFragment, NotificationMapper,
    │                                      # NotificationModel, NotificationRepository, NotificationViewModel
    ├── order/                             # OrderActionHelper, OrderCheckoutActivity, OrderCheckoutItemAdapter,
    │                                      # OrderDetailActivity, OrderHistoryActivity, OrderHistoryAdapter,
    │                                      # OrderItemModel, OrderMapper, OrderModel, OrderPlaceHelper,
    │                                      # OrderReorderHelper, OrderRepository, OrderService, OrderStatusHelper,
    │                                      # OrderSuccessDialog, OrderTransactionHelper, OrderViewModel
    ├── payment/                           # PaymentMapper, PaymentModel, PaymentRepository, PaymentService,
    │                                      # PaymentStatusHelper, PaymentViewModel
    ├── product/                           # ProductDetailActivity, ProductDetailUiHelper, ProductGalleryAdapter,
    │                                      # ProductMapper, ProductModel, ProductRepository, ProductSizeAdapter,
    │                                      # ProductSizeGuideBottomSheet, ProductVariantModel, ProductViewModel
    ├── profile/                           # ProfileAboutActivity, ProfileAddressActivity, ProfileAddressAdapter,
    │                                      # ProfileAddressModel, ProfileAddressService, ProfileEditActivity,
    │                                      # ProfileFragment, ProfileMapper, ProfileModel, ProfileRepository,
    │                                      # ProfileTermsActivity, ProfileViewModel
    ├── review/                            # ReviewAdapter, ReviewDialog, ReviewMapper, ReviewModel,
    │                                      # ReviewRepository, ReviewService, ReviewViewModel
    ├── voucher/                           # VoucherAdapter, VoucherBottomSheet, VoucherMapper, VoucherModel,
    │                                      # VoucherRepository, VoucherService
    └── wishlist/                          # WishlistAdapter, WishlistFragment, WishlistMapper,
                                           # WishlistRepository, WishlistViewModel
```

### Quy tắc phân tầng & Hướng phụ thuộc
- **`*Activity` / `*Fragment` / `*Dialog` / `*BottomSheet` / `*Adapter` / `*UiHelper`**: Tầng giao diện (UI), chỉ lắng nghe `LiveData` và gọi qua `*ViewModel` (0 tham chiếu trực tiếp `*Repository`, `FirebaseProviderHelper`, `SessionSyncHelper`).
- **`*ViewModel`**: Quản lý trạng thái hiển thị của màn hình và điều phối dữ liệu từ `*Repository`.
- **`*Repository` & `*Helper`**: Quản lý kết nối thời gian thực (`BaseFirestoreRepository`), tách riêng listener công khai và theo phiên người dùng; không tạo vòng phụ thuộc giữa các `*Repository` (`ProductRepository` và `ProfileRepository` ở tầng gốc).
- **`*Service` / `*Validator`**: Chứa quy tắc nghiệp vụ thuần Java (độc lập Android SDK) phục vụ unit test JUnit.
- **`*Mapper` & `*Model`**: Định nghĩa hằng số trường dữ liệu (`FIELD_*`) và chuyển đổi hai chiều với Cloud Firestore.

---

## 4. Cấu trúc Cơ sở dữ liệu (Cloud Firestore)

```text
Firestore Root
├── products/{productId}              # Thông tin giày & danh sách size/màu
│   ├── variants/{variantId}          # Biến thể theo màu + size EU và tồn kho thực tế (stockQty)
│   └── reviews/{reviewId}            # Đánh giá của khách hàng cho từng sản phẩm
├── vouchers/{voucherCode}            # Mã giảm giá, đơn tối thiểu, hạn mức & số lượt đã dùng
├── banners/{bannerId}                # Banner quảng cáo trên trang chủ
├── orders/{orderId}                  # Đơn hàng (items, trạng thái PENDING/SHIPPING/COMPLETED/CANCELLED)
├── payments/{paymentId}              # Giao dịch thanh toán liên kết với đơn hàng
└── users/{uid}                       # Hồ sơ người dùng (role: "admin" | "users")
    ├── cart/{lineId}                 # Các dòng sản phẩm trong giỏ hàng
    ├── wishlist/{productId}          # Danh sách sản phẩm yêu thích
    ├── addresses/{addressId}         # Sổ địa chỉ nhận hàng
    └── notifications/{notifId}       # Thông báo cá nhân của người dùng
```

---

## 5. Hướng dẫn cài đặt & Chạy dự án

### Yêu cầu môi trường
- **Android Studio** Ladybug / Meerkat trở lên.
- **JDK 11** trở lên.
- Thiết bị thật hoặc máy ảo Android (**API 24 / Android 7.0** trở lên có Google Play Services).

### Các bước khởi chạy
1. **Clone kho mã nguồn**:
   ```bash
   git clone https://github.com/Bao0807/Shoes_Store_AppMobile.git
   cd Shoes_Store_AppMobile
   ```
2. **Cấu hình Firebase**:
   - Đảm bảo file cấu hình `app/google-services.json` đã có sẵn trong thư mục `app/`.
   - Trên Firebase Console của dự án, bật **Authentication** (phương thức *Email/Password*) và **Cloud Firestore**.
3. **Biên dịch, kiểm thử và kiểm tra Lint**:
   - Biên dịch bản Debug APK:
     ```powershell
     .\gradlew.bat :app:assembleDebug
     ```
   - Chạy toàn bộ 42 bài kiểm thử đơn vị (Unit Tests):
     ```powershell
     .\gradlew.bat :app:testDebugUnitTest
     ```
   - Chạy Android Lint:
     ```powershell
     .\gradlew.bat :app:lintDebug
     ```

---

## 6. Tài khoản trải nghiệm nhanh

- **Tài khoản Quản trị viên (Admin)**:
  - **Email**: `admin@gmail.com`
  - **Mật khẩu**: `123456`
  - *Lưu ý*: Khi đăng nhập bằng tài khoản này, hệ thống tự động nhận diện quyền quản trị (`role: "admin"`) và chuyển vào màn hình **Bảng điều khiển Quản trị viên (`AdminDashboardActivity`)**.
- **Tài khoản Khách hàng (Customer)**:
  - Bạn có thể tạo tài khoản mới bất kỳ tại màn hình **Đăng ký** ngay trên ứng dụng. Mọi tài khoản đăng ký mới sẽ tự động nhận `role: "users"` và đi vào giao diện mua sắm chính.
