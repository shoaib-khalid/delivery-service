/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import lombok.ToString;

/**
 * @author user
 */
public class SpCallbackResult {
    @JsonProperty("providerId")
    public int providerId;
    @JsonProperty("spOrderId")
    public String spOrderId;
    @JsonProperty("status")
    public String status;
    @JsonProperty("driverId")
    public String driverId;
    @JsonProperty("riderName")
    public String riderName;
    @JsonProperty("riderPhone")
    public String riderPhone;
    @JsonProperty("systemStatus")
    public String systemStatus;
    @JsonProperty("trackingUrl")
    public String trackingUrl;
    @JsonProperty("driveNoPlate")
    public String driveNoPlate;
    @JsonProperty("resultCode")
    public int resultCode;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
