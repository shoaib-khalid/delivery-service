package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SpCallbackResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

public class OrderCallback extends SyncDispatcher {

    private String spOrderId;
    private final HashMap productMap;
    private final String logprefix;
    private final String location = "LalamoveOrderCallback";
    private final String systemTransactionId;
    private final JsonObject jsonBody;

    public OrderCallback(CountDownLatch latch, HashMap config, Object jsonBody, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Lalamove OrderCallback class initiliazed!!", "");
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
        response.resultCode = -1;
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }


    private SpCallbackResult extractResponseBody() {
        SpCallbackResult callbackResult = new SpCallbackResult();
        try {
            String status = jsonBody.get("data").getAsJsonObject().get("order").getAsJsonObject().get("status").getAsString();
            String spOrderId = jsonBody.get("data").getAsJsonObject().get("order").getAsJsonObject().get("orderId").getAsString();
            String spDriverId = jsonBody.get("data").getAsJsonObject().get("order").getAsJsonObject().get("driverId").getAsString();
            callbackResult.spOrderId = spOrderId;
            callbackResult.status = status;
            callbackResult.driverId = spDriverId;
            callbackResult.resultCode = 0;
            LogUtil.info(logprefix, location, "SpOrderId:" + spOrderId + " Status:" + status, "");
        } catch (Exception ex) {
            callbackResult.resultCode = -1;
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return callbackResult;
    }

}