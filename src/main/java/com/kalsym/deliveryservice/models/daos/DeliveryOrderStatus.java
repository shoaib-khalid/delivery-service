package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@Entity
@Table(name = "delivery_orders_status")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeliveryOrderStatus implements Serializable {
    @Id
    Long id;
    String orderId;
    String spOrderId;
    String status;
    String description;
    String updated;
}
