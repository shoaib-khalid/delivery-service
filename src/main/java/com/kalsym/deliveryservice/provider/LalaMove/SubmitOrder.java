package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.lalamove.getprice.Contact;
import com.kalsym.deliveryservice.models.lalamove.getprice.Delivery;
import com.kalsym.deliveryservice.models.lalamove.getprice.PlaceOrder;
import com.kalsym.deliveryservice.models.lalamove.getprice.QuotedTotalFee;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SubmitOrder extends SyncDispatcher {


    private final String baseUrl;
    private final String endpointUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveSubmitOrder";
    private String secretKey;
    private String apiKey;

    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Lalamove SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");

        PlaceOrder requestBody = generateRequestBody();


        String METHOD = "POST";
        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        JSONObject bodyJson = new JSONObject(new Gson().toJson(requestBody));
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + endpointUrl + "\r\n\r\n" + bodyJson.toString();
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + authToken);
        headers.set("X-LLM-Country", "MY_KUL");
        HttpEntity<String> request = new HttpEntity(bodyJson.toString(), headers);
        ResponseEntity<String> responses = restTemplate.exchange(baseUrl + endpointUrl, HttpMethod.POST, request, String.class);
        int statusCode = responses.getStatusCode().value();
//        PriceResult res = extractResponseBody(responses.toString());
        LogUtil.info(logprefix, location, "Process finish", "");
        if (statusCode == 200){
            response.resultCode = 0;
            response.returnObject = extractResponseBody(responses.getBody());
        }else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode=-1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private PlaceOrder generateRequestBody() {
        QuotedTotalFee quotedTotalFee = new QuotedTotalFee(
                order.getShipmentValue().toString(),"MYR"
        );

        PlaceOrder placeOrders = new PlaceOrder(quotedTotalFee,true,true,"FLEET_ALL");

        return placeOrders;
    }


    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            String orderRef = String.valueOf(jsonResp.get("orderRef"));
            LogUtil.info(logprefix, location, "OrderNumber:" + orderRef, "");

            //extract order create
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(orderRef);
            orderCreated.setSpOrderName("lalamove" + orderRef);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());

            submitOrderResult.orderCreated = orderCreated;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return submitOrderResult;
    }



}
