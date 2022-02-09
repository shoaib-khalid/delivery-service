package com.kalsym.deliveryservice.models.enums;

public enum DeliveryTypeRemarks {
    DROPSHIP("DROPSHIP", "Please drop your shipment to nearest Logistic."),
    PICKUP("PICKUP","Delivery Partner Will Contact You");


    private String name;
    private String value;

    DeliveryTypeRemarks(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
