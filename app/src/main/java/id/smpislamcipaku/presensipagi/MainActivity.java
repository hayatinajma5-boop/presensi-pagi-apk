package id.smpislamcipaku.presensipagi;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final int CAMERA_REQUEST = 701;
    private static final int STORAGE_REQUEST = 702;

    private WebView webView;
    private String homeUrl;
    private PermissionRequest pendingWebPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        homeUrl = getString(R.string.website_url);

        getWindow().setStatusBarColor(Color.rgb(10, 70, 53));
        getWindow().setNavigationBarColor(Color.rgb(10, 70, 53));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT <= 28 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    STORAGE_REQUEST
            );
        }

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST
            );
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(241, 246, 243));

        webView = createWebView();
        root.addView(
                webView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(root);
        webView.loadUrl(homeUrl);
    }

    private WebView createWebView() {
        WebView w = new WebView(this);
        WebSettings s = w.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setTextZoom(100);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowContentAccess(true);
        s.setAllowFileAccess(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        String ua = s.getUserAgentString();
        s.setUserAgentString(ua + " PresensiPagiApp/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);
        }

        w.setBackgroundColor(Color.rgb(241, 246, 243));
        w.addJavascriptInterface(new AndroidBridge(this, w), "AndroidApp");
        w.setWebViewClient(new AppWebViewClient());
        w.setWebChromeClient(new AppWebChromeClient(w));
        w.setDownloadListener(new AppDownloadListener());

        return w;
    }

    private class AppWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view,
                WebResourceRequest request
        ) {
            Uri uri = request.getUrl();
            String scheme = uri.getScheme();

            if ("http".equalsIgnoreCase(scheme) ||
                    "https".equalsIgnoreCase(scheme) ||
                    "blob".equalsIgnoreCase(scheme) ||
                    "about".equalsIgnoreCase(scheme)) {
                return false;
            }

            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception e) {
                Toast.makeText(
                        MainActivity.this,
                        "Tautan tidak dapat dibuka.",
                        Toast.LENGTH_SHORT
                ).show();
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            injectNativeHelpers(view);
        }

        @Override
        public void onReceivedError(
                WebView view,
                WebResourceRequest request,
                WebResourceError error
        ) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                Toast.makeText(
                        MainActivity.this,
                        "Koneksi bermasalah. Periksa internet lalu coba lagi.",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private class AppWebChromeClient extends WebChromeClient {

        private final WebView owner;

        AppWebChromeClient(WebView owner) {
            this.owner = owner;
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<String> grants = new ArrayList<>();
                    boolean asksCamera = false;

                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                            asksCamera = true;
                            if (checkSelfPermission(Manifest.permission.CAMERA)
                                    == PackageManager.PERMISSION_GRANTED) {
                                grants.add(resource);
                            }
                        }
                    }

                    if (asksCamera && grants.isEmpty()) {
                        pendingWebPermission = request;
                        requestPermissions(
                                new String[]{Manifest.permission.CAMERA},
                                CAMERA_REQUEST
                        );
                        return;
                    }

                    if (!grants.isEmpty()) {
                        request.grant(grants.toArray(new String[0]));
                    } else {
                        request.deny();
                    }
                }
            });
        }

        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            if (pendingWebPermission == request) {
                pendingWebPermission = null;
            }
        }

        @Override
        public boolean onCreateWindow(
                WebView view,
                boolean isDialog,
                boolean isUserGesture,
                android.os.Message resultMsg
        ) {
            final Dialog dialog = new Dialog(MainActivity.this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            WebView popup = createWebView();
            popup.setWebChromeClient(new PopupChromeClient(popup, dialog));
            popup.setWebViewClient(new AppWebViewClient());
            dialog.setContentView(popup);

            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                );
                window.setGravity(Gravity.CENTER);
            }

            WebView.WebViewTransport transport =
                    (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popup);
            resultMsg.sendToTarget();
            dialog.show();
            return true;
        }

        @Override
        public boolean onJsAlert(
                WebView view,
                String url,
                String message,
                JsResult result
        ) {
            return super.onJsAlert(view, url, message, result);
        }
    }

    private class PopupChromeClient extends AppWebChromeClient {
        private final WebView popup;
        private final Dialog dialog;

        PopupChromeClient(WebView popup, Dialog dialog) {
            super(popup);
            this.popup = popup;
            this.dialog = dialog;
        }

        @Override
        public void onCloseWindow(WebView window) {
            dialog.dismiss();
            popup.destroy();
        }
    }

    private void injectNativeHelpers(WebView target) {
        String js =
                "(function(){" +
                "try{" +
                "if(window.__PRESENSI_ANDROID__)return;" +
                "window.__PRESENSI_ANDROID__=true;" +
                "var blobStore={};" +
                "var oldCreate=URL.createObjectURL.bind(URL);" +
                "var oldRevoke=URL.revokeObjectURL.bind(URL);" +
                "URL.createObjectURL=function(blob){" +
                "var u=oldCreate(blob);blobStore[u]=blob;return u;};" +
                "URL.revokeObjectURL=function(u){delete blobStore[u];return oldRevoke(u);};" +
                "function saveBlob(blob,name){" +
                "var r=new FileReader();" +
                "r.onloadend=function(){AndroidApp.saveBase64(name||('Presensi_Pagi_'+Date.now()),String(r.result));};" +
                "r.readAsDataURL(blob);" +
                "}" +
                "document.addEventListener('click',function(e){" +
                "var a=e.target&&e.target.closest?e.target.closest('a'):null;" +
                "if(!a)return;" +
                "var h=a.href||a.getAttribute('href')||'';" +
                "if(h.indexOf('blob:')===0&&blobStore[h]){" +
                "e.preventDefault();e.stopPropagation();saveBlob(blobStore[h],a.download||'Presensi_Pagi');" +
                "}" +
                "},true);" +
                "if(typeof window.downloadBlob==='function'){" +
                "window.downloadBlob=function(blob,filename){saveBlob(blob,filename);};" +
                "}" +
                "window.print=function(){AndroidApp.printPage();};" +
                "}catch(e){}" +
                "})();";

        target.evaluateJavascript(js, null);
    }

    private class AppDownloadListener implements DownloadListener {
        @Override
        public void onDownloadStart(
                String url,
                String userAgent,
                String contentDisposition,
                String mimetype,
                long contentLength
        ) {
            if (url != null && url.startsWith("blob:")) {
                Toast.makeText(
                        MainActivity.this,
                        "Menyiapkan file...",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            try {
                DownloadManager.Request request =
                        new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                request.addRequestHeader("User-Agent", userAgent);

                String cookies = CookieManager.getInstance().getCookie(url);
                if (cookies != null) {
                    request.addRequestHeader("Cookie", cookies);
                }

                request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                );
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "PresensiPagi_" + System.currentTimeMillis()
                );

                DownloadManager dm =
                        (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);

                Toast.makeText(
                        MainActivity.this,
                        "File sedang diunduh.",
                        Toast.LENGTH_SHORT
                ).show();
            } catch (Exception e) {
                Toast.makeText(
                        MainActivity.this,
                        "Download gagal: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    public static class AndroidBridge {
        private final Activity activity;
        private final WebView webView;

        AndroidBridge(Activity activity, WebView webView) {
            this.activity = activity;
            this.webView = webView;
        }

        @JavascriptInterface
        public void saveBase64(final String inputFilename, final String inputDataUrl) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String filename = inputFilename;
                        String dataUrl = inputDataUrl;

                        if (filename == null || filename.trim().isEmpty()) {
                            filename = "PresensiPagi_" + System.currentTimeMillis();
                        }
                        filename = sanitizeFilename(filename);

                        if (dataUrl == null) {
                            throw new IllegalArgumentException("Data file kosong.");
                        }

                        int comma = dataUrl.indexOf(',');
                        if (comma < 0) {
                            throw new IllegalArgumentException("Data file tidak valid.");
                        }

                        String meta = dataUrl.substring(0, comma);
                        String payload = dataUrl.substring(comma + 1);
                        byte[] bytes = android.util.Base64.decode(
                                payload,
                                android.util.Base64.DEFAULT
                        );

                        String mime = "application/octet-stream";
                        if (meta.startsWith("data:") && meta.contains(";")) {
                            mime = meta.substring(5, meta.indexOf(';'));
                        }

                        saveBytes(filename, mime, bytes);
                        Toast.makeText(
                                activity,
                                "Tersimpan di Download/Presensi Pagi: " + filename,
                                Toast.LENGTH_LONG
                        ).show();
                    } catch (Exception e) {
                        Toast.makeText(
                                activity,
                                "Gagal menyimpan file: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void printPage() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        PrintManager pm =
                                (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
                        String jobName = "Presensi Pagi";
                        pm.print(
                                jobName,
                                webView.createPrintDocumentAdapter(jobName),
                                null
                        );
                    } catch (Exception e) {
                        Toast.makeText(
                                activity,
                                "Print gagal: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
            });
        }

        private void saveBytes(String filename, String mime, byte[] bytes)
                throws Exception {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver resolver = activity.getContentResolver();
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, filename);
                values.put(MediaStore.Downloads.MIME_TYPE, mime);
                values.put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/Presensi Pagi"
                );
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                Uri uri = resolver.insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );
                if (uri == null) {
                    throw new Exception("Folder Download tidak tersedia.");
                }

                OutputStream out = resolver.openOutputStream(uri);
                if (out == null) {
                    throw new Exception("Tidak dapat membuka file.");
                }
                out.write(bytes);
                out.flush();
                out.close();

                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                );
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new Exception("Tidak dapat membuat folder Download.");
                }
                File file = new File(dir, filename);
                FileOutputStream out = new FileOutputStream(file);
                out.write(bytes);
                out.flush();
                out.close();
            }
        }

        private String sanitizeFilename(String name) {
            return name.replaceAll("[\\\\/:*?\"<>|]", "_");
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_REQUEST && pendingWebPermission != null) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pendingWebPermission.grant(
                        new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE}
                );
            } else {
                pendingWebPermission.deny();
                Toast.makeText(
                        this,
                        "Izin kamera diperlukan untuk scan QR.",
                        Toast.LENGTH_LONG
                ).show();
            }
            pendingWebPermission = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
