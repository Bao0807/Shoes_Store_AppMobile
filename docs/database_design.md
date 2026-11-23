# Thiết kế Cơ sở dữ liệu — Shoes Store (Firebase Auth & Cloud Firestore)

Tài liệu này mô tả mô hình dữ liệu, cấu trúc collection trên **Cloud Firestore**, cách hiển thị phương thức thanh toán trên màn hình Chi tiết đơn hàng (`OrderDetailActivity`), và quy trình đồng bộ giao dịch (**Firestore Transaction**) cho nghiệp vụ **Đặt hàng** & **Hủy đơn**.

---

## 1. Class Diagram

Nguồn Mermaid gốc: [class_diagram.mmd](./class_diagram.mmd)

```mermaid
classDiagram
    direction TB

    class User {
        +String uid
        +String fullName
        +String email
        +String phone
        +String avatarUrl
        +String preferredSize
        +String runningGoal
        +String role
        +List~String~ recentSearches
        +Timestamp createdAt
        +Timestamp updatedAt
    }

    class Address {
        +String addressId
        +String userId
        +String receiverName
        +String phone
        +String addressDetail
        +String label
        +boolean isDefault
    }

    class Banner {
        +String bannerId
        +String badge
        +String title
        +String subtitle
        +String ctaLabel
        +String targetCategory
        +String imageUrl
        +int displayOrder
        +boolean isActive
    }

    class Product {
        +String productId
        +String name
        +String brand
        +String category
        +long price
        +long originalPrice
        +double ratingAvg
        +int reviewCount
        +String thumbnailUrl
        +List~String~ galleryUrls
        +String weightGrams
        +String heelDropMm
        +String cushionLevel
        +String surfaceType
        +String description
        +boolean isActive
        +Timestamp createdAt
        +hasDiscount() boolean
        +getDiscountPercent() int
    }

    class ProductVariant {
        +String variantId
        +String productId
        +String colorName
        +String colorHex
        +String imageUrl
        +String size
        +int stockQty
        +String sku
        +isInStock() boolean
        +hasEnoughStock(int qty) boolean
    }

    class Review {
        +String reviewId
        +String productId
        +String userId
        +String orderId
        +String reviewerName
        +String reviewerAvatarUrl
        +float rating
        +String comment
        +Timestamp createdAt
    }

    class WishlistItem {
        +String productId
        +String userId
        +Timestamp addedAt
    }

    class CartItem {
        +String cartItemId
        +String userId
        +String productId
        +String variantId
        +String size
        +String colorName
        +int quantity
        +boolean selected
        +Timestamp updatedAt
    }

    class Voucher {
        +String code
        +String shortBadge
        +String title
        +String description
        +long discountAmount
        +long minOrderAmount
        +int usageLimit
        +int usedCount
        +Timestamp expiryDate
        +boolean isActive
        +isEligible(long subtotal) boolean
        +hasRemainingQuota() boolean
    }

    class Order {
        +String orderId
        +String orderCode
        +String userId
        +OrderStatus status
        +PaymentMethod paymentMethod
        +String receiverName
        +String receiverPhone
        +String shippingAddress
        +String shippingMethod
        +String voucherCode
        +long subtotal
        +long shippingFee
        +long discount
        +long total
        +Timestamp createdAt
        +Timestamp updatedAt
        +getTotalQuantity() int
        +canCancel() boolean
    }

    class OrderItem {
        +String productId
        +String variantId
        +String productName
        +String thumbnailUrl
        +String size
        +String colorName
        +long unitPrice
        +int quantity
        +long lineTotal
    }

    class Payment {
        +String paymentId
        +String orderId
        +String userId
        +PaymentMethod method
        +PaymentStatus status
        +long amount
        +String transactionCode
        +Timestamp paidAt
        +Timestamp createdAt
        +isPaid() boolean
    }

    class Notification {
        +String notificationId
        +String userId
        +String title
        +String message
        +NotificationType type
        +String targetId
        +boolean unread
        +Timestamp createdAt
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        SHIPPING
        COMPLETED
        CANCELLED
    }

    class PaymentMethod {
        <<enumeration>>
        COD
        BANK_QR
        E_WALLET
    }

    class PaymentStatus {
        <<enumeration>>
        UNPAID
        PAID
        FAILED
        REFUNDED
    }

    class NotificationType {
        <<enumeration>>
        ORDER
        PROMO
        DROP
    }

    User "1" *-- "0..*" Address : sở hữu
    User "1" *-- "0..*" WishlistItem : lưu yêu thích
    User "1" *-- "0..*" CartItem : giỏ hàng
    User "1" *-- "0..*" Notification : nhận thông báo
    User "1" --> "0..*" Order : đặt hàng
    User "1" --> "0..*" Review : viết đánh giá

    Product "1" *-- "1..*" ProductVariant : biến thể màu/size
    Product "1" *-- "0..*" Review : có đánh giá

    WishlistItem "0..*" --> "1" Product : tham chiếu
    CartItem "0..*" --> "1" Product : chọn giày
    CartItem "0..*" --> "1" ProductVariant : chọn size/màu

    Order "1" *-- "1..*" OrderItem : gồm các mục
    Order "1" --> "1..*" Payment : thanh toán
    Order "0..*" --> "0..1" Voucher : áp dụng mã
    Order ..> OrderStatus : trạng thái
    Order ..> PaymentMethod : lưu nhanh phương thức
    OrderItem "0..*" --> "1" Product : snapshot từ

    Payment ..> PaymentMethod : phương thức
    Payment ..> PaymentStatus : trạng thái

    Notification ..> NotificationType : phân loại
    Banner "0..*" ..> "0..*" Product : lọc theo targetCategory
```

---

## 2. Ánh xạ Collection trên Cloud Firestore

| Lớp | Đường dẫn Firestore | Quy ước lưu trữ |
| --- | --- | --- |
| `User` | `users/{uid}` | Document ID trùng `uid` của Firebase Auth; `avatarUrl` lưu link ảnh; `preferredSize` lưu chuẩn **EU** dạng `String` (vd `"40.5"`, `"42"`). |
| `Address` | `users/{uid}/addresses/{addressId}` | Sổ địa chỉ riêng của từng tài khoản; cờ `isDefault` xác định địa chỉ mặc định khi thanh toán. |
| `WishlistItem` | `users/{uid}/wishlist/{productId}` | Dùng `productId` làm document ID để chống trùng và kiểm tra yêu thích theo $O(1)$. |
| `CartItem` | `users/{uid}/cart/{cartItemId}` | Lưu `productId`, `variantId`, `size` (EU), `colorName`, `quantity`, `selected`; giá sản phẩm luôn lấy trực tiếp từ `products/{productId}` để đồng bộ giá mới nhất. |
| `Notification` | `users/{uid}/notifications/{notificationId}` | Lưu `targetId` (`orderCode`, `productId` hoặc `voucherCode`) và `createdAt` dạng `Timestamp`. |
| `Banner` | `banners/{bannerId}` | Lọc `isActive == true`, sắp xếp tăng dần theo `displayOrder`. |
| `Product` | `products/{productId}` | Lưu thông tin chính, thông số chạy bộ (`weightGrams`, `heelDropMm`, `cushionLevel`, `surfaceType`), `thumbnailUrl` và `galleryUrls`. |
| `ProductVariant` | `products/{productId}/variants/{variantId}` | Gộp thông tin phối màu (`colorName`, `colorHex`, `imageUrl`) và size EU (`size`) cùng tồn kho thực tế (`stockQty`, `sku`). |
| `Review` | `products/{productId}/reviews/{reviewId}` | Tách subcollection theo sản phẩm để dễ phân trang và sắp xếp theo `createdAt`. |
| `Voucher` | `vouchers/{code}` | Document ID là mã voucher viết hoa (vd `RUN50K`) để tra cứu trực tiếp bằng `doc(code)`. |
| `Order` | `orders/{orderId}` | Collection gốc có `userId` (để khách xem đơn của mình và admin lọc toàn hệ thống); nhúng `items: List<OrderItem>` và lưu snapshot địa chỉ + `paymentMethod`. |
| `Payment` | `payments/{paymentId}` | Tách riêng lịch sử giao dịch thanh toán theo `orderId` và `userId`; lưu `method`, `status`, `amount`, `transactionCode`, `paidAt`. |

---

## 3. Chiến lược hiển thị Phương thức thanh toán trên `OrderDetailActivity`

Để vừa **giảm lượt đọc Firestore** vừa giữ **lịch sử giao dịch chuẩn thực tế**, hệ thống áp dụng cơ chế kết hợp 2 lớp:

1. **Lưu dự phòng `paymentMethod` trực tiếp trên `Order`**:
   - Khi mở màn **Lịch sử đơn hàng (`OrderHistoryActivity`)** hoặc **Chi tiết đơn hàng (`OrderDetailActivity`)**, chỉ cần đọc 1 document `orders/{orderId}` là có thể hiển thị ngay dòng `shippingMethod · paymentMethod` (vd: *"Giao tiêu chuẩn · Thanh toán khi nhận hàng (COD)"*), không bị độ trễ hay phát sinh $N$ truy vấn phụ cho từng đơn.
2. **Truy vấn thêm `payments` theo `orderId` khi cần chi tiết giao dịch**:
   - Tại `OrderDetailActivity`, truy vấn:
     ```text
     payments
       .whereEqualTo("orderId", order.orderId)
       .whereEqualTo("userId", currentUid)
       .orderBy("createdAt", DESCENDING)
       .limit(1)
     ```
   - Lấy bản ghi `Payment` mới nhất để hiển thị thêm trạng thái thanh toán (`UNPAID` / `PAID` / `FAILED` / `REFUNDED`), mã giao dịch (`transactionCode`) và thời gian thanh toán (`paidAt`).

---

## 4. Quy trình đồng bộ Firestore Transaction cho Đặt hàng & Hủy đơn

> **Nguyên tắc bắt buộc của Cloud Firestore Transaction**: Toàn bộ thao tác **ĐỌC (`transaction.get(...)`)** phải hoàn tất **trước** khi thực hiện bất kỳ thao tác **GHI (`set` / `update` / `delete`)** nào. Nếu bất kỳ điều kiện tồn kho hoặc voucher nào không đạt, transaction sẽ ném lỗi và tự động **rollback toàn bộ**, đảm bảo không bao giờ xảy ra lệch kho hay tạo đơn thiếu bản ghi thanh toán.

### 4.1. Luồng Đặt hàng (`placeOrder`)

```mermaid
sequenceDiagram
    actor Runner
    participant Repo as OrderRepository
    participant Tx as Firestore Transaction
    participant Variant as products/.../variants
    participant Voucher as vouchers/{code}
    participant Order as orders/{orderId}
    participant Pay as payments/{paymentId}
    participant Cart as users/{uid}/cart
    participant Notif as users/{uid}/notifications

    Runner->>Repo: Bấm "Đặt hàng" (items, address, voucherCode, paymentMethod, removeFromCart)
    Repo->>Tx: runTransaction()

    rect rgb(241, 245, 249)
        Note over Tx,Voucher: Giai đoạn 1 — ĐỌC & KIỂM TRA (All Reads First)
        Tx->>Variant: get() từng variantId trong danh sách mua
        Variant-->>Tx: stockQty hiện tại
        alt Có áp dụng voucherCode
            Tx->>Voucher: get(vouchers/{voucherCode})
            Voucher-->>Tx: isActive, expiryDate, minOrderAmount, usedCount, usageLimit
        end
    end

    Note over Tx: Kiểm tra mọi variant đều đủ stockQty >= qty<br/>và voucher hợp lệ (nếu có). Sai -> abort & báo lỗi!

    rect rgb(255, 247, 237)
        Note over Tx,Notif: Giai đoạn 2 — GHI ĐỒNG BỘ (Atomic Writes)
        Tx->>Variant: update(stockQty = stockQty - item.quantity)
        Tx->>Order: set(Order mới với status=PENDING, paymentMethod, snapshot items & address)
        Tx->>Pay: set(Payment mới: method, status=UNPAID hoặc PAID, amount)
        alt Có áp dụng voucherCode
            Tx->>Voucher: update(usedCount = usedCount + 1)
        end
        alt removeFromCart == true (đặt từ Giỏ hàng)
            Tx->>Cart: delete() các cartItemId đã thanh toán
        end
        Tx->>Notif: set(Notification mới: type=ORDER, targetId=orderCode, unread=true)
    end

    Tx-->>Repo: Commit thành công (Order + Payment)
    Repo-->>Runner: Hiển thị OrderSuccessDialog (#orderCode)
```

**Chi tiết từng bước kiểm tra & ghi trong `placeOrder`**:
1. **Kiểm tra tồn kho**: Với mỗi `item` trong `checkoutItems`, đọc `products/{item.productId}/variants/{item.variantId}`. Nếu document không tồn tại hoặc `stockQty < item.quantity` → hủy transaction với thông báo *"Sản phẩm [tên] size [size] không đủ số lượng trong kho"*.
2. **Kiểm tra Voucher (nếu `voucherCode != null`)**: Đọc `vouchers/{voucherCode}`. Kiểm tra `isActive == true`, `expiryDate > now`, `subtotal >= minOrderAmount`, và `usedCount < usageLimit` (khi `usageLimit > 0`). Nếu không đạt → hủy transaction.
3. **Trừ tồn kho**: Cập nhật `stockQty = currentStock - item.quantity` cho từng biến thể đã đọc.
4. **Tạo `Order`**: Ghi `orders/{orderId}` với `status = PENDING`, `paymentMethod`, snapshot `receiverName`, `receiverPhone`, `shippingAddress`, mảng `items` (`OrderItem` giữ sẵn `variantId` & `unitPrice`), `subtotal`, `shippingFee`, `discount`, `total`, `createdAt`, `updatedAt`.
5. **Tạo `Payment`**: Ghi `payments/{paymentId}` với `orderId`, `userId`, `method = paymentMethod`, `amount = total`, `status = UNPAID` (với `COD`; hoặc `PAID` kèm `transactionCode` & `paidAt` sau khi xác nhận thanh toán online).
6. **Cập nhật lượt dùng Voucher**: Nếu có áp mã, tăng `vouchers/{voucherCode}.usedCount` thêm `1`.
7. **Dọn giỏ hàng**: Nếu `removeFromCart == true` (không phải luồng *Mua ngay*), xóa các document `users/{uid}/cart/{cartItemId}` tương ứng.
8. **Tạo thông báo**: Thêm document `users/{uid}/notifications/{notificationId}` (`type = ORDER`, `targetId = orderCode`, `unread = true`).

---

### 4.2. Luồng Hủy đơn (`cancelOrder`)

```mermaid
sequenceDiagram
    actor Runner
    participant Repo as OrderRepository
    participant PayQuery as Query payments by orderId
    participant Tx as Firestore Transaction
    participant Order as orders/{orderId}
    participant Variant as products/.../variants
    participant Pay as payments/{paymentId}
    participant Notif as users/{uid}/notifications

    Runner->>Repo: Bấm "Hủy đơn" (orderId)
    Repo->>PayQuery: Tìm paymentId mới nhất theo orderId & userId
    PayQuery-->>Repo: paymentRef (nếu có)
    Repo->>Tx: runTransaction()

    rect rgb(241, 245, 249)
        Note over Tx,Pay: Giai đoạn 1 — ĐỌC & KIỂM TRA (All Reads First)
        Tx->>Order: get(orders/{orderId})
        Order-->>Tx: status, items, orderCode
        Note over Tx: Xác nhận status == PENDING (không cho hủy khi SHIPPING/COMPLETED/CANCELLED)
        Tx->>Variant: get() từng variantId trong order.items
        Variant-->>Tx: stockQty hiện tại
        alt Có paymentRef
            Tx->>Pay: get(payments/{paymentId})
            Pay-->>Tx: payment.status
        end
    end

    rect rgb(255, 247, 237)
        Note over Tx,Notif: Giai đoạn 2 — GHI ĐỒNG BỘ (Atomic Writes)
        Tx->>Order: update(status = CANCELLED, updatedAt = now)
        Tx->>Variant: update(stockQty = stockQty + item.quantity)
        alt Có paymentRef
            Note over Tx,Pay: Nếu payment đang PAID -> đổi sang REFUNDED<br/>Nếu payment đang UNPAID -> đổi sang FAILED
            Tx->>Pay: update(status = REFUNDED hoặc FAILED)
        end
        Tx->>Notif: set(Notification mới: "Đơn #orderCode đã được hủy")
    end

    Tx-->>Repo: Commit thành công
    Repo-->>Runner: Cập nhật UI trạng thái "Đã hủy"
```

**Lưu ý kỹ thuật cho `cancelOrder`**:
- Firestore Transaction không hỗ trợ chạy câu `Query` trực tiếp bên trong callback transaction trên Android SDK, nên cần **tìm `paymentRef` trước** (theo `orderId` + `userId`), sau đó truyền `DocumentReference` đó vào trong `runTransaction` để đọc (`transaction.get(paymentRef)`) và cập nhật an toàn.
- Chỉ cho phép hủy khi `order.status == OrderStatus.PENDING` (`order.canCancel() == true`).
- Với mỗi `OrderItem` trong `order.items`, cộng trả chính xác `item.quantity` về `products/{item.productId}/variants/{item.variantId}.stockQty`.

---

## 5. Gợi ý Firestore Security Rules (khi chuyển từ Test mode sang bảo mật)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    function isSignedIn() {
      return request.auth != null;
    }
    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }

    // Dữ liệu cá nhân dưới users/{uid}
    match /users/{uid} {
      allow read, write: if isOwner(uid);

      match /{subcollection}/{docId} {
        allow read, write: if isOwner(uid);
      }
    }

    // Danh mục công khai (Banner, Product, Variant, Review, Voucher)
    match /banners/{bannerId} {
      allow read: if true;
      allow write: if false;
    }
    match /products/{productId} {
      allow read: if true;
      allow write: if false;

      match /variants/{variantId} {
        allow read: if true;
        // Cho phép cập nhật tồn kho trong giao dịch đặt/hủy đơn (khi chưa dùng Cloud Functions)
        allow update: if isSignedIn();
      }
      match /reviews/{reviewId} {
        allow read: if true;
        allow create: if isSignedIn() && request.resource.data.userId == request.auth.uid;
      }
    }
    match /vouchers/{code} {
      allow read: if isSignedIn();
      allow update: if isSignedIn();
    }

    // Đơn hàng & Thanh toán (chỉ chủ sở hữu mới được đọc/tạo/cập nhật đơn của mình)
    match /orders/{orderId} {
      allow read: if isSignedIn() && resource.data.userId == request.auth.uid;
      allow create: if isSignedIn() && request.resource.data.userId == request.auth.uid;
      allow update: if isSignedIn() && resource.data.userId == request.auth.uid;
    }
    match /payments/{paymentId} {
      allow read: if isSignedIn() && resource.data.userId == request.auth.uid;
      allow create: if isSignedIn() && request.resource.data.userId == request.auth.uid;
      allow update: if isSignedIn() && resource.data.userId == request.auth.uid;
    }
  }
}
```
