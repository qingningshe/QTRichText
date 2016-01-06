package com.qingningshe.richtext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author wanglei
 * @version 1.0.0
 * @description 富文本显示
 * @createTime 2016/1/4
 * @editTime
 * @editor
 */
public class QTRichText extends TextView {
    private static Pattern PATTERN_IMG_TAG = Pattern.compile("\\<img(.*?)\\>");
    private static Pattern PATTERN_IMG_WIDTH = Pattern.compile("width=\"(.*?)\"");
    private static Pattern PATTERN_IMG_HEIGHT = Pattern.compile("height=\"(.*?)\"");
    private static Pattern PATTERN_IMG_SRC = Pattern.compile("src=\"(.*?)\"");

    private HashMap<String, ImageHolder> imageHolderMap = new HashMap<String, ImageHolder>();
    private OnImgClickListener imgClick;    //图片点击回调
    private OnLinkClickListener linkClick;  //超链接点击回调
    private OnImgLoadListener imgLoad;      //图片尺寸修复回调

    private int richWidth;   //控件的宽高

    public QTRichText(Context context) {
        this(context, null);
    }

    public QTRichText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QTRichText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

    }

    public void text(String text) {
        queryImgs(text);

        Spanned spanned = Html.fromHtml(text, imgGetter, null);

        SpannableStringBuilder builder;
        if (spanned instanceof SpannableStringBuilder) {
            builder = (SpannableStringBuilder) spanned;
        } else {
            builder = new SpannableStringBuilder(spanned);
        }

        ImageSpan[] imgSpans = builder.getSpans(0, builder.length(), ImageSpan.class);
        final List<String> imgUrls = new ArrayList<String>();

        for (int i = 0, size = imgSpans.length; i < size; i++) {

            ImageSpan span = imgSpans[i];
            String path = span.getSource();
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            imgUrls.add(path);

            final int position = i;
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (imgClick != null) {
                        imgClick.onClick((ArrayList<String>) imgUrls, position);
                    }
                }
            };

            ClickableSpan[] clickableSpans = builder.getSpans(start, end, ClickableSpan.class);
            if (clickableSpans != null && clickableSpans.length != 0) {
                for (ClickableSpan cs : clickableSpans) {
                    builder.removeSpan(cs);
                }
            }
            builder.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        }

        // 处理超链接点击事件
        URLSpan[] urlSpans = builder.getSpans(0, builder.length(), URLSpan.class);

        for (int i = 0, size = urlSpans == null ? 0 : urlSpans.length; i < size; i++) {
            URLSpan span = urlSpans[i];

            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);

            builder.removeSpan(span);
            builder.setSpan(new LinkSpan(span.getURL(), linkClick), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        super.setText(spanned);
        setMovementMethod(LinkMovementMethod.getInstance());

    }


    /**
     * 查询图片
     *
     * @param text
     */
    private void queryImgs(String text) {
        ImageHolder holder = null;
        Matcher imgMatcher, srcMatcher, wMatcher, hMatcher;
        int position = 0;

        imgMatcher = PATTERN_IMG_TAG.matcher(text);

        while (imgMatcher.find()) {
            String img = imgMatcher.group().trim();
            srcMatcher = PATTERN_IMG_SRC.matcher(img);

            String src = null;
            if (srcMatcher.find()) {
                src = getTextBetweenQuotation(srcMatcher.group().trim().substring(4));
            }
            if (TextUtils.isEmpty(src)) {
                continue;
            }

            holder = new ImageHolder(src, position);

            wMatcher = PATTERN_IMG_WIDTH.matcher(img);
            if (wMatcher.find()) {
                holder.setWidth(str2Int(getTextBetweenQuotation(wMatcher.group().trim().substring(6))));
            }

            hMatcher = PATTERN_IMG_HEIGHT.matcher(img);
            if (hMatcher.find()) {
                holder.setHeight(str2Int(getTextBetweenQuotation(hMatcher.group().trim().substring(6))));
            }

            imageHolderMap.put(holder.src, holder);
            position++;
        }
    }


    private Html.ImageGetter imgGetter = new Html.ImageGetter() {
        @Override
        public Drawable getDrawable(String source) {
            UrlDrawable urlDrawable = new UrlDrawable();

            ImageHolder holder = imageHolderMap.get(source);
            if (holder == null) return null;

            if (imgLoad != null) {
                imgLoad.loadBmp(QTRichText.this, urlDrawable, holder);
            }

            return urlDrawable;
        }
    };

    /**
     * 当获取到bitmap后调用此方法
     *
     * @param drawable
     * @param holder
     * @param rawBmp
     */
    public void fillBmp(UrlDrawable drawable, ImageHolder holder, Bitmap rawBmp) {
        if (drawable == null || holder == null || rawBmp == null) {
            L.e("drawable,holder or rawBmp can not be NULL");
            return;
        }
        if (imgLoad != null) {
            imgLoad.onFix(holder);
        }

        Bitmap destBmp = holder.valid(rawBmp, richWidth);
        if (destBmp == null) {
            L.e("destBmp can not be NULL");
            return;
        }
        wrapDrawable(drawable, holder, destBmp);
    }

    private void wrapDrawable(UrlDrawable drawable, ImageHolder holder, Bitmap destBmp) {
        if (destBmp.getWidth() > richWidth) return;

        Rect rect = null;
        int left = 0;
        switch (holder.style) {
            case LEFT:
                rect = new Rect(left, 0, destBmp.getWidth(), destBmp.getHeight());
                break;

            case CENTER:
                left = (richWidth - destBmp.getWidth()) / 2;
                if (left < 0) left = 0;
                rect = new Rect(left, 0, left + destBmp.getWidth(), destBmp.getHeight());
                break;

            case RIGHT:
                left = richWidth - destBmp.getWidth();
                if (left < 0) left = 0;
                rect = new Rect(left, 0, richWidth, destBmp.getHeight());
                break;
        }
        drawable.setBounds(0, 0, destBmp.getWidth(), destBmp.getHeight());
        drawable.setBitmap(destBmp, rect);
        setText(getText());
    }

    /**
     * 从双引号之间取出字符串
     */
    private static String getTextBetweenQuotation(String text) {
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 将String转化成Int
     *
     * @param text
     * @return
     */
    private int str2Int(String text) {
        int result = -1;
        try {
            result = Integer.valueOf(text);
        } catch (Exception e) {
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        richWidth = getWidth() - getPaddingLeft() - getPaddingRight();
    }

    public static enum Style {
        LEFT,       //左对齐
        CENTER,     //居中
        RIGHT       //右对齐
    }

    public class ImageHolder {
        private String src;
        private int position;
        private int width = -1;
        private int height = -1;
        private Style style = Style.CENTER;

        public ImageHolder(String src, int position) {
            this.src = src;
            this.position = position;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public Style getStyle() {
            return style;
        }

        public void setStyle(Style style) {
            this.style = style;
        }

        /**
         * 修正参数
         *
         * @param rawBmp
         * @return
         */
        public Bitmap valid(Bitmap rawBmp, int maxWidth) {
            if (rawBmp == null) return null;

            int reqWidth = width;
            int reqHeight = height;

            if (reqWidth == -1 || reqHeight == -1) {
                reqWidth = rawBmp.getWidth();
                reqHeight = rawBmp.getHeight();
            }

            L.i("imgWidth:" + reqWidth + "-->imgHeight:" + reqHeight);

            if (reqWidth >= maxWidth) {
                float ratio = maxWidth * 1.0f / reqWidth;
                reqWidth = maxWidth;
                reqHeight = (int) (reqHeight * ratio);
            }

            L.i("reqWidth:" + reqWidth + "-->reqHeight:" + reqHeight);
            width = reqWidth;
            height = reqHeight;
            return Tool.scaleImageTo(rawBmp, reqWidth, reqHeight, false);
        }
    }


    public static class LinkSpan extends URLSpan {

        private OnLinkClickListener listener;

        public LinkSpan(String url, OnLinkClickListener listener) {
            super(url);
            this.listener = listener;
        }

        @Override
        public void onClick(View widget) {
            if (listener != null && listener.onClick(getURL()))
                return;
            super.onClick(widget);
        }
    }

    public static class UrlDrawable extends BitmapDrawable {
        private Bitmap bitmap;
        private Rect rect;
        private Paint paint;

        public UrlDrawable() {
            paint = new Paint();
        }

        @Override
        public void draw(Canvas canvas) {
            if (bitmap != null) {
                canvas.drawBitmap(bitmap, rect.left, rect.top, paint);
            }
        }

        public void setBitmap(Bitmap bitmap, Rect rect) {
            this.bitmap = bitmap;
            this.rect = rect;
        }
    }


    /**
     * 设置调试模式
     *
     * @param debug
     * @return
     */
    public QTRichText DEBUG(boolean debug) {
        L.DEBUG = debug;
        return this;
    }

    /**
     * 图片点击回调
     *
     * @param listener
     * @return
     */
    public QTRichText imgClick(OnImgClickListener listener) {
        imgClick = listener;
        return this;
    }

    /**
     * 超链接点击回调
     *
     * @param listener
     * @return
     */
    public QTRichText linkClick(OnLinkClickListener listener) {
        linkClick = listener;
        return this;
    }

    /**
     * 图片加载
     *
     * @param listener
     * @return
     */
    public QTRichText imgLoad(OnImgLoadListener listener) {
        imgLoad = listener;
        return this;
    }


    /**
     * 图片点击回调
     */
    public interface OnImgClickListener {
        void onClick(ArrayList<String> imgUrls, int position);
    }

    /**
     * 超链接点击回调
     */
    public interface OnLinkClickListener {
        boolean onClick(String url);
    }

    /**
     * 图片加载回调
     */
    public interface OnImgLoadListener {
        /**
         * 图片信息修改
         *
         * @param holder
         */
        void onFix(ImageHolder holder);

        /**
         * 图片加载
         *
         * @param richText
         * @param drawable
         * @param holder
         */
        void loadBmp(QTRichText richText, UrlDrawable drawable, ImageHolder holder);

    }


    public static class Tool {
        /**
         * 缩放图片
         *
         * @param org
         * @param newWidth
         * @param newHeight
         * @param needRecycle
         * @return
         */
        private static Bitmap scaleImageTo(Bitmap org, int newWidth, int newHeight, boolean needRecycle) {
            return scaleImage(org, (float) newWidth / org.getWidth(), (float) newHeight / org.getHeight(), needRecycle);
        }

        private static Bitmap scaleImage(Bitmap org, float scaleWidth, float scaleHeight, boolean needRecycle) {
            if (org == null) {
                return null;
            }

            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            Bitmap bitmap = Bitmap.createBitmap(org, 0, 0, org.getWidth(), org.getHeight(), matrix, true);

            if (needRecycle && !org.isRecycled()) {
                org.recycle();
            }
            return bitmap;
        }

    }


    /**
     * 日志管理
     */
    public static class L {
        private static final String TAG = "QTTextView";
        private static boolean DEBUG = true;

        public static void i(Object msg) {
            if (DEBUG) {
                Log.i(TAG, msg.toString());
            }
        }

        public static void e(Object msg) {
            if (DEBUG) {
                Log.e(TAG, msg.toString());
            }
        }
    }
}
