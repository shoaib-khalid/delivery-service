package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.MrSpeedy.VehicleType;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.apache.tomcat.util.codec.binary.Base64;

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

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String domainUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveGetPrice";
    private String secretKey;
    private String apiKey;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {


        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "LalaMove GetPrice class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.domainUrl = (String) config.get("domainUrl");
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
        String transactionId = "";
        String hash = "";
        String requestBody = generateRequestBody();
        Date newDate = new Date();
        try {
            String body = newDate.getTime() + "\\r\\n" + "POST\\r\\n" + "/v2/quotations\\r\\n\\r\\n" + requestBody;


            Mac hasher = Mac.getInstance("HmacSHA256");
            hasher.init(new SecretKeySpec(secretKey.getBytes(), "HmacSHA256"));

            byte[] hashs = hasher.doFinal(body.getBytes());

            // to lowercase hexits
            DatatypeConverter.printHexBinary(hashs);

            // to base64
            hash = DatatypeConverter.printBase64Binary(hashs);
//            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
//            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
//            sha256_HMAC.init(secret_key);

//            hash = Base64.encodeBase64String(sha256_HMAC.doFinal(body.getBytes()));
            System.out.println(hash);
            System.out.println("request body :" + body);


        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }

        String token = apiKey + ":" + newDate.getTime() + ":" + hash;

        HashMap httpHeader = new HashMap();
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + token);
        httpHeader.put("X-Request-ID", this.order.getTransactionId());

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, (this.domainUrl + this.getprice_url), httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
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

        JsonArray stopsArray = new JsonArray();
        JsonObject stops = new JsonObject();
        JsonObject location = new JsonObject();
        JsonObject country = new JsonObject();
        JsonObject address = new JsonObject();

        location.addProperty("lat", "");
        location.addProperty("lng", "");
        country.addProperty("displayString", order.getPickup().getPickupAddress());
        country.addProperty("country", order.getPickup().getPickupCountry());

        address.add("MY_KUL", country);
        stops.add("location", location);
        stops.add("addresses", address);
        stopsArray.add(stops);


        JsonArray deliveryArray = new JsonArray();
        JsonObject deliverInfo = new JsonObject();
        JsonObject contact = new JsonObject();
        contact.addProperty("name", order.getPickup().getPickupContactName());
        contact.addProperty("phone", order.getPickup().getPickupContactPhone());
        deliverInfo.addProperty("toStop", 1);
        deliverInfo.add("toContact", contact);
        deliverInfo.addProperty("remarks", "");
        deliveryArray.add(deliverInfo);


        jsonReq.addProperty("scheduleAt", ""); //discuss with taufik
        jsonReq.addProperty("serviceType", VehicleType.valueOf(order.getPickup().getVehicleType().name()).getCode());
        jsonReq.add("stops", stopsArray);
        jsonReq.add("deliveries", deliveryArray);
        jsonReq.addProperty("requesterContact", order.getPickup().getPickupContactPhone());
        jsonReq.addProperty("specialRequests", "");

        return jsonReq.toString();
    }

    private PriceResult extractResponseBody(String respString) {
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String payAmount = jsonResp.get("totalFee").getAsString();
        LogUtil.info(logprefix, location, "Payment Amount:" + payAmount, "");
        PriceResult priceResult = new PriceResult();
        priceResult.price = Double.parseDouble(payAmount);
        return priceResult;
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