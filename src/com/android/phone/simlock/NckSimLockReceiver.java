package com.android.phone.simlock;

import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.net.Uri;
import android.os.SystemProperties;
import android.util.Log;

public class NckSimLockReceiver extends BroadcastReceiver {
    private static final String TAG = "NckSimLockReceiver";
    private static final String SECRECT_CODE_LOCK_AND_UNLOCK = "34113411";
    private static final String SECRECT_CODE_RESET_REMAINING_TIMES = "34123412";

    public NckSimLockReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String host = null;
        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }
        host = uri.getHost();
        Log.d(TAG, " host[" + host + "]");

        if (!NckSimLockUtil.isNckSimLockEnabled(context)) {
            return;
        }

        if (SECRECT_CODE_LOCK_AND_UNLOCK.equals(host)) {
            handleSimLock(context);
        } else if (SECRECT_CODE_RESET_REMAINING_TIMES.equals(host)) {
            handleResetRemainingTries(context);
        } else {
            Log.d(TAG, "Unhandle host[" + host + "]");
        }
    }

    private void handleSimLock(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, NckSimLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private void handleResetRemainingTries(Context context) {
        NckSimLockUtil.resetSimLockRemainTimes(context, true);
    }
}
