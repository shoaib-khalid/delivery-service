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
   Integer customerId;
   Integer merchantId;
   String productCode;
   Integer deliveryProviderId;
   
   ItemType itemType;           
   
   Integer totalWeightKg;
   String transactionId;
   String shipmentContent;
   Integer pieces;
   boolean isInsurance;
   Double shipmentValue;
   
   Pickup pickup;
   Delivery delivery;
}
