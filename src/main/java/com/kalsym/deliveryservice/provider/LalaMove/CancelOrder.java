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
        String transactionId = "";
        String signature = "";
        Date newDate = new Date();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    systemTransactionId.getBytes(StandardCharsets.UTF_8));
            String body = newDate.getTime() + "\r\n" + "PUT" + "/v2/orders/" + this.spOrderId + "/cancel";
            transactionId = encodedhash.toString();
            signature = hmac(body, secretKey);

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
            e.printStackTrace();
        }

        String token = apiKey + ":" + newDate.getTime() + ":" + signature;

        HashMap httpHeader = new HashMap();
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + token);
        httpHeader.put("X-Request-ID", transactionId);

        String[] cancelUrl = this.cancelOrder_url.split(",");
        String url = cancelUrl[0] + spOrderId + cancelUrl[1];

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("PUT", this.systemTransactionId, (this.domainUrl + url), httpHeader, "", this.connectTimeout, this.waitTimeout);
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
