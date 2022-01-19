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
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.repositories.DeliveryOrdersRepository;
import com.kalsym.deliveryservice.repositories.DeliveryQuotationRepository;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Service
public class QueryPendingDeliveryTXN {
    @Autowired
    DeliveryOrdersRepository deliveryOrdersRepository;
    @Autowired
    DeliveryService deliveryService;
    @Autowired
    DeliveryQuotationRepository deliveryQuotationRepository;
    @Autowired
    SymplifiedService symplifiedService;

    @Scheduled(cron = "${delivery-service:0 0/05 * * * ?}")
    public void dailyScheduler() throws ParseException {
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        LogUtil.info("QueryPendingDeliveryTXN", location, "QUERY PENDING TXN", "");

        List<DeliveryOrder> deliveryOrders = deliveryOrdersRepository.findBySystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
        for (DeliveryOrder o : deliveryOrders) {
            LogUtil.info("QueryPendingDeliveryTXN", location, "Request Cancel", "");

            if (o.getTotalRequest() < 2) {
                LogUtil.info("QueryPendingDeliveryTXN", location, "First Request Five After Minutes Place Order", "");

                HttpReponse result = deliveryService.cancelOrder(o.getId());
                Optional<DeliveryQuotation> quotation = deliveryQuotationRepository.findById(o.getDeliveryQuotationId());

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
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price", "");

                HttpReponse getPrice = deliveryService.getPrice(order);
                HashSet<PriceResult> priceResult = (HashSet<PriceResult>) getPrice.getData();
                SubmitDelivery submitDelivery = new SubmitDelivery();
                LogUtil.info("QueryPendingDeliveryTXN", location, "priceResult ", priceResult.toString());
                Long refId = null;
                for (PriceResult res : priceResult) {
                    if (res.providerId == o.getDeliveryProviderId()) {
                        refId = res.refId;
                    }
                }
                Optional<DeliveryQuotation> request = deliveryQuotationRepository.findById(refId);
//                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", o.getOrderId());
                deliveryService.placeOrder(o.getOrderId(), request.get(), submitDelivery);

            } else if (o.getTotalRequest() < 3) {
                LogUtil.info("QueryPendingDeliveryTXN", location, "Second Request After Ten Minutes Place Order", "");
                HttpReponse result = deliveryService.cancelOrder(o.getId());

                Optional<DeliveryQuotation> quotation = deliveryQuotationRepository.findById(o.getDeliveryQuotationId());

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
                order.setVehicleType(VehicleType.MOTORCYCLE);
                order.setDeliveryProviderId(quotation.get().getDeliveryProviderId());
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price", "");

                HttpReponse getPrice = deliveryService.getPrice(order);
                HashSet<PriceResult> priceResult = (HashSet<PriceResult>) getPrice.getData();
                SubmitDelivery submitDelivery = new SubmitDelivery();
                LogUtil.info("QueryPendingDeliveryTXN", location, "priceResult ", priceResult.toString());
                Long refId = null;
                for (PriceResult res : priceResult) {
                    if (res.providerId == o.getDeliveryProviderId()) {
                        refId = res.refId;
                    }
                }
                Optional<DeliveryQuotation> request = deliveryQuotationRepository.findById(refId);
//                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", o.getOrderId());
                HttpReponse placeOrder = deliveryService.placeOrder(o.getOrderId(), request.get(), submitDelivery);
                SubmitOrderResult submitOrderResult = (SubmitOrderResult) placeOrder.getData();
                BigDecimal priorityFee = BigDecimal.valueOf((request.get().getAmount() * 50) / 100);
                priorityFee = priorityFee.setScale(2, BigDecimal.ROUND_HALF_UP);
                deliveryService.addPriorityFee(submitOrderResult.orderCreated.getId(), priorityFee);

            } else if (o.getTotalRequest() < 4) {
                LogUtil.info("QueryPendingDeliveryTXN", location, "Third Request After Fifteen Minutes Place Order", "");
                HttpReponse result = deliveryService.cancelOrder(o.getId());

                Optional<DeliveryQuotation> quotation = deliveryQuotationRepository.findById(o.getDeliveryQuotationId());

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
                order.setVehicleType(VehicleType.MOTORCYCLE);
                order.setDeliveryProviderId(quotation.get().getDeliveryProviderId());
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Get Price", "");

                HttpReponse getPrice = deliveryService.getPrice(order);
                HashSet<PriceResult> priceResult = (HashSet<PriceResult>) getPrice.getData();
                SubmitDelivery submitDelivery = new SubmitDelivery();
                LogUtil.info("QueryPendingDeliveryTXN", location, "priceResult ", priceResult.toString());
                Long refId = null;
                for (PriceResult res : priceResult) {
                    if (res.providerId == o.getDeliveryProviderId()) {
                        refId = res.refId;
                    }
                }
                Optional<DeliveryQuotation> request = deliveryQuotationRepository.findById(refId);
//                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Submit Order", o.getOrderId());
                HttpReponse placeOrder = deliveryService.placeOrder(o.getOrderId(), request.get(), submitDelivery);
                SubmitOrderResult submitOrderResult = (SubmitOrderResult) placeOrder.getData();
                BigDecimal priorityFee = BigDecimal.valueOf((request.get().getAmount() * 100) / 100);
                priorityFee = priorityFee.setScale(2, BigDecimal.ROUND_HALF_UP);
                deliveryService.addPriorityFee(submitOrderResult.orderCreated.getId(), priorityFee);

            } else if (o.getTotalRequest() < 5) {
                LogUtil.info("QueryPendingDeliveryTXN", location, "Third Request After Fifteen Minutes Place Order", "");
                HttpReponse result = deliveryService.cancelOrder(o.getId());

                String orderStatus = "FAILED_FIND_DRIVER";
                String res = symplifiedService.updateOrderStatus(o.getOrderId(), orderStatus);

            }
        }

    }


}
