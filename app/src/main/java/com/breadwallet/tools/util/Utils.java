package com.breadwallet.tools.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.entities.PartnerNames;
import com.breadwallet.tools.manager.AnalyticsManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import timber.log.Timber;
import org.json.*;
import java.io.FileReader;
import  java.io.InputStream;

import static android.content.Context.FINGERPRINT_SERVICE;
import java.io.FileNotFoundException;
import android.content.res.AssetManager;
public class Utils {

    public static boolean isUsingCustomInputMethod(Activity context) {
        if (context == null) return false;
        InputMethodManager imm = (InputMethodManager) context.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm == null) {
            return false;
        }
        List<InputMethodInfo> mInputMethodProperties = imm.getEnabledInputMethodList();
        final int N = mInputMethodProperties.size();
        for (int i = 0; i < N; i++) {
            InputMethodInfo imi = mInputMethodProperties.get(i);
            if (imi.getId().equals(
                    Settings.Secure.getString(context.getContentResolver(),
                            Settings.Secure.DEFAULT_INPUT_METHOD))) {
                if ((imi.getServiceInfo().applicationInfo.flags &
                        ApplicationInfo.FLAG_SYSTEM) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static void printPhoneSpecs() {
        Timber.d("timber: ***************************PHONE SPECS***************************");
        Timber.d("timber: * screen X: %d , screen Y: %s", IntroActivity.screenParametersPoint.x, IntroActivity.screenParametersPoint.y);
        Timber.d("timber: * Build.CPU_ABI: %s", Build.CPU_ABI);
        Timber.d("timber: * maxMemory:%s", Runtime.getRuntime().maxMemory());
        Timber.d("timber: ----------------------------PHONE SPECS----------------------------");
    }

    public static boolean isEmulatorOrDebug(Context app) {
        String fing = Build.FINGERPRINT;
        boolean isEmulator = false;
        if (fing != null) {
            isEmulator = fing.contains("vbox") || fing.contains("generic");
        }
        return isEmulator || (0 != (app.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE));
    }

    public static String getFormattedDateFromLong(Context app, long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("M/d@ha", Locale.getDefault());
        boolean is24HoursFormat = false;
        if (app != null) {
            is24HoursFormat = android.text.format.DateFormat.is24HourFormat(app.getApplicationContext());
            if (is24HoursFormat) {
                formatter = new SimpleDateFormat("M/d H", Locale.getDefault());
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        String result = formatter.format(calendar.getTime()).toLowerCase().replace("am", "a").replace("pm", "p");
        if (is24HoursFormat) result += "h";
        return result;
    }

    public static String formatTimeStamp(long time, String pattern) {
        return android.text.format.DateFormat.format(pattern, time).toString();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrEmpty(byte[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static int getPixelsFromDps(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String createBitcoinUrl(String address, long satoshiAmount, String label, String message, String rURL) {

        Uri.Builder builder = new Uri.Builder();
        builder = builder.scheme("litecoin");
        if (address != null && !address.isEmpty())
            builder = builder.appendPath(address);
        if (satoshiAmount != 0)
            builder = builder.appendQueryParameter("amount", new BigDecimal(satoshiAmount).divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toPlainString());
        if (label != null && !label.isEmpty())
            builder = builder.appendQueryParameter("label", label);
        if (message != null && !message.isEmpty())
            builder = builder.appendQueryParameter("message", message);
        if (rURL != null && !rURL.isEmpty())
            builder = builder.appendQueryParameter("r", rURL);

        return builder.build().toString().replaceFirst("/", "");

    }

    public static boolean isFingerprintEnrolled(Context app) {
        FingerprintManager fingerprintManager = (FingerprintManager) app.getSystemService(FINGERPRINT_SERVICE);
        if (fingerprintManager == null) return false;
        // Device doesn't support fingerprint authentication
        return ActivityCompat.checkSelfPermission(app, Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
    }

    public static boolean isFingerprintAvailable(Context app) {
        FingerprintManager fingerprintManager = (FingerprintManager) app.getSystemService(FINGERPRINT_SERVICE);
        if (fingerprintManager == null) return false;
        // Device doesn't support fingerprint authentication
        if (ActivityCompat.checkSelfPermission(app, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(app, "Fingerprint authentication permission not enabled", Toast.LENGTH_LONG).show();
            return false;
        }
        return fingerprintManager.isHardwareDetected();
    }

    public static void hideKeyboard(Context app) {
        if (app != null) {
            View view = ((Activity) app).getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

    }

    public static String getAgentString(Context app, String cfnetwork) {
        int versionNumber = 0;
        if (app != null) {
            try {
                PackageInfo pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                versionNumber = pInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                Timber.e(e);
            }
        }
        return String.format(Locale.ENGLISH, "%s/%d %s Android/%s", "Litewallet", versionNumber, cfnetwork, Build.VERSION.RELEASE);
    }

    public static String reverseHex(String hex) {
        if (hex == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= hex.length() - 2; i = i + 2) {
            result.append(new StringBuilder(hex.substring(i, i + 2)).reverse());
        }
        return result.reverse().toString();
    }

    public static String join(String[] array, CharSequence separator) {
        if (array.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < array.length - 1; i++) {
            stringBuilder.append(array[i]);
            stringBuilder.append(separator);
        }
        stringBuilder.append(array[array.length - 1]);
        return stringBuilder.toString();
    }
    public static String fetchPartnerKey(Context app, PartnerNames name) {
    JSONObject keyObject;
        AssetManager assetManager = app.getAssets();
        try (InputStream inputStream = assetManager.open("partner-keys.json")) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                keyObject = new JSONObject(sb.toString()).getJSONObject("keys");
                return keyObject.get(name.getKey()).toString();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bundle   params = new Bundle();
        params.putString("error_message: %s Key not found", name.toString());
        AnalyticsManager.logCustomEventWithParams(BRConstants._20200112_ERR,params);
        return "";
    }
}
