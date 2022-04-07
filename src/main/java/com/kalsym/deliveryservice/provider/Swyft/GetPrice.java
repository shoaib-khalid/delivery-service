package com.kalsym.deliveryservice.provider.Swyft;

import com.kalsym.deliveryservice.models.Fulfillment;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashMap;
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
    private final String location = "SwyftGetPrice";
    private final String token;
    private String serviceType;
    private Fulfillment fulfillment;


    public GetPrice(CountDownLatch latch, HashMap config, Order order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository, Fulfillment fulfillment) {
        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        LogUtil.info(logprefix, location, "Swyft GetPrices class initiliazed!!", "");
        this.getprice_url = (String) config.get("getprice_url");
        this.baseUrl = (String) config.get("domainUrl");
        this.token = (String) config.get("token"); //production token - "c3ltcGxpZmllZEBrYWxzeW0uY29tOjI1NDZmMjNjYjgxM2E5ZThiNjdmMzFhNWQ5MDk4MWVl"
        this.connectTimeout = Integer.parseInt((String) config.get("getprice_connect_timeout"));
        this.waitTimeout = Integer.parseInt((String) config.get("getprice_wait_timeout"));
        productMap = (HashMap) config.get("productCodeMapping");
        this.order = order;
        this.serviceType = (String) config.get("serviceType");
        this.fulfillment = fulfillment;
    }

}

