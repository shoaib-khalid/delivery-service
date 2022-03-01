/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
    private final String submitOrder_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "MrSpeedySubmitOrder";

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy SubmitOrder class initiliazed!!", "");
        this.submitOrder_url = (String) config.get("submitorder_url");
        this.submitOrder_token = (String) config.get("submitorder_token");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-DV-Auth-Token", this.submitOrder_token);
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.submitOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode == 0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            submitOrderResult.resultCode =-1;
            response.returnObject = submitOrderResult;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();

        jsonReq.addProperty("matter", order.getItemType().name());
        jsonReq.addProperty("total_weight_kg", order.getTotalWeightKg());
        jsonReq.addProperty("vehicle_type_id", VehicleType.valueOf(order.getPickup().getVehicleType().name()).getCode());
        JsonArray addressList = new JsonArray();

        JsonObject pickupAddress = new JsonObject();
        pickupAddress.addProperty("address", order.getPickup().getPickupAddress());
        JsonObject contactPerson2 = new JsonObject();
        contactPerson2.addProperty("phone", order.getPickup().getPickupContactPhone());
        contactPerson2.addProperty("name", order.getPickup().getPickupContactName());
        pickupAddress.add("contact_person", contactPerson2);
        addressList.add(pickupAddress);

        JsonObject deliveryAddress = new JsonObject();
        deliveryAddress.addProperty("address", order.getDelivery().getDeliveryAddress());
        JsonObject contactPerson = new JsonObject();
        contactPerson.addProperty("phone", order.getDelivery().getDeliveryContactPhone());
        contactPerson.addProperty("name", order.getDelivery().getDeliveryContactName());
        deliveryAddress.add("contact_person", contactPerson);
        addressList.add(deliveryAddress);

        jsonReq.add("points", addressList);

        return jsonReq.toString();
    }


    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            boolean isSuccess = jsonResp.get("is_successful").getAsBoolean();
            JsonObject orderObject = jsonResp.get("order").getAsJsonObject();
            LogUtil.info(logprefix, location, "isSuccess:" + isSuccess, "");
            submitOrderResult.isSuccess = isSuccess;
            //extract order created
            String orderId = orderObject.get("order_id").getAsString();
            String orderName = orderObject.get("order_name").getAsString();
            String created = orderObject.get("created_datetime").getAsString();
            VehicleType vehicleType = VehicleType.valueOf(orderObject.get("vehicle_type_id").getAsInt());
            String merchantTrackingUrl = orderObject.getAsJsonArray("points").get(0).getAsJsonObject().get("tracking_url").getAsString();
            String customerTrackingUrl = orderObject.getAsJsonArray("points").get(1).getAsJsonObject().get("tracking_url").getAsString();
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(orderId);
            orderCreated.setSpOrderName(orderName);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setVehicleType(vehicleType.toString());
            orderCreated.setMerchantTrackingUrl(merchantTrackingUrl);
            orderCreated.setCustomerTrackingUrl(customerTrackingUrl);
            submitOrderResult.orderCreated = orderCreated;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }

}