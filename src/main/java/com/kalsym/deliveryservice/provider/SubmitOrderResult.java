/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import lombok.ToString;

/**
 * @author user
 */
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitOrderResult {
    @JsonProperty("deliveryProviderId")
    public int deliveryProviderId;
    @JsonProperty("orderCreated")
    public DeliveryOrder orderCreated;
    @JsonProperty("isSuccess")
    public boolean isSuccess;
    @JsonProperty("message")
    public String message;
    @JsonProperty("status")
    public String status;
    @JsonProperty("systemTransactionId")
    public String systemTransactionId;
    @JsonProperty("orderId")
    public String orderId;
    @JsonIgnore
    @JsonProperty("resultCode")
    public int resultCode;
    @JsonIgnore
    @JsonProperty("customerTrackingUrl")
    public String customerTrackingUrl;
    @JsonIgnore
    @JsonProperty("spTransactionId")
    public String spTransactionId;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
