package com.kalsym.deliveryservice.provider.Pickupp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SpCallbackResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class OrderCallback extends SyncDispatcher {

    private final HashMap productMap;
    private final String logprefix;
    private final String location = "PickuppOrderCallBack";
    private final String systemTransactionId;
    private final JsonObject jsonBody;
    private String spOrderId;

    public OrderCallback(CountDownLatch latch, HashMap config, Object jsonBody, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Pickupp OrderCallback class initiliazed!!", "");
        productMap = (HashMap) config.get("productCodeMapping");
        Gson gson = new Gson();
        String jsonString = jsonBody.toString();
        LogUtil.info(logprefix, location, "Request Body:" + jsonString, "");
        this.jsonBody = new Gson().fromJson(jsonString, JsonObject.class);
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        SpCallbackResult callbackResult = extractResponseBody();
        response.returnObject = callbackResult;
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }


    private SpCallbackResult extractResponseBody() {
        SpCallbackResult callbackResult = new SpCallbackResult();
        try {
            String status = jsonBody.get("status").getAsString();
            String spOrderId = jsonBody.get("order_number").getAsString();
            if(status.equals("CONTACTING_AGENT")){
                callbackResult.status = "new";
            }
            else if(status.equals("ACCEPTED")){
                callbackResult.status = "on_going";
            }
            else if(status.equals("ENROUTE")){
                callbackResult.status="active";
            }
            else if(status.equals("DELIVERED")){
                callbackResult.status="finished";
            }
            else{
                callbackResult.status = status;
            }
            String spDriverId = "";
            callbackResult.spOrderId = spOrderId;
//            callbackResult.status = status;
            callbackResult.driverId = spDriverId;
            callbackResult.riderName = jsonBody.get("delivery_agent_name").getAsString();
            callbackResult.riderPhone = jsonBody.get("delivery_agent_phone").getAsString();
            LogUtil.info(logprefix, location, "SpOrderId:" + spOrderId + " Status:" + status, "");
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return callbackResult;
    }

}