package com.kalsym.deliveryservice.provider;

import com.kalsym.deliveryservice.controllers.ProcessRequest;
import com.kalsym.deliveryservice.models.Order;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.models.daos.Provider;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author taufik
 */
public class ProviderThread extends Thread implements Runnable {

    private final String sysTransactionId;
    private final Provider provider;
    private final Order order;
    private final DeliveryOrder deliveryOrder;
    private final String spOrderId;
    private final HashMap providerConfig;
    private ProcessRequest caller;
    private String functionName;
    private Object requestBody;
    private SequenceNumberRepository sequenceNumberRepository;

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Order order, String functionName,
                          SequenceNumberRepository sequenceNumberRepository) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.order = order;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.sequenceNumberRepository = sequenceNumberRepository;
        this.deliveryOrder = null;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, String spOrderId, String functionName) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.spOrderId = spOrderId;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.order = null;
        this.deliveryOrder = null;
    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Object requestBody, String functionName) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.requestBody = requestBody;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.order = null;
        this.deliveryOrder = null;

    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, Order order, String functionName) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.order = order;
        this.deliveryOrder = null;

    }

    public ProviderThread(ProcessRequest caller, String sysTransactionId,
                          Provider provider, HashMap providerConfig, DeliveryOrder order, String functionName,
                          SequenceNumberRepository sequenceNumberRepository) {
        this.sysTransactionId = sysTransactionId;
        this.provider = provider;
        this.deliveryOrder = order;
        this.order = null;
        this.providerConfig = providerConfig;
        this.caller = caller;
        this.functionName = functionName;
        this.spOrderId = null;
        this.sequenceNumberRepository = sequenceNumberRepository;
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
        super.run();
        caller.addProviderThreadRunning();
        String logprefix = sysTransactionId;
        String location = "ProviderThread";

        try {

            //get the java class name from SPId->JavaClass mapping
            String className = "";
            if (functionName.equalsIgnoreCase("GetPrices")) {
                className = provider.getGetPriceClassname();
                LogUtil.info(logprefix, location, "GetPrices class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("SubmitOrder")) {
                className = provider.getSubmitOrderClassname();
                LogUtil.info(logprefix, location, "SubmitOrder class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("CancelOrder")) {
                className = provider.getCancelOrderClassname();
                LogUtil.info(logprefix, location, "CancelOrder class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("QueryOrder")) {
                className = provider.getQueryOrderClassname();
                LogUtil.info(logprefix, location, "QueryOrder class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("ProviderCallback")) {
                className = provider.getSpCallbackClassname();
                LogUtil.info(logprefix, location, "ProviderCallback class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetPickupDate")) {
                className = provider.getPickupDateClassname();
                LogUtil.info(logprefix, location, "GetPickupDate class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetPickupTime")) {
                className = provider.getPickupTimeClassname();
                LogUtil.info(logprefix, location, "GetPickupTime class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetLocationId")) {
                className = provider.getLocationIdClassname();
                LogUtil.info(logprefix, location, "GetLocationId class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetDriverDetails")) {
                className = provider.getDriverDetailsClassName();
                LogUtil.info(logprefix, location, "GetLocationId class name for SP ID:" + provider.getId() + " -> " + className, "");
            } else if (functionName.equalsIgnoreCase("GetAirwayBill")) {
                className = provider.getAirwayBillClassName();
                LogUtil.info(logprefix, location, "GetAirwayBill class name for SP ID:" + provider.getId() + " -> " + className, "");
            }
            Class classObject = Class.forName(className);
            DispatchRequest reqFactoryObj = null;
            CountDownLatch latch = new CountDownLatch(1);

            //get all constructors
            Constructor<?> cons[] = classObject.getConstructors();
            LogUtil.info(logprefix, location, "Constructors:" + cons[0].toString(), "");
            try {
                if (functionName.equalsIgnoreCase("QueryOrder") || functionName.equalsIgnoreCase("CancelOrder")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, spOrderId, this.sysTransactionId);
                } else if (functionName.equalsIgnoreCase("ProviderCallback")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, requestBody, this.sysTransactionId);
                } else if (functionName.equalsIgnoreCase("GetPickupDate") || functionName.equalsIgnoreCase("GetPickupTime") || functionName.equalsIgnoreCase("GetLocationId")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, order, this.sysTransactionId);
                }else if (functionName.equalsIgnoreCase("GetDriverDetails")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, deliveryOrder, this.sysTransactionId, this.sequenceNumberRepository);
                }
                else if (functionName.equalsIgnoreCase("GetAirwayBill")) {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, deliveryOrder, this.sysTransactionId, this.sequenceNumberRepository);
                } else {
                    reqFactoryObj = (DispatchRequest) cons[0].newInstance(latch, providerConfig, order, this.sysTransactionId, this.sequenceNumberRepository);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            LogUtil.info(logprefix, location, "Forking a new thread", "");

            Thread tReqFactory = new Thread(reqFactoryObj);
            tReqFactory.start();

            try {
                //wait until latch countdown happen
                latch.await(2, TimeUnit.MINUTES);
            } catch (Exception exp) {
                LogUtil.error(logprefix, location, "Error in awaiting", "", exp);
            }

            LogUtil.info(logprefix, location, "ProviderThread finish", "");
            ProcessResult response = reqFactoryObj.getProcessResult();
            if (functionName.equalsIgnoreCase("GetPrices")) {
                PriceResult priceResult = (PriceResult) response.returnObject;
                priceResult.providerId = provider.getId();
                caller.addPriceResult(priceResult);
            } else if (functionName.equalsIgnoreCase("SubmitOrder")) {
                SubmitOrderResult submitOrderResult = (SubmitOrderResult) response.returnObject;
                submitOrderResult.providerId = provider.getId();
                caller.setSubmitOrderResult(submitOrderResult);
            } else if (functionName.equalsIgnoreCase("CancelOrder")) {
                CancelOrderResult cancelOrderResult = (CancelOrderResult) response.returnObject;
                cancelOrderResult.providerId = provider.getId();
                caller.setCancelOrderResult(cancelOrderResult);
            } else if (functionName.equalsIgnoreCase("QueryOrder")) {
                QueryOrderResult queryOrderResult = (QueryOrderResult) response.returnObject;
                queryOrderResult.providerId = provider.getId();
                caller.setQueryOrderResult(queryOrderResult);
            } else if (functionName.equalsIgnoreCase("ProviderCallback")) {
                SpCallbackResult spCallbackResult = (SpCallbackResult) response.returnObject;
                spCallbackResult.providerId = provider.getId();
                caller.setCallbackResult(spCallbackResult);
            } else if (functionName.equalsIgnoreCase("GetPickupDate")) {
                GetPickupDateResult pickupDateResult = (GetPickupDateResult) response.returnObject;
                pickupDateResult.providerId = provider.getId();
                caller.setPickupDateResult(pickupDateResult);
            } else if (functionName.equalsIgnoreCase("GetPickupTime")) {
                GetPickupTimeResult pickupTimeResult = (GetPickupTimeResult) response.returnObject;
                pickupTimeResult.providerId = provider.getId();
                caller.setPickupTimeResult(pickupTimeResult);
            } else if (functionName.equalsIgnoreCase("GetLocationId")) {
                LocationIdResult locationIdResult = (LocationIdResult) response.returnObject;
                locationIdResult.providerId = provider.getId();
                caller.setLocationIdResult(locationIdResult);
            } else if (functionName.equalsIgnoreCase("GetDriverDetails")) {
                DriverDetailsResult driverDetailsResult = (DriverDetailsResult) response.returnObject;
                driverDetailsResult.providerId = provider.getId();
                caller.setDriverDetailsResult(driverDetailsResult);
            } else if (functionName.equalsIgnoreCase("GetAirwayBill")) {
                AirwayBillResult airwayBillResult = (AirwayBillResult) response.returnObject;
                airwayBillResult.providerId = provider.getId();
                caller.setAirwayBillResult(airwayBillResult);
            }
            LogUtil.info(logprefix, location, "Response code:" + response.resultCode + " string:" + response.resultString + " returnObject:" + response.returnObject, "");
        } catch (Exception exp) {
            LogUtil.error(logprefix, location, "Error in awaiting. Will not continue with other SP ", "", exp);
        }
        caller.deductProviderThreadRunning();
    }
}