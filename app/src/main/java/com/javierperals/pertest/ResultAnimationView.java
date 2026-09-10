package com.javierperals.pertest;

import android.content.Context;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Local-only animated WebP using the system renderer on every supported Android version. */
final class ResultAnimationView extends FrameLayout {
    private WebView webView;
    private String html;
    private boolean active;

    ResultAnimationView(Context context, String asset) {
        super(context);
        setFocusable(false);
        try {
                byte[] bytes;
                try (InputStream input = context.getAssets().open(asset);
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    bytes = output.toByteArray();
                }
                makeLoopInfinite(bytes);
                html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                        + "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}"
                        + "img{width:100%;height:100%;object-fit:contain}</style></head><body>"
                        + "<img alt='' src='data:image/webp;base64,"
                        + Base64.encodeToString(bytes, Base64.NO_WRAP) + "'></body></html>";
        } catch (IOException e) {
            // A missing/invalid replacement image must not prevent reviewing results.
            Log.w("ResultAnimation", "Cannot load " + asset, e);
        }
    }

    // RIFF ANIM stores a uint16 loop count after its four-byte background color.
    // Normalize only the in-memory copy so replacement assets also loop regardless of their encoded repeat count.
    static void makeLoopInfinite(byte[] bytes) {
        for (int pos = 12; pos + 8 <= bytes.length;) {
            long length = 0;
            for (int i = 0; i < 4; i++) length |= (long)(bytes[pos + 4 + i] & 255) << (8 * i);
            if (length > bytes.length - pos - 8) return;
            if (bytes[pos] == 'A' && bytes[pos + 1] == 'N' && bytes[pos + 2] == 'I'
                    && bytes[pos + 3] == 'M' && length >= 6) {
                bytes[pos + 12] = 0;
                bytes[pos + 13] = 0;
                return;
            }
            long next = pos + 8L + length + (length & 1);
            if (next > bytes.length) return;
            pos = (int) next;
        }
    }

    private void createWebView() {
        if (html == null || webView != null) return;
        active = false;
        webView = new WebView(getContext());
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setFocusable(false);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setBlockNetworkLoads(true);
        webView.setOnTouchListener((v, event) -> true);
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    private void updatePlayback() {
        boolean visible = isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown();
        if (webView != null && visible != active) {
            if (visible) {
                webView.onResume();
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            } else {
                webView.loadData("", "text/html", "UTF-8");
                webView.onPause();
            }
        }
        active = visible;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        createWebView();
        updatePlayback();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updatePlayback();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updatePlayback();
    }

    @Override protected void onDetachedFromWindow() {
        if (webView != null) {
            removeView(webView);
            webView.destroy();
            webView = null;
        }
        active = false;
        super.onDetachedFromWindow();
    }
}
