/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package com.kalsym.deliveryservice.provider.MrSpeedy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.DeliveryZonePriceRepository;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String getprice_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "MrSpeedyGetPrice";
    private Fulfillment fulfillment;
    private DeliveryZonePriceRepository deliveryZonePriceRepository;

    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment, DeliveryZonePriceRepository deliveryZonePriceRepository) {

        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "MrSpeedy GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.getprice_token = (String) config.get("getprice_token");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.fulfillment = fulfillment;
        this.deliveryZonePriceRepository = deliveryZonePriceRepository;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        HashMap httpHeader = new HashMap();
        httpHeader.put("X-DV-Auth-Token", this.getprice_token);
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Connection", "close");


        String requestBody = generateRequestBody();
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.getprice_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode == 0) {
            JsonObject jsonReponse = new Gson().fromJson(httpResult.responseString, JsonObject.class);
//            System.err.println("paeameters warrings :" + jsonReponse.get("warnings").getAsJsonArray());
            if (jsonReponse.get("parameter_warnings").isJsonNull()) {
                LogUtil.info(logprefix, location, "Request successful", "");
                response.resultCode = 0;
                response.returnObject = extractResponseBody(httpResult.responseString);
            } else {
                String invalidResponse = extractErrorValue(jsonReponse.get("parameter_warnings").getAsJsonObject());
                PriceResult result = new PriceResult();
                LogUtil.info(logprefix, location, "Request failed", invalidResponse);

                result.message = invalidResponse;
                result.isError = true;
                result.interval = null;
                response.resultCode = -1;
                response.returnObject = result;
            }
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
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

        String pickupContactNO;
        String deliveryContactNo;
        if (order.getPickup().getPickupContactPhone().startsWith("6")) {
            //national format
            pickupContactNO = order.getPickup().getPickupContactPhone().substring(1);
            deliveryContactNo = order.getDelivery().getDeliveryContactPhone().substring(1);
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Msisdn is national format. New Msisdn:" + pickupContactNO + " & Delivery : " + deliveryContactNo, "");
        } else if (order.getPickup().getPickupContactPhone().startsWith("+6")) {
            pickupContactNO = order.getPickup().getPickupContactPhone().substring(2);
            deliveryContactNo = order.getDelivery().getDeliveryContactPhone().substring(2);
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO + " & Delivery : " + deliveryContactNo, "");
        } else {
            pickupContactNO = order.getPickup().getPickupContactPhone();
            deliveryContactNo = order.getDelivery().getDeliveryContactPhone();
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO + " & Delivery : " + deliveryContactNo, "");
        }

        contactPerson2.addProperty("phone", pickupContactNO);
        contactPerson2.addProperty("name", order.getPickup().getPickupContactName());
        pickupAddress.add("contact_person", contactPerson2);
        addressList.add(pickupAddress);

        JsonObject deliveryAddress = new JsonObject();
        deliveryAddress.addProperty("address", order.getDelivery().getDeliveryAddress());
        JsonObject contactPerson = new JsonObject();
        contactPerson.addProperty("phone", deliveryContactNo);
        contactPerson.addProperty("name", order.getDelivery().getDeliveryContactName());
        deliveryAddress.add("contact_person", contactPerson);
        addressList.add(deliveryAddress);

        jsonReq.add("points", addressList);

        return jsonReq.toString();
    }


    private PriceResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        JsonObject orderObject = jsonResp.get("order").getAsJsonObject();
        String payAmount = orderObject.get("payment_amount").getAsString();
        LogUtil.info(logprefix, location, "Payment Amount:" + payAmount, "");
        PriceResult priceResult = new PriceResult();
        LogUtil.info(logprefix, location, "CHECK : " + jsonResp.get("parameter_warnings"), " ");
        if (jsonResp.get("parameter_warnings") == null) {
            LogUtil.info(logprefix, location, "CHECK : " + jsonResp.get("parameter_warnings"), " ");
            JsonArray failed = jsonResp.get("parameter_warnings").getAsJsonObject().getAsJsonArray("points");
            priceResult.message = failed.get(1).getAsJsonObject().get("address").getAsJsonArray().toString();
            priceResult.isError = true;
        } else {
            priceResult.isError = false;
        }
        BigDecimal bd = new BigDecimal(Double.parseDouble(payAmount));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        priceResult.price = bd;
        priceResult.fulfillment = fulfillment.getFulfillment();
        priceResult.interval = null;
        return priceResult;
    }

    private String extractErrorValue(JsonObject response) {
        JsonObject points = response.getAsJsonArray("points").get(1).getAsJsonObject();
        if (points.has("contact_person")) {
            JsonObject contact = points.getAsJsonObject("contact_person");
            System.err.println("CONTACT " + contact.has("phone"));
            if (contact.has("phone")) {
                return "ERR_INVALID_PHONE_NUMBER";
            } else {
                return "ERR_INVALID_CONTACT_PERSON_DETAILS";
            }
        } else if (points.has("address")) {
            return "ERR_ADDRESS_NOT_FOUND";
        } else {
            return "ERR_OUT_OF_SERVICE_AREA";
        }
    }


}