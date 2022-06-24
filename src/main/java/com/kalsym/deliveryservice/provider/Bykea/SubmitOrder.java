package com.kalsym.deliveryservice.provider.Bykea;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
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


    private final String submitOrderUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private String logprefix;
    private String location = "BykeaSubmitOrder";

    private String spOrderId;

    private final String auth_url;
    private final String username;
    private final String password;
    private final String paymentCode;
    private final String serviceCode;
    private final String reference;
    private String userAgent;


    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Bykea SubmitOrder class initiliazed!!", "");

        this.submitOrderUrl = (String) config.get("submitOrderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));

        this.auth_url = (String) config.get("authUrl");
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.paymentCode = (String) config.get("paymentCode");
        this.serviceCode = (String) config.get("serviceCode");
        this.reference = (String) config.get("reference");
        this.userAgent = (String) config.get("userAgent");


        this.order = order;
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");


        LogUtil.info(logprefix, location, "Confirm Delivery ", submitOrderUrl);

        String authToken = getToken();
        LogUtil.info(logprefix, location, "Get Token : {", authToken + "}");


        String requestBody = generateRequestBody();

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("x-api-customer-token", authToken);
        httpHeader.put("User-Agent", userAgent);

        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, submitOrderUrl, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
            int statusCode = httpResult.httpResponseCode;

            if (statusCode == 200) {
                response.resultCode = 0;
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
                LogUtil.info(logprefix, location, "RESPONSE CODE : ", jsonResp.get("message").getAsString());
                SubmitOrderResult submitOrderResult = new SubmitOrderResult();
                submitOrderResult.message = jsonResp.get("message").getAsString();
                submitOrderResult.resultCode = -1;
                response.returnObject = submitOrderResult;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception e) {
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            submitOrderResult.resultCode = -1;
            response.returnObject = submitOrderResult;
            LogUtil.info(logprefix, location, "Request failed", e.getMessage());

        }

        return response;
    }

    private String generateRequestBody() {
        JsonObject req = new JsonObject();
        JsonObject meta = new JsonObject();
        JsonObject customer = new JsonObject();
        JsonObject pickup = new JsonObject();
        JsonArray bookings = new JsonArray();
        JsonObject booking = new JsonObject();
        JsonObject inMeta = new JsonObject();
        JsonObject dropoff = new JsonObject();
        JsonObject details = new JsonObject();

        meta.addProperty("service_code", serviceCode);
        customer.addProperty("phone", order.getDelivery().getDeliveryContactPhone());
        pickup.addProperty("name", order.getPickup().getPickupContactName());
        pickup.addProperty("phone", order.getPickup().getPickupContactPhone());
        pickup.addProperty("lat", order.getPickup().getLatitude());
        pickup.addProperty("lng", order.getPickup().getLongitude());
        pickup.addProperty("address", order.getPickup().getPickupAddress());
        pickup.addProperty("gps_address", order.getPickup().getPickupAddress());

        inMeta.addProperty("service_code", paymentCode);
        dropoff.addProperty("name", order.getDelivery().getDeliveryContactName());
        dropoff.addProperty("phone", order.getDelivery().getDeliveryContactPhone());
        dropoff.addProperty("lat", order.getDelivery().getLatitude());
        dropoff.addProperty("lng", order.getDelivery().getLongitude());
        dropoff.addProperty("address", order.getDelivery().getDeliveryAddress());
        dropoff.addProperty("gps_address", order.getDelivery().getDeliveryAddress());
        if (order.getRemarks() != null) {
            details.addProperty("voice_note", order.getRemarks());

        } else {
            details.addProperty("voice_note", order.getOrderId());
        }
        details.addProperty("parcel_value", order.getCodAmount());
        details.addProperty("reference", reference);
        details.addProperty("insurance", true);

        booking.add("meta", inMeta);
        booking.add("dropoff", dropoff);
        booking.add("details", details);
        bookings.add(booking);
        req.add("meta", meta);
        req.add("customer", customer);
        req.add("pickup", pickup);
        req.add("bookings", bookings);
        return req.toString();
    }


    private SubmitOrderResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String message = jsonResp.get("message").getAsString();

        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            String transactionId = jsonResp.get("data").getAsJsonObject().get("bookings").getAsJsonArray().get(0).getAsJsonObject().get("booking_id").getAsString();
            String spOrderName = jsonResp.get("data").getAsJsonObject().get("bookings").getAsJsonArray().get(0).getAsJsonObject().get("booking_no").getAsString();
            LogUtil.info(logprefix, location, "the json resp for submitOrder " + jsonResp, "");
            LogUtil.info(logprefix, location, "OrderNumber:" + spOrderId, "");

            //extract order create
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(transactionId);
            orderCreated.setSpOrderName(spOrderName);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setCustomerTrackingUrl("");
            orderCreated.setStatus(DeliveryCompletionStatus.ASSIGNING_DRIVER.name());
            orderCreated.setStatusDescription(message);
            orderCreated.setStatus("ASSIGNING_DRIVER");

            submitOrderResult.orderCreated = orderCreated;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }

    private String getToken() {

        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.addProperty("password", this.password);

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("User-Agent", userAgent);


        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.auth_url, httpHeader, object.toString(), this.connectTimeout, this.waitTimeout);
        LogUtil.info(logprefix, location, "Response : ", httpResult.responseString);

        if (httpResult.httpResponseCode == 200) {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            String token = jsonResp.get("data").getAsJsonObject().get("token").getAsString();

            LogUtil.info(logprefix, location, "Request successful token : ", token);
            return token;
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
        }
        LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");
        return "";
    }
}

