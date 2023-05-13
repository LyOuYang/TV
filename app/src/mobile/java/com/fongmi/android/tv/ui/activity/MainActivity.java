package com.fongmi.android.tv.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.Updater;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.databinding.ActivityMainBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.ui.custom.FragmentStateManager;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.google.android.material.navigation.NavigationBarView;

import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;

public class MainActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener {

    private FragmentStateManager mManager;
    private ActivityMainBinding mBinding;
    private boolean confirm;

    private final static int ENCRYPTION_KEY = 1996;

    private final static String DEVICE_CODE_KEY = "deviceCode";
    private final static String ENTRY_CODE_KEY = "entryCode";

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        showInputDialog();
        initFragment(savedInstanceState);
        Updater.get().start();
        Server.get().start();
        initConfig();
    }

    private void showInputDialog() {
        /*@setView 装入一个EditView
         */
        if (!TextUtils.isEmpty(Prefers.getString(ENTRY_CODE_KEY))) {
            return;
        }

        final String deviceCode;
        if (TextUtils.isEmpty(Prefers.getString(DEVICE_CODE_KEY))) {
            deviceCode = String.format(Locale.CHINA, "%04d",new Random().nextInt(9999));
            Prefers.put(DEVICE_CODE_KEY, deviceCode);
        } else {
            deviceCode = Prefers.getString(DEVICE_CODE_KEY);
        }

        final EditText editText = new EditText(this);
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setCancelable(false);
        editText.setHint("请输入四位接入码");
        inputDialog.setTitle("你的设备码为"+deviceCode+",请输入接入码").setView(editText);
        inputDialog.setPositiveButton("确定",
                (dialog, which) -> entryCodeCheck(deviceCode, editText.getText().toString())).show();
    }

    private void entryCodeCheck(String deviceCode, String inputEntryCode) {
        if (!Pattern.matches("^\\d{4}$", deviceCode)) {
            Notify.show("接入码格式不正确");
            finish();
            return;
        }

        if (!inputEntryCode.equals(getEntryCode(deviceCode))) {
            Notify.show("接入码格错误");
            finish();
            return;
        }

        Prefers.put(ENTRY_CODE_KEY, inputEntryCode);
    }

    private String getEntryCode(String deviceCode) {
        int entryCode = Integer.parseInt(deviceCode) * ENCRYPTION_KEY % 10000;
        return String.format(Locale.CHINA, "%04d",entryCode);
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.setOnItemSelectedListener(this);
    }

    private void checkAction(Intent intent) {
        boolean push = ApiConfig.hasPush() && intent.getAction() != null;
        if (push && intent.getAction().equals(Intent.ACTION_SEND) && intent.getType().equals("text/plain")) {
            DetailActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (push && intent.getAction().equals(Intent.ACTION_VIEW)) {
            DetailActivity.file(this, FileChooser.getPathFromUri(this, intent.getData()));
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return position == 0 ? VodFragment.newInstance() : SettingFragment.newInstance();
            }
        };
        if (savedInstanceState == null) mManager.change(0);
    }

    private void initConfig() {
        WallConfig.get().init();
        LiveConfig.get().init();
        ApiConfig.get().init().load(getCallback());
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                checkAction(getIntent());
                RefreshEvent.config();
                RefreshEvent.video();
            }

            @Override
            public void error(int resId) {
                RefreshEvent.config();
                RefreshEvent.empty();
                Notify.show(resId);
            }
        };
    }

    private void setConfirm() {
        confirm = true;
        Notify.show(R.string.app_exit);
        App.post(() -> confirm = false, 2000);
    }

    @Override
    public void onRefreshEvent(RefreshEvent event) {
        super.onRefreshEvent(event);
        if (!event.getType().equals(RefreshEvent.Type.CONFIG)) return;
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (mBinding.navigation.getSelectedItemId() == item.getItemId()) return false;
        if (item.getItemId() == R.id.vod) return mManager.change(0);
        if (item.getItemId() == R.id.setting) return mManager.change(1);
        return false;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RefreshEvent.video();
    }

    @Override
    public void onBackPressed() {
        if (mManager.isVisible(1)) {
            mBinding.navigation.setSelectedItemId(R.id.vod);
        } else if (mManager.canBack(0)) {
            if (!confirm) setConfirm();
            else finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WallConfig.get().clear();
        LiveConfig.get().clear();
        ApiConfig.get().clear();
        Server.get().stop();
    }
}
