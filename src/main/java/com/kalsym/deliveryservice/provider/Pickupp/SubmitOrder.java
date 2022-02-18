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
    private String origin = "API";
    private String logprefix;
    private String serviceType;

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
        this.origin = (String) config.get("origin");
        this.serviceType = (String) config.get("serviceType");

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
        if (httpResult.httpResponseCode == 201) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            response.returnObject = submitOrderResult;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        String[] types = serviceType.split(";");
        String typeValue = "";
        for (String type : types) {
            String[] t = type.split("=");
            if (t[0].equals(order.getDeliveryType().toLowerCase())) {
                typeValue = t[1];
            }
        }

        jsonReq.addProperty("pickup_contact_person",/* "John"*/order.getPickup().getPickupContactName());
        jsonReq.addProperty("pickup_contact_phone", /*"55555252"*/order.getPickup().getPickupContactPhone());
        jsonReq.addProperty("pickup_address_line_1", /*"香港銅鑼灣勿地臣街1號 Time Square"*/ order.getPickup().getPickupAddress());
        jsonReq.addProperty("pickup_time", "2022-02-07T18:00:00+08:00");
        jsonReq.addProperty("pickup_zip_code", order.getPickup().getPickupPostcode());
        jsonReq.addProperty("pickup_city", order.getPickup().getPickupCity());
        jsonReq.addProperty("pickup_notes", order.getRemarks());
        jsonReq.addProperty("dropoff_contact_person", order.getDelivery().getDeliveryContactName());
        jsonReq.addProperty("dropoff_contact_phone", order.getDelivery().getDeliveryContactPhone());
        jsonReq.addProperty("dropoff_address_line_1", order.getDelivery().getDeliveryAddress());
        jsonReq.addProperty("dropoff_zip_code", order.getDelivery().getDeliveryPostcode());
        jsonReq.addProperty("dropoff_city", order.getDelivery().getDeliveryCity());
        jsonReq.addProperty("dropoff_notes", order.getRemarks());
        jsonReq.addProperty("region", order.getRegionCountry());
        jsonReq.addProperty("length", "");
        jsonReq.addProperty("width", "");
        jsonReq.addProperty("height", "");
        jsonReq.addProperty("weight", order.getTotalWeightKg());
        jsonReq.addProperty("origin", this.origin);
        jsonReq.addProperty("client_reference_number", systemTransactionId);
        jsonReq.addProperty("enforce_validation", true);
        jsonReq.addProperty("service_type", order.getDeliveryType().toLowerCase());
        jsonReq.addProperty("service_time", typeValue);
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
                LogUtil.info(logprefix, location, "Request failed", "");
/*
                response.resultCode = -1;
*/
            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}
