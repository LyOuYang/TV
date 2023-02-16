package com.fongmi.android.tv.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.api.LiveConfig;
import com.fongmi.android.tv.api.Updater;
import com.fongmi.android.tv.api.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Live;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivitySettingBinding;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.impl.ConfigCallback;
import com.fongmi.android.tv.impl.LiveCallback;
import com.fongmi.android.tv.impl.SiteCallback;
import com.fongmi.android.tv.net.Callback;
import com.fongmi.android.tv.ui.custom.dialog.ConfigDialog;
import com.fongmi.android.tv.ui.custom.dialog.HistoryDialog;
import com.fongmi.android.tv.ui.custom.dialog.LiveDialog;
import com.fongmi.android.tv.ui.custom.dialog.SiteDialog;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Prefers;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Utils;
import com.github.bassaer.library.BuildConfig;
import com.google.gson.Gson;
import com.permissionx.guolindev.PermissionX;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SettingActivity extends BaseActivity implements ConfigCallback, SiteCallback, LiveCallback {

    private ActivitySettingBinding mBinding;
    private Config config;

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, SettingActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivitySettingBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        mBinding.vodUrl.setText(ApiConfig.getUrl());
        mBinding.liveUrl.setText(LiveConfig.getUrl());
        mBinding.wallUrl.setText(WallConfig.getUrl());
        mBinding.versionText.setText(BuildConfig.VERSION_NAME);
        mBinding.sizeText.setText(ResUtil.getStringArray(R.array.select_size)[Prefers.getSize()]);
        mBinding.scaleText.setText(ResUtil.getStringArray(R.array.select_scale)[Prefers.getScale()]);
        mBinding.playerText.setText(ResUtil.getStringArray(R.array.select_player)[Prefers.getPlayer()]);
        mBinding.decodeText.setText(ResUtil.getStringArray(R.array.select_decode)[Prefers.getDecode()]);
        mBinding.renderText.setText(ResUtil.getStringArray(R.array.select_render)[Prefers.getRender()]);
        mBinding.qualityText.setText(ResUtil.getStringArray(R.array.select_quality)[Prefers.getQuality()]);
    }

    @Override
    protected void initEvent() {
        mBinding.url.setOnClickListener(view -> {
            delayClick(mBinding.url); Updater.get().force().updateUrl("url", getUrlCallback());
        });
        mBinding.urlBack.setOnClickListener(view -> {
            delayClick(mBinding.urlBack);
            Updater.get().force().updateUrl("url_back", getUrlCallback());
        });
        mBinding.vodHome.setOnClickListener(view -> SiteDialog.create(this).all().show());
        mBinding.liveHome.setOnClickListener(view -> LiveDialog.create(this).show());
        mBinding.vod.setOnClickListener(view -> ConfigDialog.create(this).type(0).show());
        mBinding.live.setOnClickListener(view -> ConfigDialog.create(this).type(1).show());
        mBinding.wall.setOnClickListener(view -> ConfigDialog.create(this).type(2).show());
        mBinding.vodHistory.setOnClickListener(view -> HistoryDialog.create(this).type(0).show());
        mBinding.liveHistory.setOnClickListener(view -> HistoryDialog.create(this).type(1).show());
        mBinding.version.setOnClickListener(view -> Updater.get().force().start());
        mBinding.wallDefault.setOnClickListener(view -> setWallDefault());
        mBinding.wallRefresh.setOnClickListener(view -> setWallRefresh());
        mBinding.quality.setOnClickListener(view -> setQuality());
        mBinding.player.setOnClickListener(view -> setPlayer());
        mBinding.decode.setOnClickListener(view -> setDecode());
        mBinding.render.setOnClickListener(view -> setRender());
        mBinding.scale.setOnClickListener(view -> setScale());
        mBinding.size.setOnClickListener(view -> setSize());
    }

    private void delayClick(View view) {
        view.setEnabled(false);
        App.post(() -> view.setEnabled(true),3000);
    }

    @Override
    public void setConfig(Config config) {
        this.config = config;
        checkPermission();
    }

    private void checkPermission() {
        if (config.getUrl().startsWith("file") && !Utils.hasPermission(this)) {
            PermissionX.init(this).permissions(Manifest.permission.WRITE_EXTERNAL_STORAGE).request((allGranted, grantedList, deniedList) -> loadConfig());
        } else {
            loadConfig();
        }
    }

    private void loadConfig() {
        switch (config.getType()) {
            case 0:
                Notify.progress(this);
                mBinding.vodUrl.setText(config.getUrl());
                ApiConfig.get().clear().config(config).load(getCallback());
                break;
            case 1:
                Notify.progress(this);
                mBinding.liveUrl.setText(config.getUrl());
                LiveConfig.get().clear().config(config).load(getCallback());
                break;
            case 2:
                mBinding.wallUrl.setText(config.getUrl());
                WallConfig.get().clear().config(config).load(getCallback());
                break;
        }
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                setConfig();
            }

            @Override
            public void error(int resId) {
                Notify.show(resId);
                config.delete();
                setConfig();
            }
        };
    }

    private void setConfig() {
        switch (config.getType()) {
            case 0:
                Notify.dismiss();
                RefreshEvent.video();
                RefreshEvent.history();
                mBinding.liveUrl.setText(LiveConfig.getUrl());
                mBinding.wallUrl.setText(WallConfig.getUrl());
                break;
            case 1:
                Notify.dismiss();
                break;
            case 2:
                mBinding.wallUrl.setText(WallConfig.getUrl());
                break;
        }
    }

    @Override
    public void setSite(Site item) {
        ApiConfig.get().setHome(item);
        RefreshEvent.video();
    }

    @Override
    public void setLive(Live item) {
        LiveConfig.get().setHome(item);
    }

    private void setQuality() {
        int index = Prefers.getQuality();
        String[] array = ResUtil.getStringArray(R.array.select_quality);
        Prefers.putQuality(index = index == array.length - 1 ? 0 : ++index);
        mBinding.qualityText.setText(array[index]);
        RefreshEvent.image();
    }

    private void setPlayer() {
        int index = Prefers.getPlayer();
        String[] array = ResUtil.getStringArray(R.array.select_player);
        Prefers.putPlayer(index = index == array.length - 1 ? 0 : ++index);
        mBinding.playerText.setText(array[index]);
    }

    private void setDecode() {
        int index = Prefers.getDecode();
        String[] array = ResUtil.getStringArray(R.array.select_decode);
        Prefers.putDecode(index = index == array.length - 1 ? 0 : ++index);
        mBinding.decodeText.setText(array[index]);
    }

    private void setRender() {
        int index = Prefers.getRender();
        String[] array = ResUtil.getStringArray(R.array.select_render);
        Prefers.putRender(index = index == array.length - 1 ? 0 : ++index);
        mBinding.renderText.setText(array[index]);
    }

    private void setScale() {
        int index = Prefers.getScale();
        String[] array = ResUtil.getStringArray(R.array.select_scale);
        Prefers.putScale(index = index == array.length - 1 ? 0 : ++index);
        mBinding.scaleText.setText(array[index]);
    }

    private void setSize() {
        int index = Prefers.getSize();
        String[] array = ResUtil.getStringArray(R.array.select_size);
        Prefers.putSize(index = index == array.length - 1 ? 0 : ++index);
        mBinding.sizeText.setText(array[index]);
        RefreshEvent.size();
    }

    private void setWallDefault() {
        WallConfig.refresh(Prefers.getWall() == 4 ? 1 : Prefers.getWall() + 1);
    }

    private void setWallRefresh() {
        WallConfig.get().load();
    }

    private Callback getUrlCallback() {
        return new Callback() {
            @Override
            public void success(Object url) {
                if (url instanceof String && !TextUtils.isEmpty(((String) url))){
                    App.post(() -> {
                        Notify.show("获取成功：url=" + url);
                        setConfig(Config.find((String) url, 0));
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
