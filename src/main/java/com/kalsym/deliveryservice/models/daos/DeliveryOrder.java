/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 *
 * @author user
 */
@Entity
@Table(name = "delivery_orders")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class DeliveryOrder {
    
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    Long id;
    String customerId;
    String productCode;
    String pickupAddress;
    String deliveryAddress;
    String systemTransactionId;
    String itemType;
    String pickupContactName;
    String pickupContactPhone;
    String deliveryContactName;
    String deliveryContactPhone;
    Integer deliveryProviderId;
    String spOrderId;
    String spOrderName;
    String vehicleType;
    String createdDate;
    String status;
    String systemStatus;
    String statusDescription;
    String updatedDate;
    String orderId;
    String storeId;
    Double totalWeightKg;
    String merchantTrackingUrl;
    String customerTrackingUrl;
    String driverId;
    String riderName;
    String riderPhoneNo;
    String riderCarPlateNo;
    String airwayBillURL;

    Long totalRequest;
    Long deliveryQuotationId;

    BigDecimal priorityFee;
    BigDecimal deliveryFee;
}
