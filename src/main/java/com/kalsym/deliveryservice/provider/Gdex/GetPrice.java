/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.kalsym.deliveryservice.provider.MrSpeedy.*;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String getprice_token;
    private final String getprice_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private Order order;
    private HashMap productMap;
    private String sessionToken;    
    private String sslVersion="SSL";
    private String logprefix;
    private String location="GdexGetPrice";
    private final String systemTransactionId;
    private SequenceNumberRepository sequenceNumberRepository;
    
    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository ) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Gdex GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.getprice_token = (String) config.get("getprice_token");
        this.getprice_key = (String) config.get("getprice_key");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.sequenceNumberRepository = sequenceNumberRepository;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();                        
        try {
            HashMap httpHeader = new HashMap();
            httpHeader.put("User-Token", this.getprice_token);
            httpHeader.put("Subscription-Key", this.getprice_key);
            httpHeader.put("Content-Type", "application/json-patch+json");
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
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Exception error :", "", ex);
            response.resultCode=-1;
        }
        return response;
    }
    
    private String generateRequestBody() {
        JsonArray jsonArray = new JsonArray();
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("ParcelType", order.getItemType().name());
        jsonReq.addProperty("ReferenceNumber", generateReferenceNumber());
        jsonReq.addProperty("FromPostCode", order.getPickup().getPickupPostcode());
        jsonReq.addProperty("ToPostCode", order.getDelivery().getDeliveryPostcode());
        jsonReq.addProperty("Weight", order.getTotalWeightKg());
        jsonReq.addProperty("Country", "MYS"); 
        jsonArray.add(jsonReq);
        return jsonArray.toString();
    }
    
    
    private PriceResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String statusCode = jsonResp.get("statusCode").getAsString();
        PriceResult priceResult = new PriceResult();            
        if (statusCode.equals("200")) {
            JsonArray dataArray = jsonResp.get("data").getAsJsonArray();
            JsonObject orderObject = dataArray.get(0).getAsJsonObject();
            String payAmount = orderObject.get("Rate").getAsString();
            LogUtil.info(logprefix, location, "Payment Amount:"+payAmount, "");
            priceResult.price=Double.parseDouble(payAmount);
        } else {
            priceResult.price=0.00;
        }
        return priceResult;
    }
    
    private Integer generateReferenceNumber() {
       return sequenceNumberRepository.getSequenceNumber("GDEX");
    }

}