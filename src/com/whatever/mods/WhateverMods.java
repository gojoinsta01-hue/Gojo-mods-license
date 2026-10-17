package com.whatever.mods;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhateverMods {

    // ==========================================================
    // OPEN STRINGS
    // ==========================================================
    private static final String DIALOG_NAME = "..ᴳᵒʲᵒメsᴀᴛᴏʀᴜシ Mods";
    private static final String GET_KEY_URL = "https://animezone-premiums.gt.tc/api/generate.php";
    private static final String JOIN_URL    = "https://whatsapp.com/channel/0029Vb9T2AJGJP8KUTmkz80E";
    private static final String UPDATE_LINK = "https://animezone-premiums.gt.tc/update"; 
    
    // ==========================================================
    // ENCRYPTED URLS (Base64 + XOR 0x5A)
    // ==========================================================
    // https://animezone-premiums.gt.tc/api/verify.php
    private static final String ENC_API     = "dT1SBFc9DBR/ZDV0EC12NkZ9biI2DxJxKkBPFTQhXhc9NQwbbWcmQht1OR99KSUxSUp/PkAKMWMnEFooJRVjZjQ/CVohIXR7NTVhWn9zLFYUMGBXVS9zMUhqNGMPVHgmSWt9LRIIEzUlBUt9eQQEbTQ2RWpqOR1qKjEQ";
    // https://animezone-premiums.gt.tc/update.json
    private static final String ENC_UPDJSON = "dT1SBFc9DBR/ZDV0EC12NkZ9biI2DxJxKkBPFTQhXhc9NQwbbWcmQht1OR99KSUxSUp/PkAKMWMnEFooJRVjZjQ/CVohIXR7NTVhWn9zLFYUMGBXVS9zMUhqNGMPVHgmSWt9LRIIEzUlBUt9eQQEbTQ2RWpqOR1qKjEQ";
    private static final byte XOR_KEY = 0x5A;

    public static final String LOGO_ASSET_NAME = "logo.png";
    public static final int    CURRENT_VERSION = 1;

    private static final String TAG        = "GojoMods";
    private static final String PREFS_NAME = "gojo_mods_prefs";
    private static final String KEY_SAVED  = "saved_key";
    private static final String KEY_DEVICE = "device_id_lock";
    private static final String UA         = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36";

    private static final int C_BG        = 0xFF0A0A14;
    private static final int C_BORDER    = 0xFF8B5CF6;
    private static final int C_TITLE     = 0xFFC4B5FD;
    private static final int C_SUBTITLE  = 0xFF9CA3AF;
    private static final int C_INPUT_BG  = 0xFF14141F;
    private static final int C_HINT      = 0xFF6B7280;
    private static final int C_PURPLE_A  = 0xFF8B5CF6;
    private static final int C_PURPLE_B  = 0xFF6D28D9;
    private static final int C_GREEN_A   = 0xFF10B981;
    private static final int C_GREEN_B   = 0xFF059669;
    private static final int C_JOIN_BRD  = 0xFF7C3AED;
    private static final int C_WHITE     = 0xFFFFFFFF;

    private static final Handler UI  = new Handler(Looper.getMainLooper());
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);

    public interface Callback {
        void onSuccess();
        void onFail(String msg);
    }

    private static String dec(String enc) {
        try {
            byte[] raw = Base64.decode(enc, Base64.DEFAULT);
            for (int i = 0; i < raw.length; i++) raw[i] = (byte) (raw[i] ^ XOR_KEY);
            return new String(raw, "UTF-8");
        } catch (Throwable t) {
            return "";
        }
    }

    private static String getDeviceId(Context ctx) {
        try {
            SharedPreferences p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String locked = p.getString(KEY_DEVICE, null);
            if (locked != null && locked.length() > 8) return locked;

            String androidId = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (androidId == null || androidId.length() < 8) androidId = "";

            String serial = "";
            try { serial = Build.SERIAL; } catch (Throwable ignored) {}
            if (serial == null || serial.equals(Build.UNKNOWN)) serial = "";

            String fp = (androidId + "|" + serial + "|" + Build.MODEL + "|" + Build.BRAND + "|" + Build.DEVICE);
            String hashed = sha256Hex(fp);
            String finalId = (androidId.length() >= 8 ? androidId : hashed.substring(0, 16));

            p.edit().putString(KEY_DEVICE, finalId).apply();
            return finalId;
        } catch (Throwable t) {
            return "unknown_device";
        }
    }

    private static String sha256Hex(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Throwable t) {
            return "fallback" + s.hashCode();
        }
    }

    public static void inject(final Activity act) {
        if (act == null) return;
        try {
            final SharedPreferences prefs = act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            final String saved = prefs.getString(KEY_SAVED, null);
            if (saved != null && saved.length() > 0) {
                POOL.execute(new Runnable() {
                    @Override public void run() {
                        final String deviceId = getDeviceId(act);
                        final String status = verifyKey(saved, deviceId);
                        if ("ACTIVE".equals(status)) {
                            UI.post(new Runnable() {
                                @Override public void run() {
                                    try { checkUpdate(act); } catch (Throwable t) {}
                                }
                            });
                        } else {
                            UI.post(new Runnable() {
                                @Override public void run() {
                                    try { showDialog(act, prefs); } catch (Throwable t) {}
                                }
                            });
                        }
                    }
                });
            } else {
                showDialog(act, prefs);
            }
        } catch (Throwable t) {
            Log.e(TAG, "inject failed", t);
        }
    }

    // ==========================================================
    // VERIFY (No JSON lib, manual string check)
    // ==========================================================
    private static String verifyKey(String key, String deviceId) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(dec(ENC_API));
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", UA);

            // Manual JSON string
            String body = "{\"key\":\"" + key + "\",\"device_id\":\"" + deviceId + "\",\"client\":\"whatever_client\"}";
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            Log.d(TAG, "verify HTTP: " + code);

            if (code >= 200 && code < 300) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                String resp = sb.toString().trim();
                Log.d(TAG, "verify response: " + resp);

                // MANUAL CHECK: No JSON parser to crash
                if (resp.contains("\"status\":\"ACTIVE\"") || resp.contains("\"status\": \"ACTIVE\"")) {
                    return "ACTIVE";
                } else if (resp.contains("\"status\":\"EXPIRED\"") || resp.contains("\"status\": \"EXPIRED\"")) {
                    return "EXPIRED";
                } else {
                    return "INVALID";
                }
            } else {
                return "SERVER_ERROR_" + code;
            }
        } catch (Throwable t) {
            Log.e(TAG, "verifyKey err", t);
            return "NETWORK_ERROR";
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    private static void checkUpdate(final Activity act) {
        POOL.execute(new Runnable() {
            @Override public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(dec(ENC_UPDJSON));
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setUseCaches(false);
                    conn.setRequestProperty("User-Agent", UA);

                    int code = conn.getResponseCode();
                    if (code < 200 || code >= 300) return; // Safely skip if not 200

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    String raw = sb.toString().trim();
                    Log.d(TAG, "update raw: " + raw);
                    
                    // If the server returns HTML, just skip
                    if (!raw.contains("{")) return;

                    // Simple hacky parse without org.json
                    final String msg = "New version available";
                    boolean avail = raw.contains("\"update_available\":true") || raw.contains("\"update_available\": true");
                    
                    if (avail) {
                        UI.post(new Runnable() {
                            @Override public void run() {
                                try { showUpdateDialog(act, msg, UPDATE_LINK); } catch (Throwable t) {}
                            }
                        });
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "update failed", t);
                } finally {
                    if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
                }
            }
        });
    }

    private static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    private static GradientDrawable roundedGradient(int colorA, int colorB, int radiusPx, int strokeColor, int strokePx) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{colorA, colorB});
        g.setCornerRadius(radiusPx);
        if (strokePx > 0) g.setStroke(strokePx, strokeColor);
        return g;
    }

    private static GradientDrawable solidRounded(int color, int radiusPx, int strokeColor, int strokePx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        if (strokePx > 0) g.setStroke(strokePx, strokeColor);
        return g;
    }

    private static Bitmap circularBitmap(Bitmap src) {
        int size = Math.min(src.getWidth(), src.getHeight());
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rect rect = new Rect(0, 0, size, size);
        RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.WHITE);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);
        return out;
    }

    private static Bitmap loadLogo(Activity act) {
        try {
            java.io.InputStream is = act.getAssets().open(LOGO_ASSET_NAME);
            Bitmap raw = BitmapFactory.decodeStream(is);
            is.close();
            if (raw == null) return null;
            return circularBitmap(raw);
        } catch (Throwable t) {
            return null;
        }
    }

    private static View purpleCircleFallback(Context c, int sizePx) {
        FrameLayout wrap = new FrameLayout(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(sizePx, sizePx);
        wrap.setLayoutParams(lp);
        View circle = new View(c);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(sizePx, sizePx);
        circle.setLayoutParams(cp);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(C_BORDER);
        circle.setBackground(g);
        wrap.addView(circle);
        return wrap;
    }

    private static LinearLayout.LayoutParams lpMatch(int bottomMarginPx) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = bottomMarginPx;
        return lp;
    }

    private static void showDialog(final Activity act, final SharedPreferences prefs) {
        try {
            final int radius = dp(act, 18);
            final int pad    = dp(act, 20);
            final int stroke = dp(act, 2);
            final int btnH   = dp(act, 48);

            FrameLayout root = new FrameLayout(act);
            root.setPadding(dp(act, 16), dp(act, 16), dp(act, 16), dp(act, 16));

            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(pad, pad, pad, pad);
            card.setBackground(roundedGradient(C_BG, C_BG, radius, C_BORDER, stroke));

            Bitmap logo = loadLogo(act);
            int logoSize = dp(act, 88);
            if (logo != null) {
                ImageView iv = new ImageView(act);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(logoSize, logoSize);
                ilp.gravity = Gravity.CENTER_HORIZONTAL;
                ilp.bottomMargin = dp(act, 12);
                iv.setLayoutParams(ilp);
                iv.setImageBitmap(logo);
                card.addView(iv);
            } else {
                View circle = purpleCircleFallback(act, logoSize);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(logoSize, logoSize);
                ilp.gravity = Gravity.CENTER_HORIZONTAL;
                ilp.bottomMargin = dp(act, 12);
                circle.setLayoutParams(ilp);
                card.addView(circle);
            }

            TextView title = new TextView(act);
            title.setText(DIALOG_NAME);
            title.setTextColor(C_TITLE);
            title.setTextSize(20f);
            title.setGravity(Gravity.CENTER);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            title.setLayoutParams(lpMatch(dp(act, 4)));
            card.addView(title);

            TextView sub = new TextView(act);
            sub.setText("Premium Unlocked Version");
            sub.setTextColor(C_SUBTITLE);
            sub.setTextSize(13f);
            sub.setGravity(Gravity.CENTER);
            sub.setLayoutParams(lpMatch(dp(act, 18)));
            card.addView(sub);

            final EditText input = new EditText(act);
            input.setHint("GOJO-XXXXXXXX");
            input.setHintTextColor(C_HINT);
            input.setTextColor(C_WHITE);
            input.setTextSize(15f);
            input.setSingleLine(true);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            input.setPadding(dp(act, 14), dp(act, 12), dp(act, 14), dp(act, 12));
            input.setBackground(solidRounded(C_INPUT_BG, dp(act, 10), C_BORDER, dp(act, 1)));
            LinearLayout.LayoutParams inLp = lpMatch(dp(act, 14));
            inLp.height = dp(act, 48);
            input.setLayoutParams(inLp);
            card.addView(input);

            final TextView verify = new TextView(act);
            verify.setText("VERIFY");
            verify.setTextColor(C_WHITE);
            verify.setTextSize(15f);
            verify.setGravity(Gravity.CENTER);
            verify.setTypeface(null, android.graphics.Typeface.BOLD);
            verify.setBackground(roundedGradient(C_PURPLE_A, C_PURPLE_B, dp(act, 10), 0, 0));
            LinearLayout.LayoutParams vLp = lpMatch(dp(act, 10));
            vLp.height = btnH;
            verify.setLayoutParams(vLp);
            card.addView(verify);

            TextView getKey = new TextView(act);
            getKey.setText("GET KEY");
            getKey.setTextColor(C_WHITE);
            getKey.setTextSize(15f);
            getKey.setGravity(Gravity.CENTER);
            getKey.setTypeface(null, android.graphics.Typeface.BOLD);
            getKey.setBackground(roundedGradient(C_GREEN_A, C_GREEN_B, dp(act, 10), 0, 0));
            LinearLayout.LayoutParams gLp = lpMatch(dp(act, 10));
            gLp.height = btnH;
            getKey.setLayoutParams(gLp);
            card.addView(getKey);

            TextView join = new TextView(act);
            join.setText("JOIN US");
            join.setTextColor(C_TITLE);
            join.setTextSize(15f);
            join.setGravity(Gravity.CENTER);
            join.setTypeface(null, android.graphics.Typeface.BOLD);
            join.setBackground(solidRounded(C_BG, dp(act, 10), C_JOIN_BRD, dp(act, 2)));
            LinearLayout.LayoutParams jLp = lpMatch(dp(act, 14));
            jLp.height = btnH;
            join.setLayoutParams(jLp);
            card.addView(join);

            TextView footer = new TextView(act);
            footer.setText("Powered by " + DIALOG_NAME);
            footer.setTextColor(C_SUBTITLE);
            footer.setTextSize(11f);
            footer.setGravity(Gravity.CENTER);
            card.addView(footer);

            ScrollView sv = new ScrollView(act);
            sv.addView(card);
            root.addView(sv);

            final AlertDialog dlg = new AlertDialog.Builder(act)
                    .setView(root)
                    .setCancelable(false)
                    .create();

            Window w = dlg.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }

            verify.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    final String key = input.getText().toString().trim().toUpperCase();
                    if (key.length() < 4) {
                        Toast.makeText(act, "Enter a valid key", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    verify.setEnabled(false);
                    verify.setText("VERIFYING...");

                    POOL.execute(new Runnable() {
                        @Override public void run() {
                            final String deviceId = getDeviceId(act);
                            final String status   = verifyKey(key, deviceId);
                            final String msg;

                            if ("ACTIVE".equals(status)) {
                                msg = "✓ Access Granted! Enjoy.";
                            } else if ("EXPIRED".equals(status)) {
                                msg = "Key expired!";
                            } else if (status.startsWith("SERVER_ERROR")) {
                                msg = "Server error: " + status.replace("SERVER_ERROR_", "");
                            } else if ("NETWORK_ERROR".equals(status)) {
                                msg = "Network Error. Try again.";
                            } else {
                                msg = "Invalid or Expired Key!";
                            }

                            UI.post(new Runnable() {
                                @Override public void run() {
                                    try {
                                        if ("ACTIVE".equals(status)) {
                                            prefs.edit().putString(KEY_SAVED, key).apply();
                                            Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
                                            try { dlg.dismiss(); } catch (Throwable ignored) {}
                                            checkUpdate(act);
                                        } else {
                                            verify.setEnabled(true);
                                            verify.setText("VERIFY");
                                            Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
                                        }
                                    } catch (Throwable t) {}
                                }
                            });
                        }
                    });
                }
            });

            getKey.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        String deviceId = getDeviceId(act);
                        String url = GET_KEY_URL;
                        if (url.contains("?")) url += "&device_id=" + deviceId;
                        else                  url += "?device_id=" + deviceId;
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(i);
                    } catch (Throwable t) {}
                }
            });

            join.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(JOIN_URL));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(i);
                    } catch (Throwable t) {}
                }
            });

            dlg.show();
        } catch (Throwable t) {}
    }

    private static void showUpdateDialog(final Activity act, String message, final String updateLink) {
        try {
            LinearLayout card = new LinearLayout(act);
            card.setOrientation(LinearLayout.VERTICAL);
            int pad = dp(act, 20);
            card.setPadding(pad, pad, pad, pad);
            card.setBackground(roundedGradient(C_BG, C_BG, dp(act, 18), C_BORDER, dp(act, 2)));

            TextView t = new TextView(act);
            t.setText("🚀 Update Available");
            t.setTextColor(C_TITLE);
            t.setTextSize(18f);
            t.setGravity(Gravity.CENTER);
            t.setTypeface(null, android.graphics.Typeface.BOLD);
            t.setLayoutParams(lpMatch(dp(act, 10)));
            card.addView(t);

            TextView m = new TextView(act);
            m.setText(message);
            m.setTextColor(C_SUBTITLE);
            m.setTextSize(13f);
            m.setGravity(Gravity.CENTER);
            m.setLayoutParams(lpMatch(dp(act, 16)));
            card.addView(m);

            TextView updateBtn = new TextView(act);
            updateBtn.setText("UPDATE NOW");
            updateBtn.setTextColor(C_WHITE);
            updateBtn.setTextSize(15f);
            updateBtn.setGravity(Gravity.CENTER);
            updateBtn.setTypeface(null, android.graphics.Typeface.BOLD);
            updateBtn.setBackground(roundedGradient(C_PURPLE_A, C_PURPLE_B, dp(act, 10), 0, 0));
            LinearLayout.LayoutParams uLp = lpMatch(dp(act, 10));
            uLp.height = dp(act, 48);
            updateBtn.setLayoutParams(uLp);
            card.addView(updateBtn);

            TextView laterBtn = new TextView(act);
            laterBtn.setText("Later");
            laterBtn.setTextColor(C_SUBTITLE);
            laterBtn.setTextSize(14f);
            laterBtn.setGravity(Gravity.CENTER);
            laterBtn.setBackground(solidRounded(C_BG, dp(act, 10), C_JOIN_BRD, dp(act, 2)));
            LinearLayout.LayoutParams lLp = lpMatch(0);
            lLp.height = dp(act, 44);
            laterBtn.setLayoutParams(lLp);
            card.addView(laterBtn);

            FrameLayout root = new FrameLayout(act);
            root.setPadding(dp(act, 16), dp(act, 16), dp(act, 16), dp(act, 16));
            root.addView(card);

            final AlertDialog dlg = new AlertDialog.Builder(act)
                    .setView(root)
                    .setCancelable(false)
                    .create();

            Window w = dlg.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            }

            updateBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(updateLink));
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        act.startActivity(i);
                    } catch (Throwable t) {}
                }
            });

            laterBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { dlg.dismiss(); } catch (Throwable ignored) {}
                }
            });

            dlg.show();
        } catch (Throwable t) {}
    }
}
