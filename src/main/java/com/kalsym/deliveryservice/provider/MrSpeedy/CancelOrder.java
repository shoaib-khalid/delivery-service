/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.CancelOrderResult;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;

public class CancelOrder extends SyncDispatcher {

    private final String cancelOrder_url;
    private final String cancelOrder_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="MrSpeedyCancelOrder";
    private final String systemTransactionId;
    
    public CancelOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy CancelOrder class initiliazed!!", "");
        this.cancelOrder_url = (String) config.get("cancelorder_url");
        this.cancelOrder_token = (String) config.get("cancelorder_token");
        this.connectTimeout = Integer.parseInt((String) config.get("cancelorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("cancelorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.sslVersion = (String) config.get("ssl_version");
        this.spOrderId = spOrderId;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();            
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-DV-Auth-Token", this.cancelOrder_token);
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.cancelOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
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
        
        jsonReq.addProperty("order_id", this.spOrderId);
        
        return jsonReq.toString();
    }
    
     private CancelOrderResult extractResponseBody(String respString) {
        CancelOrderResult cancelOrderResult = new CancelOrderResult();       
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            boolean isSuccess = jsonResp.get("is_successful").getAsBoolean();
            JsonObject orderObject = jsonResp.get("order").getAsJsonObject();
            LogUtil.info(logprefix, location, "isSuccess:"+isSuccess, "");
             cancelOrderResult.isSuccess=isSuccess;
            //extract order cancelled
            String orderId = orderObject.get("order_id").getAsString();
            String orderName = orderObject.get("order_name").getAsString();
            String created = orderObject.get("created_datetime").getAsString();  
            VehicleType vehicleType = VehicleType.valueOf(orderObject.get("vehicle_type_id").getAsInt());
            DeliveryOrder orderCancelled = new DeliveryOrder();
            orderCancelled.setSpOrderId(orderId);
            orderCancelled.setSpOrderName(orderName);
            orderCancelled.setCreatedDate(created);
            orderCancelled.setVehicleType(vehicleType.toString());
            cancelOrderResult.orderCancelled=orderCancelled;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return cancelOrderResult;
    }

}