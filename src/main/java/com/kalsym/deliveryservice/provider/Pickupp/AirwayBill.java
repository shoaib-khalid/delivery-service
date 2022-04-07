package com.kalsym.deliveryservice.provider.Pickupp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.AirwayBillResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class AirwayBill extends SyncDispatcher {
    private final String getAirwayBill_url;
    private final DeliveryOrder order;
    private final String logprefix;
    private final String location = "Pickupp";
    private String sslVersion = "SSL";
    private String token;
    private String systemTransactionId;
    private int connectTimeout;
    private int waitTimeout;
    private String airwayBillType;
    private String language;

    public AirwayBill(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {

        super(latch);
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Pickupp AirwayBill class initiliazed!!", "");
        this.getAirwayBill_url = (String) config.get("airwayBillURL");
        this.order = order;
        this.token = (String) config.get("token"); //production token - "c3ltcGxpZmllZEBrYWxzeW0uY29tOjI1NDZmMjNjYjgxM2E5ZThiNjdmMzFhNWQ5MDk4MWVl"
        this.connectTimeout = Integer.parseInt((String) config.get("getairway_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getairway_wait_timeout"));
        this.systemTransactionId = systemTransactionId;
        this.airwayBillType = (String) config.get("airwayBillType");
        this.language = (String) config.get("language");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", token);
        JsonObject requestBody = generateRequestBody();
        String GET_AIRWAY_BILL = this.getAirwayBill_url;

        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, GET_AIRWAY_BILL, httpHeader, requestBody.toString(), this.connectTimeout, this.waitTimeout);
            if (httpResult.httpResponseCode == 200) {


                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                int code = jsonResp.getAsJsonObject("meta").get("code").getAsInt();
                if (code == 200) {
                    JsonObject data = jsonResp.get("data").getAsJsonObject();
                    LogUtil.info(logprefix, location, "Response successful", data.toString());

                    AirwayBillResult airwayBillResult = new AirwayBillResult();
                    airwayBillResult.providerId = order.getDeliveryProviderId();
                    airwayBillResult.orderId = order.getOrderId();
                    airwayBillResult.airwayBillUrl = data.get("link").getAsString();
                    airwayBillResult.consignmentNote = null;
                    response.resultCode = 0;
                    response.returnObject = airwayBillResult;
                } else {
                    LogUtil.info(logprefix, location, "Request failed", "");
                    response.resultCode = -1;
                }
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
                SubmitOrderResult submitOrderResult = new SubmitOrderResult();
                submitOrderResult.resultCode = -1;
                response.returnObject = submitOrderResult;
            }
            LogUtil.info(logprefix, location, "Process finish", "");

        } catch (Exception e) {
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            submitOrderResult.resultCode = -1;
            submitOrderResult.message = e.getMessage();
            response.returnObject = submitOrderResult;
            LogUtil.info(logprefix, location, "Request failed PICKUPP EXCEPTION", e.getMessage());

        }

        return response;
    }


    private JsonObject generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("type", airwayBillType);
        JsonArray array = new JsonArray();
        array.add(order.getSpOrderId());
        jsonReq.add("order_ids", array);
        jsonReq.addProperty("language", language);
        return jsonReq;
    }
}