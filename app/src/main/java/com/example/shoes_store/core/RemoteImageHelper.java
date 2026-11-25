package com.example.shoes_store.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.shoes_store.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tiện ích tải & cache ảnh thống nhất cho sản phẩm, banner và avatar
 * (hỗ trợ http/https URL kèm LruCache, drawable://tên_ảnh, file:// và content://).
 */
public final class RemoteImageHelper {

    public interface OnRemoteImageResultListener {
        void onResult(boolean success, @Nullable String errorMessage);
    }

    public static final String DRAWABLE_PREFIX = "drawable://";

    private static final String TAG = "RemoteImageHelper";
    private static final int MAX_REMOTE_DIMENSION_PX = 960;
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_DOWNLOAD_BYTES = 8 * 1024 * 1024;

    private static final ExecutorService NETWORK_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(16 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return Math.max(1, value.getByteCount() / 1024);
        }
    };

    private RemoteImageHelper() {
    }

    public static boolean isHttpUrl(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        Uri uri = Uri.parse(value.trim());
        String scheme = uri.getScheme();
        String host = uri.getHost();
        return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && !TextUtils.isEmpty(host);
    }

    public static void bindImage(
            @NonNull ImageView imageView,
            @Nullable String source,
            @DrawableRes int fallbackResId
    ) {
        String trimmed = source != null ? source.trim() : "";
        imageView.setTag(R.id.tag_remote_image, trimmed);

        if (trimmed.startsWith(DRAWABLE_PREFIX)) {
            String resName = trimmed.substring(DRAWABLE_PREFIX.length()).trim();
            Context context = imageView.getContext();
            int resId = !resName.isEmpty()
                    ? context.getResources().getIdentifier(resName, "drawable", context.getPackageName())
                    : 0;
            if (resId != 0) {
                imageView.setImageResource(resId);
                return;
            }
        }

        if (isHttpUrl(trimmed)) {
            Bitmap cached = MEMORY_CACHE.get(trimmed);
            if (cached != null && !cached.isRecycled()) {
                imageView.setImageBitmap(cached);
                return;
            }
            if (fallbackResId != 0) {
                imageView.setImageResource(fallbackResId);
            }
            WeakReference<ImageView> viewRef = new WeakReference<>(imageView);
            NETWORK_EXECUTOR.execute(() -> {
                Bitmap bitmap = downloadBitmap(trimmed, MAX_REMOTE_DIMENSION_PX);
                if (bitmap != null) {
                    MEMORY_CACHE.put(trimmed, bitmap);
                    MAIN_HANDLER.post(() -> {
                        ImageView target = viewRef.get();
                        if (target != null && trimmed.equals(target.getTag(R.id.tag_remote_image))) {
                            target.setImageBitmap(bitmap);
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
                    Bitmap cached = MEMORY_CACHE.get(trimmed);
                    if (cached != null && !cached.isRecycled()) {
                        imageView.setImageBitmap(cached);
                        return;
                    }
                    if (fallbackResId != 0) {
                        imageView.setImageResource(fallbackResId);
                    }
                    String filePath = uri.getPath();
                    WeakReference<ImageView> viewRef = new WeakReference<>(imageView);
                    NETWORK_EXECUTOR.execute(() -> {
                        try {
                            File file = new File(filePath);
                            if (file.exists()) {
                                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                                if (bitmap != null) {
                                    MEMORY_CACHE.put(trimmed, bitmap);
                                    MAIN_HANDLER.post(() -> {
                                        ImageView target = viewRef.get();
                                        if (target != null && trimmed.equals(target.getTag(R.id.tag_remote_image))) {
                                            target.setImageBitmap(bitmap);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Failed to decode local image file (" + filePath + "): " + e.getMessage());
                        }
                    });
                    return;
                } else {
                    imageView.setImageURI(null);
                    imageView.setImageURI(uri);
                    if (imageView.getDrawable() != null) {
                        return;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to bind local image URI (" + trimmed + "): " + e.getMessage());
            }
        }

        if (fallbackResId != 0) {
            imageView.setImageResource(fallbackResId);
        }
    }

    public static void loadImageFromUrlWithCallback(
            @NonNull ImageView imageView,
            @NonNull String rawUrl,
            @Nullable OnRemoteImageResultListener listener
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
            imageView.setTag(R.id.tag_remote_image, trimmed);
            imageView.setImageBitmap(cached);
            if (listener != null) {
                listener.onResult(true, null);
            }
            return;
        }
        WeakReference<ImageView> viewRef = new WeakReference<>(imageView);
        NETWORK_EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(trimmed, MAX_REMOTE_DIMENSION_PX);
            if (bitmap != null) {
                MEMORY_CACHE.put(trimmed, bitmap);
            }
            MAIN_HANDLER.post(() -> {
                if (bitmap != null) {
                    ImageView target = viewRef.get();
                    if (target != null) {
                        target.setTag(R.id.tag_remote_image, trimmed);
                        target.setImageBitmap(bitmap);
                    }
                    if (listener != null) {
                        listener.onResult(true, null);
                    }
                } else if (listener != null) {
                    listener.onResult(false, "Không tải được ảnh từ link này. Vui lòng kiểm tra lại (ưu tiên https://)");
                }
            });
        });
    }

    @Nullable
    public static Bitmap downloadBitmap(@NonNull String urlString, int maxDimensionPx) {
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
                    Log.w(TAG, "HTTP " + status + " when loading image: " + currentUrl);
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
                decodeOptions.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimensionPx);
                Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, decodeOptions);
                if (decoded == null) {
                    return null;
                }
                Bitmap scaled = scaleDownIfNeeded(decoded, maxDimensionPx);
                if (scaled != decoded && !decoded.isRecycled()) {
                    decoded.recycle();
                }
                return scaled;
            } catch (IOException | SecurityException e) {
                Log.w(TAG, "Failed to download image: " + currentUrl + " (" + e.getMessage() + ")");
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

    public static int calculateInSampleSize(int width, int height, int maxDimensionPx) {
        int inSampleSize = 1;
        if (height > maxDimensionPx || width > maxDimensionPx) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= maxDimensionPx
                    && (halfWidth / inSampleSize) >= maxDimensionPx) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    @NonNull
    public static Bitmap scaleDownIfNeeded(@NonNull Bitmap source, int maxDimensionPx) {
        int width = source.getWidth();
        int height = source.getHeight();
        int maxSide = Math.max(width, height);
        if (maxSide <= maxDimensionPx || width <= 0 || height <= 0) {
            return source;
        }
        float scale = (float) maxDimensionPx / maxSide;
        int targetW = Math.max(1, Math.round(width * scale));
        int targetH = Math.max(1, Math.round(height * scale));
        return Bitmap.createScaledBitmap(source, targetW, targetH, true);
    }
}
