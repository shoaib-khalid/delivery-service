package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.Delivery;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.DeliveryZonePriceRepository;
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
import java.math.RoundingMode;
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
    private final String logprefix;
    private final String location = "LalaMoveGetPrice";
    private final String secretKey;
    private final String apiKey;
    private Fulfillment fulfillment;

    public GetPrice(CountDownLatch latch, Integer providerId, HashMap config, Order order, String systemTransactionId,
                    SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment,
                    DeliveryZonePriceRepository deliveryZonePriceRepository) {

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
        this.order = order;
        this.fulfillment = fulfillment;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
        String secretKey = this.secretKey;
        String apiKey = this.apiKey;
        String METHOD = "POST";
        Mac mac = null;

        String BASE_URL = this.baseUrl;
        LogUtil.info(logprefix, location, "BASEURL :" + BASE_URL + " ENDPOINT :" + this.getprice_url, "");
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        String pickupTime = "";
        if (fulfillment.getFulfillment().equals("FOURHOURS") || fulfillment.getFulfillment().equals("NEXTDAY")
                || fulfillment.getFulfillment().equals("FOURDAYS")) {
            Calendar cal = Calendar.getInstance(); // creates calendar
            cal.setTime(new Date()); // sets calendar time/date
            cal.add(Calendar.HOUR_OF_DAY, fulfillment.getInterval()); // adds one hour
            cal.getTime();
            pickupTime = cal.getTime().toInstant().toString();
        }

        JsonObject requ = generateRequestBody(pickupTime);
        LogUtil.info(logprefix, location, "REQUST BODY FOR GET PRICE : ", requ.toString());

        JSONObject bodyJson = new JSONObject(new Gson().toJson(requ));
        String timeStamp = String.valueOf(System.currentTimeMillis());

        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + "/v3/quotations" + "\r\n\r\n"
                + bodyJson.toString();
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();

        String authToken = apiKey + ":" + timeStamp + ":" + signature;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "hmac " + authToken);
        headers.set("X-LLM-Country", "MY_KUL");

        HashMap<String, String> httpHeader = new HashMap<>();
        httpHeader.put("Content-Type", "application/json");
        httpHeader.put("Authorization", "hmac " + authToken);
        httpHeader.put("X-LLM-Country", "MY_KUL");
        httpHeader.put("Market", "MY");
        HttpEntity<String> request = new HttpEntity<>(bodyJson.toString(), headers);

        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId,
                BASE_URL + "/v3/quotations", httpHeader, bodyJson.toString(), this.connectTimeout, this.waitTimeout);

        if (httpResult.httpResponseCode == 201) {
            LogUtil.info(logprefix, location, "Request successful", "");

            try {
                response.resultCode = 0;
                response.returnObject = extractResponseBody(httpResult.responseString, pickupTime);
            } catch (Exception ex) {
                response.resultCode = -1;
                response.returnObject = ex.getMessage();
                LogUtil.info(logprefix, location, "Request failed", ex.getMessage());

            }
        } else {
            JsonObject jsonResp = new Gson().fromJson(httpResult.responseString, JsonObject.class);
            PriceResult result = new PriceResult();
            try {
                LogUtil.info(logprefix, location, "Request failed", jsonResp.get("errors").getAsString());
            } catch (Exception ex) {
                LogUtil.info(logprefix, location, "Request failed", ex.getMessage());

            }
            // result.message = jsonResp.get("message").getAsString();
            result.message = jsonResp.get("errors").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
            result.isError = true;
            response.returnObject = result;
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, String.valueOf(httpResult.httpResponseCode), "");
        return response;
    }

    private JsonObject generateRequestBody(String pickupTime) {
        List<Delivery> deliveries = new ArrayList<>();

        JsonObject data = new JsonObject();
        JsonObject requestBody = new JsonObject();


        String pickupContactNO;
        String deliveryContactNo;
        if (order.getPickup().getPickupContactPhone().startsWith("6")) {
            // national format
            pickupContactNO = (order.getPickup().getPickupContactPhone().substring(1)).replaceAll(" ", "");
            LogUtil.info(logprefix, location,
                    "[" + systemTransactionId + "] Msisdn is national format. New Msisdn:" + pickupContactNO, "");
        } else if (order.getPickup().getPickupContactPhone().startsWith("+6")) {
            pickupContactNO = (order.getPickup().getPickupContactPhone().substring(2)).replaceAll(" ", "");
            LogUtil.info(logprefix, location,
                    "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO, "");
        } else {
            pickupContactNO = (order.getPickup().getPickupContactPhone()).replaceAll(" ", "");
            LogUtil.info(logprefix, location,
                    "[" + systemTransactionId + "] Remove is national format. New Msisdn:" + pickupContactNO, "");
        }
        if (order.getDelivery().getDeliveryContactPhone().startsWith("6")) {
            // national format
            deliveryContactNo = (order.getDelivery().getDeliveryContactPhone().substring(1)).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId
                    + "] Msisdn is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        } else if (order.getDelivery().getDeliveryContactPhone().startsWith("+6")) {
            deliveryContactNo = (order.getDelivery().getDeliveryContactPhone().substring(2));
            LogUtil.info(logprefix, location, "[" + systemTransactionId
                    + "] Remove is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        } else {
            deliveryContactNo = (order.getDelivery().getDeliveryContactPhone()).replaceAll(" ", "");
            LogUtil.info(logprefix, location, "[" + systemTransactionId
                    + "] Remove is national format. New Msisdn: & Delivery : " + deliveryContactNo, "");
        }

        JsonObject stops = new JsonObject();
        JsonObject stops1 = new JsonObject();
        JsonObject coordinates = new JsonObject();
        JsonObject coordinates2 = new JsonObject();

        JsonArray stop = new JsonArray();
        stops.addProperty("address", order.getPickup().getPickupAddress());
        coordinates.addProperty("lat", String.valueOf(order.getPickup().getLatitude()));
        coordinates.addProperty("lng", String.valueOf(order.getPickup().getLongitude()));
        stops.add("coordinates", coordinates);
        stops1.addProperty("address", order.getDelivery().getDeliveryAddress());
        coordinates2.addProperty("lat", String.valueOf(order.getDelivery().getLatitude()));
        coordinates2.addProperty("lng", String.valueOf(order.getDelivery().getLongitude()));
        stops1.add("coordinates", coordinates2);
        stop.add(stops);
        stop.add(stops1);
        data.addProperty("serviceType", order.getPickup().getVehicleType().name());
        data.addProperty("language", "en_MY");
        data.add("stops", stop);
        data.addProperty("isRouteOptimized", true);
        requestBody.add("data", data);


        return requestBody;
    }

    private PriceResult extractResponseBody(String respString, String pickupTime) {

        LogUtil.info(logprefix, location, "Response: ", respString);
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        LogUtil.info(logprefix, location, "Lalamove jsonResp: " + jsonResp, "");
        String payAmount = jsonResp.get("data").getAsJsonObject().get("priceBreakdown").getAsJsonObject().get("total").getAsString();
        String quotationId = jsonResp.get("data").getAsJsonObject().get("quotationId").getAsString();
        String deliveryStopId = jsonResp.get("data").getAsJsonObject().get("stops").getAsJsonArray().get(1).getAsJsonObject().get("stopId").getAsString();
        String pickupStopId = jsonResp.get("data").getAsJsonObject().get("stops").getAsJsonArray().get(0).getAsJsonObject().get("stopId").getAsString();
        LogUtil.info(logprefix, location, "Payment Amount:" + payAmount, "");
        PriceResult priceResult = new PriceResult();
        BigDecimal bd = BigDecimal.valueOf(Double.parseDouble(payAmount));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        priceResult.price = bd;
        priceResult.pickupDateTime = pickupTime;
        priceResult.fulfillment = fulfillment.getFulfillment();
        priceResult.isError = false;
        priceResult.quotationId = quotationId;
        priceResult.deliveryStopId = deliveryStopId;
        priceResult.pickupStopId = pickupStopId;

        if (fulfillment.getInterval() != null) {
            priceResult.interval = fulfillment.getInterval();
        } else {
            priceResult.interval = null;
        }
        return priceResult;
    }
}