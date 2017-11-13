package github.tornaco.xposedmoduletest.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import java.util.List;

import github.tornaco.xposedmoduletest.R;
import github.tornaco.xposedmoduletest.bean.BlockRecord;
import github.tornaco.xposedmoduletest.loader.BlockRecordLoader;
import github.tornaco.xposedmoduletest.ui.adapter.BlockRecordListAdapter;
import github.tornaco.xposedmoduletest.util.XExecutor;

public class BlockRecordViewerActivity extends WithRecyclerView {

    private static final String EXTRA_PKG = "target_pkg";

    private SwipeRefreshLayout swipeRefreshLayout;

    protected BlockRecordListAdapter lockKillAppListAdapter;

    private String mTargetPkgName;

    public static void start(Context context, String pkg) {
        Intent intent = new Intent(context, BlockRecordViewerActivity.class);
        intent.putExtra(EXTRA_PKG, pkg);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
        showHomeAsUp();
        initView();
        startLoading();
    }

    protected int getLayoutRes() {
        return R.layout.block_record_viewer;
    }

    @Override
    public void onResume() {
        super.onResume();
        startLoading();
    }

    protected void initView() {
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);
        swipeRefreshLayout.setColorSchemeColors(getResources().getIntArray(R.array.polluted_waves));

        lockKillAppListAdapter = onCreateAdapter();
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(lockKillAppListAdapter);


        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        startLoading();
                    }
                });
    }


    protected BlockRecordListAdapter onCreateAdapter() {
        return new BlockRecordListAdapter(this);
    }

    protected void startLoading() {
        // Reslove intent.
        Intent intent = getIntent();
        mTargetPkgName = intent.getStringExtra(EXTRA_PKG);

        swipeRefreshLayout.setRefreshing(true);
        XExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final List<BlockRecord> res = performLoading();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        lockKillAppListAdapter.update(res);
                    }
                });
            }
        });
    }

    protected List<BlockRecord> performLoading() {
        return BlockRecordLoader.Impl.create(this).loadAll(mTargetPkgName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
