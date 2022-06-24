package com.kalsym.deliveryservice.provider.TCS;

import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryZonePrice;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.DeliveryZoneCityRepository;
import com.kalsym.deliveryservice.repositories.DeliveryZonePriceRepository;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    //    private final Integer providerId;
    @Autowired
    private DeliveryZoneCityRepository deliveryZoneCityRepository;
    private Order order;
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "TCSGetPrice";
    private Integer GST;
    private Integer fuelFee;
    private SequenceNumberRepository sequenceNumberRepository;
    @Autowired
    private DeliveryZonePriceRepository deliveryZonePriceRepository;
    private Fulfillment fulfillment;

    private Integer providerId;


    public GetPrice(CountDownLatch latch, Integer providerId, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment, DeliveryZonePriceRepository deliveryZonePriceRepository) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "TCS GetPrices class initiliazed!!", "");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        this.GST = Integer.parseInt((String) config.get("gst"));
        this.fuelFee = Integer.parseInt((String) config.get("fuelCharge"));
//        this.providerId = Integer.parseInt((String) config.get("providerId"));
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.fulfillment = fulfillment;
        this.deliveryZonePriceRepository = deliveryZonePriceRepository;
        this.providerId = providerId;
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();
        String pickupCity = order.getPickup().getPickupCity();
        String deliveryCity = order.getDelivery().getDeliveryCity();

        String zonePickup = order.getPickup().getPickupZone();
        String zoneDelivery = order.getDelivery().getDeliveryZone();


        Double lastMileLogistics;
        Double fuelCharges;
        Double gst;
        Double totalCharge = 0.00;

        System.out.println(" CHECK STATEMENT = " + pickupCity.equals("null"));
        System.out.println(" CHECK STATEMENT = " + deliveryCity.equals("null"));
        System.out.println(" CHECK STATEMENT = " + zonePickup.equals("null"));
        System.out.println(" CHECK STATEMENT = " + zoneDelivery.equals("null"));


        Double weight = order.getTotalWeightKg();
        if (!zonePickup.equals("null") && !zoneDelivery.equals("null") && !pickupCity.equals("null") && !deliveryCity.equals("null")) {
            DeliveryZonePrice deliveryZonePrice;
            if (pickupCity.equals(deliveryCity)) {
//                if (weight <= 0.5) {
//                    lastMileLogistics = 120.00;
//                } else if (weight > 0.5 && weight == 1) {
//                    lastMileLogistics = 130.00;
//                } else {
//                    lastMileLogistics = 130 * weight;
//                }
                if (weight <= 0.5) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(providerId), 0.5);
                    lastMileLogistics = deliveryZonePrice.getWithInCity().doubleValue();
                } else if (weight > 0.5 && weight <= 1.0) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(providerId), 1.0);
                    lastMileLogistics = deliveryZonePrice.getWithInCity().doubleValue();
                } else {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(providerId), 1.1);
                    lastMileLogistics = BigDecimal.valueOf(deliveryZonePrice.getWithInCity().doubleValue() * weight).doubleValue();
                }


            } else if (zonePickup.equals(zoneDelivery)) {
/*
                if (weight <= 0.5) {
                    lastMileLogistics = 170.00;
                } else if (weight > 0.5 && weight == 1) {
                    lastMileLogistics = 180.00;
                } else {
                    lastMileLogistics = 180 * weight;
                }*/

                if (weight <= 0.5) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(order.getDeliveryProviderId()), 0.5);
                    lastMileLogistics = deliveryZonePrice.getSameZone().doubleValue();
                } else if (weight > 0.5 && weight <= 1) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(order.getDeliveryProviderId()), 1);
                    lastMileLogistics = deliveryZonePrice.getSameZone().doubleValue();
                } else {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(order.getDeliveryProviderId()), 1.1);
                    lastMileLogistics = BigDecimal.valueOf(deliveryZonePrice.getSameZone().doubleValue() * weight).doubleValue();
                }
            } else {

             /*   if (weight <= 0.5) {
                    lastMileLogistics = 210.00;
                } else if (weight > 0.5 && weight == 1) {
                    lastMileLogistics = 270.00;
                } else {
                    lastMileLogistics = 270 * weight;
                }*/

                if (weight <= 0.5) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(order.getDeliveryProviderId()), 0.5);
                    lastMileLogistics = deliveryZonePrice.getDifferentZone().doubleValue();
                } else if (weight > 0.5 && weight <= 1) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(order.getDeliveryProviderId()), 1);
                    lastMileLogistics = deliveryZonePrice.getDifferentZone().doubleValue();
                } else {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(String.valueOf(order.getDeliveryProviderId()), 1.1);
                    lastMileLogistics = BigDecimal.valueOf(deliveryZonePrice.getDifferentZone().doubleValue() * weight).doubleValue();
                }
            }
            fuelCharges = (lastMileLogistics * 20) / 100;
            gst = (lastMileLogistics * 16) / 100;
            totalCharge = lastMileLogistics + fuelCharges + gst;

            PriceResult priceResult = new PriceResult();
            BigDecimal bd = new BigDecimal(totalCharge);
            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
            priceResult.price = bd;
            priceResult.pickupCity = pickupCity;
            priceResult.deliveryCity = deliveryCity;
            priceResult.pickupZone = zonePickup;
            priceResult.deliveryZone = zoneDelivery;
            priceResult.fulfillment = fulfillment.getFulfillment();
            priceResult.interval = null;
            response.resultCode = 0;
            response.returnObject = priceResult;

        } else {
            PriceResult result = new PriceResult();
            result.message = "ERR_OUT_OF_SERVICE_AREA";
            result.interval = null;
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;
        }
        return response;
    }
   /* public ProcessResult process() {

        LogUtil.info(logprefix, location, "Process TCS GET PRICE", "3");

        ProcessResult response = new ProcessResult();
        String pickupCity = order.getPickup().getPickupCity();
        String deliveryCity = order.getDelivery().getDeliveryCity();
        LogUtil.info(logprefix, location, "Delivery City : ", pickupCity + " delivery City : " + deliveryCity);
        List<DeliveryZoneCity> cities = DeliveryZonePriceRepositoryfindAll();

*//*        DeliveryZoneCity pickupZone = deliveryZoneCityRepository.findByCityContains(pickupCity);
        DeliveryZoneCity deliveryZone = deliveryZoneCityRepository.findByCityContains(deliveryCity);


        String zonePickup = pickupZone.getZone();
        String zoneDelivery = deliveryZone.getZone();*//*
        DeliveryZonePrice deliveryZonePrice;

        BigDecimal lastMileLogistics;
        BigDecimal fuelCharges;
        BigDecimal gst;
        BigDecimal totalCharge;
        double weight = order.getTotalWeightKg();

        if (!pickupCity.isEmpty() && !deliveryCity.isEmpty()) {
            if (pickupCity.equals(deliveryCity)) {
                if (weight <= 0.5) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 0.5);
                    lastMileLogistics = deliveryZonePrice.getWithInCity();
                } else if (weight > 0.5 && weight <= 1) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 1);
                    lastMileLogistics = deliveryZonePrice.getWithInCity();
                } else {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 1.1);
                    lastMileLogistics = BigDecimal.valueOf(deliveryZonePrice.getWithInCity().doubleValue() * weight);
                }
            }
        *//*    else if (zonePickup.equals(zoneDelivery)) {
                if (weight <= 0.5) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 0.5);
                    lastMileLogistics = deliveryZonePrice.getSameZone();
                } else if (weight > 0.5 && weight <= 1) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 1);
                    lastMileLogistics = deliveryZonePrice.getSameZone();
                } else {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 1.1);
                    lastMileLogistics = BigDecimal.valueOf(deliveryZonePrice.getSameZone().doubleValue() * weight);
                }
            } *//*
            else {
                if (weight <= 0.5) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 0.5);
                    lastMileLogistics = deliveryZonePrice.getDifferentZone();
                } else if (weight > 0.5 && weight <= 1) {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 1);
                    lastMileLogistics = deliveryZonePrice.getDifferentZone();
                } else {
                    deliveryZonePrice = deliveryZonePriceRepository.findBySpIdAndWeight(providerId, 1.1);
                    lastMileLogistics = BigDecimal.valueOf(deliveryZonePrice.getDifferentZone().doubleValue() * weight);
                }
            }
            fuelCharges = BigDecimal.valueOf((lastMileLogistics.doubleValue() * this.fuelFee) / 100);
            gst = BigDecimal.valueOf((lastMileLogistics.doubleValue() * this.GST) / 100);
            totalCharge = BigDecimal.valueOf(lastMileLogistics.doubleValue() + fuelCharges.doubleValue() + gst.doubleValue());
            PriceResult priceResult = new PriceResult();
            priceResult.price = totalCharge;
            response.resultCode = 0;
            response.returnObject = priceResult;
        } else {
            PriceResult result = new PriceResult();
            LogUtil.info(logprefix, location, "Request failed", " PickupCity/DeliveryCity Not Found");
            result.message = "PickupCity/DeliveryCity Not Found";
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;
        }
        return response;
    }*/
}
