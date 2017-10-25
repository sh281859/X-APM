package github.tornaco.xposedmoduletest.x;

import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import github.tornaco.apigen.GithubCommitSha;
import github.tornaco.xposedmoduletest.BuildConfig;

/**
 * Created by guohao4 on 2017/10/19.
 * Email: Tornaco@163.com
 */
@GithubCommitSha
class XModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {

    XStatus xStatus = XStatus.UNKNOWN;

    XAppGuardService mAppGuardService = new XAppGuardService();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if ("android".equals(lpparam.packageName)) {
            onLoadingAndroid(lpparam);
        }
    }

    void onLoadingAndroid(XC_LoadPackage.LoadPackageParam lpparam) {
        xStatus = XStatus.GOOD;
        hookAMSStart(lpparam);
        hookSystemServiceRegister(lpparam);
        hookAMSSystemReady(lpparam);
        hookAMSShutdown(lpparam);
        hookFPService(lpparam);
        hookScreenshotApplications(lpparam);
    }

    private void hookAMSShutdown(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookAMSShutdown...");
        try {
            Class ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(ams, "shutdown", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    mAppGuardService.shutdown();
                }
            });
            XLog.logV("hookAMSShutdown OK");
        } catch (Exception e) {
            XLog.logV("Fail hookAMSShutdown");
            xStatus = XStatus.ERROR;
        }
    }

    private void hookSystemServiceRegister(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookSystemServiceRegister...");
        XLog.logV("hookSystemServiceRegister OK");
    }

    private void hookAMSSystemReady(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookAMSSystemReady...");
        try {
            Class ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(ams, "systemReady", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    mAppGuardService.systemReady();
                    mAppGuardService.setStatus(xStatus);
                }
            });
            XLog.logV("hookAMSSystemReady OK");
        } catch (Exception e) {
            XLog.logV("Fail hookAMSSystemReady");
            xStatus = XStatus.ERROR;
        }
    }

    private void hookAMSStart(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookAMSStart...");
        try {
            Class ams = XposedHelpers.findClass("com.android.server.am.ActivityManagerService",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(ams, "start", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                    mAppGuardService.attachContext(context);
                    mAppGuardService.publish();
                }
            });
            XLog.logV("hookAMSStart OK");
        } catch (Exception e) {
            XLog.logV("Fail hook hookAMSStart");
            xStatus = XStatus.ERROR;
        }
    }

    // http://androidxref.com/7.0.0_r1/xref/frameworks/base/services/core/java/com/android/server/fingerprint/FingerprintService.java
    // http://androidxref.com/6.0.1_r10/xref/frameworks/base/services/core/java/com/android/server/fingerprint/FingerprintService.java
    private void hookFPService(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookFPService...");
        try {
            XposedBridge.hookAllMethods(
                    XposedHelpers.findClass("com.android.server.fingerprint.FingerprintService",
                            lpparam.classLoader),
                    "canUseFingerprint", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            Object pkg = param.args[0];
                            if (BuildConfig.APPLICATION_ID.equals(pkg)) {
                                param.setResult(true);
                                XLog.logV("ALLOWING APPGUARD TO USE FP ANYWAY");
                            }
                        }
                    });
            XLog.logV("hookFPService OK");
            mAppGuardService.publishFeature(XAppGuardManager.Feature.FP);
        } catch (Exception e) {
            XLog.logV("Fail hookFPService" + e);
            if (xStatus != XStatus.ERROR) xStatus = XStatus.WITH_WARN;
        }
    }

    /**
     * Not work, we can not receive any call through this.
     */
    @Deprecated
    private void hookTaskRecordSetLastThumbnail(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookTaskRecordSetLastThumbnail...");
        try {
            Class clz = XposedHelpers.findClass("com.android.server.am.TaskRecord",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(clz, "setLastThumbnail", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    // Retrieve package name first.
                    Object me = param.thisObject;
                    // FIXME Using aff instead of PKG.
                    final String affinity = (String) XposedHelpers.getObjectField(me, "affinity");
                    final int effectiveUid = (int) XposedHelpers.getObjectField(me, "effectiveUid");
                    XLog.logV("affinity:" + affinity + ", effectiveUid:" + effectiveUid);
                    String pkgName = affinity;
                    if (mAppGuardService.isBlurForPkg(pkgName)
                            && param.getResult() != null) {

                        Bitmap res = (Bitmap) param.args[0];
                        XLog.logV("Blur bitmap start");
                        param.args[0] = (XBitmapUtil.createBlurredBitmap(res));
                        XLog.logV("Blur bitmap end");
                    }
                }
            });
            XLog.logV("hookTaskRecordSetLastThumbnail OK");
        } catch (Exception e) {
            XLog.logV("Fail hookTaskRecordSetLastThumbnail:" + e);
        }
    }

    /**
     * @see #onScreenshotApplications(XC_MethodHook.MethodHookParam)
     */
    @Deprecated
    private void hookScreenshotApplications(XC_LoadPackage.LoadPackageParam lpparam) {
        XLog.logV("hookScreenshotApplications...");
        try {
            Class clz = XposedHelpers.findClass("com.android.server.wm.WindowManagerService",
                    lpparam.classLoader);
            XposedBridge.hookAllMethods(clz,
                    "screenshotApplications", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            super.afterHookedMethod(param);
                            try {
                                onScreenshotApplications(param);
                            } catch (Exception e) {
                                XLog.logV("Fail onScreenshotApplications:" + e);
                            }
                        }
                    });
            XLog.logV("hookScreenshotApplications OK");
            mAppGuardService.publishFeature(XAppGuardManager.Feature.BLUR);
        } catch (Exception e) {
            XLog.logV("Fail hookScreenshotApplications:" + e);
        }
    }

    /**
     * @deprecated We can not get package name using this hook.
     */
    @Deprecated
    private void onScreenshotApplications(XC_MethodHook.MethodHookParam param) throws RemoteException {
        IBinder token = (IBinder) param.args[0];
        ComponentName activityClassForToken = ActivityManagerNative.getDefault().getActivityClassForToken(token);
        XLog.logV("screenshotApplications, activityClassForToken:" + activityClassForToken);

        String pkgName = activityClassForToken == null ? null : activityClassForToken.getPackageName();
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        if (mAppGuardService.isBlurForPkg(pkgName)
                && param.getResult() != null) {

            Bitmap res = (Bitmap) param.getResult();
            XLog.logV("Blur bitmap start");
            param.setResult(XBitmapUtil.createBlurredBitmap(res));
            XLog.logV("Blur bitmap end");
        }
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        XLog.logV("initZygote...");
    }

    boolean isLauncherIntent(Intent intent) {
        return intent != null
                && intent.getCategories() != null
                && intent.getCategories().contains("android.intent.category.HOME");
    }
}