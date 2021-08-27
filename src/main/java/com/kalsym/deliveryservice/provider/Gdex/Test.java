/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.Pickup;
import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.enums.ItemType;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author user
 */
public class Test {
    public static void main(String[] args) {
        
       
        try {
            CountDownLatch latch = new CountDownLatch(1);
            HashMap configMap = new HashMap();
            configMap.put("getprice_url", "https://myopenapi.gdexpress.com/test/api/MyGDex/GetShippingRate");
            configMap.put("getprice_token", "6GT86PveZUa28mWWrpStA");
            configMap.put("getprice_key", "b4495c44bc08433b8bfb1126a5b34a26");
            configMap.put("getprice_connect_timeout", "10000");
            configMap.put("getprice_wait_timeout", "40000");
            configMap.put("ssl_version", "TLSv1.2");
            
            HashMap productMapping = new HashMap();
            productMapping.put("UMOBILE", "UB");
            configMap.put("productCodeMapping", productMapping);
            
            String systemTransactionId = "DS123456";
            Order order = new Order();
            order.setItemType(ItemType.PARCEL);
            order.setTransactionId(systemTransactionId);
            Pickup pickup = new Pickup();
            pickup.setPickupPostcode("43650");
            order.setPickup(pickup);
            Delivery delivery = new Delivery();
            delivery.setDeliveryPostcode("43800");
            order.setDelivery(delivery);
            order.setTotalWeightKg(1.00);
            
            //SequenceNumberRepository sequenceNumberRepository =null;
            
            //GetPrices getPrice = new GetPrices(latch, configMap, order, systemTransactionId, sequenceNumberRepository);
            //getPrice.process();
            
            configMap.put("getpickupdate_url", "https://myopenapi.gdexpress.com/test/api/MyGDex/GetPickupDateListing");
            configMap.put("getpickupdate_key", "b4495c44bc08433b8bfb1126a5b34a26");
            configMap.put("getpickupdate_connect_timeout", "10000");
            configMap.put("getpickupdate_wait_timeout", "40000");
            configMap.put("ssl_version", "TLSv1.2");
            
            GetPickupDate pickupDate = new GetPickupDate(latch, configMap, order, systemTransactionId);
            pickupDate.process();
            
        } catch (Exception ex) {
            
        }
        
    } 
}
