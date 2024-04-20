/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.kalsym.deliveryservice.models.daos.DeliveryPeriod;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@Setter
@Getter

public class PriceResult {
    @JsonProperty("refId")
    public Long refId;
    @JsonProperty("providerId")
    public int providerId;
    @JsonProperty("price")
    public BigDecimal price;
    @JsonProperty("validUpTo")
    public String validUpTo;
    @JsonProperty("message")
    public String message;
    @JsonProperty("isError")
    public boolean isError;
    @JsonProperty("providerName")
    public String providerName;
    @JsonProperty("deliveryType")
    public String deliveryType;
    @JsonProperty("providerImage")
    public String providerImage;
    @JsonProperty("vehicleType")
    public String vehicleType;
    @JsonProperty("pickupDateTime")
    public String pickupDateTime;
    @JsonProperty("deliveryPeriod")
    public DeliveryPeriod deliveryPeriod;
    @JsonProperty("quotationId")
    public String quotationId;
    @JsonProperty("pickupStopId")
    public String pickupStopId;
    @JsonProperty("deliveryStopId")
    public String deliveryStopId;


    @JsonIgnore
    @JsonProperty("resultCode")
    public int resultCode;
    @JsonProperty("fulfillment")
    public String fulfillment;
    @JsonProperty("priorityFee")
    public BigDecimal priorityFee;
    //only for pakistan
    @JsonProperty("pickupCity")
    public String pickupCity;
    @JsonProperty("deliveryCity")
    public String deliveryCity;
    @JsonProperty("pickupZone")
    public String pickupZone;
    @JsonProperty("deliveryZone")
    public String deliveryZone;
    @JsonProperty("interval")
    public Integer interval;
    @JsonProperty("signature")
    public String signature;

    @JsonProperty("lat")
    public BigDecimal lat;
    @JsonProperty("log")
    public BigDecimal log;

}
