package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.lalamove.getprice.*;
import com.kalsym.deliveryservice.provider.MrSpeedy.VehicleType;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
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
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String domainUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final Order order;
    private final HashMap productMap;
    private final String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private final String logprefix;
    private final String location = "LalaMoveGetPrice";
    private final String secretKey;
    private final String apiKey;


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
        String secretKey = "7p0CJjVxlfEpg/EJWi/y9+6pMBK9yvgYzVeOUKSYZl4/IztYSh6ZhdcdpRpB15ty";
        String apiKey = "6e4e7adb5797632e54172dc2dd2ca748";
        String BASE_URL = "https://rest.sandbox.lalamove.com";
        String ENDPOINT_URL = "/v2/quotations";
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

/*        List<com.kalsym.deliveryservice.models.lalamove.getprice.Delivery> deliveries = new ArrayList<>();

        deliveries.add(
                new com.kalsym.deliveryservice.models.lalamove.getprice.Delivery(
                        1,
                        new Contact(order.getDelivery().getDeliveryContactName(), order.getDelivery().getDeliveryContactPhone()),
                        ""
                )
        );

        com.kalsym.deliveryservice.models.lalamove.getprice.GetPrice req = new com.kalsym.deliveryservice.models.lalamove.getprice.GetPrice();
        req.serviceType = "MOTORCYCLE";
        req.specialRequests = null;
        Stop s1 = new Stop();
        s1.addresses = new Addresses(
                new MsMY(order.getPickup().getPickupAddress(),
                        "MY_KUL")
        );
        Stop s2 = new Stop();
        s2.addresses = new Addresses(
                new MsMY(order.getPickup().getPickupAddress(),
                        "MY_KUL"));
        List<Stop> stopList = new ArrayList<>();
        stopList.add(s1);
        stopList.add(s2);

        req.stops = stopList;
        req.requesterContact = new Contact(order.getPickup().getPickupContactName(), order.getPickup().getPickupContactPhone());
        req.deliveries = deliveries;
        System.out.println("Request: " + req);*/
        com.kalsym.deliveryservice.models.lalamove.getprice.GetPrice requ = generateRequestBody();

        //JSONObject bodyJson = new JSONObject("{\"serviceType\":\"MOTORCYCLE\",\"specialRequests\":[],\"stops\":[{\"location\":{\"lat\":\"3.048593\",\"lng\":\"101.671568\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"Bumi Bukit Jalil, No 2-1, Jalan Jalil 1, Lebuhraya Bukit Jalil, Sungai Besi, 57000 Kuala Lumpur, Malaysia\",\"country\":\"MY_KUL\"}}},{\"location\":{\"lat\":\"2.754873\",\"lng\":\"101.703744\"},\"addresses\":{\"ms_MY\":{\"displayString\":\"64000 Sepang, Selangor, Malaysia\",\"country\":\"MY_KUL\"}}}],\"requesterContact\":{\"name\":\"Chris Wong\",\"phone\":\"0376886555\"},\"deliveries\":[{\"toStop\":1,\"toContact\":{\"name\":\"Shen Ong\",\"phone\":\"0376886555\"},\"remarks\":\"Remarks for drop-off point (#1).\"}]}");
        JSONObject bodyJson = new JSONObject(new Gson().toJson(requ));
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + ENDPOINT_URL + "\r\n\r\n" + bodyJson.toString();
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
        ResponseEntity<String> responses = restTemplate.exchange(BASE_URL + ENDPOINT_URL, HttpMethod.POST, request, String.class);
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


    private com.kalsym.deliveryservice.models.lalamove.getprice.GetPrice generateRequestBody() {
        List<com.kalsym.deliveryservice.models.lalamove.getprice.Delivery> deliveries = new ArrayList<>();

        deliveries.add(
                new com.kalsym.deliveryservice.models.lalamove.getprice.Delivery(
                        1,
                        new Contact(order.getDelivery().getDeliveryContactName(), order.getDelivery().getDeliveryContactPhone()),
                        ""
                )
        );

        com.kalsym.deliveryservice.models.lalamove.getprice.GetPrice req = new com.kalsym.deliveryservice.models.lalamove.getprice.GetPrice();
        req.serviceType = "MOTORCYCLE";
        req.specialRequests = null;
        Stop s1 = new Stop();
        s1.addresses = new Addresses(
                new MsMY(order.getPickup().getPickupAddress(),
                        "MY_KUL")
        );
        Stop s2 = new Stop();
        s2.addresses = new Addresses(
                new MsMY(order.getPickup().getPickupAddress(),
                        "MY_KUL"));
        List<Stop> stopList = new ArrayList<>();
        stopList.add(s1);
        stopList.add(s2);

        req.stops = stopList;
        req.requesterContact = new Contact(order.getPickup().getPickupContactName(), order.getPickup().getPickupContactPhone());
        req.deliveries = deliveries;
        return req;
    }

    private PriceResult extractResponseBody(String respString) {
        System.err.println("Lalamove : " + respString);
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        System.err.println("Lalamove jsonResp: " + jsonResp);
        String payAmount = jsonResp.get("totalFee").getAsString();


        LogUtil.info(logprefix, location, "Payment Amount:" + payAmount, "");
        PriceResult priceResult = new PriceResult();
        priceResult.price = Double.parseDouble(payAmount);
        return priceResult;
    }
}