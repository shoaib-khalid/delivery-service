package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "delivery_orders_status")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class DeliveryOrderStatus {
    @Id
    Long id;
    String orderId;
    String spOrderId;
    String status;
    String description;
    Date updated;
    String systemTransactionId;
    String deliveryCompletionStatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deliveryOrderId")
    @NotFound(action = NotFoundAction.IGNORE)
    private DeliveryOrder order;
}
