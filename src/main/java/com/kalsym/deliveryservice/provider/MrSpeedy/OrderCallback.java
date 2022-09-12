/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SpCallbackResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

public class OrderCallback extends SyncDispatcher {

    private final HashMap productMap;
    private final String logprefix;
    private final String location = "MrSpeedyOrderCallback";
    private final String systemTransactionId;
    private final JsonObject jsonBody;
    private String spOrderId;

    public OrderCallback(CountDownLatch latch, HashMap config, Object jsonBody, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy OrderCallback class initialized!!", "");
        productMap = (HashMap) config.get("productCodeMapping");
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonBody, LinkedHashMap.class);
        LogUtil.info(logprefix, location, "Request Body:" + jsonString, "");
        this.jsonBody = new Gson().fromJson(jsonString, JsonObject.class);
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        response.returnObject = extractResponseBody();
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }


    private SpCallbackResult extractResponseBody() {
        SpCallbackResult callbackResult = new SpCallbackResult();
        try {
            String status = jsonBody.get("delivery").getAsJsonObject().get("status").getAsString();
            String spOrderId = jsonBody.get("delivery").getAsJsonObject().get("order_id").getAsString();
            String driverId = "";
            String systemStatus = "";

            String riderName = "";
            String riderPhone = "";
            String carNoPlate = "";
            String trackingLink = "";
            switch (status) {
                case "new":
                case "available":
                    systemStatus = DeliveryCompletionStatus.ASSIGNING_RIDER.name();
                    break;
                case "courier_assigned":
                case "courier_departed":
                    driverId = jsonBody.get("delivery").getAsJsonObject().get("courier").getAsJsonObject().get("courier_id").getAsString();
                    systemStatus = DeliveryCompletionStatus.AWAITING_PICKUP.name();
                    break;
                case "parcel_picked_up":
                case "courier_arrived":
                    systemStatus = DeliveryCompletionStatus.BEING_DELIVERED.name();
                    break;
                case "completed":
                    systemStatus = DeliveryCompletionStatus.COMPLETED.name();
                    break;
                case "canceled":
                    systemStatus = DeliveryCompletionStatus.CANCELED.name();
                    break;
            }


            callbackResult.spOrderId = spOrderId;
            callbackResult.status = status;
            callbackResult.driverId = driverId;
            callbackResult.systemStatus = systemStatus;
            callbackResult.trackingUrl = trackingLink;
            callbackResult.riderName = riderName;
            callbackResult.riderPhone = riderPhone;
            callbackResult.driveNoPlate = carNoPlate;
            callbackResult.resultCode=0;
            LogUtil.info(logprefix, location, "SpOrderId: " + spOrderId + " Status: " + status + " Rider Id : " + driverId, "");
        } catch (Exception ex) {
            callbackResult.resultCode =-1;
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return callbackResult;
    }

}