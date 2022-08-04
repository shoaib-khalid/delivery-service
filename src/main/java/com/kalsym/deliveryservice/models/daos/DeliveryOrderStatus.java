package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    @Transient
    private String orderTimeConverted;


    public DeliveryOrderStatus(String deliveryCompletionStatus) {
        super();
        this.deliveryCompletionStatus = deliveryCompletionStatus;
    }

    public String getOrderTimeConverted() {
        LocalDateTime datetime = DateTimeUtil.convertToLocalDateTimeViaInstant(updated, ZoneId.of("Asia/Kuala_Lumpur"));
        DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return formatter1.format(datetime);
    }
//    @Override
//    public int compareTo(DeliveryOrderStatus orderStatus) {
//        if (this.getDeliveryCompletionStatus() > orderStatus.getDeliveryCompletionStatus())
//            return 1;
//        else return -1;
//    }
}
