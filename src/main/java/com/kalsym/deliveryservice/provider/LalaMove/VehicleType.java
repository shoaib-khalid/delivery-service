package com.kalsym.deliveryservice.provider.LalaMove;

import java.util.HashMap;
import java.util.Map;

public enum VehicleType {
    MOTORCYCLE("MOTORCYCLE"),
    CAR("7"),
    VAN("VAN"),
    TRUCK("TRUCK550"),
    COURIER("WALKER");

    private static Map<String, com.kalsym.deliveryservice.provider.LalaMove.VehicleType> map = new HashMap<String, com.kalsym.deliveryservice.provider.LalaMove.VehicleType>();

    static {
        for (com.kalsym.deliveryservice.provider.LalaMove.VehicleType legEnum : com.kalsym.deliveryservice.provider.LalaMove.VehicleType.values()) {
            map.put(legEnum.code, legEnum);
        }
    }

    private final String code;

    VehicleType(String code) {
        this.code = code;
    }


    public String getCode() {
        return code;
    }
}
