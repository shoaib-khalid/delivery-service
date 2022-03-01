package com.kalsym.deliveryservice.provider.JnT;

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
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {

    private final String submitOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private String logprefix;
    private String location = "JnTSubmitOrder";
    private String apiKey;
    private String username;
    private String passowrd;
    private String cuscode;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT SubmitOrder class initiliazed!!", "");
        this.submitOrder_url = (String) config.get("submitOrder_url");
        this.apiKey = (String) config.get("apiKey");
        this.cuscode = (String) config.get("cuscode");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        this.order = order;
        this.username = (String) config.get("username");
        this.passowrd = (String) config.get("password");
    }

    @Override
    public ProcessResult process() {

        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        JsonObject requestBody = generateRequestBody();
//        LogUtil.info(logprefix, location, "JnT request body for Submit Order requestBody : " + requestBody, "");

        String data_digest = requestBody.toString().concat(this.apiKey);
//        LogUtil.info(logprefix, location, "JnT request body for Submit Order data_digest : " + data_digest, "");

        String encode_key = "";
        try {

            MessageDigest md = MessageDigest.getInstance("MD5");
            // digest() method is called to calculate message digest
            //  of an input digest() return array of byte
            byte[] messageDigest = md.digest(data_digest.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            String base64Key = Base64.getEncoder().encodeToString(hashtext.getBytes());
            encode_key = base64Key;
            LogUtil.info(logprefix, location, "encode_key :", encode_key);


        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("data_param", requestBody);
        postParameters.add("data_sign", encode_key);
        LogUtil.info(logprefix, location, "JnT Request body for Submit Order  : " + postParameters, "");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(postParameters, headers);

        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responses = restTemplate.exchange(submitOrder_url, HttpMethod.POST, request, String.class);

            int statusCode = responses.getStatusCode().value();
            LogUtil.info(logprefix, location, "Responses", responses.getBody());
            if (statusCode == 200) {
                response.resultCode = 0;
                LogUtil.info(logprefix, location, "JnT Response for Submit Order: " + responses.getBody(), "");
                SubmitOrderResult result = extractResponseBody(responses.getBody());
                if (result.isSuccess) {
                    response.returnObject = extractResponseBody(responses.getBody());
                } else {
                    response.returnObject = extractResponseBody(responses.getBody());
                }
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
            response.returnObject = submitOrderResult;
            LogUtil.info(logprefix, location, "Request failed JNT EXCEPTION", e.getMessage());

        }

        //TODO : handle varity execption

        return response;
    }

    private JsonObject generateRequestBody() {

        String pattern = "yyyy-MM-dd hh:mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        Date startPickScheduleDate = null;
        Date endPickScheduleDate = null;
        try {
            if (order.getPickup().getPickupDate() != null) {
                startPickScheduleDate = simpleDateFormat.parse(order.getPickup().getPickupDate() + " " + order.getPickup().getPickupTime() + ":00");
            }
            if (order.getPickup().getEndPickupDate() != null) {
                endPickScheduleDate = simpleDateFormat.parse(order.getPickup().getEndPickupDate() + " " + order.getPickup().getEndPickupTime() + ":00");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        JsonObject jsonReq = new JsonObject();
        JsonArray detailsArray = new JsonArray();
        JsonObject details = new JsonObject();
        details.addProperty("username", this.username);
        details.addProperty("api_key", this.passowrd);
        details.addProperty("cuscode", this.cuscode);
        details.addProperty("orderid", systemTransactionId);
        details.addProperty("shipper_contact", order.getPickup().getPickupContactName());
        details.addProperty("shipper_name", order.getPickup().getPickupContactName());
        details.addProperty("shipper_phone", order.getPickup().getPickupContactPhone());
        details.addProperty("shipper_addr", order.getPickup().getPickupAddress());
        details.addProperty("sender_zip", order.getPickup().getPickupPostcode());
        details.addProperty("receiver_name", order.getDelivery().getDeliveryContactName());
        details.addProperty("receiver_addr", order.getDelivery().getDeliveryAddress());
        details.addProperty("receiver_phone", order.getDelivery().getDeliveryContactPhone());
        details.addProperty("receiver_zip", order.getDelivery().getDeliveryPostcode());
//        if (order.getPieces() == 0) {
        details.addProperty("qty", "1");
//        } else {
//            details.addProperty("qty", order.getPieces().toString());
//        }
        order.setServiceType(true);
        details.addProperty("weight", order.getTotalWeightKg().toString());
        details.addProperty("item_name", order.getProductCode());
        details.addProperty("goodsdesc", order.getShipmentContent());
        details.addProperty("goodsvalue", order.getShipmentValue());
        details.addProperty("payType", "PP_PM");
        details.addProperty("expresstype", "EZ");
        details.addProperty("goodType", order.getItemType().name());
        if (!order.getServiceType()) {
            LogUtil.info(logprefix, location, "This order is Pickup", "");
            details.addProperty("servicetype", "1");
        } else {
            LogUtil.info(logprefix, location, "This order is Dropoff", "");
            details.addProperty("servicetype", "6");
        }
//        if (startPickScheduleDate != null) {
//            details.addProperty("sendstarttime", startPickScheduleDate.toString());
//        }
//        if (endPickScheduleDate != null) {
//            details.addProperty("sendendtime", endPickScheduleDate.toString());
//        }
        details.addProperty("offerFeeFlag", order.isInsurance());
        detailsArray.add(details);
        jsonReq.add("detail", detailsArray);

        return jsonReq;
    }

    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            LogUtil.info(logprefix, location, "the json resp for submitOrder " + jsonResp, "");
            JsonArray dataArray = jsonResp.get("details").getAsJsonArray();

            JsonObject detailsData = dataArray.get(0).getAsJsonObject();
            String awbNo1 = detailsData.get("awb_no").getAsString();
            LogUtil.info(logprefix, location, "the awb number for submitOrder " + awbNo1, "");
            String status = detailsData.get("status").getAsString();
            LogUtil.info(logprefix, location, "the status for submitOrder " + status, "");
            if (status.equalsIgnoreCase("success")) {
                LogUtil.info(logprefix, location, "inside submitOrder if", "");
                // getting spOrderId
                String awbNo = detailsData.get("awb_no").getAsString();
                LogUtil.info(logprefix, location, "the awb number for submitOrder " + awbNo, "");
                JsonObject data = detailsData.get("data").getAsJsonObject();
                DeliveryOrder orderCreated = new DeliveryOrder();
                orderCreated.setSpOrderId(awbNo);

                orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
                submitOrderResult.orderCreated = orderCreated;
                submitOrderResult.isSuccess = true;
                submitOrderResult.resultCode = 0;
            } else {
                submitOrderResult.isSuccess = false;
                submitOrderResult.resultCode = -1;
                submitOrderResult.message = detailsData.get("msg").getAsString();
                LogUtil.info(logprefix, location, "Request failed", "");
            }
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }
}
