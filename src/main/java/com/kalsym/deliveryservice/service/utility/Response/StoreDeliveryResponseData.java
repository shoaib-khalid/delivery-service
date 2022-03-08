package com.kalsym.deliveryservice.service.utility.Response;

import com.kalsym.deliveryservice.models.enums.ItemType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class StoreDeliveryResponseData {
    String storeId;
    String type;
    ItemType itemType;
    int maxOrderQuantityForBike;
    List<StoreDeliveryPeriod> storeDeliveryPeriodList;

}
