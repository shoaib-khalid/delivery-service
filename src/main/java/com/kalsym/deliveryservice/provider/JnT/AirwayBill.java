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
import java.math.BigInteger;
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
    private final DeliveryOrder order;
    private final String logprefix;
    private final String location = "JnTGetAirwayBill";
    private String sslVersion = "SSL";
    private String username;
    private String passowrd ;
    private String customerCode ;

    public AirwayBill(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {

        super(latch);
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT AirwayBill class initiliazed!!", "");
        this.getAirwayBill_url = (String) config.get("airwayBillURL");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.username = (String) config.get("username");
        this.passowrd = (String) config.get("password");
        this.customerCode = (String) config.get("cuscode");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-www-form-urlencoded");

//            String data_digest = "630020026924";
            String data_digest =order.getSpOrderId();
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            md.update(data_digest.getBytes());
//            byte[] digest = md.digest();
//            String hash = DatatypeConverter.printHexBinary(digest).toLowerCase();
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

            MultiValueMap<String, Object> requestBody = generateRequestBody(hashtext);
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

    private MultiValueMap generateRequestBody(String hash) {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("billcode", order.getSpOrderId());
        jsonReq.addProperty("account", username);
        jsonReq.addProperty("password", passowrd);
        jsonReq.addProperty("customercode", customerCode);

        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("logistics_interface", jsonReq);
        postParameters.add("msg_type", "1");
        postParameters.add("data_digest", hash);
        return postParameters;

    }
}