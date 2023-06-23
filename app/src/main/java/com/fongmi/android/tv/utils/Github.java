package com.fongmi.android.tv.utils;

import android.text.TextUtils;
import android.util.Log;

import com.fongmi.android.tv.Constant;
import com.github.catvod.net.OkHttp;

import java.io.IOException;

import okhttp3.OkHttpClient;

public class Github {

    public static final String A = "https://raw.githubusercontent.com/";
    public static final String B = "https://gh.api.99988866.xyz/";
    public static final String C = "https://ghproxy.com/";
    public static final String REPO = "LyOuYang/TV/";
    public static final String RELEASE = "release";
    public static final String DEV = "dev";

    private final OkHttpClient client;
    private String proxy;

    private static class Loader {
        static volatile Github INSTANCE = new Github();
    }

    public static Github get() {
        return Loader.INSTANCE;
    }

    public Github() {
        client = OkHttp.client(Constant.TIMEOUT_GITHUB);
        if (check(C) && check(B)) {
            Log.i("github", "proxy set failed");
        }
    }

    private boolean check(String url) {
        try {
            if (getProxy().length() > 0) return true;
            int code = OkHttp.newCall(client, url).execute().code();
            if (code == 200) setProxy(url);
            return true;
        } catch (IOException ignored) {
            Log.e("GITHUB", String.format("check: [%s] error.", url));
        }
        return false;
    }

    private void setProxy(String url) {
        this.proxy = url.equals(C) ? url + A + REPO : url + REPO;
    }

    private String getProxy() {
        return TextUtils.isEmpty(proxy) ? "" : proxy;
    }

    public String getReleasePath(String path) {
        return getProxy() + RELEASE + path;
    }

    public String getBranchPath(String branch, String path) {
        return getProxy() + branch + path;
    }
}
