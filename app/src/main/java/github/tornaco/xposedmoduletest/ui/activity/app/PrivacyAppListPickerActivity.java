package github.tornaco.xposedmoduletest.ui.activity.app;

import android.content.Context;
import android.content.Intent;

import java.util.List;

import github.tornaco.android.common.Collections;
import github.tornaco.android.common.Consumer;
import github.tornaco.xposedmoduletest.loader.PrivacyPackageLoader;
import github.tornaco.xposedmoduletest.model.CommonPackageInfo;
import github.tornaco.xposedmoduletest.ui.activity.common.CommonPackageInfoListPickerActivity;
import github.tornaco.xposedmoduletest.ui.adapter.common.CommonPackageInfoAdapter;
import github.tornaco.xposedmoduletest.ui.adapter.common.CommonPackageInfoPickerAdapter;
import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;

public class PrivacyAppListPickerActivity extends CommonPackageInfoListPickerActivity {

    public static void start(Context context) {
        Intent starter = new Intent(context, PrivacyAppListPickerActivity.class);
        context.startActivity(starter);
    }

    @Override
    protected void doOnFabClickInWorkThread() {
        Collections.consumeRemaining(getCommonPackageInfoAdapter().getCommonPackageInfos(),
                new Consumer<CommonPackageInfo>() {
                    @Override
                    public void accept(CommonPackageInfo commonPackageInfo) {
                        if (commonPackageInfo.isChecked()) {
                            XAshmanManager.get().addOrRemoveFromPrivacyList(
                                    commonPackageInfo.getPkgName(),
                                    XAshmanManager.Op.ADD);
                        }
                    }
                });
    }

    @Override
    protected CommonPackageInfoAdapter onCreateAdapter() {
        return new CommonPackageInfoPickerAdapter(this);
    }

    @Override
    protected List<CommonPackageInfo> performLoading() {
        return PrivacyPackageLoader.Impl.create(this).loadInstalled(false);
    }
}
