package io.lokal.notifications;

import com.facebook.react.bridge.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import jp.wasabeef.glide.transformations.BlurTransformation;
import java.io.ByteArrayOutputStream;

public class NotificationReCreatorModule extends ReactContextBaseJavaModule {
    private static final String MODULE_NAME = "NotificationReCreatorModule";
    private ReactApplicationContext reactContext;

    public NotificationReCreatorModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void loadImageAsBase64(String imageUrl, Promise promise) {
        try {
            Glide.with(reactContext)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            String base64 = bitmapToBase64(resource);
                            promise.resolve(base64);
                        } catch (Exception e) {
                            promise.reject("BITMAP_CONVERT_ERROR", e.getMessage());
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        promise.reject("IMAGE_LOAD_ERROR", "Failed to load image");
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        promise.reject("IMAGE_LOAD_FAILED", "Image loading failed");
                    }
                });
        } catch (Exception e) {
            promise.reject("LOAD_IMAGE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void loadBlurredImageAsBase64(String imageUrl, int radius, int sampling, Promise promise) {
        try {
            // Load image with blur transformation matching Android BlurTransformation(radius, sampling)
            Glide.with(reactContext)
                .asBitmap()
                .load(imageUrl)
                .transform(new BlurTransformation(radius, sampling))
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            String base64 = bitmapToBase64(resource);
                            promise.resolve(base64);
                        } catch (Exception e) {
                            promise.reject("BLUR_CONVERT_ERROR", e.getMessage());
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        promise.reject("BLUR_LOAD_ERROR", "Failed to load blurred image");
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        promise.reject("BLUR_LOAD_FAILED", "Blurred image loading failed");
                    }
                });
        } catch (Exception e) {
            promise.reject("LOAD_BLUR_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void preloadImage(String imageUrl, Promise promise) {
        try {
            // Preload image into Glide cache
            Glide.with(reactContext)
                .asBitmap()
                .load(imageUrl)
                .preload();
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("PRELOAD_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void clearImageCache(Promise promise) {
        try {
            // Clear Glide cache
            Glide.get(reactContext).clearMemory();
            
            // Clear disk cache on background thread
            new Thread(() -> {
                Glide.get(reactContext).clearDiskCache();
            }).start();
            
            promise.resolve(true);
        } catch (Exception e) {
            promise.reject("CLEAR_CACHE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getImageCacheSize(Promise promise) {
        try {
            // This is an approximation - Glide doesn't provide direct cache size access
            long cacheSize = 0;
            
            // Get cache directory size (approximate)
            java.io.File cacheDir = reactContext.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                cacheSize = getFolderSize(cacheDir);
            }
            
            promise.resolve((double) cacheSize);
        } catch (Exception e) {
            promise.reject("GET_CACHE_SIZE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void loadImageWithSize(String imageUrl, int width, int height, Promise promise) {
        try {
            Glide.with(reactContext)
                .asBitmap()
                .load(imageUrl)
                .override(width, height)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        try {
                            String base64 = bitmapToBase64(resource);
                            promise.resolve(base64);
                        } catch (Exception e) {
                            promise.reject("BITMAP_CONVERT_ERROR", e.getMessage());
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        promise.reject("IMAGE_LOAD_ERROR", "Failed to load image with size");
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        promise.reject("IMAGE_LOAD_FAILED", "Image loading with size failed");
                    }
                });
        } catch (Exception e) {
            promise.reject("LOAD_IMAGE_SIZE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void base64ToBitmap(String base64String, Promise promise) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            
            if (bitmap != null) {
                WritableMap result = Arguments.createMap();
                result.putInt("width", bitmap.getWidth());
                result.putInt("height", bitmap.getHeight());
                result.putBoolean("success", true);
                promise.resolve(result);
            } else {
                promise.reject("DECODE_ERROR", "Failed to decode base64 to bitmap");
            }
        } catch (Exception e) {
            promise.reject("BASE64_TO_BITMAP_ERROR", e.getMessage());
        }
    }

    // Private helper methods
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private long getFolderSize(java.io.File directory) {
        long length = 0;
        try {
            if (directory.isDirectory()) {
                java.io.File[] files = directory.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isFile()) {
                            length += file.length();
                        } else {
                            length += getFolderSize(file);
                        }
                    }
                }
            } else {
                length = directory.length();
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return length;
    }
}
