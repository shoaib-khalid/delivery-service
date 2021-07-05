/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String getprice_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="MrSpeedyGetPrice";
    private final String systemTransactionId;
    
    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository ) {
   
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.getprice_token = (String) config.get("getprice_token");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();            
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-DV-Auth-Token", this.getprice_token);
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Connection", "close");



        String requestBody = generateRequestBody();
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.getprice_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode==0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode=0;            
            response.returnObject=extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode=-1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }
    
    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("matter", order.getItemType().name());
        jsonReq.addProperty("total_weight_kg", order.getTotalWeightKg());
        jsonReq.addProperty("vehicle_type_id", VehicleType.valueOf(order.getPickup().getVehicleType().name()).getCode());
        JsonArray addressList = new JsonArray();

        JsonObject pickupAddress = new JsonObject();
        pickupAddress.addProperty("address", order.getPickup().getPickupAddress());
        JsonObject contactPerson2 = new JsonObject();
        contactPerson2.addProperty("phone", order.getPickup().getPickupContactPhone());
        contactPerson2.addProperty("name", order.getPickup().getPickupContactName());        
        pickupAddress.add("contact_person", contactPerson2); 
        addressList.add(pickupAddress);
        
        JsonObject deliveryAddress = new JsonObject();
        deliveryAddress.addProperty("address", order.getDelivery().getDeliveryAddress());  
        JsonObject contactPerson = new JsonObject();
        contactPerson.addProperty("phone", order.getDelivery().getDeliveryContactPhone());
        contactPerson.addProperty("name", order.getDelivery().getDeliveryContactName());        
        deliveryAddress.add("contact_person", contactPerson);        
        addressList.add(deliveryAddress);
        
        jsonReq.add("points", addressList);
        
        return jsonReq.toString();
    }
    
    
    private PriceResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        JsonObject orderObject = jsonResp.get("order").getAsJsonObject();
        String payAmount = orderObject.get("payment_amount").getAsString();
        LogUtil.info(logprefix, location, "Payment Amount:"+payAmount, "");
        PriceResult priceResult = new PriceResult();
        priceResult.price=Double.parseDouble(payAmount);
        return priceResult;
    }
    
    

}