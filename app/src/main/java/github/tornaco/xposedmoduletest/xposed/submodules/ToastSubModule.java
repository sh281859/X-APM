package github.tornaco.xposedmoduletest.xposed.submodules;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import github.tornaco.android.common.util.ApkUtil;
import github.tornaco.xposedmoduletest.util.OSUtil;
import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;
import github.tornaco.xposedmoduletest.xposed.util.PkgUtil;
import github.tornaco.xposedmoduletest.xposed.util.XposedLog;

/**
 * Created by guohao4 on 2017/10/31.
 * Email: Tornaco@163.com
 */

class ToastSubModule extends AndroidSubModule {

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
        hookMakeToast();
        if (OSUtil.isOOrAbove()) {
            hookMakeToastOreoAddon();
        }
    }

    private void hookMakeToastOreoAddon() {
        XposedLog.verbose("hookMakeToastOreoAddon...");
        try {
            Class clz = XposedHelpers.findClass("android.widget.Toast", null);
            @SuppressWarnings("unchecked") Method m
                    = clz.getDeclaredMethod("makeText", Context.class, Looper.class, CharSequence.class, int.class);
            XC_MethodHook.Unhook unHooks = XposedBridge.hookMethod(m,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (isToastCallerEnabled()) {
                                XposedLog.verbose("makeTextOreoAddon: " + Arrays.toString(param.args));
                                String appLabel = String.valueOf(PkgUtil.loadNameByPkgName((Context) param.args[0],
                                        AndroidAppHelper.currentPackageName()));
                                String atAppLebal = "@" + appLabel;
                                if (!param.args[2].toString().contains(atAppLebal)) {
                                    String newText = atAppLebal + "\t" + param.args[2];
                                    param.args[2] = newText;
                                }
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            handleMakeToastIcon(param);
                        }
                    });
            XposedLog.verbose("hookMakeToastOreoAddon OK:" + unHooks);
            setStatus(unhookToStatus(unHooks));
        } catch (Exception e) {
            XposedLog.verbose("Fail hookMakeToastOreoAddon:" + e);
            setStatus(SubModuleStatus.ERROR);
            setErrorMessage(Log.getStackTraceString(e));
        }
    }

    private boolean isToastCallerEnabled() {
        return XAshmanManager.get().isServiceAvailable() && XAshmanManager.get()
                .isOptFeatureEnabled(XAshmanManager.OPT.TOAST.name());
    }

    private boolean isToastCallerIconEnabled() {
        return XAshmanManager.get().isServiceAvailable() && XAshmanManager.get()
                .isOptFeatureEnabled(XAshmanManager.OPT.TOAST_ICON.name());
    }

    private void hookMakeToast() {
        XposedLog.verbose("hookMakeToast...");
        try {
            Class clz = XposedHelpers.findClass("android.widget.Toast", null);
            @SuppressWarnings("unchecked") Method m = clz.getDeclaredMethod("makeText",
                    Context.class, CharSequence.class, int.class);
            XC_MethodHook.Unhook unHooks = XposedBridge.hookMethod(m,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (isToastCallerEnabled()) {
                                XposedLog.verbose("makeText: " + Arrays.toString(param.args));
                                String appLabel = String.valueOf(PkgUtil.loadNameByPkgName((Context) param.args[0],
                                        AndroidAppHelper.currentPackageName()));
                                String newText = "@" + appLabel + "\t" + param.args[1];
                                param.args[1] = newText;
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            handleMakeToastIcon(param);
                        }
                    });
            XposedLog.verbose("hookMakeToast OK:" + unHooks);
            setStatus(unhookToStatus(unHooks));
        } catch (Exception e) {
            XposedLog.verbose("Fail hookMakeToast:" + e);
            setStatus(SubModuleStatus.ERROR);
            setErrorMessage(Log.getStackTraceString(e));
        }
    }

    private void handleMakeToastIcon(XC_MethodHook.MethodHookParam param) {
        if (isToastCallerIconEnabled()) {
            try {
                Toast t = (Toast) param.getResult();
                ViewGroup v = (ViewGroup) t.getView();
                if (!hasToastIconImageView(v)) {
                    ImageView iconView = new ImageView(v.getContext());
                    iconView.setTag(ICON_VIEW_TAG);
                    TextView tv = v.findViewById(com.android.internal.R.id.message);
                    Drawable d = ApkUtil.loadIconByPkgName(v.getContext(), AndroidAppHelper.currentPackageName());
                    iconView.setImageDrawable(d);
                    int textSize = (int) (tv.getTextSize() * 1.5);
                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(textSize, textSize);
                    v.addView(iconView, params);
                    XposedLog.verbose("handleMakeToastIcon add icon: " + Arrays.toString(param.args));
                }
            } catch (Throwable e) {
                XposedLog.wtf("Fail handleMakeToastIcon add icon: " + Log.getStackTraceString(e));
            }
        }
    }

    private static final String ICON_VIEW_TAG = "APM-TOAST-ICON";

    private static boolean hasToastIconImageView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View c = viewGroup.getChildAt(i);
            if (c instanceof ImageView && ICON_VIEW_TAG.equals(c.getTag())) {
                return true;
            }
        }
        return false;
    }
}
