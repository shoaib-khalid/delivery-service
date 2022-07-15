package com.kalsym.deliveryservice.provider.Swyft;

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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {
    private final String baseUrl;
    private final String api_key;
    private final String submit_order_endpoint;
    private final String vendorId;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private String logprefix;
    private String location = "SwyftSubmitOrder";
    private String packagingType;

    private String timeSlotId;
    private String createPickupReqUrl;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Swyft SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("base_url");
        this.api_key = (String) config.get("api_key");
        this.submit_order_endpoint = (String) config.get("submit_order_endpoint");
        this.vendorId = (String) config.get("vendorId");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.packagingType = (String) config.get("packagingType");
        this.order = order;
        this.timeSlotId = (String) config.get("timeSlotId");
        this.createPickupReqUrl = (String) config.get("createPickupReqUrl");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", api_key);
        String requestBody = generateBody();
        try {
            HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, baseUrl + vendorId + submit_order_endpoint, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
            if (httpResult.httpResponseCode == 200) {
                response.resultCode = 0;
                LogUtil.info(logprefix, location, "Swyft Response for Submit Order: " + httpResult.responseString, "");
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                SubmitOrderResult submitOrderResult = new SubmitOrderResult();
                submitOrderResult.resultCode = -1;
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
            LogUtil.info(logprefix, location, "Request failed Swyft EXCEPTION : ", e.getMessage());

        }
        return response;
    }

    private String generateBody() {
        LogUtil.info(logprefix, location, "OrderId : ", order.getOrderId());

        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("ORDER_ID", order.getOrderId());
        if (order.getPaymentType().equals("COD")) {
            LogUtil.info(logprefix, location, "Total Cod Value Body: ", String.valueOf(order.getOrderAmount() + order.getShipmentValue()));
            jsonRequest.addProperty("ORDER_TYPE", "COD");
            jsonRequest.addProperty("COD", order.getOrderAmount());
        } else {
            jsonRequest.addProperty("ORDER_TYPE", "NONCOD");
            jsonRequest.addProperty("COD", 0);
        }
        jsonRequest.addProperty("SHIPPER_ADDRESS_ID", order.getPickup().getCostCenterCode());
        jsonRequest.addProperty("CONSIGNEE_FIRST_NAME", order.getDelivery().getDeliveryContactName());
        jsonRequest.addProperty("CONSIGNEE_LAST_NAME", "");
        jsonRequest.addProperty("CONSIGNEE_ADDRESS", order.getDelivery().getDeliveryAddress());
        jsonRequest.addProperty("CONSIGNEE_PHONE", order.getDelivery().getDeliveryContactPhone());
        jsonRequest.addProperty("CONSIGNEE_EMAIL", order.getDelivery().getDeliveryContactEmail());
        jsonRequest.addProperty("ORIGIN_CITY", order.getPickup().getPickupCity());
        jsonRequest.addProperty("CONSIGNEE_CITY", order.getDelivery().getDeliveryCity());
        jsonRequest.addProperty("WEIGHT", order.getTotalWeightKg());
        jsonRequest.addProperty("PIECES", order.getPieces());
        jsonRequest.addProperty("DESCRIPTION", "Testing");
        jsonRequest.addProperty("PACKAGING", packagingType);

        LogUtil.info(logprefix, location, "Request Body: ", jsonRequest.toString());
        JsonArray array = new JsonArray();
        array.add(jsonRequest);

        return array.toString();
    }

    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        String spOrderName = "";
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject result = jsonResp.getAsJsonArray("data").get(0).getAsJsonObject();
            DeliveryOrder orderCreated = new DeliveryOrder();
            String parcelId = result.get("parcelId").getAsString();

            JsonArray array = new JsonArray();
            JsonObject createPickup = new JsonObject();

            String pattern = "yyyy-MM-dd";
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            String date = simpleDateFormat.format(new Date());

            createPickup.addProperty("estimatedParcels", order.getTotalParcel());
            createPickup.addProperty("timeSlotId", this.timeSlotId);
            createPickup.addProperty("date", date);
            createPickup.addProperty("pickupLocationId", order.getPickup().getCostCenterCode());
            array.add(createPickup);


            HashMap httpHeader = new HashMap();
            httpHeader.put("Content-Type", "application/json");
            httpHeader.put("Authorization", api_key);

            LogUtil.info(logprefix, location, "Request Create Pickup : ", array.toString());
            try {
                HttpResult responsePickup = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, baseUrl + vendorId + createPickupReqUrl, httpHeader, array.toString(), this.connectTimeout, this.waitTimeout);
                if (responsePickup.httpResponseCode == 200) {
                    JsonObject pickupReqRes = new Gson().fromJson(responsePickup.responseString, JsonObject.class);
                    LogUtil.info(logprefix, location, "Response  Create Pickup : ", pickupReqRes.toString());
                    spOrderName = pickupReqRes.get("id").getAsString();

                } else {
                    LogUtil.info(logprefix, location, "Request failed", "");
                }
                LogUtil.info(logprefix, location, "Process finish", "");
            } catch (Exception e) {
                LogUtil.info(logprefix, location, "Request failed Swyft EXCEPTION : ", e.getMessage());
            }

            orderCreated.setSpOrderId(parcelId);
            orderCreated.setSpOrderName(spOrderName);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setStatus("ASSIGNING_DRIVER");
            submitOrderResult.orderCreated = orderCreated;
            submitOrderResult.isSuccess = true;
            submitOrderResult.resultCode = 0;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
            submitOrderResult.isSuccess = false;
            submitOrderResult.resultCode = -1;
            submitOrderResult.message = ex.getMessage();
        }
        return submitOrderResult;
    }

}
