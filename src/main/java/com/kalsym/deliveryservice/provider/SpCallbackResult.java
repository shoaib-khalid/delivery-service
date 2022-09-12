/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import lombok.ToString;

/**
 * @author user
 */
public class SpCallbackResult {
    public int providerId;
    public String spOrderId;
    public String status;
    public String driverId;
    public String riderName;
    public String riderPhone;
    public String systemStatus;
    public String trackingUrl;
    public String driveNoPlate;
    public int resultCode;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
