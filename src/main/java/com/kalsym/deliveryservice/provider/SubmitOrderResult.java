/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;

/**
 *
 * @author user
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitOrderResult {
    @JsonIgnore
    public int resultCode;
    public int deliveryProviderId;
    public DeliveryOrder orderCreated;
    public boolean isSuccess;
    public String message;
    public String status;
    public String customerTrackingUrl;
    public String spTransactionId;
    public String systemTransactionId;
    public String orderId;

}
