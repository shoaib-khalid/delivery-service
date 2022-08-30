package com.kalsym.deliveryservice.service;

import com.kalsym.deliveryservice.controllers.DeliveryService;
import com.kalsym.deliveryservice.models.Delivery;
import com.kalsym.deliveryservice.models.HttpReponse;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.SubmitDelivery;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.DeliveryQuotation;
import com.kalsym.deliveryservice.models.daos.Provider;
import com.kalsym.deliveryservice.models.enums.DeliveryCompletionStatus;
import com.kalsym.deliveryservice.models.enums.VehicleType;
import com.kalsym.deliveryservice.provider.PriceResult;
import com.kalsym.deliveryservice.provider.SubmitOrderResult;
import com.kalsym.deliveryservice.repositories.DeliveryOrdersRepository;
import com.kalsym.deliveryservice.repositories.DeliveryQuotationRepository;
import com.kalsym.deliveryservice.repositories.ProviderRepository;
import com.kalsym.deliveryservice.service.utility.SymplifiedService;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class QueryPendingDeliveryTXN {
    @Autowired
    DeliveryOrdersRepository deliveryOrdersRepository;
    @Autowired
    DeliveryService deliveryService;
    @Autowired
    DeliveryQuotationRepository deliveryQuotationRepository;
    @Autowired
    ProviderRepository providerRepository;
    @Autowired
    SymplifiedService symplifiedService;

        @Scheduled(cron = "${delivery-service:0 0/05 * * * ?}")
    public void dailyScheduler() throws ParseException {
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        LogUtil.info("QueryPendingDeliveryTXN", location, "QUERY PENDING TXN TO RETRY", "");

        List<DeliveryOrder> deliveryOrders = deliveryOrdersRepository.findBySystemStatus(DeliveryCompletionStatus.ASSIGNING_RIDER.name());
        for (DeliveryOrder o : deliveryOrders) {
            Provider provider = providerRepository.findOneById(o.getDeliveryProviderId());
            if (provider.getRetry() == true) {
                LogUtil.info("QueryPendingDeliveryTXN", location, "Request Cancel", "");

                long currentTimestamp = System.currentTimeMillis();
                Date parsedDate = null;
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    parsedDate = dateFormat.parse(o.getCreatedDate());
                    System.err.println(" Parse Date : " + parsedDate);
                    Timestamp timestamp = new java.sql.Timestamp(parsedDate.getTime());
                } catch (Exception e) { //this generic but you can control another types of exception
                    // look the origin of excption
                    LogUtil.info("QueryPendingDeliveryTXN", location, "Exception = " + e.getMessage(), "");
                }
                assert parsedDate != null;
                long searchTimestamp = parsedDate.getTime();// this also gives me back timestamp in 13 digit (1425506040493)

                long difference = Math.abs(currentTimestamp - searchTimestamp);
                LogUtil.info("QueryPendingDeliveryTXN", location, "Time differences = " + difference + " " + (difference > 10 * 60 * 1000), "");

                if (difference > 10 * 60 * 1000) {
                    LogUtil.info("QueryPendingDeliveryTXN", location, "Print Here", "");

                    HttpReponse result = deliveryService.cancelOrder(o.getId(), "");
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

                    HttpReponse getPrice = deliveryService.getPrice(order, "");
                    HashSet<PriceResult> priceResult = (HashSet<PriceResult>) getPrice.getData();
                    SubmitDelivery submitDelivery = new SubmitDelivery();
                    LogUtil.info("QueryPendingDeliveryTXN", location, "priceResult ", priceResult.toString());
                    Long refId = null;
                    for (PriceResult res : priceResult) {
                        if (res.providerId != o.getDeliveryProviderId()) {
                            refId = res.refId;
                        }
                    }

                }
            } else {
                LogUtil.info("QueryPendingDeliveryTXN", location, "Provider Not Allowed :  " + provider.getName(), "");
            }
        }
    }


        @Scheduled(cron = "${pending-transaction:0 0/05 * * * ?}")
    public void QueryPendingTransaction() throws ParseException {
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        LogUtil.info("QueryPendingTXN", location, "QUERY PENDING TXN", "");
        List<String> status = new ArrayList<>();
        status.add(DeliveryCompletionStatus.COMPLETED.name());
        status.add(DeliveryCompletionStatus.CANCELED.name());
        status.add(DeliveryCompletionStatus.EXPIRED.name());
        status.add(DeliveryCompletionStatus.REJECTED.name());

        List<DeliveryOrder> deliveryOrders = deliveryOrdersRepository.findByStatusNotIn(status);
        for (DeliveryOrder order : deliveryOrders) {
            LogUtil.info("QueryPendingTXN", location, "Order Id : " + order.getOrderId(), "");
            System.err.println("Query Pending Transaction : " + order.getOrderId());
            deliveryService.queryOrder(order.getId(), "QueryPendingTransaction");

            LogUtil.info("QueryPendingTXN Status ", location, "Order Id : " + order.getOrderId() + " Order Status : " + order.getSystemStatus(), "");
        }
    }


    @Scheduled(cron = "${pending-transaction:0 0 23 * * ?}")
    public void RemovePendingQuotation() throws ParseException {
        String location = Thread.currentThread().getStackTrace()[1].getMethodName();

        LogUtil.info("QueryPendingTXN", location, "RemovePendingQuotation", "");

        List<DeliveryQuotation> deliveryQuotations = deliveryQuotationRepository.findAllByUnusedQuotation();
        for (DeliveryQuotation deliveryQuotation : deliveryQuotations) {
            long day30 = 30L * 24 * 60 * 60 * 1000;
            boolean olderThan30 = new Date().after(new Date(deliveryQuotation.getCreatedDate().getTime() + day30));
            if (olderThan30) {
                deliveryQuotationRepository.delete(deliveryQuotation);
                LogUtil.info("QueryUnusedQuotation", location, "After 30 Days deliveryQuotation Id : " + deliveryQuotation.getId(), "");
            } else
                LogUtil.info("QueryUnusedQuotation", location, "Before 30 Days deliveryQuotation Id : " + deliveryQuotation.getId(), "");

//            deliveryQuotationRepository.delete(deliveryQuotation);
        }
    }

}

