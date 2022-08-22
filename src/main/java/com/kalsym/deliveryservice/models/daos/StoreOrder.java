package com.kalsym.deliveryservice.models.daos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kalsym.deliveryservice.models.enums.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

//OrderTable
@Entity
@Table(name = "order")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreOrder implements Serializable {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private String storeId;
    private Double subTotal;
    private Double deliveryCharges;
    private Double total;
    private String customerNotes;
    private String privateAdminNotes;
    private String cartId;
    private String customerId;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date created;
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updated;

    private String invoiceId;

    @Column(nullable = true)
    private Double klCommission;

    @Column(nullable = true)
    private Double storeServiceCharges;

    @Column(nullable = true)
    private Double storeShare;

    private String paymentType;

    private String deliveryType;

    @Column(nullable = true)
    private Double appliedDiscount;

    @Column(nullable = true)
    private Double deliveryDiscount;

    private String appliedDiscountDescription ;

    private String deliveryDiscountDescription ;

    private Boolean beingProcess;

    @Enumerated(EnumType.STRING)
    private OrderStatus completionStatus;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storeId", insertable = false, updatable = false)
    private Store store;


    /*    @OneToMany(fetch = FetchType.LAZY, mappedBy = "orderMain")
    private List<OrderItem> orderItem;
     */
    public void update(StoreOrder order) {
        if (null != order.getStoreId()) {
            this.setStoreId(order.getStoreId());
        }
        subTotal = order.getSubTotal();
        storeServiceCharges = order.getStoreServiceCharges();
        deliveryCharges = order.getDeliveryCharges();
        total = order.getTotal();
        customerNotes = order.getCustomerNotes();
        privateAdminNotes = order.getPrivateAdminNotes();
        customerId = order.getCustomerId();
        cartId = order.getCartId();
        //created = order.getCreated();
        //updated = order.getUpdated();
    }
}
