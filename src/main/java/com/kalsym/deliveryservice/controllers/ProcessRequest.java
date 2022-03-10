/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.controllers;

import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.*;
import com.kalsym.deliveryservice.provider.*;
import com.kalsym.deliveryservice.repositories.*;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * @author Taufik
 */
public class ProcessRequest {
    Order order;
    DeliveryOrder deliveryOrder;
    String sysTransactionId;
    String logprefix;
    String location;
    ProviderRatePlanRepository providerRatePlanRepository;
    ProviderConfigurationRepository providerConfigurationRepository;
    ProviderRepository providerRepository;
    int providerThreadRunning;
    List<PriceResult> priceResultLists;
    PriceResult priceResultList;
    List<DeliveryQuotation> deliveryQuotations;
    List<Fulfillment> fulfillments;

    SubmitOrderResult submitOrderResult;
    CancelOrderResult cancelOrderResult;
    QueryOrderResult queryOrderResult;
    SpCallbackResult spCallbackResult;
    GetPickupDateResult pickupDateResult;
    GetPickupTimeResult pickupTimeResult;
    LocationIdResult locationIdResult;
    DriverDetailsResult driverDetailsResult;
    AirwayBillResult airwayBillResult;
    AdditionalInfoResult additionalInfoResult;
    Object requestBody;
    Store store;
    SequenceNumberRepository sequenceNumberRepository;
    DeliverySpTypeRepository deliverySpTypeRepository;
    StoreDeliverySpRepository storeDeliveryDetailSp;
    @Autowired
    DeliveryQuotationRepository deliveryQuotationRepository;

    public ProcessRequest(String sysTransactionId, Order order, ProviderRatePlanRepository providerRatePlanRepository,
                          ProviderConfigurationRepository providerConfigurationRepository, ProviderRepository providerRepository,
                          SequenceNumberRepository sequenceNumberRepository, DeliverySpTypeRepository deliverySpTypeRepository, StoreDeliverySpRepository storeDeliveryDetailSp, List<Fulfillment> fulfillments) {
        this.sysTransactionId = sysTransactionId;
        this.order = order;
        this.logprefix = sysTransactionId;
        this.location = "ProcessRequest";
        this.providerRatePlanRepository = providerRatePlanRepository;
        this.providerConfigurationRepository = providerConfigurationRepository;
        this.providerRepository = providerRepository;
        this.providerThreadRunning = 0;
        this.priceResultLists = new ArrayList<>();
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.deliverySpTypeRepository = deliverySpTypeRepository;
        this.storeDeliveryDetailSp = storeDeliveryDetailSp;
        this.fulfillments = fulfillments;
    }

    public ProcessRequest(String sysTransactionId, Order order, ProviderRatePlanRepository providerRatePlanRepository,
                          ProviderConfigurationRepository providerConfigurationRepository, ProviderRepository providerRepository,
                          SequenceNumberRepository sequenceNumberRepository, DeliverySpTypeRepository deliverySpTypeRepository, StoreDeliverySpRepository storeDeliveryDetailSp) {
        this.sysTransactionId = sysTransactionId;
        this.order = order;
        this.logprefix = sysTransactionId;
        this.location = "ProcessRequest";
        this.providerRatePlanRepository = providerRatePlanRepository;
        this.providerConfigurationRepository = providerConfigurationRepository;
        this.providerRepository = providerRepository;
        this.providerThreadRunning = 0;
        this.priceResultLists = new ArrayList<>();
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.deliverySpTypeRepository = deliverySpTypeRepository;
        this.storeDeliveryDetailSp = storeDeliveryDetailSp;
    }

    public ProcessRequest(String sysTransactionId, DeliveryOrder deliveryOrder, ProviderRatePlanRepository providerRatePlanRepository,
                          ProviderConfigurationRepository providerConfigurationRepository, ProviderRepository providerRepository) {
        this.sysTransactionId = sysTransactionId;
        this.deliveryOrder = deliveryOrder;
        this.logprefix = sysTransactionId;
        this.location = "ProcessRequest";
        this.providerRatePlanRepository = providerRatePlanRepository;
        this.providerConfigurationRepository = providerConfigurationRepository;
        this.providerRepository = providerRepository;
        this.providerThreadRunning = 0;
        this.priceResultLists = new ArrayList<>();
    }

    public ProcessRequest(String sysTransactionId, Object requestBody, ProviderRatePlanRepository providerRatePlanRepository,
                          ProviderConfigurationRepository providerConfigurationRepository, ProviderRepository providerRepository) {
        this.sysTransactionId = sysTransactionId;
        this.requestBody = requestBody;
        this.logprefix = sysTransactionId;
        this.location = "ProcessRequest";
        this.providerRatePlanRepository = providerRatePlanRepository;
        this.providerConfigurationRepository = providerConfigurationRepository;
        this.providerRepository = providerRepository;
        this.providerThreadRunning = 0;
        this.priceResultLists = new ArrayList<>();
    }

    public ProcessRequest(String sysTransactionId, Store requestBody, ProviderRatePlanRepository providerRatePlanRepository,
                          ProviderConfigurationRepository providerConfigurationRepository, ProviderRepository providerRepository) {
        this.sysTransactionId = sysTransactionId;
        this.store = requestBody;
        this.logprefix = sysTransactionId;
        this.location = "ProcessRequest";
        this.providerRatePlanRepository = providerRatePlanRepository;
        this.providerConfigurationRepository = providerConfigurationRepository;
        this.providerRepository = providerRepository;
        this.providerThreadRunning = 0;
        this.priceResultLists = new ArrayList<>();
    }

    public ProcessResult GetPrice() {
        //get provider rate plan  
        LogUtil.info(logprefix, location, "Find provider rate plan for productCode:" + order.getProductCode(), "");
        if (order.getDeliveryProviderId() == null) {
            for (Fulfillment f : fulfillments) {
                List<DeliverySpType> deliverySpTypes = deliverySpTypeRepository.findAllByDeliveryTypeAndRegionCountryAndFulfilment(order.getDeliveryType(), order.getRegionCountry(), f.getFulfillment());
                for (int i = 0; i < deliverySpTypes.size(); i++) {
                    LogUtil.info(logprefix, location, "Get Price The Provider ID IS NULL  : " + sysTransactionId + " FulfillmentType : " + deliverySpTypes.get(i).getFulfilment(), "");

                    Fulfillment fulfillment = new Fulfillment();
                    fulfillment.setFulfillment(deliverySpTypes.get(i).getFulfilment());
                    fulfillment.setInterval(deliverySpTypes.get(i).getInterval());

                    LogUtil.info(logprefix, location, "Find Fulfillment Type :" + f.getFulfillment(), "");
                    List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(deliverySpTypes.get(i).getProvider().getId());
                    HashMap config = new HashMap();
                    for (int j = 0; j < providerConfigList.size(); j++) {
                        String fieldName = providerConfigList.get(j).getId().getConfigField();
                        String fieldValue = providerConfigList.get(j).getConfigValue();
                        config.put(fieldName, fieldValue);
                    }
                    ProviderThread dthread = new ProviderThread(this, sysTransactionId, deliverySpTypes.get(i).getProvider(), config, order, "GetPrice", sequenceNumberRepository, fulfillment);
                    dthread.start();
                }
            }
        } else {
            List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(order.getDeliveryProviderId());
            Provider provider = providerRepository.findOneById(order.getDeliveryProviderId());
            List<StoreDeliverySp> storeDeliverySps = storeDeliveryDetailSp.findByStoreId(order.getStoreId());
            if (storeDeliverySps.isEmpty()) {
                List<DeliverySpType> deliverySpTypes = deliverySpTypeRepository.findAllByProviderAndDeliveryTypeAndRegionCountryAndFulfilment(provider, order.getDeliveryType(), order.getRegionCountry(), order.getDeliveryPeriod());
                for (DeliverySpType deliverySpType : deliverySpTypes) {
                    LogUtil.info(logprefix, location, "Get Price The Store DeliverySP is Empty : " + sysTransactionId + " FulfillmentType : " + deliverySpType.getFulfilment(), "");

                    Fulfillment fulfillment = new Fulfillment();

                    fulfillment.setFulfillment(deliverySpType.getFulfilment());
                    fulfillment.setInterval(deliverySpType.getInterval());

                    HashMap config = new HashMap();
                    for (int j = 0; j < providerConfigList.size(); j++) {
                        String fieldName = providerConfigList.get(j).getId().getConfigField();
                        String fieldValue = providerConfigList.get(j).getConfigValue();
                        config.put(fieldName, fieldValue);
                    }
                    ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider, config, order, "GetPrice", sequenceNumberRepository, fulfillment);
                    dthread.start();

                }
            } else {
                LogUtil.info(logprefix, location, "If Store Store Delivery SP Is Not Empty ", "");

                for (StoreDeliverySp storeDeliverySp : storeDeliverySps) {
                    LogUtil.info(logprefix, location, "Get Price The Store DeliverySP : " + storeDeliverySp.getStoreId() + " FulfillmentType : " + storeDeliverySp.getFulfilment(), "");
                    Fulfillment fulfillment = new Fulfillment();
                    if (fulfillment.getFulfillment().equals("FOURHOURS") || fulfillment.getFulfillment().equals("NEXTDAY") || fulfillment.getFulfillment().equals("FOURDAYS")) {
                        List<DeliverySpType> deliverySpTypes = deliverySpTypeRepository.findAllByProviderAndDeliveryTypeAndRegionCountryAndFulfilment(storeDeliverySp.getProvider(), order.getDeliveryType(), order.getRegionCountry(), storeDeliverySp.getFulfilment());
                        fulfillment.setInterval(deliverySpTypes.get(0).getInterval());
                    }
                    fulfillment.setFulfillment(storeDeliverySp.getFulfilment());

                    HashMap config = new HashMap();
                    for (int j = 0; j < providerConfigList.size(); j++) {
                        String fieldName = providerConfigList.get(j).getId().getConfigField();
                        String fieldValue = providerConfigList.get(j).getConfigValue();
                        config.put(fieldName, fieldValue);
                    }
                    ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider, config, order, "GetPrice", sequenceNumberRepository, fulfillment);
                    dthread.start();

                }
            }

        }

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
            LogUtil.info(logprefix, location, "Get Price a) : " + ex.getMessage(), "");


        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
                LogUtil.info(logprefix, location, "Get Price b) : " + ex.getMessage(), "");
            }
        }

        ProcessResult response = new ProcessResult();
        if (priceResultLists.size() != 0) {
            response.resultCode = 0;
            response.returnObject = priceResultLists;
        } else {
            response.resultCode = -1;
            response.returnObject = priceResultLists;
        }
        LogUtil.info(logprefix, location, "GetPrices finish. resultCode:" + response.resultCode, " priceResult count:" + priceResultLists.size());
        return response;
    }


    public ProcessResult SubmitOrder() {
        //get provider rate plan  
        LogUtil.info(logprefix, location, "ProviderId:" + order.getDeliveryProviderId() + " productCode:" + order.getProductCode(), "");
        Optional<Provider> provider = providerRepository.findById(order.getDeliveryProviderId());
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(order.getDeliveryProviderId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, order, "SubmitOrder");
        dthread.start();

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        //store result in delivery order
        DeliveryOrder deliveryOrder = new DeliveryOrder();
        deliveryOrder.setCustomerId(order.getCustomerId());
        deliveryOrder.setPickupAddress(order.getPickup().getPickupAddress());
        deliveryOrder.setDeliveryAddress(order.getDelivery().getDeliveryAddress());
        deliveryOrder.setDeliveryContactName(order.getDelivery().getDeliveryContactName());
        deliveryOrder.setDeliveryContactPhone(order.getDelivery().getDeliveryContactPhone());
        deliveryOrder.setPickupContactName(order.getPickup().getPickupContactName());
        deliveryOrder.setPickupContactPhone(order.getPickup().getPickupContactPhone());
        deliveryOrder.setItemType(order.getItemType().name());
        deliveryOrder.setTotalWeightKg(order.getTotalWeightKg());

        ProcessResult response = new ProcessResult();
        if (submitOrderResult.resultCode == 0) {
            LogUtil.info(logprefix, location, "Order succesfully created", "");
            DeliveryOrder orderCreated = submitOrderResult.orderCreated;
            deliveryOrder.setCreatedDate(orderCreated.getCreatedDate());
            deliveryOrder.setSpOrderId(orderCreated.getSpOrderId());
            deliveryOrder.setSpOrderName(orderCreated.getSpOrderName());
            deliveryOrder.setVehicleType(orderCreated.getVehicleType());

            //TODO: changes by nadeem //Start

//            deliveryOrder.setCustomerTrackingUrl(orderCreated.getCustomerTrackingUrl());
//            deliveryOrder.setMerchantTrackingUrl(orderCreated.getMerchantTrackingUrl());
//
//            System.out.println(deliveryOrder.toString());

            // changes by nadeem //End

            response.resultCode = 0;
        } else {
            response.resultCode = submitOrderResult.resultCode;
            response.resultString = submitOrderResult.message;
            LogUtil.info(logprefix, location, "Fail to create order", "");
        }

        response.returnObject = submitOrderResult;
        LogUtil.info(logprefix, location, "SubmitOrder finish. resultCode:" + response.resultCode, " SubmitOrderResult:" + submitOrderResult);
        return response;
    }


    public ProcessResult CancelOrder() {
        //get provider rate plan  
        LogUtil.info(logprefix, location, "ProviderId:" + deliveryOrder.getDeliveryProviderId() + " productCode:" + deliveryOrder.getProductCode(), "");
        Optional<Provider> provider = providerRepository.findById(deliveryOrder.getDeliveryProviderId());
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(deliveryOrder.getDeliveryProviderId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, this.deliveryOrder.getSpOrderId(), "CancelOrder");
        dthread.start();

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        if (cancelOrderResult.resultCode == 0) {
            response.resultCode = 0;
            response.returnObject = cancelOrderResult;
        } else {
            response.resultCode = -1;
            response.returnObject = cancelOrderResult;
        }
        LogUtil.info(logprefix, location, "CancelOrder finish. resultCode:" + response.resultCode, " cancelOrderResult:" + cancelOrderResult);
        return response;
    }


    public ProcessResult QueryOrder() {
        //get provider rate plan  
        LogUtil.info(logprefix, location, "ProviderId:" + deliveryOrder.getDeliveryProviderId() + " SpOrderId:" + deliveryOrder.getSpOrderId(), "");
        Optional<Provider> provider = providerRepository.findById(deliveryOrder.getDeliveryProviderId());
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(deliveryOrder.getDeliveryProviderId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, this.deliveryOrder.getSpOrderId(), "QueryOrder");
        dthread.start();

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        response.resultCode = 0;
        response.returnObject = queryOrderResult;
        LogUtil.info(logprefix, location, "SubmitOrder finish. resultCode:" + response.resultCode, " queryOrderResult:" + queryOrderResult);
        return response;
    }

    public ProcessResult ProcessCallback(String spIP, ProviderIpRepository providerIpRepository, int providerId) {
        //get provider rate plan  
        LogUtil.info(logprefix, location, "Caller IP:" + spIP, "");
        //get provider based on IP
        Optional<ProviderIp> spId = providerIpRepository.findById(spIP);

        ProcessResult response = new ProcessResult();
        //if (spId.isPresent()) {
        //int providerId = spId.get().getSpId();
//        int providerId = 1;
        LogUtil.info(logprefix, location, "Provider found. SpId:" + providerId, "");
        Optional<Provider> provider = providerRepository.findById(providerId);
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(providerId);
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, requestBody, "ProviderCallback");
        dthread.start();

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        response.resultCode = 0;
        response.returnObject = spCallbackResult;
        /*} else {
            LogUtil.info(logprefix, location, "IP not recognize", "");
            response.resultCode=-1;
            response.returnObject=null;
        }*/

        LogUtil.info(logprefix, location, "ProcessCallback finish. resultCode:" + response.resultCode, " spCallbackResult:" + spCallbackResult);
        return response;
    }

    public ProcessResult GetPickupDate(int providerId) {
        //get provider rate plan  
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(providerId);
        Optional<Provider> provider = providerRepository.findById(providerId);
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, order, "GetPickupDate", sequenceNumberRepository);
        dthread.start();

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        response.resultCode = 0;
        response.returnObject = pickupDateResult;
        LogUtil.info(logprefix, location, "GetPickupDate finish. resultCode:" + response.resultCode, " pickupDateResult:" + pickupDateResult);
        return response;
    }

    public ProcessResult GetPickupTime(int providerId) {
        //get provider rate plan  
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(providerId);
        Optional<Provider> provider = providerRepository.findById(providerId);
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, order, "GetPickupTime", sequenceNumberRepository);
        dthread.start();

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        response.resultCode = 0;
        response.returnObject = pickupTimeResult;
        LogUtil.info(logprefix, location, "GetPickupTime finish. resultCode:" + response.resultCode, " pickupTimeResult:" + pickupTimeResult);
        return response;
    }


    public ProcessResult GetLocationId() {
        //get provider rate plan  
        LogUtil.info(logprefix, location, "Find provider rate plan for productCode:" + order.getProductCode(), "");
        List<ProviderRatePlan> providerRatePlanList = providerRatePlanRepository.findByIdProductCode(order.getProductCode());

        for (int i = 0; i < providerRatePlanList.size(); i++) {
            List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(providerRatePlanList.get(i).getProvider().getId());
            HashMap config = new HashMap();
            for (int j = 0; j < providerConfigList.size(); j++) {
                String fieldName = providerConfigList.get(j).getId().getConfigField();
                String fieldValue = providerConfigList.get(j).getConfigValue();
                config.put(fieldName, fieldValue);
            }
            ProviderThread dthread = new ProviderThread(this, sysTransactionId, providerRatePlanList.get(i).getProvider(), config, order, "GetLocationId", sequenceNumberRepository);
            dthread.start();
        }

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        response.resultCode = 0;
        response.returnObject = locationIdResult;
        LogUtil.info(logprefix, location, "GetLocationId finish. resultCode:" + response.resultCode, " locationIdResult count:" + locationIdResult);
        return response;
    }

    public ProcessResult GetDriverDetails() {
        //get provider rate plan
        LogUtil.info(logprefix, location, "Find provider rate plan for productCode:" + deliveryOrder.getDeliveryProviderId(), "");
        Provider provider = providerRepository.findOneById(deliveryOrder.getDeliveryProviderId());
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(provider.getId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }

        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider, config, deliveryOrder, "GetDriverDetails", sequenceNumberRepository);
        dthread.start();


        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        response.resultCode = driverDetailsResult.resultCode;
        response.returnObject = driverDetailsResult;
        LogUtil.info(logprefix, location, "GetDriverDetails finish. resultCode:" + response.resultCode, " driverDetailsResult count:" + driverDetailsResult);
        return response;
    }

    public ProcessResult GetAirwayBill() {
        //get provider rate plan
//        LogUtil.info(logprefix, location, "Find provider rate plan for productCode:" + order.getProductCode(), "");
        Provider provider = providerRepository.getOne(deliveryOrder.getDeliveryProviderId());


        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(provider.getId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider, config, deliveryOrder, "GetAirwayBill", sequenceNumberRepository);
        dthread.start();


        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        response.resultCode = 0;
        response.returnObject = airwayBillResult;
        LogUtil.info(logprefix, location, "GetAirwayBill finish. resultCode:" + response.resultCode, " airwayBillResult count:" + airwayBillResult);
        return response;
    }


    public ProcessResult GetAdditionalInfo() {
        //get provider rate plan
//        LogUtil.info(logprefix, location, "Find provider rate plan for productCode:" + order.getProductCode(), "");
        Provider provider = providerRepository.getOne(store.getProviderId());
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(provider.getId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider, config, store, "GetAdditionalInfo", sequenceNumberRepository);
        dthread.start();


        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        if (additionalInfoResult.resultCode == 0) {
            response.resultCode = 0;
            response.returnObject = additionalInfoResult;
        } else {
            response.resultCode = -1;
            response.returnObject = additionalInfoResult;
        }
        LogUtil.info(logprefix, location, "GetAirwayBill finish. resultCode:" + response.resultCode, " airwayBillResult count:" + airwayBillResult);
        return response;
    }

    public ProcessResult addPriorityFee() {
        //get provider rate plan
//        LogUtil.info(logprefix, location, "Find provider rate plan for productCode:" + order.getProductCode(), "");
        Optional<Provider> provider = providerRepository.findById(deliveryOrder.getDeliveryProviderId());
        List<ProviderConfiguration> providerConfigList = providerConfigurationRepository.findByIdSpId(provider.get().getId());
        HashMap config = new HashMap();
        for (int j = 0; j < providerConfigList.size(); j++) {
            String fieldName = providerConfigList.get(j).getId().getConfigField();
            String fieldValue = providerConfigList.get(j).getConfigValue();
            config.put(fieldName, fieldValue);
        }
        ProviderThread dthread = new ProviderThread(this, sysTransactionId, provider.get(), config, deliveryOrder, "AddPriorityFee", sequenceNumberRepository);
        dthread.start();


        try {
            Thread.sleep(100);
        } catch (Exception ex) {
        }

        while (providerThreadRunning > 0) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            //LogUtil.info(logprefix, location, "Current ProviderThread running:"+providerThreadRunning, "");
        }

        ProcessResult response = new ProcessResult();
        if (priceResultList.resultCode == 0) {
            response.resultCode = 0;
            response.returnObject = priceResultList;
        } else {
            response.resultCode = -1;
            response.returnObject = priceResultList;
        }
        LogUtil.info(logprefix, location, "AddPriorityFee finish. resultCode:" + response.resultCode, " AddPriorityFee count:" + priceResultList);
        return response;
    }


    public synchronized void addPriceResult(PriceResult priceResult) {
        priceResultLists.add(priceResult);
    }

    public synchronized void setPriceResultList(PriceResult priceResult) {
        this.priceResultList = priceResult;
    }

    public synchronized void setSubmitOrderResult(SubmitOrderResult orderResult) {
        this.submitOrderResult = orderResult;
    }

    public synchronized void setCancelOrderResult(CancelOrderResult orderResult) {
        this.cancelOrderResult = orderResult;
    }

    public synchronized void setQueryOrderResult(QueryOrderResult orderResult) {
        this.queryOrderResult = orderResult;
    }

    public synchronized void setCallbackResult(SpCallbackResult spCallbackResult) {
        this.spCallbackResult = spCallbackResult;
    }

    public synchronized void setPickupDateResult(GetPickupDateResult pickupDateResult) {
        this.pickupDateResult = pickupDateResult;
    }

    public synchronized void setPickupTimeResult(GetPickupTimeResult pickupTimeResult) {
        this.pickupTimeResult = pickupTimeResult;
    }

    public synchronized void setLocationIdResult(LocationIdResult locationIdResult) {
        this.locationIdResult = locationIdResult;
    }

    public synchronized void addProviderThreadRunning() {
        providerThreadRunning++;
    }

    public synchronized void deductProviderThreadRunning() {
        providerThreadRunning--;
    }

    public synchronized void setDriverDetailsResult(DriverDetailsResult driverDetailsResult) {
        this.driverDetailsResult = driverDetailsResult;
    }

    public synchronized void setAirwayBillResult(AirwayBillResult airwayBillResult) {
        this.airwayBillResult = airwayBillResult;
    }

    public synchronized void setAdditionalInfoResult(AdditionalInfoResult additionalInfoResult) {
        this.additionalInfoResult = additionalInfoResult;
    }


}
