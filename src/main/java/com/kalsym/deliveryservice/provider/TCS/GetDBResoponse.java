package com.kalsym.deliveryservice.provider.TCS;

import com.kalsym.deliveryservice.models.daos.DeliveryZoneCity;
import com.kalsym.deliveryservice.repositories.DeliveryZoneCityRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class GetDBResoponse {
    @Autowired
    private  DeliveryZoneCityRepository deliveryZoneCityRepository;

    public List<DeliveryZoneCity> getResult(String city) throws InstantiationException, IllegalAccessException {
        List<DeliveryZoneCity> deliveryZoneCity = deliveryZoneCityRepository.findAll();
        return deliveryZoneCity;
    }
}
