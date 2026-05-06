package com.xcan.naviauto;

final class Place {
    final String name;
    final String address;
    final Double latitude;
    final Double longitude;

    Place(String name, String address) {
        this(name, address, null, null);
    }

    Place(String name, String address, Double latitude, Double longitude) {
        this.name = name;
        this.address = address == null ? "" : address.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    boolean hasAddress() {
        return !address.isEmpty();
    }

    boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    String displayName() {
        return hasAddress() ? address : name;
    }
}
