package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.models.enums.ItemType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "delivery_quotation")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    String updatedDate;
    Double totalWeightKg;
    Double amount;
    Date validationPeriod;

    @Override
    public String toString() {
        return "DeliveryQuotation{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", productCode='" + productCode + '\'' +
                ", pickupAddress='" + pickupAddress + '\'' +
                ", deliveryAddress='" + deliveryAddress + '\'' +
                ", systemTransactionId='" + systemTransactionId + '\'' +
                ", itemType='" + itemType + '\'' +
                ", pickupContactName='" + pickupContactName + '\'' +
                ", pickupContactPhone='" + pickupContactPhone + '\'' +
                ", deliveryContactName='" + deliveryContactName + '\'' +
                ", deliveryContactPhone='" + deliveryContactPhone + '\'' +
                ", deliveryProviderId=" + deliveryProviderId +
                ", spOrderId='" + spOrderId + '\'' +
                ", spOrderName='" + spOrderName + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", createdDate=" + createdDate +
                ", status='" + status + '\'' +
                ", statusDescription='" + statusDescription + '\'' +
                ", updatedDate='" + updatedDate + '\'' +
                ", totalWeightKg=" + totalWeightKg +
                ", amount=" + amount +
                ", validationPeriod=" + validationPeriod +
                '}';
    }
}