package com.example.commuteauto;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

final class MusicLauncher {
    boolean launch(Context context, MusicApp musicApp) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(musicApp.packageName);
        if (intent == null) {
            AppInstallHelper.openPlayStore(context, musicApp.packageName);
            return false;
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return true;
    }
}
