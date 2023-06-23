package com.fongmi.android.tv.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.widget.EditText;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class VerifyDialogUtil {
    private final static int ENCRYPTION_KEY = 1996;

    private final static String DEVICE_CODE_KEY = "deviceCode";
    private final static String ENTRY_CODE_KEY = "entryCode";

    public static void showInputDialog(Activity activity) {
        /*@setView 装入一个EditView
         */
        if (!TextUtils.isEmpty(Prefers.getString(ENTRY_CODE_KEY))) {
            return;
        }

        final String deviceCode;
        if (TextUtils.isEmpty(Prefers.getString(DEVICE_CODE_KEY))) {
            deviceCode = String.format(Locale.CHINA, "%04d", new Random().nextInt(9999));
            Prefers.put(DEVICE_CODE_KEY, deviceCode);
        } else {
            deviceCode = Prefers.getString(DEVICE_CODE_KEY);
        }

        final EditText editText = new EditText(activity);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(activity);
        inputDialog.setCancelable(false);
        editText.setHint("请输入四位接入码");
        inputDialog.setTitle("你的设备码为" + deviceCode + ",请输入接入码").setView(editText);
        inputDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    String inputEntryCode = editText.getText().toString();
                    int checkCode = entryCodeCheck(deviceCode, inputEntryCode);
                    switch (checkCode) {
                        case 0:
                            Prefers.put(ENTRY_CODE_KEY, inputEntryCode);
                            return;
                        case 1:
                            Notify.show("接入码格式不正确");
                            break;
                        case 2:
                            Notify.show("接入码格错误");
                        default:
                            Notify.show("未知错误");
                            break;
                    }
                    activity.finish();
                }).show();
    }

    private static int entryCodeCheck(String deviceCode, String inputEntryCode) {
        if (!Pattern.matches("^\\d{4}$", deviceCode)) {
            return 1;
        }

        if (!inputEntryCode.equals(getEntryCode(deviceCode))) {
            return 2;
        }

        return 0;
    }

    private static String getEntryCode(String deviceCode) {
        int entryCode = Integer.parseInt(deviceCode) * ENCRYPTION_KEY % 10000;
        return String.format(Locale.CHINA, "%04d", entryCode);
    }
}
