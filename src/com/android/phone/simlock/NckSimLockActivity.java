package com.android.phone.simlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.os.PowerManager;
import android.text.method.PasswordTransformationMethod;
import android.text.TextUtils;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.util.Log;

import com.android.phone.R;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.GsmCdmaPhone;
import com.android.internal.telephony.PhoneFactory;
import com.android.sprd.telephony.RadioInteractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class NckSimLockActivity extends Activity {
    private static final String TAG = "NckSimLockActivity";

    private static final int EVENT_QUERY_FACILITY_LOCK_DONE = 100;
    private static final int EVENT_SET_FACILITY_LOCK_DONE = 200;
    private static final int EVENT_SET_FACILITY_UNLOCK_DONE = 300;
    private static final int EVENT_REBOOT = 400;

    private static final int MODE_DEFAULT = 0;
    private static final int MODE_UNLOCK = 1;
    private static final int MODE_RECOVERY = 2;
    private static final int MODE_LOCK = 3;
    private int mDefaultMode = MODE_DEFAULT;

    private static final String DEFAULT_FACILITY = "PN";
    private static final int DEFAULT_SERVICE_CLASS = 7;

    private Phone mPhone;
    private Context mContext;
    private RadioInteractor mRadioInteractor;
    private MyHandler mHandler = new MyHandler();
    private List mOpList;
    private boolean mIsLocked;

    private ListView mListView;
    private TextView mTextView;
    private TextView mTxtConfirm;
    private TextView mTxtRemainTimesMsg;
    private TextView mTxtRemainTimesValue;
    private EditText mEditPwd;
    private EditText mEditPwdConfirm;
    private Button mBtnOK;
    private Button mBtnCancel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.nck_simlock_listview);

        mPhone = PhoneFactory.getPhone(0);
        mContext = mPhone.getContext();
        mRadioInteractor = new RadioInteractor(this);
        updateMode(MODE_DEFAULT);

        initiViews();
        updateViews(MODE_DEFAULT);

        queryFacilityLock();
    }

    private void initiViews() {
        String[] opArray= getResources().getStringArray(R.array.nck_simlock_item_array);
        mOpList= Arrays.asList(opArray);

        SimLockAdapter adapter = new SimLockAdapter(this, mOpList);
        mListView = (ListView)findViewById(R.id.nck_simlock_listview);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new OnItemClickListenerImpl());

        mTextView = (TextView)findViewById(R.id.text_simlock_indicator);
        mEditPwd = (EditText)findViewById(R.id.edt_password);
        mEditPwd.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditPwd.setTransformationMethod(PasswordTransformationMethod.getInstance());

        mTxtConfirm = (TextView)findViewById(R.id.text_simlock_confirm_indicator);
        mEditPwdConfirm = (EditText)findViewById(R.id.edt_password_confirm);
        mEditPwdConfirm.setInputType(InputType.TYPE_CLASS_NUMBER);
        mEditPwdConfirm.setTransformationMethod(PasswordTransformationMethod.getInstance());

        mTxtRemainTimesMsg = (TextView)findViewById(R.id.text_simlock_remaintimes_indicator);
        mTxtRemainTimesValue = (TextView)findViewById(R.id.text_simlock_remaintimes_value);

        mBtnOK = (Button)findViewById(R.id.btn_unlock);
        mBtnOK.setOnClickListener(mLockUnLockListener);

        mBtnCancel = (Button)findViewById(R.id.btn_cancel);
        mBtnCancel.setOnClickListener(mCancelListener);
    }

    private void updateMode(int mode) {
        mDefaultMode = mode;
        Log.d(TAG, "mode updated: " + mDefaultMode);
    }

    private int getMode() {
        return mDefaultMode;
    }

    private void updateViews(int mode) {
        if (mode == MODE_RECOVERY) {
            mBtnOK.setEnabled(false);
            mBtnCancel.setEnabled(false);
        } else if (mode == MODE_UNLOCK) {
            mTextView.setVisibility(View.VISIBLE);
            mEditPwd.setVisibility(View.VISIBLE);
            mBtnOK.setVisibility(View.VISIBLE);
            mBtnCancel.setVisibility(View.VISIBLE);
            mTxtRemainTimesMsg.setVisibility(View.VISIBLE);
            mTxtRemainTimesValue.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mTxtConfirm.setVisibility(View.GONE);
            mEditPwdConfirm.setVisibility(View.GONE);

            if (getRemainTimes() == 0) {
                mBtnOK.setEnabled(false);
                mBtnCancel.setEnabled(false);
            }

            mTxtRemainTimesValue.setText(String.valueOf(getRemainTimes()));
        } else if (mode == MODE_LOCK) {
            mTextView.setVisibility(View.VISIBLE);
            mEditPwd.setVisibility(View.VISIBLE);
            mTxtConfirm.setVisibility(View.VISIBLE);
            mEditPwdConfirm.setVisibility(View.VISIBLE);
            mBtnOK.setVisibility(View.VISIBLE);
            mBtnCancel.setVisibility(View.VISIBLE);

            mListView.setVisibility(View.GONE);
            mTxtRemainTimesMsg.setVisibility(View.GONE);
            mTxtRemainTimesValue.setVisibility(View.GONE);

        } else if(mode == MODE_DEFAULT) {
            mListView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
            mEditPwd.setVisibility(View.GONE);
            mTxtConfirm.setVisibility(View.GONE);
            mEditPwdConfirm.setVisibility(View.GONE);
            mBtnOK.setVisibility(View.GONE);
            mBtnCancel.setVisibility(View.GONE);
            mTxtRemainTimesMsg.setVisibility(View.GONE);
            mTxtRemainTimesValue.setVisibility(View.GONE);
        }
    }

    public View.OnClickListener mLockUnLockListener = new View.OnClickListener() {
        public void onClick(View v) {
            verifyNck();
        }
    };

    public View.OnClickListener mCancelListener = new View.OnClickListener() {
        public void onClick(View v) {
            mEditPwd.setText("");
            mEditPwdConfirm.setText("");
            mEditPwd.requestFocus();
        }
    };

    private class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            switch (msg.what) {
            case EVENT_QUERY_FACILITY_LOCK_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar.exception != null) {
                    Log.d(TAG, "EVENT_QUERY_FACILITY_LOCK_DONE: ar.exception=" + ar.exception);
                } else {
                    int infoArray[] = (int[]) ar.result;
                    mIsLocked = infoArray[0] == 1;
                    Log.d(TAG, "mIsLocked=" + mIsLocked);
                }
                break;
            case EVENT_SET_FACILITY_LOCK_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception != null) {
                    Log.d(TAG, "EVENT_SET_FACILITY_LOCK_DONE: ar.exception=" + ar.exception);
                } else {
                    sendMessageDelayed(obtainMessage(EVENT_REBOOT), 300);
                }
                break;
            case EVENT_SET_FACILITY_UNLOCK_DONE:
                ar = (AsyncResult) msg.obj;
                if (ar != null && ar.exception != null) {
                    Log.d(TAG, "EVENT_SET_FACILITY_UNLOCK_DONE: ar.exception=" + ar.exception);
                } else {
                    sendMessageDelayed(obtainMessage(EVENT_REBOOT), 300);
                }
                break;
            case EVENT_REBOOT:
                PowerManager pm = (PowerManager)mContext.getSystemService(
                        Context.POWER_SERVICE);
                pm.reboot("SIMLOCK Reboot");
                break;
            default:
                break;
            }
        }
    };

    private class OnItemClickListenerImpl implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int simlockType, long id) {
            if (id == 0 && !mIsLocked){
                updateMode(MODE_LOCK);
                updateViews(MODE_LOCK);
            } else if (id == 1 && mIsLocked) {
                updateMode(MODE_UNLOCK);
                updateViews(MODE_UNLOCK);
            } else {
                Log.d(TAG, "Invalid item clicked!");
            }
        }
    }

    private void queryFacilityLock() {
        // Password is not necessary for querying, set a default value 00000000.
        ((GsmCdmaPhone)mPhone).mCi.queryFacilityLock(DEFAULT_FACILITY,"00000000", DEFAULT_SERVICE_CLASS,
                mHandler.obtainMessage(EVENT_QUERY_FACILITY_LOCK_DONE));
    }

    private void doLock() {
        mRadioInteractor.setFacilityLockByUser(
                "PN", true, mHandler.obtainMessage(EVENT_SET_FACILITY_LOCK_DONE), 0);
    }

    private void doUnLock() {
        mRadioInteractor.setFacilityLockByUser(
                "PN", false, mHandler.obtainMessage(EVENT_SET_FACILITY_LOCK_DONE), 0);
    }

    private int getRemainTimes() {
        return NckSimLockUtil.getRemainTimes(mContext);
    }

    private void verifyNck() {
        String nckCode = NckSimLockUtil.getNckCode(mContext);
        String input = mEditPwd.getText().toString();
        if (getMode() == MODE_UNLOCK) {
            if (!TextUtils.isEmpty(input)) {
                if(input.equals(nckCode)) {
                    showUnlockAlert(true, R.string.msg_unlock_successful);
                } else {
                    showUnlockAlert(false, R.string.msg_unlock_unsuccessful);
                }
            } else {
                showUnlockAlert(false, R.string.alert_passwork_invalid);
            }
        } else if (getMode() == MODE_LOCK) {
            String inputConfirm = mEditPwdConfirm.getText().toString();
            if (!TextUtils.isEmpty(input) && !TextUtils.isEmpty(inputConfirm)) {
                if (input.equals(inputConfirm)) {
                    if (input.equals(nckCode)) {
                        showLockAlert(true, R.string.msg_lock_sucessful);
                    } else {
                        showLockAlert(false, R.string.msg_lock_unsuccessful);
                    }
                } else {
                    showLockAlert(false, R.string.alert_password_not_match);
                }
            } else {
                showLockAlert(false, R.string.alert_passwork_invalid);
            }
        }
    }

    private void showUnlockAlert(boolean success, int msgId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NckSimLockActivity.this);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(getString(R.string.simlock_dialog_title));
        builder.setMessage(getString(msgId));
        builder.setPositiveButton(getString(R.string.simlock_button_ok), (dialog, which) -> {
            if (success) {
                NckSimLockUtil.resetSimLockRemainTimes(mContext, false);
                doUnLock();
            } else {
                mEditPwd.setText("");

                if (msgId != R.string.alert_passwork_invalid) {
                    int remainTimes = getRemainTimes() -1;
                    mTxtRemainTimesValue.setText(String.valueOf(remainTimes));
                    NckSimLockUtil.saveRemainTimes(mContext, remainTimes);

                    if (remainTimes == 0) {
                        updateMode(MODE_RECOVERY);
                        mBtnCancel.setEnabled(false);
                        mBtnOK.setEnabled(false);
                    }
                }
            }
        }).create();
        builder.show();
    }

    private void showLockAlert(boolean success, int msgId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NckSimLockActivity.this);
        builder.setIconAttribute(android.R.attr.alertDialogIcon);
        builder.setTitle(getString(R.string.simlock_dialog_title));
        builder.setMessage(getString(msgId));
        builder.setPositiveButton(getString(R.string.simlock_button_ok), (dialog, which) -> {
            if (success) {
                doLock();
            } else {
                mEditPwd.setText("");
                mEditPwdConfirm.setText("");
                mEditPwd.requestFocus();
            }
        }).create();
        builder.show();
    }

    public class SimLockAdapter extends BaseAdapter {
        private LayoutInflater mInflater = null;
        private Context mContext;
        private List mList;
        public SimLockAdapter(Context context, List list) {
            this.mContext = context;
            this.mList = list;
            this.mInflater = LayoutInflater.from(context);
        }

        public class ViewHolder {
            TextView mTextView;
        }

        @Override
        public int getCount() {
            return (mList != null) ? mList.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            if (mList != null && position >= 0 && position < mList.size()) {
                return mList.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convert, ViewGroup parent) {
            ViewHolder holder;
            if (convert == null) {
                convert = mInflater.inflate(R.layout.nck_simlock_listitem, null);
                holder = new ViewHolder();
                holder.mTextView = (TextView) convert.findViewById(R.id.nck_simlock_listitem);
                convert.setTag(holder);
            } else {
                holder = (ViewHolder) convert.getTag();
            }

            if (position == 0) {
                holder.mTextView.setEnabled(!mIsLocked);
                holder.mTextView.setText(R.string.simlock_lock);
            } else if (position ==1) {
                holder.mTextView.setEnabled(mIsLocked);
                holder.mTextView.setText(R.string.simlock_unlock);
            }
            return convert;
        }
    }
}
