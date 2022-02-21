/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * @author user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    public String deliveryPeriod;
    public String pickupDateTime;

    @JsonIgnore
    public int resultCode;
    public BigDecimal priorityFee;
    //only for pakistan
    public String pickupCity;
    public String deliveryCity;
    public String pickupZone;
    public String deliveryZone;
}
