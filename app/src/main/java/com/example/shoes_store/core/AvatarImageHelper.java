package com.example.shoes_store.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;
import com.example.shoes_store.module.profile.ProfileModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiện ích hiển thị, tải từ URL và lưu ảnh đại diện hồ sơ người dùng.
 */
public final class AvatarImageHelper {

    public interface OnAvatarUrlResultListener {
        void onResult(boolean success, @Nullable String errorMessage);
    }

    private static final int MAX_AVATAR_DIMENSION_PX = 512;
    private static final int JPEG_QUALITY = 88;
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<>(16);

    private AvatarImageHelper() {
    }

    public static boolean isHttpUrl(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        String trimmed = value.trim();
        Uri uri = Uri.parse(trimmed);
        String scheme = uri.getScheme();
        String host = uri.getHost();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && !TextUtils.isEmpty(host);
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
        String trimmed = avatarUri != null ? avatarUri.trim() : "";
        imageView.setTag(R.id.tag_avatar_uri, trimmed);

        if (isHttpUrl(trimmed)) {
            Bitmap cached = MEMORY_CACHE.get(trimmed);
            if (cached != null && !cached.isRecycled()) {
                imageView.setImageBitmap(cached);
                return;
            }
            if (fallbackResId != 0) {
                imageView.setImageResource(fallbackResId);
            }
            NETWORK_EXECUTOR.execute(() -> {
                Bitmap bitmap = downloadBitmap(trimmed);
                if (bitmap != null) {
                    MEMORY_CACHE.put(trimmed, bitmap);
                    MAIN_HANDLER.post(() -> {
                        Object currentTag = imageView.getTag(R.id.tag_avatar_uri);
                        if (trimmed.equals(currentTag)) {
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }
            });
            return;
        }

        if (!TextUtils.isEmpty(trimmed)) {
            try {
                Uri uri = Uri.parse(trimmed);
                if ("file".equalsIgnoreCase(uri.getScheme()) && uri.getPath() != null) {
                    File file = new File(uri.getPath());
                    if (file.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            return;
                        }
                    }
                } else {
                    imageView.setImageURI(null);
                    imageView.setImageURI(uri);
                    if (imageView.getDrawable() != null) {
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Fallback về ảnh mặc định nếu URI không còn hợp lệ
            }
        }
        if (fallbackResId != 0) {
            imageView.setImageResource(fallbackResId);
        }
    }

    public static void loadAvatarFromUrl(
            @NonNull ImageView imageView,
            @NonNull String rawUrl,
            @DrawableRes int fallbackResId,
            @Nullable OnAvatarUrlResultListener listener
    ) {
        String trimmed = rawUrl.trim();
        if (!isHttpUrl(trimmed)) {
            if (listener != null) {
                listener.onResult(false, "Link ảnh phải bắt đầu bằng http:// hoặc https://");
            }
            return;
        }

        Bitmap cached = MEMORY_CACHE.get(trimmed);
        if (cached != null && !cached.isRecycled()) {
            imageView.setTag(R.id.tag_avatar_uri, trimmed);
            imageView.setImageBitmap(cached);
            if (listener != null) {
                listener.onResult(true, null);
            }
            return;
        }

        NETWORK_EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(trimmed);
            MAIN_HANDLER.post(() -> {
                if (bitmap != null) {
                    MEMORY_CACHE.put(trimmed, bitmap);
                    imageView.setTag(R.id.tag_avatar_uri, trimmed);
                    imageView.setImageBitmap(bitmap);
                    if (listener != null) {
                        listener.onResult(true, null);
                    }
                } else {
                    if (listener != null) {
                        listener.onResult(false, "Không tải được ảnh từ link này. Vui lòng kiểm tra lại (ưu tiên https://)");
                    }
                }
            });
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
        Bitmap output = scaleDownIfNeeded(bitmap);
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
    private static Bitmap downloadBitmap(@NonNull String urlString) {
        String currentUrl = urlString;
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(currentUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                int status = connection.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_SEE_OTHER
                        || status == 307
                        || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (TextUtils.isEmpty(location)) {
                        return null;
                    }
                    currentUrl = new URL(url, location).toString();
                    continue;
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    return null;
                }

                byte[] bytes = readBytesLimited(connection.getInputStream());
                if (bytes == null || bytes.length == 0) {
                    return null;
                }

                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    return null;
                }

                BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
                decodeOptions.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight);
                Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOptions);
                if (decoded == null) {
                    return null;
                }
                Bitmap scaled = scaleDownIfNeeded(decoded);
                if (scaled != decoded && !decoded.isRecycled()) {
                    decoded.recycle();
                }
                return scaled;
            } catch (IOException | SecurityException e) {
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    @Nullable
    private static byte[] readBytesLimited(@Nullable InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return null;
        }
        try (InputStream in = inputStream;
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_DOWNLOAD_BYTES) {
                    return null;
                }
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
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
        decodeOptions.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight);
        try (InputStream decodeStream = context.getContentResolver().openInputStream(sourceUri)) {
            if (decodeStream == null) {
                return null;
            }
            return BitmapFactory.decodeStream(decodeStream, null, decodeOptions);
        }
    }

    private static int calculateInSampleSize(int width, int height) {
        int inSampleSize = 1;
        if (height > MAX_AVATAR_DIMENSION_PX || width > MAX_AVATAR_DIMENSION_PX) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= MAX_AVATAR_DIMENSION_PX
                    && (halfWidth / inSampleSize) >= MAX_AVATAR_DIMENSION_PX) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    @NonNull
    private static Bitmap scaleDownIfNeeded(@NonNull Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int maxSide = Math.max(width, height);
        if (maxSide <= MAX_AVATAR_DIMENSION_PX || width <= 0 || height <= 0) {
            return source;
        }
        float scale = (float) MAX_AVATAR_DIMENSION_PX / maxSide;
        int targetW = Math.max(1, Math.round(width * scale));
        int targetH = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, targetW, targetH, true);
    }
}
