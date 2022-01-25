/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;

/**
 *
 * @author user
 */
public class CancelOrderResult {
    @JsonIgnore
    public int resultCode;
    public int providerId;
    public DeliveryOrder orderCancelled;
    public boolean isSuccess;
}
