/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.models;

import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import java.util.List;

/**
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
    String orderId;
    String shipmentContent;
    Integer pieces;
    boolean isInsurance;
    Double shipmentValue;
    String storeId;
    String cartId;
    Pickup pickup;
    @Valid
    Delivery delivery;
    String regionCountry;
    Boolean serviceType;
    VehicleType vehicleType;
    String deliveryPeriod;
    Integer interval;
    //   String deliveryService;
    String remarks;
    String deliveryType;

}
