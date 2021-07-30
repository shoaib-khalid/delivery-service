/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

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
}
