package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "delivery_orders_status")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class DeliveryOrderStatus  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String orderId;
    String spOrderId;
    String status;
    String description;
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    Date updated;
    String systemTransactionId;
    String deliveryCompletionStatus;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deliveryOrderId")
    @NotFound(action = NotFoundAction.IGNORE)
    @ToString.Exclude
    private DeliveryOrder order;


    public DeliveryOrderStatus(String deliveryCompletionStatus) {
        super();
        this.deliveryCompletionStatus = deliveryCompletionStatus;
    }

//    @Override
//    public int compareTo(DeliveryOrderStatus orderStatus) {
//        if (this.getDeliveryCompletionStatus() > orderStatus.getDeliveryCompletionStatus())
//            return 1;
//        else return -1;
//    }
}
