/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.kalsym.deliveryservice.provider.MrSpeedy.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author user
 */
public enum VehicleType 
{
    MOTORCYCLE("Motorbike"), 
    CAR("Car");
    
    private final String code;
    
    private static Map<String, VehicleType> map = new HashMap<String, VehicleType>();
    
    VehicleType(String code) {
        this.code = code;
    }
 
    public String getCode() {
        return code;
    }
    
    static {
        for (VehicleType legEnum : VehicleType.values()) {
            map.put(legEnum.code, legEnum);
        }
    }
    
    
}
