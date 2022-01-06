package com.kalsym.deliveryservice.service;

import com.kalsym.deliveryservice.controllers.OrdersController;
import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.SubmitDelivery;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.repositories.DeliveryOrdersRepository;
import com.kalsym.deliveryservice.repositories.DeliveryQuotationRepository;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.util.List;

@Service
public class QueryPendingDeliveryTXN {
    @Autowired
    DeliveryOrdersRepository deliveryOrdersRepository;
    @Autowired

    DeliveryQuotationRepository deliveryQuotationRepository;

    @Autowired
    OrdersController controller;

    @Scheduled(fixedRate = 50000)
    public void dailyScheduler() throws ParseException {
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        LogUtil.info("QueryPendingDeliveryTXN", location, "QUERY PENDING TXN", "");
        HttpServletRequest request = null;

        List<DeliveryOrder> deliveryOrders = deliveryOrdersRepository.findBySystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER);
        for (DeliveryOrder o : deliveryOrders) {
            LogUtil.info("QueryPendingDeliveryTXN", location, "Request Cancel", "");

            ResponseEntity<HttpReponse> result = controller.cancelOrder(request, o.getId());
            if (o.getTotalRequest() < 2) {
                Order order = new Order();
                Delivery delivery = new Delivery();
                order.setCustomerId(o.getCustomerId());
                DeliveryQuotation quotation = deliveryQuotationRepository.getOne(o.getDeliveryQuotationId());
                order.setCartId(quotation.getCartId());
                order.setStoreId(o.getStoreId());
                String[] address = quotation.getDeliveryAddress().split(quotation.getDeliveryPostcode().concat(","));
                delivery.setDeliveryAddress(address[0]);
                delivery.setDeliveryCity(quotation.getDeliveryCity());
                String[] state = address[1].split(",");
                delivery.setDeliveryState(state[1]);
                delivery.setDeliveryContactName(o.getDeliveryContactName());
                order.setDelivery(delivery);
                order.setVehicleType(VehicleType.CAR);
                order.setDeliveryProviderId(o.getDeliveryProviderId());
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price", "");

                ResponseEntity<HttpReponse> getPrice = controller.getPrice(request, order);
                PriceResult priceResult = (PriceResult) getPrice.getBody().getData();
                SubmitDelivery submitDelivery = new SubmitDelivery();
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", "");

                controller.submitOrder(request, priceResult.refId, o.getOrderId(), submitDelivery);

            }
        }

    }
}
