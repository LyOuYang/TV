package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.OnChildViewHolderSelectedListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.Constant;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.ApiConfig;
import com.fongmi.android.tv.bean.Collect;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivityCollectBinding;
import com.fongmi.android.tv.model.SiteViewModel;
import com.fongmi.android.tv.ui.fragment.CollectFragment;
import com.fongmi.android.tv.ui.presenter.CollectPresenter;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PausableThreadPoolExecutor;
import com.fongmi.android.tv.utils.ResUtil;

import org.chromium.base.CollectionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CollectActivity extends BaseActivity {

    private ActivityCollectBinding mBinding;
    private ArrayObjectAdapter mAdapter;
    private PausableThreadPoolExecutor mExecutor;
    private SiteViewModel mViewModel;
    private PageAdapter mPageAdapter;
    private List<Site> mSites;
    private View mOldView;

    public static void start(Activity activity, String keyword) {
        start(activity, keyword, false);
    }

    public static void start(Activity activity, String keyword, boolean clear) {
        Intent intent = new Intent(activity, CollectActivity.class);
        if (clear) intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("keyword", keyword);
        activity.startActivityForResult(intent, 1000);
    }

    private String getKeyword() {
        return getIntent().getStringExtra("keyword");
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCollectBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void initView() {
        setRecyclerView();
        setViewModel();
        setPager();
        setSite();
        search();
    }

    @Override
    protected void initEvent() {
        mBinding.pager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mBinding.recycler.setSelectedPosition(position);
            }
        });
        mBinding.recycler.addOnChildViewHolderSelectedListener(new OnChildViewHolderSelectedListener() {
            @Override
            public void onChildViewHolderSelected(@NonNull RecyclerView parent, @Nullable RecyclerView.ViewHolder child, int position, int subposition) {
                mBinding.pager.setCurrentItem(position);
                onChildSelected(child);
            }
        });
    }

    private void setRecyclerView() {
        mBinding.recycler.setHorizontalSpacing(ResUtil.dp2px(16));
        mBinding.recycler.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mBinding.recycler.setAdapter(new ItemBridgeAdapter(mAdapter = new ArrayObjectAdapter(new CollectPresenter())));
    }

    private void setViewModel() {
        mViewModel = new ViewModelProvider(this).get(SiteViewModel.class);
        mViewModel.search.observe(this, result -> {
            mAdapter.add(Collect.create(result.getList()));
            getFragment().addVideo(result.getList());
            mPageAdapter.notifyDataSetChanged();
        });
    }

    private void setPager() {
        mBinding.pager.setAdapter(mPageAdapter = new PageAdapter(getSupportFragmentManager()));
    }

    private void setSite() {
        mSites = new ArrayList<>();
        for (Site site : ApiConfig.get().getSites()) if (site.isSearchable()) mSites.add(site);
        Site home = ApiConfig.get().getHome();
        if (!mSites.contains(home)) return;
        mSites.remove(home);
        mSites.add(0, home);
    }

    private void search() {
        mAdapter.add(Collect.all());
        mPageAdapter.notifyDataSetChanged();
        int core = Runtime.getRuntime().availableProcessors();
        int corePoolSize = Math.max(Constant.THREAD_POOL, core);
        mExecutor = new PausableThreadPoolExecutor(corePoolSize, corePoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(100));
        mBinding.result.setText(getString(R.string.collect_result, getKeyword()));
        if (mViewModel.effectiveCount == 0) Notify.progress(this);
        App.post(() -> {
            if (mViewModel.effectiveCount == 0) {
                Notify.dismiss();
                Notify.show("未搜索到资源");
            }
        }, 40000L);

        for (Site site : mSites) mExecutor.execute(() -> search(site));
    }

    private void search(Site site) {
        try {
            mViewModel.searchContent(site, getKeyword());
        } catch (Throwable ignored) {
        }
    }

    private void onChildSelected(@Nullable RecyclerView.ViewHolder child) {
        if (mOldView != null) mOldView.setActivated(false);
        if (child == null) return;
        mOldView = child.itemView;
        mOldView.setActivated(true);
    }

    private CollectFragment getFragment() {
        return (CollectFragment) mPageAdapter.instantiateItem(mBinding.pager, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mExecutor == null) return;
        mExecutor.shutdownNow();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mExecutor == null) return;
        mExecutor.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mExecutor == null) return;
        mExecutor.resume();
    }

    class PageAdapter extends FragmentStatePagerAdapter {

        public PageAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mAdapter.size();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return CollectFragment.newInstance(((Collect) mAdapter.get(position)).getList());
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }
    }
}
