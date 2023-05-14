package com.fongmi.android.tv.utils;
import android.text.TextUtils;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.net.Callback;
public class SourceUrlUtil {
    public static Callback getUrlCallback(ConfigCallback configCallback) {
        return new Callback() {
            public void success(String url) {
                if (!TextUtils.isEmpty((url))){
                    App.post(() -> {
                        Notify.show("获取成功：url=" + url);
                        configCallback.setConfig(Config.find(url, 0));
                    });
                } else {
                    Notify.show(R.string.error_empty);
                }
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
            }
        };
    }
}
