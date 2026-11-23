package com.example.shoes_store.module.auth;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.core.AppPrefsHelper;
import com.example.shoes_store.core.FirebaseProviderHelper;
import com.example.shoes_store.core.FirestoreReadUtil;
import com.example.shoes_store.core.ResourceCallback;
import com.example.shoes_store.module.admin.AdminService;
import com.example.shoes_store.module.notification.NotificationMapper;
import com.example.shoes_store.module.notification.NotificationModel;
import com.example.shoes_store.module.profile.ProfileMapper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Repository xử lý Đăng nhập, Đăng ký, Quên mật khẩu và Đăng xuất qua Firebase Authentication.
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private static volatile AuthRepository instance;

    private final FirebaseProviderHelper provider = FirebaseProviderHelper.getInstance();

    private AuthRepository() {
    }

    public static AuthRepository getInstance() {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository();
                }
            }
        }
        return instance;
    }

    // ===== 1. XÁC THỰC =====

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
                    FirebaseUser user = authResult.getUser();
                    if (user != null && AdminService.isDefaultAdminEmail(cleanEmail)) {
                        ensureAdminUserDocument(user, cleanEmail, callback);
                    } else {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    if (AdminService.isDefaultAdminEmail(cleanEmail)
                            && AdminService.DEFAULT_ADMIN_PASS.equals(password)) {
                        autoCreateDefaultAdminAccount(cleanEmail, password, e, callback);
                    } else {
                        callback.onError(mapAuthError(e));
                    }
                });
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

    /**
     * Xác định tài khoản vừa đăng nhập có `users/{uid}.role == "admin"` hay `"users"`.
     */
    public void resolveUserIsAdmin(
            @Nullable FirebaseUser user,
            @NonNull ResourceCallback<Boolean> callback
    ) {
        if (user == null) {
            callback.onSuccess(false);
            return;
        }
        if (AdminService.isDefaultAdminEmail(user.getEmail())) {
            callback.onSuccess(true);
            return;
        }
        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    boolean isAdmin = false;
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String role = FirestoreReadUtil.readString(task.getResult(), ProfileMapper.FIELD_ROLE, "");
                        isAdmin = AdminService.isAdminRole(role);
                    }
                    callback.onSuccess(isAdmin);
                });
    }

    // ===== 2. PRIVATE HELPERS =====

    private void autoCreateDefaultAdminAccount(
            @NonNull String adminEmail,
            @NonNull String adminPassword,
            @Nullable Exception signInError,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        provider.getAuth()
                .createUserWithEmailAndPassword(adminEmail, adminPassword)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) {
                        callback.onError(mapAuthError(signInError));
                        return;
                    }
                    AppPrefsHelper.saveLastLoginEmail(adminEmail);
                    ensureAdminUserDocument(user, adminEmail, callback);
                })
                .addOnFailureListener(createErr -> callback.onError(mapAuthError(signInError)));
    }

    private void ensureAdminUserDocument(
            @NonNull FirebaseUser user,
            @NonNull String email,
            @NonNull ResourceCallback<FirebaseUser> callback
    ) {
        String uid = user.getUid();
        Timestamp now = Timestamp.now();
        Map<String, Object> adminData = new HashMap<>();
        adminData.put(ProfileMapper.FIELD_UID, uid);
        adminData.put(ProfileMapper.FIELD_FULL_NAME, "Quản trị viên (Admin)");
        adminData.put(ProfileMapper.FIELD_EMAIL, email);
        adminData.put(ProfileMapper.FIELD_ROLE, AdminService.ROLE_ADMIN);
        adminData.put(ProfileMapper.FIELD_UPDATED_AT, now);

        provider.getFirestore()
                .collection(FirebaseProviderHelper.COL_USERS)
                .document(uid)
                .set(adminData, SetOptions.merge())
                .addOnCompleteListener(task -> callback.onSuccess(user));
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
        Map<String, Object> userData = ProfileMapper.newUserToMap(
                uid, fullName, email, phone, AppPrefsHelper.getRecentSearches(), now
        );
        batch.set(userRef, userData);

        DocumentReference notifRef = userRef.collection(FirebaseProviderHelper.SUB_NOTIFICATIONS).document("welcome");
        Map<String, Object> notifData = NotificationMapper.toMap(
                "welcome",
                uid,
                "Chào mừng " + fullName + " đến với Shoes Store!",
                "Dùng mã NEWRUNNER100K để giảm 100.000₫ cho đơn hàng đầu tiên.",
                NotificationModel.Type.PROMO,
                "NEWRUNNER100K",
                now
        );
        batch.set(notifRef, notifData);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess(user))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Create initial user batch failed: " + e.getMessage());
                    callback.onSuccess(user);
                });
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
