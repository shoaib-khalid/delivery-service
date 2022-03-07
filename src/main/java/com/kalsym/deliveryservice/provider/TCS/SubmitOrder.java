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
    private String logprefix;
    private String location = "TCSSubmitOrder";

    private String clientId;

    private String username;
    private String password;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "TCS SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.submitOrder_url = (String) config.get("submitOrder_url");
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.order = order;
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.clientId = (String) config.get("clientId");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("X-IBM-Client-Id", clientId);
        String requestBody = generateBody();
        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, submitOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);

            if (httpResult.httpResponseCode == 200) {
                response.resultCode = 0;
                LogUtil.info(logprefix, location, "TCS Response for Submit Order: " + httpResult.responseString, "");
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                SubmitOrderResult submitOrderResult = new SubmitOrderResult();
                submitOrderResult.resultCode =-1;
                response.returnObject = submitOrderResult;

                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception e) {
            response.resultCode = -1;
            SubmitOrderResult submitOrderResult = new SubmitOrderResult();
            submitOrderResult.resultCode = -1;
            submitOrderResult.message = e.getMessage();
            response.returnObject = submitOrderResult;
            LogUtil.info(logprefix, location, "Request failed TCS EXCEPTION : ", e.getMessage());

        }
        return response;
    }

    private String generateBody() {

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("userName", username);
        jsonRequest.addProperty("password", password);
        jsonRequest.addProperty("costCenterCode", order.getPickup().getCostCenterCode());
        jsonRequest.addProperty("consigneeName", order.getPickup().getPickupContactName());
        jsonRequest.addProperty("consigneeAddress", order.getPickup().getPickupAddress());
        jsonRequest.addProperty("consigneeMobNo", order.getPickup().getPickupContactPhone());
        jsonRequest.addProperty("consigneeEmail", order.getPickup().getPickupContactEmail());
        jsonRequest.addProperty("originCityName", order.getPickup().getPickupCity());
        jsonRequest.addProperty("destinationCityName", order.getDelivery().getDeliveryCity());
        jsonRequest.addProperty("weight", order.getTotalWeightKg());
        jsonRequest.addProperty("pieces", 1);
        jsonRequest.addProperty("codAmount", order.getShipmentValue());
        jsonRequest.addProperty("customerReferenceNo", order.getOrderId());
        jsonRequest.addProperty("services", "O"); // incase future need this need to handle
        jsonRequest.addProperty("fragile", "yes");
        jsonRequest.addProperty("remarks", order.getPickup().getRemarks());
        if (order.isInsurance() == true) {
            jsonRequest.addProperty("insuranceValue", 1);
        } else {
            jsonRequest.addProperty("insuranceValue", 0);
        }
        LogUtil.info(logprefix, location, "Request Body: ", jsonRequest.toString());

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
            if (code.equals("0200")) {
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
            } else if (code.equals("0400")) {

                submitOrderResult.deliveryProviderId = order.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

                LogUtil.info(logprefix, location, "TCS: Bad Request / Custom validation message. Message: " + message, "");
            } else if (code.equals("0404")) {
                submitOrderResult.deliveryProviderId = order.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

                LogUtil.info(logprefix, location, "TCS: Data Not Found.", "");
            } else if (code.equals("0408")) {
                submitOrderResult.deliveryProviderId = order.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

                LogUtil.info(logprefix, location, "TCS: The server is taking too long to respond, please try later.", "");
            } else {
                LogUtil.info(logprefix, location, "TCS: An internal error has occurred.", "");
                submitOrderResult.deliveryProviderId = order.getDeliveryProviderId();
                submitOrderResult.isSuccess = false;
                submitOrderResult.message = message;

            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}
