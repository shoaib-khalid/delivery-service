package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.apache.tomcat.util.codec.binary.Base64;
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
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {

    private final String queryOrder_url;
    private final String domainUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveQueryOrder";
    private String secretKey;
    private String apiKey;

    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "LalaMove QueryOrder class initiliazed!!", "");
        this.queryOrder_url = (String) config.get("queryorder_url");
        this.domainUrl = (String) config.get("domainUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.spOrderId = spOrderId;
        this.sslVersion = (String) config.get("ssl_version");
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String transactionId = "";
        Mac mac = null;
        String METHOD = "GET";

        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        String url = this.queryOrder_url + spOrderId;
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v2/orders/" + spOrderId + "\r\n\r\n";
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String token = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + token);
        headers.set("X-LLM-Country", "MY_KUL");
        headers.set("X-Request-ID", transactionId);
        HttpEntity<String> request = new HttpEntity(headers);
        System.err.println("url for orderDetails" + url);
        ResponseEntity<String> responses = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        int statusCode = responses.getStatusCode().value();
        System.err.println("responses for order details: " + responses);

        if (statusCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(responses.getBody());
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            System.err.println("jsonResp from orderDetail: " + jsonResp);
            boolean isSuccess = true;
//            JsonArray pod = jsonResp.get("pod").getAsJsonArray();
            LogUtil.info(logprefix, location, "isSuccess:" + isSuccess, "");
            queryOrderResult.isSuccess = isSuccess;

            String driverId = jsonResp.get("driverId").getAsString();
            String shareLink = jsonResp.get("shareLink").getAsString();
            String status = jsonResp.get("status").getAsString();

            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
            orderFound.setCustomerTrackingUrl(shareLink);
            orderFound.setMerchantTrackingUrl(shareLink);
            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }

}
