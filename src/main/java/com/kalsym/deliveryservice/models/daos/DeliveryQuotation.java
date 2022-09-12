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
    private Long id;
    private String itemType;
    //    private Integer spId;
    private String customerId;
    private String type;
    private String productCode;
    private String pickupAddress;
    private String deliveryAddress;
    private String systemTransactionId;
    private String pickupContactName;
    private String pickupContactPhone;
    private String deliveryContactName;
    private String deliveryContactPhone;
    private Integer deliveryProviderId;
    private String spOrderId;
    private String spOrderName;
    private String vehicleType;
    private Date createdDate;
    private String status;
    private String cartId;
    private String statusDescription;
    private Date updatedDate;
    private Double totalWeightKg;
    private Double amount;
    private Date validationPeriod;
    private String storeId;
    private String orderId;
    private String pickupPostcode;
    private String deliveryPostcode;
    private Double serviceFee;
    private String pickupCity;
    private String deliveryCity;
    private String pickupZone;
    private String deliveryZone;
    private String fulfillmentType;
    private String pickupTime;
    private Integer intervalTime;
    private String signature;
    private String pickupLatitude;
    private String pickupLongitude;
    private String deliveryLatitude;
    private String deliveryLongitude;
    private String deliveryContactEmail;
    private Integer totalPieces;
    private String quotationId;
    private String pickupStopId;
    private String deliveryStopId;
    private boolean isCombinedDelivery;
}
