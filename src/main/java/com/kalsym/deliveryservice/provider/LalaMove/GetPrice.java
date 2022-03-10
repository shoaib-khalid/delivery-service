package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.*;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class GetPrice extends SyncDispatcher {

    private final String getprice_url;
    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private final Order order;
    private final HashMap productMap;
    private final String atxProductCode = "";
    private final String logprefix;
    private final String location = "LalaMoveGetPrice";
    private final String secretKey;
    private final String apiKey;
    private String sessionToken;
    private String sslVersion = "SSL";
    private Fulfillment fulfillment;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment) {


        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "LalaMove GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.baseUrl = (String) config.get("domainUrl");

        this.secretKey = (String) config.get("secretKey");
        this.apiKey = (String) config.get("apiKey");
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.fulfillment = fulfillment;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String secretKey = this.secretKey;
        String apiKey = this.apiKey;
        String ENDPOINT_URL = this.getprice_url;
        String METHOD = "POST";
        Mac mac = null;


        String BASE_URL = this.baseUrl;
        String ENDPOINT_URL_PLACEORDER = this.getprice_url;
        LogUtil.info(logprefix, location, "BASEURL :" + BASE_URL + " ENDPOINT :" + ENDPOINT_URL_PLACEORDER, "");
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        String pickupTime = "";
        if (fulfillment.getFulfillment().equals("FOURHOURS") || fulfillment.getFulfillment().equals("NEXTDAY") || fulfillment.getFulfillment().equals("FOURDAYS")) {
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(new Date());               // sets calendar time/date
            cal.add(Calendar.HOUR_OF_DAY, fulfillment.getInterval());      // adds one hour
            cal.getTime();
            pickupTime = cal.getTime().toInstant().toString();
        }

        GetPrices requ = generateRequestBody(pickupTime);
        LogUtil.info(logprefix, location, "REQUST BODY FOR GET PRICE : ", requ.toString());

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

        HashMap httpHeader = new HashMap();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + authToken);
        httpHeader.put("X-LLM-Country", "MY_KUL");
        HttpEntity<String> request = new HttpEntity(bodyJson.toString(), headers);

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, BASE_URL + ENDPOINT_URL, httpHeader, bodyJson.toString(), this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString, pickupTime);
        } else {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            PriceResult result = new PriceResult();
            LogUtil.info(logprefix, location, "Request failed", jsonResp.get("message").getAsString());

            result.message = jsonResp.get("message").getAsString();
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");
        return response;
    }


    private GetPrices generateRequestBody(String pickupTime) {
        List<Delivery> deliveries = new ArrayList<>();

        String pickupContactNO;
        String deliveryContactNo;
        if (order.getPickup().getPickupContactPhone().startsWith("6")) {
            //national format
            pickupContactNO = (order.getPickup().getPickupContactPhone().substring(1)).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Msisdn is national format. New Msisdn:" + pickupContactNO, "");
        } else if (order.getPickup().getPickupContactPhone().startsWith("+6")) {
            pickupContactNO = (order.getPickup().getPickupContactPhone().substring(2)).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO, "");
        } else {
            pickupContactNO = (order.getPickup().getPickupContactPhone()).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO, "");
        }
        if (order.getDelivery().getDeliveryContactPhone().startsWith("6")) {
            //national format
            deliveryContactNo = (order.getDelivery().getDeliveryContactPhone().substring(1)).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Msisdn is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        } else if (order.getDelivery().getDeliveryContactPhone().startsWith("+6")) {
            deliveryContactNo = (order.getDelivery().getDeliveryContactPhone().substring(2));
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Remove is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        } else {
            deliveryContactNo = (order.getDelivery().getDeliveryContactPhone()).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId + "] Remove is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        }


        deliveries.add(
                new Delivery(
                        1,
                        new Contact(order.getDelivery().getDeliveryContactName(), deliveryContactNo),
                        ""
                )
        );

        GetPrices req = new GetPrices();
        req.serviceType = order.getPickup().getVehicleType().name();
        req.specialRequests = null;

        if (fulfillment.getFulfillment().equals("FOURHOURS") || fulfillment.getFulfillment().equals("NEXTDAY") || fulfillment.getFulfillment().equals("FOURDAYS")) {
            req.scheduleAt = pickupTime;
        }
        Stop s1 = new Stop();
        s1.addresses = new Addresses(
                new MsMY(order.getPickup().getPickupAddress(),
                        "MY_KUL")
        );
        Stop s2 = new Stop();
        s2.addresses = new Addresses(
                new MsMY(order.getDelivery().getDeliveryAddress(),
                        "MY_KUL"));
        List<Stop> stopList = new ArrayList<>();
        stopList.add(s1);
        stopList.add(s2);

        req.stops = stopList;
        req.requesterContact = new Contact(order.getPickup().getPickupContactName(), pickupContactNO);
        req.deliveries = deliveries;
        return req;
    }

    private PriceResult extractResponseBody(String respString, String pickupTime) {
//        if (order.getDeliveryType().equals("SCHEDULED")) {
//            Calendar cal = Calendar.getInstance(); // creates calendar
//            cal.setTime(new Date());               // sets calendar time/date
//            cal.add(Calendar.HOUR_OF_DAY, order.getInterval());      // adds one hour
//            cal.getTime();        }


        LogUtil.info(logprefix, location, "Response: ", respString);
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        LogUtil.info(logprefix, location, "Lalamove jsonResp: " + jsonResp, "");
        String payAmount = jsonResp.get("totalFee").getAsString();
        LogUtil.info(logprefix, location, "Payment Amount:" + payAmount, "");
        PriceResult priceResult = new PriceResult();
        BigDecimal bd = new BigDecimal(Double.parseDouble(payAmount));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        priceResult.price = bd;
        priceResult.pickupDateTime = pickupTime;
        priceResult.fulfillment = fulfillment.getFulfillment();
        priceResult.isError = false;
        if (fulfillment.getInterval() != null) {
            priceResult.interval = fulfillment.getInterval();
        } else {
            priceResult.interval = null;
        }
        return priceResult;
    }
}