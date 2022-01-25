package com.kalsym.deliveryservice.provider.TCS;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.kalsym.deliveryservice.models.daos.DeliveryOrder;
import com.kalsym.deliveryservice.provider.AirwayBillResult;
import com.kalsym.deliveryservice.provider.ProcessResult;
import com.kalsym.deliveryservice.provider.SyncDispatcher;
import com.kalsym.deliveryservice.repositories.SequenceNumberRepository;
import com.kalsym.deliveryservice.utils.LogUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;


public class AirwayBill extends SyncDispatcher {
    private final String systemTransactionId;
    private final DeliveryOrder order;
    private final String logprefix;
    private final String location = "TCSGetAirwayBill";
    private String sessionToken;
    private String sslVersion = "SSL";
    private String airwayHtmlPath;


    public AirwayBill(CountDownLatch latch, HashMap config, DeliveryOrder order, String systemTransactionId, SequenceNumberRepository sequenceNumberRepository) {

        super(latch);
        this.systemTransactionId = systemTransactionId;
        logprefix = systemTransactionId;
        this.airwayHtmlPath = (String) config.get("airwayHtmlPath");
        this.order = order;
        LogUtil.info(logprefix, location, "TCS AirwayBill class initiliazed!!", "");
    }

    @Override
    public ProcessResult process() {
        LogUtil.info(logprefix, location, "Process start", "");
        ProcessResult response = new ProcessResult();

        try {


//            File invoiceHTML = new File("src/main/java/com/kalsym/deliveryservice/provider/TCS/airwayBillDesign.html");
            File invoiceHTML = new File(this.airwayHtmlPath);
            Document doc = Jsoup.parse(invoiceHTML, "UTF-8", "http://example.com/");


            Element consignmentNo = doc.getElementById("consignmentNo").text(order.getSpOrderId());
            Element postcodeRec = doc.getElementById("postcodeRec").text("");
            Element customerName = doc.getElementById("customerName").text(order.getDeliveryContactName());
            Element customerAdd1 = doc.getElementById("customerAdd1").text(order.getDeliveryAddress());
            Element customerPhoneNo = doc.getElementById("customerPhoneNo").text(order.getDeliveryContactPhone());


            Element senderPostcode = doc.getElementById("senderPostcode").text("");
            Element senderName = doc.getElementById("senderName").text(order.getPickupContactName());
            Element senderPhoneNo = doc.getElementById("senderPhoneNo").text(order.getPickupContactPhone());
            Element senderAddress = doc.getElementById("senderAddress").text(order.getPickupAddress());
            Element weight = doc.getElementById("weight").text(order.getTotalWeightKg().toString());

            Element sendDate = doc.getElementById("sendDate").text(new Date().toString());
            Element consignmentNo1 = doc.getElementById("consignmentNo1").text(order.getSpOrderId());
            Element customerPostcode1 = doc.getElementById("customerPostcode1").text("");
            Element customerName1 = doc.getElementById("customerName1").text(order.getDeliveryContactName());
            Element customerPhoneNo1 = doc.getElementById("customerPhoneNo1").text(order.getDeliveryContactPhone());
            Element customerAdd2 = doc.getElementById("customerAdd2").text(order.getDeliveryAddress());

            Element consignmentNo3 = doc.getElementById("consignmentNo3").text(order.getSpOrderId());
            Element customerPostcode2 = doc.getElementById("customerPostcode2").text("");
            Element customerName2 = doc.getElementById("customerName2").text(order.getDeliveryContactName());
            Element customerPhoneNo2 = doc.getElementById("customerPhoneNo2").text(order.getDeliveryContactPhone());
            Element customerAdd3 = doc.getElementById("customerAdd3").text(order.getDeliveryAddress());

            Element senderPostcode2 = doc.getElementById("senderPostcode2").text("");
            Element senderName2 = doc.getElementById("senderName2").text(order.getPickupContactName());
            Element senderPhoneNo2 = doc.getElementById("senderPhoneNo2").text(order.getPickupContactPhone());
            Element senderAddress2 = doc.getElementById("senderAddress2").text(order.getPickupAddress());

            Element date = doc.getElementById("date").text(new Date().toString());
            Element parcelWight = doc.getElementById("parcelWight").text(order.getTotalWeightKg().toString());


            String docHtml = doc.html();
            System.out.println("docHtml : " + docHtml);

            ByteArrayOutputStream invoicePdfOutput = new ByteArrayOutputStream();
//      Html to Pdf Converter
            ConverterProperties properties = new ConverterProperties();
            HtmlConverter.convertToPdf(docHtml, invoicePdfOutput, properties);

            AirwayBillResult airwayBillResult = new AirwayBillResult();
            airwayBillResult.providerId = order.getDeliveryProviderId();
            airwayBillResult.orderId = order.getOrderId();
            airwayBillResult.consignmentNote = invoicePdfOutput.toByteArray();
            response.resultCode = 0;
            response.returnObject = airwayBillResult;

//            response.returnObject = extractResponseBody(responses.getBody().toString());

            LogUtil.info(logprefix, location, "Process finish", "");

        } catch (Exception ex) {

            LogUtil.error(logprefix, location, "Exception error :", "", ex);
            response.resultCode = -1;
        }
        return response;
    }

}