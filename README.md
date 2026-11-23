# Shoes Store — Ứng Dụng Thương Mại Điện Tử Giày Thể Thao (Android)

Ứng dụng di động mua sắm và quản lý cửa hàng giày chạy bộ, giày thể thao được phát triển trên nền tảng **Android (Java)** theo kiến trúc **MVVM**, tích hợp **Firebase Authentication** và **Cloud Firestore** đồng bộ dữ liệu theo thời gian thực.

---

## 1. Tính năng chính

Hệ thống tự động phân luồng giao diện dựa trên trường `role` (`"users"` hoặc `"admin"`) của tài khoản ngay sau khi đăng nhập.

### Phân hệ Khách hàng (`role: "users"`)
- **Xác thực tài khoản (`auth`)**:
  - Đăng ký tài khoản mới, đăng nhập, đăng xuất và duy trì phiên đăng nhập với Firebase Authentication.
  - Tự động khởi tạo hồ sơ người dùng, địa chỉ mặc định và dữ liệu mẫu khi đăng nhập lần đầu.
- **Trang chủ & Tìm kiếm nâng cao (`home`)**:
  - Hiển thị banner chương trình khuyến mãi, danh sách sản phẩm nổi bật theo thời gian thực.
  - Tìm kiếm theo tên/thương hiệu, lọc đa tiêu chí theo dòng giày (`Road`, `Race`, `Trail`, `Daily`), khoảng giá, thương hiệu và sắp xếp theo giá hoặc đánh giá.
- **Chi tiết sản phẩm & Đánh giá (`product`)**:
  - Xem bộ sưu tập hình ảnh, thông số kỹ thuật chi tiết (trọng lượng, độ dốc đế, công nghệ đệm, bề mặt chạy) và bảng hướng dẫn chọn kích cỡ (Size Guide).
  - Chọn size giày theo tồn kho thực tế của từng biến thể (`variants`).
  - Xem danh sách đánh giá và gửi đánh giá sao kèm nhận xét (hỗ trợ kiểm tra điều kiện khách đã mua và nhận hàng thành công).
- **Danh sách yêu thích (`wishlist`)**:
  - Thêm/xóa sản phẩm yêu thích đồng bộ thời gian thực với tài khoản cá nhân.
- **Giỏ hàng & Mã giảm giá (`cart`, `voucher`)**:
  - Quản lý sản phẩm trong giỏ theo từng cặp `productId + size`, kiểm tra giới hạn tồn kho khi tăng/giảm số lượng.
  - Chọn và áp dụng mã giảm giá (`VoucherBottomSheet`) với kiểm tra tự động điều kiện giá trị đơn tối thiểu, thời hạn và số lượt sử dụng.
- **Đặt hàng & Thanh toán (`order`, `payment`)**:
  - Xử lý đặt hàng an toàn bằng **Firestore Transaction**: kiểm tra tồn kho thực tế, trừ kho theo đúng size, tăng lượt dùng voucher, tạo đơn hàng và bản ghi thanh toán (`COD`, `BANK_TRANSFER`, `E_WALLET`).
  - Theo dõi chi tiết đơn hàng với tiến trình 4 bước trực quan (`Chờ xác nhận` → `Đã xác nhận` → `Đang giao` → `Đã giao`) và trạng thái thanh toán (`UNPAID`, `PAID`, `REFUNDED`, `FAILED`).
  - Hỗ trợ khách hủy đơn khi đơn đang ở trạng thái `PENDING` (tự động hoàn lại tồn kho và hoàn lượt dùng voucher) hoặc mua lại nhanh các sản phẩm từ đơn cũ.
- **Thông báo & Hồ sơ cá nhân (`notification`, `profile`)**:
  - Nhận thông báo tự động khi đặt hàng hoặc khi trạng thái đơn hàng thay đổi.
  - Cập nhật thông tin cá nhân, ảnh đại diện và quản lý sổ địa chỉ giao hàng (thêm, sửa, xóa, đặt địa chỉ mặc định).

### Phân hệ Quản trị viên (`role: "admin"`)
- **Quản lý đơn hàng**:
  - Theo dõi toàn bộ đơn hàng trên hệ thống theo thời gian thực, lọc nhanh theo từng trạng thái (`Tất cả`, `Chờ duyệt`, `Đã duyệt`, `Đang giao`, `Đã giao`, `Đã hủy`).
  - Thực hiện quy trình xử lý đơn một chạm:
    - **Duyệt đơn**: `PENDING` → `CONFIRMED`
    - **Giao hàng**: `CONFIRMED` → `SHIPPING`
    - **Đã giao hàng**: `SHIPPING` → `DELIVERED` (tự động chuyển trạng thái thanh toán COD sang `PAID`)
    - **Hủy đơn**: Tự động hoàn lại tồn kho từng size cho sản phẩm, hoàn lại lượt dùng voucher và chuyển trạng thái thanh toán sang `REFUNDED` (nếu đã thanh toán).
  - Tự động gửi thông báo đẩy vào mục Thông báo của khách hàng tương ứng sau mỗi thao tác.
- **Quản lý sản phẩm**:
  - Thêm sản phẩm mới, chỉnh sửa thông tin, giá bán, cấu hình tồn kho theo từng size (ví dụ: `39:5, 40:10, 41:8, 42:12`), ẩn/hiện hoặc xóa sản phẩm.
- **Quản lý mã giảm giá (Voucher)**:
  - Tạo mã khuyến mãi mới, chỉnh sửa mức giảm, đơn hàng tối thiểu, tổng hạn mức lượt dùng và bật/tắt trạng thái kích hoạt.

---

## 2. Công nghệ & Thư viện sử dụng

| Thành phần | Công nghệ / Phiên bản |
| --- | --- |
| **Ngôn ngữ** | Java 11 |
| **Giao diện (UI)** | Android XML Views, Material Design 3 (`1.13.0`), ConstraintLayout (`2.2.1`) |
| **Kiến trúc** | MVVM (`Activity`/`Fragment` → `ViewModel` + `LiveData` → `Repository` & `Service` → `Mapper` & `Model`) |
| **Backend & Database** | Firebase BOM `35.2.0` (`firebase-auth`, `firebase-firestore`) |
| **Build System** | Gradle Kotlin DSL + Version Catalog (`gradle/libs.versions.toml`), AGP `9.1.0` |
| **Android SDK** | `minSdk 24` (Android 7.0) — `targetSdk 36` / `compileSdk 36` |
| **Unit Testing** | JUnit `4.13.2` (27 bài kiểm thử nghiệp vụ cho toàn bộ các module) |

---

## 3. Kiến trúc & Cấu trúc thư mục

Dự án tổ chức theo kiến trúc **Flat Feature Modules** kết hợp phân tầng trách nhiệm rõ ràng trong từng module:

```text
app/src/main/java/com/example/shoes_store/
├── MainActivity.java                 # Điểm vào luồng Khách hàng (Bottom Navigation 5 tab)
├── ShoesStoreApplication.java        # Khởi tạo cấu hình toàn cục của ứng dụng
├── core/                             # Tiện ích & lớp nền tảng dùng chung toàn dự án
│   ├── AppPrefsHelper.java
│   ├── AvatarImageHelper.java
│   ├── CategoryStyleHelper.java
│   ├── CurrencyFormatUtil.java
│   ├── FirebaseProviderHelper.java
│   ├── FirestoreReadUtil.java
│   ├── FirestoreSeedHelper.java
│   ├── RemoteImageHelper.java
│   ├── ResourceCallback.java
│   ├── SessionSyncHelper.java
│   └── UiInsetHelper.java
└── module/                           # Các module tính năng độc lập (cấu trúc phẳng)
    ├── admin/                        # Quản trị sản phẩm, voucher, đơn hàng
    ├── auth/                         # Đăng nhập, đăng ký, phân quyền điều hướng
    ├── cart/                         # Giỏ hàng & tính toán tạm tính
    ├── home/                         # Trang chủ, banner, tìm kiếm & bộ lọc
    ├── notification/                 # Thông báo đơn hàng & khuyến mãi
    ├── order/                        # Đặt hàng, lịch sử & chi tiết tiến trình đơn
    ├── payment/                      # Nghiệp vụ & trạng thái thanh toán
    ├── product/                      # Chi tiết giày, biến thể size & đánh giá
    ├── profile/                      # Hồ sơ cá nhân & sổ địa chỉ giao hàng
    ├── voucher/                      # Danh sách & kiểm tra điều kiện mã giảm giá
    └── wishlist/                     # Danh sách sản phẩm yêu thích
```

### Phân tầng bên trong mỗi Module
- **`*Activity` / `*Fragment` / `*Dialog` / `*BottomSheet` / `*Adapter`**: Tầng giao diện (View), chỉ lắng nghe `LiveData` từ `ViewModel` và xử lý thao tác người dùng.
- **`*ViewModel`**: Quản lý trạng thái hiển thị của màn hình và điều phối gọi dữ liệu qua `Repository`.
- **`*Repository`**: Quản lý kết nối thời gian thực (`SnapshotListener`) và truy vấn đọc/ghi với Cloud Firestore.
- **`*Service`**: Chứa toàn bộ quy tắc nghiệp vụ thuần Java (tính giá, kiểm tra tồn kho, ràng buộc chuyển trạng thái đơn, điều kiện voucher...), độc lập với Android SDK để dễ dàng kiểm thử bằng JUnit.
- **`*Mapper` & `*Model`**: Định nghĩa hằng số trường dữ liệu (`FIELD_*`) và chuyển đổi hai chiều giữa `DocumentSnapshot` của Firestore và đối tượng Java (`Model`).

---

## 4. Cấu trúc Cơ sở dữ liệu (Cloud Firestore)

```text
Firestore Root
├── products/{productId}              # Danh mục giày & map tồn kho theo size (variants)
│   └── reviews/{reviewId}            # Đánh giá của khách hàng cho từng sản phẩm
├── vouchers/{voucherCode}            # Mã giảm giá, đơn tối thiểu, hạn mức & số lượt đã dùng
├── banners/{bannerId}                # Banner quảng cáo trên trang chủ
├── orders/{orderId}                  # Đơn hàng (items, trạng thái đơn, thông tin giao nhận)
├── payments/{paymentId}              # Giao dịch thanh toán liên kết 1-1 với đơn hàng
└── users/{uid}                       # Hồ sơ người dùng (chứa trường role: "admin" | "users")
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
3. **Biên dịch và kiểm thử bằng dòng lệnh (hoặc chạy trực tiếp từ Android Studio)**:
   - Biên dịch bản Debug APK:
     ```powershell
     .\gradlew.bat :app:assembleDebug
     ```
   - Chạy toàn bộ bộ kiểm thử đơn vị (Unit Tests):
     ```powershell
     .\gradlew.bat :app:testDebugUnitTest
     ```

---

## 6. Tài khoản trải nghiệm nhanh

- **Tài khoản Quản trị viên (Admin)**:
  - **Email**: `admin@gmail.com`
  - **Mật khẩu**: `123456`
  - *Lưu ý*: Khi đăng nhập bằng tài khoản này, hệ thống tự động gán `role: "admin"` trên Firestore và chuyển thẳng vào màn hình **Bảng điều khiển Quản trị viên (`AdminDashboardActivity`)**.
- **Tài khoản Khách hàng (Customer)**:
  - Bạn có thể tạo tài khoản mới bất kỳ tại màn hình **Đăng ký** ngay trên ứng dụng. Mọi tài khoản đăng ký mới sẽ tự động nhận `role: "users"` và đi vào giao diện mua sắm chính.
