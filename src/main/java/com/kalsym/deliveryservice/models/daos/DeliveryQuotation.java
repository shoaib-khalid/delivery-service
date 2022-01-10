package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "delivery_quotation")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DeliveryQuotation {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String itemType;
    Long spId;
    String customerId;
    String productCode;
    String pickupAddress;
    String deliveryAddress;
    String systemTransactionId;
    String pickupContactName;
    String pickupContactPhone;
    String deliveryContactName;
    String deliveryContactPhone;
    Integer deliveryProviderId;
    String spOrderId;
    String spOrderName;
    String vehicleType;
    Date createdDate;
    String status;
    String cartId;
    String statusDescription;
    Date updatedDate;
    Double totalWeightKg;
    Double amount;
    Date validationPeriod;
    String storeId;
    String orderId;
    String pickupPostcode;
    String deliveryPostcode;
    Double serviceFee;

    String pickupCity;
    String deliveryCity;
    String pickupZone;
    String deliveryZone;

}
