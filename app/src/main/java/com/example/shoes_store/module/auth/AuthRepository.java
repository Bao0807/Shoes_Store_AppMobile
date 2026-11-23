package com.example.shoes_store.module.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.notification.NotificationModel;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository xử lý Đăng nhập, Đăng ký, Quên mật khẩu và Đăng xuất qua Firebase Authentication.
 */
public class AuthRepository {

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    public void login(
            @NonNull String email,
            @NonNull String password,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        String cleanEmail = email.trim();
        provider.getAuth()
                .signInWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(authResult -> {
                    AppPrefsHelper.saveLastLoginEmail(cleanEmail);
                    callback.onSuccess(authResult.getUser());
                })
                .addOnFailureListener(e -> callback.onError(mapAuthError(e)));
    }

    public void register(
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull String password,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        String cleanName = fullName.trim();
        String cleanPhone = phone.trim().replaceAll("\\s+", "");
        String cleanEmail = email.trim();

        FirebaseAuth auth = provider.getAuth();
        auth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError("Không thể khởi tạo tài khoản");
                        return;
                    }
                    AppPrefsHelper.saveLastLoginEmail(cleanEmail);
                    createInitialUserDocument(user, cleanName, cleanPhone, cleanEmail, callback);
                })
                .addOnFailureListener(e -> callback.onError(mapAuthError(e)));
    }

    public void sendPasswordReset(
            @NonNull String email,
            @NonNull ResourceCallback<Void> callback
    ) {
        provider.getAuth()
                .sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(unused -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(mapAuthError(e)));
    }

    public void logout() {
        provider.getAuth().signOut();
    }

    private void createInitialUserDocument(
            @NonNull FirebaseUser user,
            @NonNull String fullName,
            @NonNull String phone,
            @NonNull String email,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        String uid = user.getUid();
        FirebaseFirestore db = provider.getFirestore();
        Timestamp now = Timestamp.now();

        WriteBatch batch = db.batch();

        DocumentReference userRef = db.collection(FirebaseProviderHelper.COL_USERS).document(uid);
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("fullName", fullName);
        userData.put("email", email);
        userData.put("phone", phone);
        userData.put("avatarUrl", "");
        userData.put("preferredSize", "42");
        userData.put("runningGoal", "Daily");
        userData.put("role", "CUSTOMER");
        userData.put("recentSearches", new ArrayList<>(AppPrefsHelper.getRecentSearches()));
        userData.put("createdAt", now);
        userData.put("updatedAt", now);
        batch.set(userRef, userData);

        DocumentReference notifRef = userRef.collection(FirebaseProviderHelper.SUB_NOTIFICATIONS).document("welcome");
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("notificationId", "welcome");
        notifData.put("userId", uid);
        notifData.put("title", "Chào mừng " + fullName + " đến với Shoes Store!");
        notifData.put("message", "Dùng mã NEWRUNNER100K để giảm 100.000₫ cho đơn hàng đầu tiên.");
        notifData.put("type", NotificationModel.Type.PROMO.name());
        notifData.put("targetId", "NEWRUNNER100K");
        notifData.put("unread", true);
        notifData.put("createdAt", now);
        batch.set(notifRef, notifData);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(e -> callback.onSuccess(user));
    }

    @NonNull
    private String mapAuthError(@Nullable Exception e) {
        if (e instanceof FirebaseNetworkException) {
            return "Không có kết nối mạng. Vui lòng thử lại";
        }
        if (e instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) e).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return "Email không đúng định dạng";
                case "ERROR_WRONG_PASSWORD":
                case "ERROR_INVALID_CREDENTIAL":
                    return "Email hoặc mật khẩu không chính xác";
                case "ERROR_USER_NOT_FOUND":
                    return "Tài khoản không tồn tại";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Email này đã được đăng ký";
                case "ERROR_WEAK_PASSWORD":
                    return "Mật khẩu quá yếu (tối thiểu 6 ký tự)";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Bạn thao tác quá nhanh, vui lòng thử lại sau ít phút";
                default:
                    break;
            }
        }
        return e != null && e.getLocalizedMessage() != null
                ? e.getLocalizedMessage()
                : "Thao tác thất bại, vui lòng thử lại";
    }
}
