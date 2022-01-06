package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.CancelOrderResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class CancelOrder extends SyncDispatcher {

    private final String cancelOrder_url;
    private final String domainUrl;
    private final String cancelOrder_token;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveCancelOrder";
    private String secretKey;
    private String apiKey;

    public CancelOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "LalaMove CancelOrder class initiliazed!!", "");
        this.cancelOrder_url = (String) config.get("cancelorder_url");
        this.domainUrl = (String) config.get("domainUrl");
        this.cancelOrder_token = (String) config.get("cancelorder_token");
        this.connectTimeout = Integer.parseInt((String) config.get("cancelorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("cancelorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.sslVersion = (String) config.get("ssl_version");
        this.spOrderId = spOrderId;
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String METHOD = "PUT";
        String timeStamp = String.valueOf(System.currentTimeMillis());
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


        JSONObject bodyJson = new JSONObject("{}");
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v2/orders/" + this.spOrderId + "/cancel" + "\r\n\r\n" + bodyJson.toString();
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        HashMap httpHeader = new HashMap();
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Content-Type", "application/json; charset=utf-8");
        httpHeader.put("Authorization", "hmac " + authToken);

        String[] cancelUrl = this.cancelOrder_url.split(",");
        String url = cancelUrl[0] + spOrderId + cancelUrl[1];

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("PUT", this.systemTransactionId, url, httpHeader, bodyJson.toString(), this.connectTimeout, this.waitTimeout);
        CancelOrderResult cancelOrderResult = new CancelOrderResult();
        if (httpResult.httpResponseCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            cancelOrderResult.resultCode = 0;
            cancelOrderResult.isSuccess = true;
            response.returnObject = cancelOrderResult;
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
            cancelOrderResult.resultCode = -1;
            cancelOrderResult.isSuccess = false;
            response.returnObject = extractResponseBody(httpResult.responseString);
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private CancelOrderResult extractResponseBody(String respString) {
        CancelOrderResult cancelOrderResult = new CancelOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            boolean isSuccess;
            String message = "";
            if (jsonResp.isJsonNull()) {
                isSuccess = true;
            } else {
                isSuccess = false;
                message = jsonResp.get("message").toString();
            }

            cancelOrderResult.isSuccess = isSuccess;
            //extract order cancelled
            String orderId = spOrderId;
            DeliveryOrder orderCancelled = new DeliveryOrder();
            orderCancelled.setSpOrderId(orderId);
            orderCancelled.setStatusDescription(message);
            orderCancelled.setCreatedDate(DateTimeUtil.currentTimestamp());
            cancelOrderResult.resultCode = -1;
            cancelOrderResult.isSuccess = true;
            cancelOrderResult.orderCancelled = orderCancelled;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return cancelOrderResult;
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
