package tk.hacker1024.miuiglobaldarkmodedisabler;

import android.content.ContentResolver;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Module implements IXposedHookLoadPackage {
    // This setting key corresponds to the standard Android 10 dark them option.
    static final String KEY_DARK_MODE_ENABLE = "dark_mode_enable";

    // This setting key appears to effect the status bar icons in mysterious ways.
    static final String KEY_SMART_DARK_ENABLE = "smart_dark_enable";

    // This prop corresponds to the force dark mode developer option in AOSP. Xiaomi turn it on with the normal dark theme switch.
    static final String PROP_FORCE_DARK = "debug.hwui.force_dark";

    // These are some methods to grab things we need to pass to the functions to change settings.
    // The methods used are hidden in the Android SDK, so we get them from the system with the help of the Xposed API.
    Class<?> getSystemSetttings(ClassLoader classLoader) {
        return XposedHelpers.findClass("android.provider.Settings$System", classLoader);
    }

    Class<?> getSystemProperties(ClassLoader classLoader) {
        return XposedHelpers.findClass("android.os.SystemProperties", classLoader);
    }

    ContentResolver getContentResolver (Object thisObject) {
        return ((Context) XposedHelpers.callMethod(thisObject,"getContext")).getContentResolver();
    }

    // The next methods turn off the necessary settings and props for our fix.
    void disableSmartDark(Class<?> systemSettings, ContentResolver contentResolver, int userId) {
        XposedHelpers.callStaticMethod(systemSettings, "putIntForUser", contentResolver, KEY_SMART_DARK_ENABLE, 0, userId);
    }

    void disableForceDark(Class<?> systemProperties) {
        XposedHelpers.callStaticMethod(systemProperties, "set", PROP_FORCE_DARK, "false");
    }

    // Time to hook!
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        // We're hooking an android system service, in the com.android.server package. This lies in the "android" package in the system.
        if (!lpparam.packageName.equals("android")) return;

        // We first hook the "setDarkProp" method, which sets all the settings. We execute code directly afterwards to unset
        // necessary settings.
        // For those interested, the method's code lies in com.android.server.UiModeManagerService, in /system/framework/services.jar.
        XposedHelpers.findAndHookMethod(
                "com.android.server.UiModeManagerService",
                lpparam.classLoader,
                "setDarkProp",
                int.class, // The dark mode setting to set
                int.class, // The user id to set the settings for
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        // Unset the settings.
                        // param.args[0] is the dark mode setting, which we ignore.
                        // param.args[1] is the user ID, which we keep.
                        disableSmartDark(getSystemSetttings(lpparam.classLoader), getContentResolver(param.thisObject), (int) param.args[1]);
                        disableForceDark(getSystemProperties(lpparam.classLoader));
                    }
                }
        );

        // We then hook the "setForceDark" method, which appears to be called when booting.
        // This is the reason why ADB commands aren't enough to permanently disable forced dark mode.
        // We stop this method doing anything to allow changes to the prop to persist.
        // For those interested, the method's code also lies in com.android.server.UiModeManagerService, in /system/framework/services.jar.
        XposedHelpers.findAndHookMethod(
                "com.android.server.UiModeManagerService",
                lpparam.classLoader,
                "setForceDark",
                Context.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return null; // This is a void method, the null won't be used.
                    }
                }
        );

        // Below is an alternative (and untested) implementation of this module's goal.
        // This method is hooked to always report that an app's specific force dark mode setting is off.
        //
        // I decided on the hooks above instead of using this because while the above hooks
        // execute only during boot and when a setting's changes, this hook would execute every time a view's inflated
        // (or at least whenever an app's opened; I'm not entirely sure how optimized MIUI's dark mode is in this respect).
        // Furthermore, the above hooks stop the hooked method here even being called at all.
        //
        // For those interested, the method's code also lies in com.miui.server.SecurityManagerService, in /system/framework/services.jar.
//        XposedHelpers.findAndHookMethod(
//                "com.miui.server.SecurityManagerService",
//                lpparam.classLoader,
//                "getAppDarkModeForUser",
//                String.class, // The package name
//                int.class, // The user ID
//                new XC_MethodReplacement() {
//                    @Override
//                    protected Object replaceHookedMethod(MethodHookParam param) {
//                        return false; // No matter the current settings, report that force dark mode is disable for this app.
//                    }
//                }
//        );
    }
}
