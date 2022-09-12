/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;

/**
 * @author user
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryOrderResult {
    public int providerId;
    @JsonProperty("orderFound")
    public DeliveryOrder orderFound;
    @JsonProperty("isSuccess")
    public boolean isSuccess;

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

}
