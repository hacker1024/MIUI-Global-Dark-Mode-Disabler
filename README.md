# ​<img src="https://github.com/hacker1024/MIUI-Global-Dark-Mode-Disabler/raw/master/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="64"> MIUI Global Dark Mode Disabler
Since MIUI 12, MIUI enables a system flag to forcibly "convert" light app resources and assets to darker colors.
This causes graphical issue in several apps, which is why this is just a developer option in AOSP.

Unfortunately, it seems Xiaomi don't care about third-party apps, other than the ones they've whitelisted to disable by default inside MIUI's code.

Despite the setting being modifiable with an ADB command, MIUI resets the setting on boot - hence the need for this Xposed module.

**This module stops MIUI forcing nonsupporting third-party apps to display darker colors.**

**For MIUI 12 and Android 10 only.**

Take a look at the source code if you want to know what MIUI code's hooked to achieve this - It's fully documented.

As this was a fairly simple mod, I've chosen Java over Kotlin for reduced app sizes and performance overhead.

Enjoy!