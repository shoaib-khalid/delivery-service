/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider;

import com.kalsym.deliveryservice.models.daos.DeliveryOrder;

/**
 *
 * @author user
 */
public class QueryOrderResult {
    public int providerId;
    public DeliveryOrder orderFound;
    public boolean isSuccess;
}
