/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SpCallbackResult;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.LinkedHashMap;
        
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.Gson;

public class OrderCallback extends SyncDispatcher {

    private String spOrderId;
    private final HashMap productMap;
    private final String logprefix;
    private final String location="MrSpeedyOrderCallback";
    private final String systemTransactionId;
    private final JsonObject jsonBody;
    
    public OrderCallback(CountDownLatch latch, HashMap config, Object jsonBody, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy OrderCallback class initiliazed!!", "");
        productMap = (HashMap) config.get("productCodeMapping");
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonBody,LinkedHashMap.class);
        LogUtil.info(logprefix, location, "Request Body:"+jsonString, "");
        this.jsonBody = new Gson().fromJson(jsonString, JsonObject.class);
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();            
        SpCallbackResult callbackResult =extractResponseBody();
        response.returnObject=callbackResult;
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    
     private SpCallbackResult extractResponseBody() {
        SpCallbackResult callbackResult = new SpCallbackResult();       
        try {
            String status = jsonBody.get("delivery").getAsJsonObject().get("status").getAsString();
            String spOrderId = jsonBody.get("delivery").getAsJsonObject().get("order_id").getAsString();
            String driverId = jsonBody.get("delivery").getAsJsonObject().get("courier").getAsJsonObject().get("courier_id").getAsString();
            callbackResult.spOrderId=spOrderId;
            callbackResult.status=status;
            callbackResult.driverId = driverId;
            LogUtil.info(logprefix, location, "SpOrderId:"+spOrderId+" Status:"+status, "");
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return callbackResult;
    }

}