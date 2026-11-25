package com.example.shoes_store.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiện ích hiển thị, tải từ URL và lưu ảnh đại diện hồ sơ người dùng.
 */
public final class AvatarImageHelper {

    private static final String TAG = "AvatarImageHelper";

    public interface OnAvatarUrlResultListener {
        void onResult(boolean success, @Nullable String errorMessage);
    }

    private static final int MAX_AVATAR_DIMENSION_PX = 512;
    private static final int JPEG_QUALITY = 88;
    private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private AvatarImageHelper() {
    }

    public static boolean isHttpUrl(@Nullable String value) {
        return RemoteImageHelper.isHttpUrl(value);
    }

    public static void bindAvatar(
            @NonNull ImageView imageView,
            @Nullable String avatarUri,
            @DrawableRes int fallbackResId
    ) {
        RemoteImageHelper.bindImage(imageView, avatarUri, fallbackResId);
    }

    public static void loadAvatarFromUrl(
            @NonNull ImageView imageView,
            @NonNull String rawUrl,
            @Nullable OnAvatarUrlResultListener listener
    ) {
        RemoteImageHelper.loadImageFromUrlWithCallback(imageView, rawUrl, (success, errorMessage) -> {
            if (listener != null) {
                listener.onResult(success, errorMessage);
            }
        });
    }

    public static void saveAvatarFromUri(
            @NonNull Context context,
            @NonNull Uri sourceUri,
            @NonNull ResourceCallback<String> callback
    ) {
        Context appContext = context.getApplicationContext();
        IO_EXECUTOR.execute(() -> {
            String savedUri = saveAvatarFromUriSync(appContext, sourceUri);
            MAIN_HANDLER.post(() -> {
                if (savedUri != null) {
                    callback.onSuccess(savedUri);
                } else {
                    callback.onError("Không thể đọc ảnh đã chọn");
                }
            });
        });
    }

    public static void saveAvatarBitmap(
            @NonNull Context context,
            @NonNull Bitmap bitmap,
            @NonNull ResourceCallback<String> callback
    ) {
        Context appContext = context.getApplicationContext();
        IO_EXECUTOR.execute(() -> {
            String savedUri = saveAvatarBitmapSync(appContext, bitmap);
            MAIN_HANDLER.post(() -> {
                if (savedUri != null) {
                    callback.onSuccess(savedUri);
                } else {
                    callback.onError("Không thể lưu ảnh vừa chụp");
                }
            });
        });
    }

    @Nullable
    private static String saveAvatarFromUriSync(@NonNull Context appContext, @NonNull Uri sourceUri) {
        Bitmap bitmap = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(appContext.getContentResolver(), sourceUri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int width = info.getSize().getWidth();
                    int height = info.getSize().getHeight();
                    int maxSide = Math.max(width, height);
                    if (maxSide > MAX_AVATAR_DIMENSION_PX && width > 0 && height > 0) {
                        float scale = (float) MAX_AVATAR_DIMENSION_PX / maxSide;
                        int targetW = Math.max(1, Math.round(width * scale));
                        int targetH = Math.max(1, Math.round(height * scale));
                        decoder.setTargetSize(targetW, targetH);
                    }
                });
            } else {
                bitmap = decodeSampledBitmap(appContext, sourceUri);
            }
            if (bitmap == null) {
                return null;
            }
            return saveAvatarBitmapSync(appContext, bitmap);
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Save avatar from URI failed: " + e.getMessage());
            return null;
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    @Nullable
    private static String saveAvatarBitmapSync(@NonNull Context appContext, @NonNull Bitmap bitmap) {
        Bitmap output = RemoteImageHelper.scaleDownIfNeeded(bitmap, MAX_AVATAR_DIMENSION_PX);
        File file = new File(appContext.getFilesDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (!output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)) {
                return null;
            }
            fos.flush();
            return Uri.fromFile(file).toString();
        } catch (IOException e) {
            Log.w(TAG, "Save avatar bitmap failed: " + e.getMessage());
            return null;
        } finally {
            if (output != bitmap && !output.isRecycled()) {
                output.recycle();
            }
        }
    }

    @Nullable
    private static Bitmap decodeSampledBitmap(@NonNull Context context, @NonNull Uri sourceUri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream boundsStream = context.getContentResolver().openInputStream(sourceUri)) {
            if (boundsStream == null) {
                return null;
            }
            BitmapFactory.decodeStream(boundsStream, null, bounds);
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = RemoteImageHelper.calculateInSampleSize(
                bounds.outWidth,
                bounds.outHeight,
                MAX_AVATAR_DIMENSION_PX
        );
        try (InputStream decodeStream = context.getContentResolver().openInputStream(sourceUri)) {
            if (decodeStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        }
    }
}
