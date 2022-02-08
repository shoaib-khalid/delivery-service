package com.kalsym.deliveryservice.provider.Pickupp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {

    private final String submitOrder_url;
    private final String trackingUrl;
    private final String baseUrl;
    private final String token;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "PickuppSubmitOrder ";


    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Pickupp SubmitOrder class initiliazed!!", "");
        this.submitOrder_url = (String) config.get("submitorder_url");
        this.baseUrl = (String) config.get("domainUrl");
        this.token = (String) config.get("token");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.trackingUrl = (String) config.get("trackingUrl");
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", token);
        String requestBody = generateRequestBody();
        String SUBMIT_ORDER_URL = this.baseUrl + this.submitOrder_url;
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, SUBMIT_ORDER_URL, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode == 0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();

        jsonReq.addProperty("pickup_contact_person", "John");
        jsonReq.addProperty("pickup_contact_phone", "55555252");
        jsonReq.addProperty("pickup_address_line_1", "香港銅鑼灣勿地臣街1號 Time Square");
        jsonReq.addProperty("pickup_time", "2022-02-07T18:00:00+08:00");
        jsonReq.addProperty("pickup_zip_code", "999077");
        jsonReq.addProperty("pickup_city", "Kowloon City");
        jsonReq.addProperty("pickup_notes", "1x Red Pen");
        jsonReq.addProperty("dropoff_contact_person", "Christine");
        jsonReq.addProperty("dropoff_contact_phone", "55555928");
        jsonReq.addProperty("dropoff_address_line_1", "荔枝角青山道500號百美工業大廈");
        jsonReq.addProperty("dropoff_zip_code", "518000");
        jsonReq.addProperty("dropoff_city", "Kwai Chung");
        jsonReq.addProperty("dropoff_notes", "Make sure to call recipient before dropoff");
        jsonReq.addProperty("region", "HK");
        jsonReq.addProperty("length", "10");
        jsonReq.addProperty("width", 20);
        jsonReq.addProperty("height", 30);
        jsonReq.addProperty("weight", 05);
        jsonReq.addProperty("origin", "API");
        jsonReq.addProperty("client_reference_number", "AW2223456MY");
        jsonReq.addProperty("enforce_validation", true);
        jsonReq.addProperty("service_type", "express");
        jsonReq.addProperty("service_time", "120");
        jsonReq.addProperty("item_name", "apple*2");
        jsonReq.addProperty("is_pickupp_care", false);


        return jsonReq.toString();
    }


    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject data = jsonResp.get("data").getAsJsonObject();
            JsonObject meta = jsonResp.get("meta").getAsJsonObject();
            if (meta.get("code").getAsInt() == 201) {

                LogUtil.info(logprefix, location, "isSuccess: " + respString, "");
                submitOrderResult.isSuccess = true;
                //extract order created
                String orderId = data.get("order_number").getAsString();

                String orderName = data.get("id").getAsString();
                String created = data.get("created_at").getAsString();
                String customerTrackingUrl = trackingUrl + orderId;
                DeliveryOrder orderCreated = new DeliveryOrder();
                orderCreated.setSpOrderId(orderId);
                orderCreated.setSpOrderName(orderName);
                orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
                orderCreated.setCustomerTrackingUrl(customerTrackingUrl);
                submitOrderResult.orderCreated = orderCreated;
            } else {

            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}
