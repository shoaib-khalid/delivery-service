/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.models.enums;

/**
 * @author user
 */
public enum VehicleType {
    MOTORCYCLE(1),
    CAR(2),
    VAN(3),
    TRUCK(4);

    final public int number;

    VehicleType(final int number) {
        this.number = number;
    }
    }
