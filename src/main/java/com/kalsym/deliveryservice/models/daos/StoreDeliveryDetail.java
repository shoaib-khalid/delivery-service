package com.kalsym.deliveryservice.models.daos;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "store_delivery_detail")
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreDeliveryDetail {
    @Id
    public String storeId;
    public String type;
    public String itemType;
    public int maxOrderQuantityForBike;
    public boolean allowsStorePickup;

}
