package com.kalsym.deliveryservice.provider.Swyft;

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

    private final String systemTransactionId;
    @Autowired
    private DeliveryZoneCityRepository deliveryZoneCityRepository;
    private Order order;
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "SwyftGetPrice";
    private Double gst;
    private Double fuelChargeRate;
    private SequenceNumberRepository sequenceNumberRepository;
    @Autowired
    private DeliveryZonePriceRepository deliveryZonePriceRepository;
    private Fulfillment fulfillment;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment, DeliveryZonePriceRepository deliveryZonePriceRepository) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Swyft GetPrices class initiliazed!!", "");
        this.gst = Double.parseDouble((String) config.get("gst"));
        this.fuelChargeRate = Double.parseDouble((String) config.get("fuel_charges"));
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.fulfillment = fulfillment;
        this.deliveryZonePriceRepository = deliveryZonePriceRepository;
    }


    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();
        String pickupCity = order.getPickup().getPickupCity();
        String deliveryCity = order.getDelivery().getDeliveryCity();

        String zonePickup = order.getPickup().getPickupZone();
        String zoneDelivery = order.getDelivery().getDeliveryZone();


        Double fuelChargeRate = 16.0;
        Double gstRate = 8.5;
        Double totalCharge = 0.00;

        if (!zonePickup.equals("null") && !zoneDelivery.equals("null") && !pickupCity.equals("null")
                && !deliveryCity.equals("null")) {
            if (pickupCity.equals(deliveryCity)) {
                totalCharge = calculatePrice(order.getTotalWeightKg(), 119.0, 125.0, 50.0);
            } else if (zonePickup.equals(zoneDelivery)) {
                totalCharge = calculatePrice(order.getTotalWeightKg(), 125.0, 138.0, 60.0);
            } else {
                totalCharge = calculatePrice(order.getTotalWeightKg(), 151.0, 163.0, 70.0);
            }
            Double fuelCharge = totalCharge * fuelChargeRate / 100.0;
            Double gst = totalCharge * gstRate / 100.0;
            totalCharge += fuelCharge + gst;


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

    private Double calculatePrice(Double weight, Double lowerBasePrice,
                                  Double higherBasePrice, Double additionalPrice) {
        if (weight <= 0.5) {
            return lowerBasePrice;
        }

        if (weight <= 1.0) {
            return higherBasePrice;
        }

        return higherBasePrice + (weight - 1) * additionalPrice;
    }
}
