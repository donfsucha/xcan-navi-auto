package com.xcan.naviauto;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import com.kakao.sdk.common.KakaoSdk;
import com.kakao.sdk.navi.Constants;
import com.kakao.sdk.navi.NaviClient;
import com.kakao.sdk.navi.model.CoordType;
import com.kakao.sdk.navi.model.Location;
import com.kakao.sdk.navi.model.NaviOption;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class NavigationLauncher {
    private static boolean kakaoInitialized = false;

    boolean previewAddress(Context context, NavigationApp navigationApp, Place destination) {
        if (navigationApp == NavigationApp.NAVER_MAP && destination != null && destination.hasCoordinates()) {
            return launchNaverPlace(context, destination);
        }
        if (destination != null && destination.hasCoordinates()) {
            if (launchGeoPlacePreview(context, destination)) {
                return true;
            }
        }
        if (destination != null && destination.hasAddress()) {
            if (launchGeoSearchPreview(context, destination.address)) {
                return true;
            }
        }
        return launchPackageOnly(context, navigationApp);
    }

    boolean launch(Context context, NavigationApp navigationApp, Place destination) {
        if (navigationApp == NavigationApp.NAVER_MAP) {
            if (launchNaverMap(context, destination)) {
                return true;
            }
        }
        if (navigationApp == NavigationApp.KAKAO_NAVI) {
            if (launchKakaoNavi(context, destination)) {
                return true;
            }
            return false;
        }

        if (destination != null && destination.hasCoordinates()) {
            if (launchGeoPlace(context, navigationApp, destination)) {
                return true;
            }
        }

        if (destination != null && destination.hasAddress()) {
            if (launchGeoSearch(context, navigationApp, destination.address)) {
                return true;
            }
        }

        return launchPackageOnly(context, navigationApp);
    }

    private boolean launchKakaoNavi(Context context, Place destination) {
        if (destination == null || !destination.hasCoordinates()) {
            return launchPackageOnly(context, NavigationApp.KAKAO_NAVI);
        }
        if (BuildConfig.KAKAO_NATIVE_APP_KEY == null || BuildConfig.KAKAO_NATIVE_APP_KEY.trim().isEmpty()) {
            return false;
        }

        ensureKakaoInitialized(context);

        if (!NaviClient.getInstance().isKakaoNaviInstalled(context)) {
            Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.WEB_NAVI_INSTALL));
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                context.startActivity(installIntent);
                return false;
            } catch (ActivityNotFoundException | SecurityException e) {
                AppInstallHelper.openPlayStore(context, NavigationApp.KAKAO_NAVI.packageName);
                return false;
            }
        }

        Location kakaoDestination = new Location(
                destination.displayName(),
                formatCoordinate(destination.longitude),
                formatCoordinate(destination.latitude)
        );
        NaviOption option = new NaviOption(CoordType.WGS84, null, null, null, null, null, null, null);
        Intent intent = NaviClient.getInstance().navigateIntent(kakaoDestination, option, Collections.emptyList());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private void ensureKakaoInitialized(Context context) {
        if (!kakaoInitialized) {
            KakaoSdk.init(context.getApplicationContext(), BuildConfig.KAKAO_NATIVE_APP_KEY);
            kakaoInitialized = true;
        }
    }

    private boolean launchNaverMap(Context context, Place destination) {
        String url;
        if (destination != null && destination.hasCoordinates()) {
            url = "nmap://navigation?dlat=" + destination.latitude
                    + "&dlng=" + destination.longitude
                    + "&dname=" + encode(destination.displayName())
                    + "&appname=" + context.getPackageName();
        } else if (destination != null && destination.hasAddress()) {
            url = "nmap://search?query=" + encode(destination.address) + "&appname=" + context.getPackageName();
        } else {
            url = "nmap://map?&appname=" + context.getPackageName();
        }
        return startNaverIntent(context, Uri.parse(url));
    }

    private boolean launchNaverPlace(Context context, Place destination) {
        String url = "nmap://place?lat=" + destination.latitude
                + "&lng=" + destination.longitude
                + "&name=" + encode(destination.displayName())
                + "&appname=" + context.getPackageName();
        return startNaverIntent(context, Uri.parse(url));
    }

    private boolean launchGeoSearch(Context context, NavigationApp navigationApp, String address) {
        Uri uri = Uri.parse("geo:0,0?q=" + encode(address));
        return startViewIntent(context, uri, navigationApp.packageName);
    }

    private boolean launchGeoPlace(Context context, NavigationApp navigationApp, Place destination) {
        Uri uri = Uri.parse("geo:" + destination.latitude + "," + destination.longitude
                + "?q=" + destination.latitude + "," + destination.longitude
                + "(" + encode(destination.displayName()) + ")");
        return startViewIntent(context, uri, navigationApp.packageName);
    }

    private boolean launchGeoSearchPreview(Context context, String address) {
        Uri uri = Uri.parse("geo:0,0?q=" + encode(address));
        return startViewIntent(context, uri, null);
    }

    private boolean launchGeoPlacePreview(Context context, Place destination) {
        Uri uri = Uri.parse("geo:" + destination.latitude + "," + destination.longitude
                + "?q=" + destination.latitude + "," + destination.longitude
                + "(" + encode(destination.displayName()) + ")");
        return startViewIntent(context, uri, null);
    }

    private boolean launchPackageOnly(Context context, NavigationApp navigationApp) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(navigationApp.packageName);
        if (intent == null) {
            AppInstallHelper.openPlayStore(context, navigationApp.packageName);
            return false;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private boolean startNaverIntent(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        List<ResolveInfo> handlers = context.getPackageManager()
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (handlers == null || handlers.isEmpty()) {
            AppInstallHelper.openPlayStore(context, NavigationApp.NAVER_MAP.packageName);
            return false;
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return launchPackageOnly(context, NavigationApp.NAVER_MAP);
        }
    }

    private boolean startViewIntent(Context context, Uri uri, String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    private String formatCoordinate(double coordinate) {
        return String.format(Locale.US, "%.7f", coordinate);
    }
}
