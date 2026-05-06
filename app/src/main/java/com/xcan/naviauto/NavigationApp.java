package com.xcan.naviauto;

enum NavigationApp {
    TMAP("T-Map", "com.skt.tmap.ku"),
    KAKAO_NAVI("KakaoNavi", "com.locnall.KimGiSa"),
    NAVER_MAP("Naver Map", "com.nhn.android.nmap"),
    GOOGLE_MAPS("Google Maps", "com.google.android.apps.maps");

    final String label;
    final String packageName;

    NavigationApp(String label, String packageName) {
        this.label = label;
        this.packageName = packageName;
    }

    @Override
    public String toString() {
        return label;
    }
}
