package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
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

public class SubmitOrder extends SyncDispatcher {


    private final String submitOrder_url;
    private final String domainUrl;
    private final String submitOrder_token;
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
        this.submitOrder_url = (String) config.get("submitorder_url");
        this.domainUrl = (String) config.get("domainUrl");
        this.submitOrder_token = (String) config.get("submitorder_token");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
    }

    @Override
    public ProcessResult process() {
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");
        String transactionId = "";
        String signature = "";
        String requestBody = generateRequestBody();
        Date newDate = new Date();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(
                    systemTransactionId.getBytes(StandardCharsets.UTF_8));
            String body = newDate.getTime() + "\r\n" + "POST" + "/v2/orders" + requestBody;
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

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, (this.domainUrl + this.submitOrder_url), httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
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

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        JsonObject quotedTotalFee = new JsonObject();

        quotedTotalFee.addProperty("amount", order.getShipmentValue());
        quotedTotalFee.addProperty("currency", "MYR");
        jsonReq.add("quotedTotalFee", quotedTotalFee);
        jsonReq.addProperty("sms", false);
        jsonReq.addProperty("pod", true);
        jsonReq.addProperty("fleetOption", "FLEET_ALL");

        return jsonReq.toString();
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
