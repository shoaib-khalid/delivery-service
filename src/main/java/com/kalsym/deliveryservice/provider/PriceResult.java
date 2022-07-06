/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.models.daos.DeliveryPeriod;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class PriceResult {
    public Long refId;
    public int providerId;
    public BigDecimal price;
    public String validUpTo;
    public String message;
    public boolean isError;
    public String providerName;
    public String deliveryType;
    public String providerImage;
    public String vehicleType;
    public String pickupDateTime;
    public DeliveryPeriod deliveryPeriod;
    public String quotationId;
    public String pickupStopId;
    public String deliveryStopId;


    @JsonIgnore
    public int resultCode;
    public String fulfillment;
    public BigDecimal priorityFee;
    //only for pakistan
    public String pickupCity;
    public String deliveryCity;
    public String pickupZone;
    public String deliveryZone;
    public Integer interval;
    public String signature;

    public BigDecimal lat;
    public BigDecimal log;

}
