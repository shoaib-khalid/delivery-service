package com.kalsym.deliveryservice.provider.JnT;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.AirwayBillResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.DatatypeConverter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class AirwayBill extends SyncDispatcher {
    private final String getAirwayBill_url;
    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final DeliveryOrder order;
    private final HashMap productMap;
    private final String atxProductCode = "";
    private final String logprefix;
    private final String location = "JnTGetPrice";
    private final String secretKey;
    private final String apiKey;
    private String sessionToken;
    private String sslVersion = "SSL";

    public AirwayBill(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {

        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT AirwayBill class initiliazed!!", "");
        this.getAirwayBill_url = "http://47.57.89.30/jandt_web/print/facelistAction!print.action";
        this.baseUrl = (String) config.get("domainUrl");

        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

            String data_digest = "630020026924";
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data_digest.getBytes());
            byte[] digest = md.digest();
            String hash = DatatypeConverter.printHexBinary(digest).toLowerCase();

            MultiValueMap<String, Object> requestBody = generateRequestBody(hash);
            LogUtil.info(logprefix, location, "REQUEST BODY OF JNT FOR GET PRICE : ", requestBody.toString());


            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<String> request = new HttpEntity(requestBody, headers);
            ResponseEntity<byte[]> responses = restTemplate.exchange(getAirwayBill_url, HttpMethod.POST, request, byte[].class);

            LogUtil.info(logprefix, location, "Responses", responses.getBody().toString());


            byte[] bytes = responses.getBody();
            LogUtil.info(logprefix, location, "Responses", responses.getBody().toString());
            LogUtil.info(logprefix, location, "Order " , order.getOrderId().toString());

            int statusCode = responses.getStatusCode().value();
            LogUtil.info(logprefix, location, "Responses", responses.getBody().toString());
            if (statusCode == 200) {
                AirwayBillResult airwayBillResult = new AirwayBillResult();
                airwayBillResult.providerId = order.getDeliveryProviderId();
                airwayBillResult.orderId = order.getOrderId();
                airwayBillResult.consignmentNote = responses.getBody();
                response.resultCode = 0;
                response.returnObject = airwayBillResult;

//            response.returnObject = extractResponseBody(responses.getBody().toString());
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");

        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Exception error :", "", ex);
            response.resultCode = -1;
        }
        return response;
    }

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("billcode'", "630019479435");
        jsonReq.addProperty("account'", "TEST");
        jsonReq.addProperty("password'", "TES123");
        jsonReq.addProperty("customercode'", "'ITTEST0001");

        return jsonReq.toString();
    }

    private MultiValueMap generateRequestBody(String hash) {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("billcode", "630020026924");
        jsonReq.addProperty("account", "TEST");
        jsonReq.addProperty("password", "TES123");
        jsonReq.addProperty("customercode", "ITTEST0001");

        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("logistics_interface", jsonReq);
        postParameters.add("msg_type", "1");
        postParameters.add("data_digest", hash);
        return postParameters;

    }
}