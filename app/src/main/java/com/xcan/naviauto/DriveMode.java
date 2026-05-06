package com.xcan.naviauto;

enum DriveMode {
    GO_TO_WORK("출근 모드", "회사"),
    GO_HOME("퇴근 모드", "집"),
    WEEKEND("주말 모드", "목적지 없음"),
    MANUAL("기본 실행 모드", "목적지 없음");

    final String label;
    final String destinationLabel;

    DriveMode(String label, String destinationLabel) {
        this.label = label;
        this.destinationLabel = destinationLabel;
    }
}
