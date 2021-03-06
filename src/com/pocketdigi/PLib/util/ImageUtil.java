package com.pocketdigi.PLib.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

/**
 * 图片处理util
 * Created by fhp on 14-9-7.
 */
public class ImageUtil {
    private static final String TAG = "ImageUtil";

    /**
     * Android api 17实现的虚化
     * 某些机型上可能会Crash
     * @param context
     * @param sentBitmap
     * @param radius 大于1小于等于25
     * @return
     */
    @SuppressLint("NewApi")
    public static Bitmap fastblur(Context context, Bitmap sentBitmap, int radius) {
        if (sentBitmap == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT > 16) {
            Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

            final RenderScript rs = RenderScript.create(context);
            final Allocation input = Allocation.createFromBitmap(rs,
                    sentBitmap, Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT);
            final Allocation output = Allocation.createTyped(rs,
                    input.getType());
            final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs,
                    Element.U8_4(rs));
            script.setRadius(radius /* e.g. 3.f */);
            script.setInput(input);
            script.forEach(output);
            output.copyTo(bitmap);
            return bitmap;
        }
        return stackblur(context, sentBitmap, radius);
    }

    /**
     * 纯Java实现的虚化，适用老版本api，外部只需调fastblur，会自动判断
     *
     * @param context
     * @param sentBitmap
     * @param radius
     * @return
     */
    private static Bitmap stackblur(Context context, Bitmap sentBitmap,
                                    int radius) {

        Bitmap bitmap = null;
        try {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return sentBitmap;
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16)
                        | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }


    /**
     * 从文件解析出图片，可以是缩略图
     *
     * @param filePath
     *            　文件路径
     * @param inSampleSize
     *            　缩小系数，1为原图,如果是2,宽高均为原图的1/2,类推
     * @return
     */
    public static Bitmap decodeFromFile(String filePath, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
        return bitmap;
    }

    /**
     * 通过最大高宽度计算SampleSize
     *
     * @param filePath
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    private static int getSampleSizeFromFile(String filePath, int maxWidth, int maxHeight) {
        // 计算图片的真实尺寸，但不读出图片
        int[] size = getBitmapSize(filePath);
        float realWidth = size[0];
        float realHeight = size[1];

        // 如果图片尺寸比最大值小，直接返回
        if (maxWidth > realWidth && maxHeight > realHeight) {
            return 1;
        }
        // 计算宽高比
        float target_ratio = (float) maxWidth / maxHeight;
        float real_ratio = realWidth / realHeight;
        int inSampleSize = 1;
        if (real_ratio > target_ratio) {
            // 如果width太大，height太小，以width为基准，把realWidth设为maxWidth,realHeight缩放
            inSampleSize = (int) realWidth / maxWidth;
        } else {
            inSampleSize = (int) realHeight / maxHeight;
        }
        return inSampleSize;
    }

    /**
     * 通过最大高宽度计算SampleSize
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    private static int getSampleSizeFromResource(Context context, int resId, int maxWidth, int maxHeight) {
        // 计算图片的真实尺寸，但不读出图片
        int[] size = getBitmapSize(context, resId);
        return getSampleSize(size, maxWidth, maxHeight);
    }

    /**
     * 计算真实尺寸
     *
     * @param bmpSize
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    private static int getSampleSize(int[] bmpSize, int maxWidth, int maxHeight) {
        float realWidth = bmpSize[0];
        float realHeight = bmpSize[1];
        // 如果图片尺寸比最大值小，直接返回
        if (maxWidth > realWidth && maxHeight > realHeight) {
            return 1;
        }
        // 计算宽高比
        float target_ratio = (float) maxWidth / maxHeight;
        float real_ratio = realWidth / realHeight;
        int inSampleSize = 1;
        if (real_ratio > target_ratio) {
            // 如果width太大，height太小，以width为基准，把realWidth设为maxWidth,realHeight缩放
            inSampleSize = (int) realWidth / maxWidth;
        } else {
            inSampleSize = (int) realHeight / maxHeight;
        }
        return inSampleSize;
    }

    /**
     * 从文件解析出图片，可以是缩略图
     *
     * @param filePath
     * @param maxWidth
     *            缩略图最大宽度
     * @param maxHeight
     *            　缩略图最大高度
     * @return
     */
    public static Bitmap decodeFromFile(String filePath, int maxWidth, int maxHeight) {
        int inSampleSize = getSampleSizeFromFile(filePath, maxWidth, maxHeight);
        return decodeFromFile(filePath, inSampleSize);
    }

    /**
     * 从ResourceID 解析图片
     *
     * @param context
     * @param resId
     * @param maxWidth
     * @param maxHeight
     * @return
     */
    public static Bitmap decodeFromResource(Context context, int resId, int maxWidth, int maxHeight) {
        // 计算图片的真实尺寸，但不读出图片
        int inSampleSize = getSampleSizeFromResource(context, resId, maxWidth, maxHeight);
        return decodeFromResource(context, resId, inSampleSize);
    }

    /**
     * 从ResourceID 解析图片
     *
     * @param context
     * @param resId
     * @param inSampleSize
     * @return
     */
    public static Bitmap decodeFromResource(Context context, int resId, int inSampleSize) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);
        return bitmap;
    }

    /**
     * 获取图片的尺寸,不会真正读取,省资源
     *
     * @param context
     * @param resId
     */
    public static int[] getBitmapSize(Context context, int resId) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), resId, options);
        int[] size = new int[2];
        size[0] = options.outWidth;
        size[1] = options.outHeight;
        return size;
    }

    /**
     * 获取图片的尺寸,不会真正读取,省资源
     */
    public static int[] getBitmapSize(String jpgPath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(jpgPath, options);
        int[] size = new int[2];
        size[0] = options.outWidth;
        size[1] = options.outHeight;
        return size;
    }

}