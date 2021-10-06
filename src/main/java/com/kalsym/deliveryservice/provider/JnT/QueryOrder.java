package com.kalsym.deliveryservice.provider.JnT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.Gdex.VehicleType;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.DateTimeUtil;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class QueryOrder extends SyncDispatcher {

    private final String baseUrl;
    private final String endpointUrl;
    private final String queryOrder_url;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "JnTSubmitOrder";
    private String secretKey;
    private String apiKey;
    private String spOrderId;
    private String driverId;
    private String shareLink;
    private String status;

    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT SubmitOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.queryOrder_url = "http://47.57.89.30/common/track/trackings";
        this.secretKey = (String) config.get("secretKey");
        this.apiKey = "x1Wbjv";
        this.endpointUrl = (String) config.get("place_orderUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("queryorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.spOrderId = spOrderId;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();
//        HashMap httpHeader = new HashMap();
//        httpHeader.put("Content-Type", "application/json-patch+json");
//        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        LogUtil.info(logprefix, location, "JnT request body for Query Order: " + requestBody, "");
        // data signature part
        String data_digest = requestBody + "ffe62df84bb3d8e4b1eaa2c22775014d";
        String encode_key = "";
        // encryption
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data_digest.getBytes());
            byte[] digest = md.digest();
            String myHash = DatatypeConverter.printHexBinary(digest).toLowerCase();
            String base64Key = Base64.getEncoder().encodeToString(myHash.getBytes());
            encode_key = base64Key;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }

        MultiValueMap<String, Object> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("logistics_interface", requestBody);
        postParameters.add("data_digest", encode_key);
        postParameters.add("msg_type", "TRACK");
        postParameters.add("eccompanyid", "TEST");
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded");
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(postParameters, headers);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responses = restTemplate.exchange(queryOrder_url, HttpMethod.POST, request, String.class);

        int statusCode = responses.getStatusCode().value();
        LogUtil.info(logprefix, location, "Responses", responses.getBody());
        if (statusCode == 200) {
            response.resultCode = 0;
            LogUtil.info(logprefix, location, "JnT Response for Submit Order: " + responses.getBody(), "");
            response.returnObject = extractResponseBody(responses.getBody());
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode = -1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
//        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.queryOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
//        if (httpResult.resultCode==0) {
//            LogUtil.info(logprefix, location, "Request successful", "");
//            response.resultCode=0;
//            response.returnObject=httpResult.responseString; // need to implement extractResponseBody here
//        } else {
//            LogUtil.info(logprefix, location, "Request failed", "");
//            response.resultCode=-1;
//        }
//        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }

    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("queryType", 1);
        jsonReq.addProperty("language","2");
        jsonReq.addProperty("queryCodes", spOrderId);

        return jsonReq.toString();
    }

    private QueryOrderResult extractResponseBody(String respString) {
        QueryOrderResult queryOrderResult = new QueryOrderResult();
        try {
            JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
            JsonObject responseItems = jsonResp.get("responseitems").getAsJsonObject();
            JsonArray data = responseItems.get("data").getAsJsonArray();
            JsonObject dataFirstObject = data.get(0).getAsJsonObject();
            JsonArray details = dataFirstObject.get("details").getAsJsonArray();
            if (details.size() > 0) {
                status = details.get(0).getAsJsonObject().get("scanStatus").getAsString();
            } else {
                JsonObject orderDetail = dataFirstObject.get("orderDetail").getAsJsonObject();
                status = orderDetail.get("orderstatus").getAsString();
            }
            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }
}
