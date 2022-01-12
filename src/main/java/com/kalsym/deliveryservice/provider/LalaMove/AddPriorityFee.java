package com.kalsym.deliveryservice.provider.LalaMove;

import com.google.gson.Gson;
import com.kalsym.deliveryservice.models.RequestBodies.lalamoveGetPrice.AddPriorityFeeRequestBody;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.DriverDetailsResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LalamoveUtils;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class AddPriorityFee extends SyncDispatcher {
    private final String addPriorityFee_url;
    private final String place_orderUrl;
    private final String baseUrl;
    private final int connectTimeout;
    private final int waitTimeout;
    private final String systemTransactionId;
    private String spOrderId;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String logprefix;
    private String location = "LalaMoveAddPriorityFee";
    private String secretKey;
    private String apiKey;
    private DeliveryOrder order;


    public AddPriorityFee(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "LalaMove AddPriorityFee class initiliazed!!", "");
        this.addPriorityFee_url = (String) config.get("addPriorityFee_url");
        this.place_orderUrl = (String) config.get("place_orderUrl");
        this.baseUrl = (String) config.get("domainUrl");
        this.connectTimeout = Integer.parseInt((String) config.get("queryRiderDetails_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("queryRiderDetails_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.secretKey = (String) config.get("queryRiderDetails_secretKey");
        this.apiKey = (String) config.get("queryRiderDetails_apikey");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start Priority Fee", "");
        ProcessResult response = new ProcessResult();

        LogUtil.info(logprefix, location, "Process start", "");

        AddPriorityFeeRequestBody requestBody = generateRequestBody();

        String METHOD = "PUT";

        String BASE_URL = this.baseUrl;
        String ENDPOINT_URL_PRIORITY_FEE = this.place_orderUrl;
        System.err.println("BASEURL :" + BASE_URL + " ENDPOINT :" + ENDPOINT_URL_PRIORITY_FEE);
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
        System.err.println("getSpOrderId : " + order.getSpOrderId());


        JSONObject bodyJson = new JSONObject(new Gson().toJson(requestBody));
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String rawSignature = timeStamp + "\r\n" + METHOD + "\r\n" + ENDPOINT_URL_PRIORITY_FEE + "/" + order.getSpOrderId() + "/tips" + "\r\n\r\n" + bodyJson.toString();
        LogUtil.info(logprefix, location, "rawSignature", rawSignature);
        byte[] byteSig = mac.doFinal(rawSignature.getBytes());
        String signature = DatatypeConverter.printHexBinary(byteSig);
        signature = signature.toLowerCase();
        LogUtil.info(logprefix, location, "SIGNATURE ", signature);
        String authToken = apiKey + ":" + timeStamp + ":" + signature;


        JSONObject orderBody = new JSONObject(new Gson().toJson(requestBody));

        String URL = BASE_URL + ENDPOINT_URL_PRIORITY_FEE + "/" + order.getSpOrderId() + addPriorityFee_url;
        System.err.println("URL : " + URL);

        HttpHeaders headers = new HttpHeaders();
//        headers.set("Content-Type", "application/json");
//        headers.set("Authorization", "hmac " + authToken);
//        headers.set("X-LLM-Country", "MY_KUL");
        HttpEntity<String> request = new HttpEntity(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<String> orderRequest = null;

        try {
            orderRequest = LalamoveUtils.composeRequest(URL, "PUT", orderBody, headers, secretKey, apiKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }


        try {
            ResponseEntity<JSONObject> responses = restTemplate.exchange(URL, HttpMethod.PUT, orderRequest, JSONObject.class);
            int statusCode = responses.getStatusCode().value();
            LogUtil.info(logprefix, location, "Responses for Priority Fee details: ", responses.toString());

            if (statusCode == 200) {
                LogUtil.info(logprefix, location, "Request successful", "");
                response.resultCode = 0;
            } else {
                LogUtil.info(logprefix, location, "Request failed", "");
                response.resultCode = -1;
            }
            LogUtil.info(logprefix, location, "Process finish", "");
        } catch (Exception exception) {
            System.err.println("url for orderDetails" + exception.getMessage());
            DriverDetailsResult result = new DriverDetailsResult();
            result.resultCode = -1;
            response.returnObject = result;
            response.resultString = exception.getMessage();
        }
        return response;
    }

    private AddPriorityFeeRequestBody generateRequestBody() {
//        System.err.println("PriorityFee : " + order.getPriorityFee());
        AddPriorityFeeRequestBody addPriorityFeeRequestBody = new AddPriorityFeeRequestBody("5.00");


        return addPriorityFeeRequestBody;
    }

}
