/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author user
 */
@Entity
@Table(name = "delivery_orders")
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @JsonProperty("spOrderId")
    String spOrderId;
    String spOrderName;
    String vehicleType;
    String createdDate;
    @JsonProperty("status")
    String status;
    @JsonProperty("systemStatus")
    String systemStatus;
    String statusDescription;
    String updatedDate;
    String orderId;
    String storeId;
    Double totalWeightKg;
    String merchantTrackingUrl;
    @JsonProperty("customerTrackingUrl")
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

    BigDecimal codAmount;

    public DeliveryOrder() {}
    public DeliveryOrder(Object data) {}

    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
