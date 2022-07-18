package com.kalsym.deliveryservice.provider.Bykea;

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
    private final String location = "BykeaOrderCallback";
    private final String systemTransactionId;
    private final JsonObject jsonBody;
    private String spOrderId;

    public OrderCallback(CountDownLatch latch, HashMap config, Object jsonBody, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Bykea OrderCallback class initialized!!", "");
        productMap = (HashMap) config.get("productCodeMapping");
        Gson gson = new Gson();
        String jsonString = gson.toJson(jsonBody, LinkedHashMap.class);
        LogUtil.info(logprefix, location, "Bykea Callback Body:" + jsonString, "");
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
            String status = jsonBody.get("event").getAsString();
            String spOrderId = jsonBody.get("data").getAsJsonObject().get("trip_id").getAsString();
            String driverId = "";
            String riderName = "";
            String riderPhone = "";
            String carNoPlate = "";
            String trackingLink = "";
            String systemStatus = "";
            switch (status) {
                case "booking.created":
                case "booking.opened":
                    systemStatus = DeliveryCompletionStatus.ASSIGNING_RIDER.name();
                    break;
                case "booking.accepted":
                case "booking.arrived":
                case "booking.updated.trackinglink":
//                    driverId = jsonBody.get("data").getAsJsonObject().get("trip_id").getAsString();
                    try {
                        trackingLink = jsonBody.get("data").getAsJsonObject().get("tracking_link").getAsString();
                    } catch (Exception ex) {
                        LogUtil.info(logprefix, location, "SpOrderId: " + spOrderId, "Exception Get Tracking Url: " + ex.getMessage());
                    }
                    try {
                        riderName = jsonBody.get("data").getAsJsonObject().get("partner").getAsJsonObject().get("name").getAsString();
                    } catch (Exception ex) {
                        LogUtil.info(logprefix, location, "SpOrderId: " + spOrderId, "Exception Get Name: " + ex.getMessage());
                    }
                    try {
                        riderPhone = jsonBody.get("data").getAsJsonObject().get("partner").getAsJsonObject().get("mobile").getAsString();
                    } catch (Exception ex) {
                        LogUtil.info(logprefix, location, "SpOrderId: " + spOrderId, "Exception Get Phone: " + ex.getMessage());
                    }
                    try {
                        carNoPlate = jsonBody.get("data").getAsJsonObject().get("partner").getAsJsonObject().get("plate_no").getAsString();
                    } catch (Exception ex) {
                        LogUtil.info(logprefix, location, "SpOrderId: " + spOrderId, "Exception Get Plate No : " + ex.getMessage());
                    }
                    systemStatus = DeliveryCompletionStatus.AWAITING_PICKUP.name();
                    break;
                case "booking.started":
                case "courier_arrived":
                    systemStatus = DeliveryCompletionStatus.BEING_DELIVERED.name();
                    break;
                case "booking.finished":
                case "booking.feedback.partner":
                    systemStatus = DeliveryCompletionStatus.COMPLETED.name();
                    break;
                case "booking.expired":
                case "booking.cancelled.partner":
                    systemStatus = DeliveryCompletionStatus.CANCELED.name();
                    break;
            }

            callbackResult.spOrderId = spOrderId;
            callbackResult.status = status;
            callbackResult.trackingUrl = trackingLink;
            callbackResult.systemStatus = systemStatus;
            callbackResult.riderName = riderName;
            callbackResult.riderPhone = riderPhone;
            callbackResult.driveNoPlate = carNoPlate;
            LogUtil.info(logprefix, location, "SpOrderId: " + spOrderId + " Status: " + status + " Rider Id : " + driverId, "");
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return callbackResult;
    }

}
