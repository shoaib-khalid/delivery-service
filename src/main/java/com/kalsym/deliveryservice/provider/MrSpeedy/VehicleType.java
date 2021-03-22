/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author user
 */
public enum VehicleType 
{
    MOTORCYCLE(8), 
    CAR(7);
    
    private final int code;
    
    private static Map<Integer, VehicleType> map = new HashMap<Integer, VehicleType>();
    
    VehicleType(int code) {
        this.code = code;
    }
 
    public int getCode() {
        return code;
    }
    
    static {
        for (VehicleType legEnum : VehicleType.values()) {
            map.put(legEnum.code, legEnum);
        }
    }
    
    public static VehicleType valueOf(int legNo) {
        return map.get(legNo);
    }
}
