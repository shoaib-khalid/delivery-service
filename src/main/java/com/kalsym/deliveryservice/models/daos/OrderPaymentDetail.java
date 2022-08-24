package com.kalsym.deliveryservice.models.daos;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@ToString
@Setter
@Table(name = "order_payment_detail")
public class OrderPaymentDetail implements Serializable {

    private String accountName;
    private String gatewayId;
    //private String couponId;
    private Date time;
    @Id
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", insertable = false, updatable = false)
    private StoreOrder order;


    private String  deliveryQuotationReferenceId;
    private Double deliveryQuotationAmount;

    @Column(columnDefinition = "TINYINT(1) default 0")
    private Boolean isCombinedDelivery;

    public void update(OrderPaymentDetail orderPaymentDetail) {

    }
}
