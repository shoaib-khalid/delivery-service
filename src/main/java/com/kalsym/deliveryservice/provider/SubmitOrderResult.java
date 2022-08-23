/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import lombok.ToString;

/**
 * @author user
 */
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitOrderResult {
    public int deliveryProviderId;
    public DeliveryOrder orderCreated;
    public boolean isSuccess;
    public String message;
    public String status;
    public String systemTransactionId;
    public String orderId;
    @JsonIgnore
    public int resultCode;
    @JsonIgnore
    public String customerTrackingUrl;
    @JsonIgnore
    public String spTransactionId;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
