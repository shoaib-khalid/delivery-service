package com.kalsym.deliveryservice.controllers;

import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.SubmitDelivery;
import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.repositories.DeliveryQuotationRepository;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.util.HashSet;
import java.util.Optional;

public class RetryThread extends Thread implements Runnable {


    private DeliveryService deliveryService;
    private DeliveryQuotationRepository deliveryQuotationRepository;
    private String sysTransactionId;
    private DeliveryQuotation quotation;
    private SymplifiedService symplifiedService;


    public RetryThread(DeliveryQuotation quotation, String sysTransactionId, DeliveryQuotationRepository deliveryQuotationRepository, DeliveryService deliveryService, SymplifiedService symplifiedService) {
        this.sysTransactionId = sysTransactionId;
        this.quotation = quotation;
        this.deliveryQuotationRepository = deliveryQuotationRepository;
        this.deliveryService = deliveryService;
        this.symplifiedService = symplifiedService;

    }


    public void run() {
        super.run();
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        Optional<DeliveryQuotation> quotation = deliveryQuotationRepository.findById(this.quotation.getId());

        Order order = new Order();
        Delivery delivery = new Delivery();
        order.setCustomerId(quotation.get().getCustomerId());
        order.setCartId(quotation.get().getCartId());
        order.setStoreId(quotation.get().getStoreId());
        String[] address = quotation.get().getDeliveryAddress().split(quotation.get().getDeliveryPostcode().concat(","));
        delivery.setDeliveryAddress(address[0].substring(0, address[0].length() - 1));
        delivery.setDeliveryCity(quotation.get().getDeliveryCity());
        String[] state = address[1].split(",");
        delivery.setDeliveryState(state[1]);
        delivery.setDeliveryContactName(quotation.get().getDeliveryContactName());
        delivery.setDeliveryContactPhone(quotation.get().getDeliveryContactPhone());
        delivery.setDeliveryPostcode(quotation.get().getDeliveryPostcode());
        order.setDelivery(delivery);
        order.setVehicleType(VehicleType.CAR);
        order.setDeliveryProviderId(quotation.get().getDeliveryProviderId());
        LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price : ", order.toString());

        HttpReponse getPrice = deliveryService.getPrice(order, "");
        HashSet<PriceResult> priceResult = (HashSet<PriceResult>) getPrice.getData();
        SubmitDelivery submitDelivery = new SubmitDelivery();
        LogUtil.info("QueryPendingDeliveryTXN", location, "priceResult ", priceResult.toString());
        Long refId = null;
        for (PriceResult res : priceResult) {
            if (res.providerId == this.quotation.getDeliveryProviderId()) {
                refId = res.refId;
            }
        }
        Optional<DeliveryQuotation> request = deliveryQuotationRepository.findById(refId);
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", this.quotation.getOrderId());
        HttpReponse response = deliveryService.placeOrder(this.quotation.getOrderId(), request.get(), submitDelivery);
        String orderStatus = "";
        if (response.getStatus() == 200) {
            orderStatus = "ASSIGNING_DRIVER";
        } else {
            orderStatus = "REQUESTING_DELIVERY_FAILED";
        }
        String res = symplifiedService.updateOrderStatus(this.quotation.getOrderId(), orderStatus);
    }
}
