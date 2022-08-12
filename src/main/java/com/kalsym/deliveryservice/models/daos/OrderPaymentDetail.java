package com.kalsym.deliveryservice.models.daos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Getter
@Setter
@ToString
@Table(name = "order_payment_detail")
public class OrderPaymentDetail implements Serializable {

    private String accountName;
    private String gatewayId;
    //private String couponId;
    private Date time;
    @Id
    private String orderId;


    private String  deliveryQuotationReferenceId;
    private Double deliveryQuotationAmount;

    @Column(columnDefinition = "TINYINT(1) default 0")
    private Boolean isCombinedDelivery;

    public void update(OrderPaymentDetail orderPaymentDetail) {

    }
}
