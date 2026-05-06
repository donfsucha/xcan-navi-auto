package com.example.commuteauto;

final class DestinationEntry {
    final String name;
    final String address;
    final Double latitude;
    final Double longitude;

    DestinationEntry(String name, String address) {
        this(name, address, null, null);
    }

    DestinationEntry(String name, String address, Double latitude, Double longitude) {
        this.name = name == null ? "" : name.trim();
        this.address = address == null ? "" : address.trim();
        this.latitude = latitude;
        this.longitude = longitude;
    }

    boolean hasName() {
        return !name.isEmpty();
    }

    boolean hasAddress() {
        return !address.isEmpty();
    }

    boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    Place asPlace() {
        return new Place(hasName() ? name : "목적지", address, latitude, longitude);
    }
}
