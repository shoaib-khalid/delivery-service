package com.kalsym.deliveryservice.controllers;

import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.repositories.DeliveryQuotationRepository;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;

public class RetryThread extends Thread implements Runnable {


    private DeliveryService deliveryService;
    private DeliveryQuotationRepository deliveryQuotationRepository;
    private String sysTransactionId;
    private DeliveryQuotation quotation;
    private SymplifiedService symplifiedService;
    private PriceResult priceResult;
    private Boolean difProvider;


//    public RetryThread(DeliveryQuotation quotation, String sysTransactionId, DeliveryQuotationRepository deliveryQuotationRepository, DeliveryService deliveryService, SymplifiedService symplifiedService, PriceResult priceResult) {
//        this.sysTransactionId = sysTransactionId;
//        this.quotation = quotation;
//        this.deliveryQuotationRepository = deliveryQuotationRepository;
//        this.deliveryService = deliveryService;
//        this.symplifiedService = symplifiedService;
//        this.priceResult = priceResult;
//
//    }

    public RetryThread(DeliveryQuotation quotation, String sysTransactionId, DeliveryQuotationRepository deliveryQuotationRepository, DeliveryService deliveryService, SymplifiedService symplifiedService, PriceResult priceResult, Boolean difProvider) {
        this.sysTransactionId = sysTransactionId;
        this.quotation = quotation;
        this.deliveryQuotationRepository = deliveryQuotationRepository;
        this.deliveryService = deliveryService;
        this.symplifiedService = symplifiedService;
        this.priceResult = priceResult;
        this.difProvider = difProvider;

    }


    public void run() {
        super.run();
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
        deliveryService.retryOrder(quotation.getId(), difProvider);
    }
}
