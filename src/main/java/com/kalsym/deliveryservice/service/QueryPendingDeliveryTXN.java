package com.kalsym.deliveryservice.service;

import com.kalsym.deliveryservice.controllers.DeliveryService;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;

@Service
public class QueryPendingDeliveryTXN {
//    @Autowired
//    DeliveryOrdersRepository deliveryOrdersRepository;
//    @Autowired
//    DeliveryService deliveryService;
//    @Autowired
//    private DeliveryQuotationRepository deliveryQuotationRepository;
//
//    @Scheduled(fixedRate = 50000)
//    public void dailyScheduler() throws ParseException {
//        String location = Thread.currentThread().getStackTrace()[1].getMethodName();
//
//        LogUtil.info("QueryPendingDeliveryTXN", location, "QUERY PENDING TXN", "");
//
//        List<DeliveryOrder> deliveryOrders = deliveryOrdersRepository.findBySystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
//        for (DeliveryOrder o : deliveryOrders) {
//            LogUtil.info("QueryPendingDeliveryTXN", location, "Request Cancel", "");
//            DeliveryQuotation quotation = deliveryService.getQuotaion(6055L);
//
//            HttpReponse result = deliveryService.cancelOrder(o.getId());
//            if (o.getTotalRequest() < 2) {
//                Order order = new Order();
//                Delivery delivery = new Delivery();
//                order.setCustomerId(o.getCustomerId());
//                order.setCartId(quotation.getCartId());
//                order.setStoreId(o.getStoreId());
//                String[] address = quotation.getDeliveryAddress().split(quotation.getDeliveryPostcode().concat(","));
//                delivery.setDeliveryAddress(address[0]);
//                delivery.setDeliveryCity(quotation.getDeliveryCity());
//                String[] state = address[1].split(",");
//                delivery.setDeliveryState(state[1]);
//                delivery.setDeliveryContactName(o.getDeliveryContactName());
//                order.setDelivery(delivery);
//                order.setVehicleType(VehicleType.CAR);
//                order.setDeliveryProviderId(o.getDeliveryProviderId());
//                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price", "");
//
//                HttpReponse getPrice = deliveryService.getPrice(order);
//                PriceResult priceResult = (PriceResult) getPrice.getData();
//                SubmitDelivery submitDelivery = new SubmitDelivery();
//                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", "");
//                deliveryService.submitOrder(o.getOrderId(), priceResult.refId, submitDelivery);
//
//            }
//        }
//
//    }
}
