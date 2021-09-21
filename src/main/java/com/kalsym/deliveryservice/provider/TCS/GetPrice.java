package com.kalsym.deliveryservice.provider.TCS;

import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryZoneCity;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.DeliveryZoneCityRepository;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {
//    private final String getprice_url;
//    private final String getprice_token;
//    private final String getprice_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private Order order;
    private HashMap productMap;
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="TCSGetPrice";
    private final String systemTransactionId;
    private SequenceNumberRepository sequenceNumberRepository;
    DeliveryZoneCityRepository deliveryZoneCityRepository;

    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "TCS GetPrices class initiliazed!!", "");
//        this.getprice_url = (String) config.get("getprice_url");
//        this.baseUrl = (String) config.get("domainUrl");
//
//        this.secretKey = (String) config.get("secretKey");
//        this.apiKey = (String) config.get("apiKey");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();
        String pickupCity = order.getPickup().getPickupCity();
        String deliveryCity = order.getDelivery().getDeliveryCity();

        DeliveryZoneCity pickupZone = deliveryZoneCityRepository.findByCity(pickupCity);
        DeliveryZoneCity deliveryZone = deliveryZoneCityRepository.findByCity(deliveryCity);

        String zonePickup = pickupZone.getZone();
        String zoneDelivery = deliveryZone.getZone();

        Double lastMileLogistics;
        Double fuelCharges;
        Double gst;
        Double totalCharge = 0.00;

        Double weight = order.getTotalWeightKg();
        if (pickupCity == deliveryCity) {
            if (weight <= 0.5) {
                lastMileLogistics = 120.00;
            } else if (weight > 0.5 && weight == 1) {
                lastMileLogistics = 130.00;
            } else {
                lastMileLogistics = 130 * weight;
            }

        } else if (zonePickup == zoneDelivery) {
            if (weight <= 0.5) {
                lastMileLogistics = 170.00;
            } else if (weight > 0.5 && weight == 1) {
                lastMileLogistics = 180.00;
            } else {
                lastMileLogistics = 180 * weight;
            }
        } else {
            if (weight <= 0.5) {
                lastMileLogistics = 210.00;
            } else if (weight > 0.5 && weight == 1) {
                lastMileLogistics = 270.00;
            } else {
                lastMileLogistics = 270 * weight;
            }
        }
        fuelCharges = (lastMileLogistics * 20) / 100;
        gst = (lastMileLogistics * 16) / 100;
        totalCharge = lastMileLogistics + fuelCharges + gst;
        PriceResult priceResult = new PriceResult();
        BigDecimal bd = new BigDecimal(totalCharge);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        priceResult.price=bd;
        response.resultCode=0;
        response.returnObject= priceResult;

        return response;
    }
}
