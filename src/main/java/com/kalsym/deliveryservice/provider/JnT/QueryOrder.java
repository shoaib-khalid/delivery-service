package com.kalsym.deliveryservice.provider.JnT;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.QueryOrderResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.xml.bind.DatatypeConverter;
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
    private String location = "JnTQueryOrder";
    private String password;
    private String secretKey;
    private String spOrderId;
    private String username;
    private String msgType;
    private String driverId;
    private String shareLink;
    private String status;

    public QueryOrder(CountDownLatch latch, HashMap config, String spOrderId, String systemTransactionId) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "JnT QueryOrder class initiliazed!!", "");

        this.baseUrl = (String) config.get("domainUrl");
        this.queryOrder_url = (String) config.get("queryOrder_url");
        this.username = (String) config.get("username");
        this.password = (String) config.get("password");
        this.secretKey = (String) config.get("secretKey");
        this.msgType = (String) config.get("msgType");
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
        String data_digest = requestBody + this.secretKey;
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
        postParameters.add("msg_type", this.msgType);
        postParameters.add("eccompanyid", this.username);
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
        jsonReq.addProperty("language", "2");
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
                status = details.get(0).getAsJsonObject().get("scanstatus").getAsString();
            }

            DeliveryOrder orderFound = new DeliveryOrder();
            orderFound.setSpOrderId(spOrderId);
            orderFound.setStatus(status);
            if (status.equals("Picked Up")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            }
            else if (status.equals("On Hold")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            }
            else if (status.equals("Departure")) {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            }
            else if(status.equals("Delivered")){
                orderFound.setSystemStatus(DeliveryCompletionStatus.COMPLETED.name());
            }
            else if(status.equals("On Return")){
                orderFound.setSystemStatus(DeliveryCompletionStatus.FAILED.name());
            }
            else {
                orderFound.setSystemStatus(DeliveryCompletionStatus.BEING_DELIVERED.name());
            }


            queryOrderResult.orderFound = orderFound;
        } catch (Exception ex) {
            LogUtil.error(logprefix, location, "Error extracting result", "", ex);
        }
        return queryOrderResult;
    }
}
