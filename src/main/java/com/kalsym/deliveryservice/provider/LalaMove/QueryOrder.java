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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
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
        String signature = "";

        Date newDate = new Date();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    systemTransactionId.getBytes(StandardCharsets.UTF_8));
            String body = newDate.getTime() + "\r\n" + "GET" + "/v2/orders/" + spOrderId;
            transactionId = encodedhash.toString();
            signature = hmac(body, secretKey);

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        String url = this.queryOrder_url + spOrderId;

        String token = apiKey + ":" + newDate.getTime() + ":" + signature;

        HashMap httpHeader = new HashMap();
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + token);
        httpHeader.put("X-Request-ID", transactionId);

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("GET", this.systemTransactionId, url, httpHeader, "", this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode == 0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
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
            boolean isSuccess = true;
            JsonArray pod = jsonResp.get("pod").getAsJsonArray();
            LogUtil.info(logprefix, location, "isSuccess:" + isSuccess, "");
            queryOrderResult.isSuccess = isSuccess;

            String driverId = jsonResp.get("driverId").getAsString();
            String shareLink = jsonResp.get("shareLink").getAsString();
            String status = jsonResp.get("status").getAsString();

            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
//            orderFound.setSpOrderName(orderName);
            orderFound.setStatus(status);
            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }


    public String hmac(String body, String key) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        SecretKey secretKey;
        Mac mac = Mac.getInstance("HMACSHA256");
        byte[] keyBytes = key.getBytes();
        secretKey = new SecretKeySpec(keyBytes, mac.getAlgorithm());
        mac.init(secretKey);
        byte[] text = body.getBytes(StandardCharsets.UTF_8);
        byte[] encodedText = mac.doFinal(text);
        return new String(Base64.encodeBase64(encodedText)).trim();

    }

}
