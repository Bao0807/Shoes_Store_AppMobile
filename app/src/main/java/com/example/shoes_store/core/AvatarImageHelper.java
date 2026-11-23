package com.example.shoes_store.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.module.profile.ProfileModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tiện ích hiển thị, tải từ URL và lưu ảnh đại diện hồ sơ người dùng.
 */
public final class AvatarImageHelper {

    public interface OnAvatarUrlResultListener {
        void onResult(boolean success, @Nullable String errorMessage);
    }

    private static final int MAX_AVATAR_DIMENSION_PX = 512;
    private static final int JPEG_QUALITY = 88;

    private AvatarImageHelper() {
    }

    public static boolean isHttpUrl(@Nullable String value) {
        return RemoteImageHelper.isHttpUrl(value);
    }

    public static void bindAvatar(@NonNull ImageView imageView, @Nullable ProfileModel profile) {
        if (profile == null) {
            return;
        }
        bindAvatar(imageView, profile.getAvatarUri(), profile.getAvatarResId());
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
            @DrawableRes int fallbackResId,
            @Nullable OnAvatarUrlResultListener listener
    ) {
        RemoteImageHelper.loadImageFromUrlWithCallback(imageView, rawUrl, (success, errorMessage) -> {
            if (listener != null) {
                listener.onResult(success, errorMessage);
            }
        });
    }

    @Nullable
    public static String saveAvatarFromUri(@NonNull Context context, @NonNull Uri sourceUri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), sourceUri);
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
                bitmap = decodeSampledBitmap(context, sourceUri);
            }
            if (bitmap == null) {
                return null;
            }
            return saveAvatarBitmap(context, bitmap);
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    @Nullable
    public static String saveAvatarBitmap(@NonNull Context context, @NonNull Bitmap bitmap) {
        Bitmap output = RemoteImageHelper.scaleDownIfNeeded(bitmap, MAX_AVATAR_DIMENSION_PX);
        File file = new File(context.getFilesDir(), "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            if (!output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)) {
                return null;
            }
            fos.flush();
            return Uri.fromFile(file).toString();
        } catch (IOException e) {
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
