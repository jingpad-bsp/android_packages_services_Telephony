package com.android.phone.simlock;

import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.android.phone.R;

public class NckSimLockUtil {

    private static final String TAG = "NckSimLockUtil";

    public static final String NCK_SIMLOCK_REMAIN_TIMES = "NCK_SIMLOCK_REMAIN_TIMES";
    public static final int NCK_SIMLOCK_REMAIN_TIMES_MAX = 5;

    public static void saveRemainTimes(Context context, int value) {
        Settings.System.putInt(context.getContentResolver(), NCK_SIMLOCK_REMAIN_TIMES, value);
        Log.d(TAG, "saveRemainTimes: " + value);
    }

    public static int getRemainTimes(Context context) {
        try{
            int remainTimes = Settings.System.getInt(context.getContentResolver(), NCK_SIMLOCK_REMAIN_TIMES);
            Log.d(TAG, "getRemainTimes: " + remainTimes);
            return remainTimes;
        } catch (SettingNotFoundException ex) {
            Log.d(TAG, "getRemainTimes: -1");
            return -1;
        }
    }

    public static void resetSimLockRemainTimes(Context context, boolean showToast) {
        String msg = "";
        if (NCK_SIMLOCK_REMAIN_TIMES_MAX
                == getRemainTimes(context)) {
            msg = context.getString(R.string.reset_noneed);
        } else {
            saveRemainTimes(context, NCK_SIMLOCK_REMAIN_TIMES_MAX);
            msg = context.getString(R.string.reset_remain_times_successful);
        }
        if (showToast) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
        return;
    }

    public static String getNckCode(Context context) {
        String nckCode = "";
        String imeiStr= TelephonyManager.from(context).getImei(0);
        if (imeiStr.length() < 15) {
            Log.d(TAG, "Invalid IMEI " + imeiStr);
            return "";
        }
        try {
            long imeiLong = Long.valueOf(imeiStr).longValue();
            long tmpLong = imeiLong * 20 + Long.valueOf(imeiStr.substring(imeiStr.length()-8)).longValue();
            String tempStr = String.valueOf(tmpLong);
            if (tempStr.length() < 16) {
                return "";
            }
            nckCode = tempStr.substring(tempStr.length()-16);
            Log.d(TAG, "getNckCode: " + nckCode);
        } catch (NumberFormatException e) {
            Log.d(TAG, "getNckCode exception: " + e);
        }
        return nckCode;
    }

    public static boolean isNckSimLockEnabled(Context context) {
        Resources res = context.getResources();
        return res != null
                && res.getBoolean(com.android.internal.R.bool.config_operator_nck_simlock);
    }
}
