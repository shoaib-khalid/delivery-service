/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package com.kalsym.deliveryservice.provider.Gdex;

import com.kalsym.deliveryservice.provider.MrSpeedy.*;
import com.kalsym.deliveryservice.utils.HttpResult;
import com.kalsym.deliveryservice.utils.HttpsPostConn;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import com.kalsym.deliveryservice.utils.DateTimeUtil;

public class SubmitOrder extends SyncDispatcher {

    private final String submitOrder_url;
    private final String submitOrder_token;
    private final String submitorder_key;
    private final int connectTimeout;
    private final int waitTimeout;
    private Order order;
    private HashMap productMap;
    private String atxProductCode = "";
    private String sessionToken;
    private String sslVersion="SSL";
    private String logprefix;
    private String location="GdexSubmitOrder";
    private final String systemTransactionId;
    private SequenceNumberRepository sequenceNumberRepository;
    
    public SubmitOrder(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {
        super(latch);
        logprefix = systemTransactionId;
        this.systemTransactionId = systemTransactionId;
        LogUtil.info(logprefix, location, "Gdex SubmitOrder class initiliazed!!", "");
        this.submitOrder_url = (String) config.get("submitorder_url");
        this.submitOrder_token = (String) config.get("submitorder_token");
        this.submitorder_key = (String) config.get("submitorder_key");
        this.connectTimeout = Integer.parseInt((String) config.get("submitorder_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("submitorder_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.sslVersion = (String) config.get("ssl_version");
        this.sequenceNumberRepository = sequenceNumberRepository;
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();            
        HashMap httpHeader = new HashMap();
        httpHeader.put("User-Token", this.submitOrder_token);
        httpHeader.put("Subscription-Key", this.submitorder_key);
        httpHeader.put("Content-Type", "application/json-patch+json");
        httpHeader.put("Connection", "close");
        String requestBody = generateRequestBody();
        HttpResult httpResult = HttpsPostConn.SendHttpsRequest("POST", this.systemTransactionId, this.submitOrder_url, httpHeader, requestBody, this.connectTimeout, this.waitTimeout);
        if (httpResult.resultCode==0) {
            LogUtil.info(logprefix, location, "Request successful", "");
            response.resultCode=0;            
            response.returnObject=extractResponseBody(httpResult.responseString);
        } else {
            LogUtil.info(logprefix, location, "Request failed", "");
            response.resultCode=-1;
        }
        LogUtil.info(logprefix, location, "Process finish", "");
        return response;
    }
    
    private String generateRequestBody() {
        JsonObject jsonReq = new JsonObject();
        jsonReq.addProperty("Name", order.getPickup().getPickupContactName());
        jsonReq.addProperty("Mobile", order.getPickup().getPickupContactPhone());
        jsonReq.addProperty("Email", order.getPickup().getPickupContactEmail());
        jsonReq.addProperty("Address1", order.getPickup().getPickupAddress());
        jsonReq.addProperty("Address2", "");
        jsonReq.addProperty("Address3", "");
        jsonReq.addProperty("Postcode", order.getDelivery().getDeliveryPostcode());
        jsonReq.addProperty("LocationId", order.getPickup().getPickupLocationId());
        jsonReq.addProperty("Location", order.getPickup().getPickupAddress());
        jsonReq.addProperty("City", order.getPickup().getPickupCity());
        jsonReq.addProperty("State", order.getPickup().getPickupState());        
        
        JsonObject pickupDetails = new JsonObject();
        VehicleType vehicleType = VehicleType.valueOf(order.getPickup().getVehicleType().name());            
        pickupDetails.addProperty("Transportation", vehicleType.getCode());
        pickupDetails.addProperty("ParcelReadyTime", order.getPickup().getParcelReadyTime());
        pickupDetails.addProperty("PickupDate", order.getPickup().getPickupDate());
        pickupDetails.addProperty("PickupRemark", order.getPickup().getRemarks());
        pickupDetails.addProperty("IsTrolleyRequired", order.getPickup().isTrolleyRequired());
        jsonReq.add("Pickup", pickupDetails);
        
        JsonArray consignmentArray = new JsonArray();
        JsonObject consignment = new JsonObject();
        consignment.addProperty("OrderId", order.getTransactionId());
        consignment.addProperty("ShipmentContent", order.getShipmentContent());
        consignment.addProperty("ParcelType", order.getItemType().name());
        consignment.addProperty("ShipmentValue", order.getShipmentValue());
        consignment.addProperty("Pieces", order.getPieces());
        consignment.addProperty("Weight", order.getTotalWeightKg());
        consignment.addProperty("Name", order.getDelivery().getDeliveryContactName());
        consignment.addProperty("Mobile", order.getDelivery().getDeliveryContactPhone());
        consignment.addProperty("Email", order.getDelivery().getDeliveryContactEmail());
        consignment.addProperty("Address1",order.getDelivery().getDeliveryAddress());
        consignment.addProperty("Address2","");
        consignment.addProperty("Address3","");
        consignment.addProperty("Postcode", order.getDelivery().getDeliveryPostcode());
        consignment.addProperty("City", order.getDelivery().getDeliveryCity());
        consignment.addProperty("State", order.getDelivery().getDeliveryState());
        consignment.addProperty("Country", order.getDelivery().getDeliveryCountry());
        consignment.addProperty("IsInsurance", order.isInsurance());
        consignmentArray.add(consignment);
        jsonReq.add("Consignments", consignmentArray);
           
        return jsonReq.toString();
    }
    
    
    private SubmitOrderResult extractResponseBody(String respString) {
        SubmitOrderResult submitOrderResult = new SubmitOrderResult();            
        JsonObject jsonResp = new Gson().fromJson(respString, JsonObject.class);
        String statusCode = jsonResp.get("statusCode").getAsString();
        if (statusCode.equals("200")) {
            JsonObject dataObject = jsonResp.get("data").getAsJsonObject();            
            JsonArray consignment = dataObject.get("ConsignmentNumbers").getAsJsonArray();
            String InvoiceNumber = dataObject.get("InvoiceNumber").getAsString();
            DeliveryOrder orderCreated = new DeliveryOrder();
            orderCreated.setSpOrderId(consignment.get(0).getAsString());
            orderCreated.setSpOrderName(InvoiceNumber);
            orderCreated.setCreatedDate(DateTimeUtil.currentTimestamp());
            orderCreated.setVehicleType(order.getPickup().getVehicleType().name());
            submitOrderResult.orderCreated=orderCreated;            
        } 
        return submitOrderResult;
    }

}