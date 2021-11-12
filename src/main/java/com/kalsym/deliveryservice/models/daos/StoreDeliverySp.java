package com.kalsym.deliveryservice.models.daos;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

@Entity
@Table(name = "store_delivery_sp")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreDeliverySp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    String id;
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "storeId", insertable = false, updatable = false)
//    @Fetch(FetchMode.JOIN)
//    private StoreDeliveryDetailRepository storeDeliveryDetail;
    String storeId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deliverySpId", insertable = false, updatable = false)
    @Fetch(FetchMode.JOIN)
    private Provider provider;
}
