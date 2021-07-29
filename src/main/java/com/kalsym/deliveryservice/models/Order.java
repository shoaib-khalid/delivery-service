/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.models;

import com.kalsym.deliveryservice.models.enums.*;

import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author user
 */
@Getter
@Setter
public class Order {
   String customerId;
   Integer merchantId;
   String productCode;
   Integer deliveryProviderId;
   
   ItemType itemType;           
   
   Double totalWeightKg;
   String transactionId;
   String shipmentContent;
   Integer pieces;
   boolean isInsurance;
   Double shipmentValue;
   String storeId;
   String cartId;
   Pickup pickup;
   Delivery delivery;
}
