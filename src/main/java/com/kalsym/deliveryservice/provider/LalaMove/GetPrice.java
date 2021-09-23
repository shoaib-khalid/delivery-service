package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {


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
        System.err.println("BASEURL :" + BASE_URL + " ENDPOINT :" + ENDPOINT_URL_PLACEORDER);
        try {
            mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(secret_key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        GetPrices requ = generateRequestBody();
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
        /*LogUtil.info(logprefix, location, "Request Body  : ", bodyJson.toString());
        try {
            ResponseEntity<String> responses = restTemplate.exchange(BASE_URL + ENDPOINT_URL, HttpMethod.POST, request, String.class);
            int statusCode = responses.getStatusCode().value();
//        PriceResult res = extractResponseBody(responses.toString());
            LogUtil.info(logprefix, location, "Responses", responses.getBody());
            if (statusCode == 200) {
                response.resultCode = 0;
                response.returnObject = extractResponseBody(responses.getBody());
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception ex) {
            PriceResult priceResult = new PriceResult();
            String message  = ex.getMessage();


            priceResult.message = ex.getMessage();;
            response.resultCode = -1;
            response.returnObject = priceResult;
            LogUtil.info(logprefix, location, "Exception", ex.getMessage());

        }*/


        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, BASE_URL + ENDPOINT_URL, httpHeader, bodyJson.toString(), this.connectTimeout, this.waitTimeout);
        if (httpResult.httpResponseCode == 200) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode = 0;
            response.returnObject = extractResponseBody(httpResult.responseString);
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
//        return response;
    }


    private GetPrices generateRequestBody() {
        List<Delivery> deliveries = new ArrayList<>();

        deliveries.add(
                new Delivery(
                        1,
                        new Contact(order.getDelivery().getDeliveryContactName(), order.getDelivery().getDeliveryContactPhone()),
                        ""
                )
        );

        GetPrices req = new GetPrices();
        req.serviceType = order.getPickup().getVehicleType().name();
        req.specialRequests = null;
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
        req.requesterContact = new Contact(order.getPickup().getPickupContactName(), order.getPickup().getPickupContactPhone());
        req.deliveries = deliveries;
        return req;
    }

    private PriceResult extractResponseBody(String respString) {
        LogUtil.info(logprefix, location, "Response: ", respString);
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        System.err.println("Lalamove jsonResp: " + jsonResp);
        String payAmount = jsonResp.get("totalFee").getAsString();
        LogUtil.info(logprefix, location, "Payment Amount:" + payAmount, "");
        PriceResult priceResult = new PriceResult();
        BigDecimal bd = new BigDecimal(Double.parseDouble(payAmount));
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        priceResult.price = bd;
        priceResult.isError = false;
        return priceResult;
    }
}