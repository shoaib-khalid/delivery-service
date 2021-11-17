package com.kalsym.deliveryservice.provider.TCS;

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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {
    private final String baseUrl;
    private final String endpointUrl;
    private final String submitOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "TCSSubmitOrder";
    private String secretKey;
    private String apiKey;
    private String spOrderId;
    private String driverId;
    private String shareLink;
    private String status;

    private String username;
    private String passowrd ;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "TCS SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.submitOrder_url = "https://api.tcscourier.com/sandbox/v1/cod/create-order";
        this.secretKey = (String) config.get("secretKey");
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.username = (String) config.get("username");
        this.passowrd = (String) config.get("password");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        RestTemplate restTemplate = new RestTemplate();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("X-IBM-Client-Id", "e6966d77-3b34-4594-b84d-612a66adb01d");
        String requestBody = generateBody();

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, submitOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 200) {
            response.resultCode = 0;
            LogUtil.info(logprefix, location, "TCS Response for Submit Order: " + httpResult.responseString, "");
            response.returnObject = extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");

        return response;
    }

    private String generateBody() {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("userName", username);
        jsonRequest.addProperty("password", passowrd);
        jsonRequest.addProperty("costCenterCode", "1123");
        jsonRequest.addProperty("consigneeName", order.getPickup().getPickupContactName());
        jsonRequest.addProperty("consigneeAddress", order.getPickup().getPickupAddress());
        jsonRequest.addProperty("consigneeMobNo", order.getPickup().getPickupContactPhone());
        jsonRequest.addProperty("consigneeEmail", order.getPickup().getPickupContactEmail());
        jsonRequest.addProperty("originCityName", "Karachi"); //TODO : SAFE IN DB
        jsonRequest.addProperty("destinationCityName", "Lahore");
        jsonRequest.addProperty("weight", order.getTotalWeightKg());
        jsonRequest.addProperty("pieces", 1);
        jsonRequest.addProperty("codAmount", 231.20);
        jsonRequest.addProperty("customerReferenceNo", order.getOrderId());
        jsonRequest.addProperty("services", "O");
        jsonRequest.addProperty("fragile", "yes");
        jsonRequest.addProperty("remarks", order.getPickup().getRemarks());
        if (order.isInsurance() == true) {
            jsonRequest.addProperty("insuranceValue", 1);
        } else {
            jsonRequest.addProperty("insuranceValue", 0);
        }
        return jsonRequest.toString();
    }

    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject returnStatus = jsonResp.get("returnStatus").getAsJsonObject();
            String message = returnStatus.get("message").getAsString();
            String status = returnStatus.get("status").getAsString();
            String code = returnStatus.get("code").getAsString();

            DeliveryOrder orderCreated = new DeliveryOrder();
            if (code == "0200") {
                JsonObject bookingReply = jsonResp.get("bookingReply").getAsJsonObject();
                String consignmentNote = bookingReply.get("result").getAsString();
                String extractedCN = consignmentNote.substring(21);

                orderCreated.setSpOrderId(extractedCN);
                orderCreated.setSpOrderName(extractedCN);
                orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
                orderCreated.setStatus(status);
                submitOrderResult.orderCreated = orderCreated;
                submitOrderResult.isSuccess = true;

                LogUtil.info(logprefix, location, "Consignment note for TCS: " + extractedCN, "");
            } else if (code == "0400") {

                submitOrderResult.providerId = orderCreated.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

                LogUtil.info(logprefix, location, "TCS: Bad Request / Custom validation message. Message: " + message, "");
            } else if (code == "0404") {
                submitOrderResult.providerId = orderCreated.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

                LogUtil.info(logprefix, location, "TCS: Data Not Found.", "");
            } else if (code == "0408") {
                submitOrderResult.providerId = orderCreated.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

                LogUtil.info(logprefix, location, "TCS: The server is taking too long to respond, please try later.", "");
            } else {
                LogUtil.info(logprefix, location, "TCS: An internal error has occurred.", "");
                submitOrderResult.providerId = orderCreated.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}
