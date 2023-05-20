package com.fongmi.android.tv.utils;

import android.view.View;

import com.fongmi.android.tv.App;

public class ViewUtil {
    public static void delayClick(View view) {
        view.setEnabled(false);
        App.post(() -> view.setEnabled(true), 3000);
    }
}
