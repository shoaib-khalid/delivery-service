##############################################################################################
# **Version v.3.3.4| 10-March-2022*
###############################################################################################
1.Bug Fixed Confirmed Delivery Order

##############################################################################################
# **Version v.3.3.3| 27-Oct-2022*
###############################################################################################
1.Bug Fixed Query Order

##############################################################################################
# **Version v.3.3.2| 19-Oct-2022*
###############################################################################################
1.Bug Fixed Get Price


##############################################################################################
# **Version v.3.3.1| 04-Oct-2022*
###############################################################################################
1. Added New Provider PandaGo- New Api - Create Client Vendor Id
2. Change Datatype for Provider Id 
please update the alter table 
   1. delivery_sp
   2. store_delivery_sp
   3. delivery_Store_Sp
                              


##############################################################################################
# **Version v.3.3.0| 12-Sept-2022*
###############################################################################################
1. Added New Provider PandaGo- New Api
2. Please upload the pandaGo jar file before patch to production - delivery asset folder

##############################################################################################
# **Version v.3.2.10| 1-Sept-2022*
###############################################################################################
1. Bug Fixed For Lalamove
Lalamove Bug Fixed For Driver Details And Query Order


##############################################################################################
# **Version v.3.2.9| 29-Aug-2022*
###############################################################################################
1. Bug Fixed For Lalamove
   ALTER TABLE symplified.delivery_orders MODIFY COLUMN createdDate datetime NOT NULL;



##############################################################################################
# **Version v.3.2.8| 24-Aug-2022*
###############################################################################################
1. Pending Delivery Order Update order-service with tracking url and tracking no


##############################################################################################
# **Version v.3.2.7| 23-Aug-2022*
###############################################################################################
1. Order Service Update Tracking Url for Combined Shipping


##############################################################################################
# **Version v.3.2.6| 23-Aug-2022*
###############################################################################################
1. Delivery Sp Status Description - Updated
 * Please refer **Version v.3.2.1| 15-Aug-2022* for db table creation. 
 * For patch in production please copy staging table data to production.


##############################################################################################
# **Version v.3.2.5| 22-Aug-2022*
###############################################################################################
1. Bug Fixed - Query Unused Quotation


##############################################################################################
# **Version v.3.2.4| 19-Aug-2022*
###############################################################################################
1. Bug Fixed 



##############################################################################################
# **Version v.3.2.3| 17-Aug-2022*
###############################################################################################
1. Bug Fixed - Combined Shipping Add Restaurant Name List


##############################################################################################
# **Version v.3.2.2| 16-Aug-2022*
###############################################################################################
1. Bug Fixed - added new field in delivery quotation
2. Added New Error Message

##############################################################################################
# **Version v.3.2.1| 15-Aug-2022*
###############################################################################################

1. Bug Fixed

ALTER TABLE symplified.delivery_quotation ADD isCombinedDelivery TINYINT DEFAULT 0 NULL;

//Next Patch
CREATE TABLE symplified.delivery_sp_status (
provider_status varchar(100) NULL,
status varchar(100) NULL,
provider_status_description varchar(100) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COLLATE=utf8_general_ci;


-- 
delivery_error_description

CREATE TABLE symplified.delivery_error_description (
error varchar(100) NULL,
error_description varchar(100) NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COLLATE=utf8_general_ci;



##############################################################################################
# **Version v.3.2.0| 12-Aug-2022*
###############################################################################################

### Code Changes:
1. Added New Feature - Get Price - Combined Shipping 
2. Added New Feature - Submit Order - Combined Shipping 
3. Added New Feature - Callback Provider - Combined Shipping 
4. Self Delivery
ALTER TABLE symplified.state_delivery_charges ADD distance decimal(15,3) DEFAULT 5 NULL;

##############################################################################################
# **Version v.3.1.1| 3-Aug-2022**
###############################################################################################

### Code Changes:

1. Bug fixed lalamove bug fixed

##############################################################################################
# **Version v.3.0.3| 2-Aug-2022**
###############################################################################################

### Code Changes:

1. Added New Retry Option When Order Cancel with first provider choose by the customer
2. Bug Fix For lalamove callback v3

# **Version v.3.0.8| 4-Aug-2022**
###############################################################################################
###  Lalamove Callback Bug Fixed _ Callback Tracking Details Fixed

##############################################################################################
# **Version v.3.0.7| 26-July-2022**
###############################################################################################
###  Bykea Provider Bug Fixed _ Callback Tracking Details Fixed


##############################################################################################
# **Version v.3.0.6| 25-July-2022**
###############################################################################################
### Change Customer No To Merchant No. Bykea Provider Bug Fixed


##############################################################################################
# **Version v.3.0.5| 21 -July-2022**
###############################################################################################
### Bug Fixed Delivery Service Created Store Center Code

##############################################################################################
# **Version v.3.0.4| 20 -July-2022**
###############################################################################################
### Bug Fixed For QUery Order 


##############################################################################################
# **Version v.3.0.3| 20 -July-2022**
###############################################################################################
### Updated Date Time Response - chang at local


##############################################################################################
# **Version v.3.0.2| 20 -July-2022**
###############################################################################################

### Updated Date Time Response

##############################################################################################
# **Version v.2.11.8| 15 -July-2022**
###############################################################################################

### Bug fixed for get delivery order status

##############################################################################################
# **Version v.2.11.7| 06 -July-2022**
###############################################################################################

### Update Bug OrderAmount For Cod Payment

###############################################################################################
# **Version v.3.0.1| 19 -July-2022**
###############################################################################################

### Lalamove Testing - V3 In Staging

##############################################################################################
# **Version v.3.0.0| 07 -July-2022**
###############################################################################################

### Lalamove Testing - V3 In Staging

##############################################################################################
# **Version v.2.11.6| 06 -July-2022**
###############################################################################################

### Update PreAuthorize at every endpoint

##############################################################################################
# **Version v.2.11.5| 06 -July-2022**
###############################################################################################

### Bug Fixed Get Prices

##############################################################################################
# **Version v.2.11.4| 05 -July-2022**
###############################################################################################

### Code Changes:

1.Update Get Prices List

##############################################################################################
# **Version v.2.11.3| 05 -July-2022**
###############################################################################################
### Code Changes:

1.Order Status Endpoint Response Update

##############################################################################################
# **Version v.2.11.2| 04 -July-2022**
###############################################################################################

### Code Changes:

1.Bug Fixes to store status

##############################################################################################
# **Version v.2.11.1| 04 -July-2022**
###############################################################################################

### Code Changes:

1. Upgrade Version
   Api Get Quotation Accept Multiple Cart - Change endpoint url
2. Lalamove callback return to 200 OK

##############################################################################################
# **Version v.2.10.16| 04 -July-2022**
###############################################################################################

### Code Changes:

Api Get Quotation Accept Multiple Cart

##############################################################################################
# **Version v.2.10.15| 01 -July-2022**
###############################################################################################

### Code Changes:

added new api configuration for swyft to create pickup request ;

//update in delivery sp config table
INSERT INTO symplified.delivery_sp_config (spId,configField,configValue) VALUES
(9,'createPickupReqUrl','/create-pickup-request'),
(9,'timeSlotId','5ddba65337121f0012e26e2e');

##############################################################################################
# **Version v.2.10.14| 29 -June-2022**
###############################################################################################

### Code Changes:

Bug Fixed For Production

##############################################################################################
# **Version v.2.10.13| 29 -June-2022**
###############################################################################################

### Code Changes:

Added new logic for swyft create centre code

##############################################################################################
# **Version v.2.10.12| 27 -June-2022**
###############################################################################################

### Code Changes:

Production Hot Fix Get Price (service fee ) default Value is 0.00
update delivery discount charges

##############################################################################################
# **Version v.2.10.11| 24 -June-2022**
###############################################################################################

### Code Changes:

Production Hot Fix Get Price (service fee ) default Value is 0.00
update delivery discount charges

##############################################################################################
# **Version v.2.10.10| 24 -June-2022**
###############################################################################################

### Code Changes:

Production Hot Fix Get Price (service fee ) default Value is 0.00

##############################################################################################
# **Version v.2.10.9| 24 -June-2022**
###############################################################################################

### Code Changes:

Bug Fixed Bykea Provider And Get Price Markup Price

##############################################################################################
# **Version v.2.10.8| 22 -June-2022**
###############################################################################################

### Code Changes:

Uncomment Line For Delivery Order Status;

##############################################################################################
# **Version v.2.10.7| 21 -June-2022**
###############################################################################################

### Code Changes:

1. Bug Fix For Staging

ALTER TABLE symplified.delivery_service_charge MODIFY COLUMN startTime time NULL;
ALTER TABLE symplified.delivery_service_charge MODIFY COLUMN endTime time NULL;
ALTER TABLE symplified.delivery_service_charge ADD basedOnPrice tinyint DEFAULT 0 NOT NULL;
ALTER TABLE symplified.delivery_service_charge ADD basenOnTime tinyint DEFAULT 0 NOT NULL;

DROP FUNCTION IF EXISTS symplified.getMarkupPrice;
DELIMITER $$
$$
CREATE DEFINER=`root`@`%` FUNCTION `symplified`.`getMarkupPrice`(deliveryId VARCHAR(50), deliveryPrice decimal(10,2))
RETURNS decimal(10,2)
DETERMINISTIC
BEGIN

	DECLARE newPrice DECIMAL(10,2);
	DECLARE currentPrice DECIMAL(10,2);
	DECLARE serviceFees DECIMAL(10,2);

	
	SELECT dsc.serviceFee INTO serviceFees FROM symplified.delivery_service_charge dsc WHERE dsc.deliverySpId = deliveryId AND basedOnPrice = TRUE AND dsc.priceBelowRange >= deliveryPrice  LIMIT 1 ;
	IF (serviceFees != NULL) THEN
		SET newPrice = 	serviceFees + deliveryPrice;
		RETURN newPrice;
	ELSE
		SELECT dsc.serviceFee INTO serviceFees FROM symplified.delivery_service_charge dsc WHERE dsc.deliverySpId = deliveryId AND basedOnPrice = TRUE AND  deliveryPrice >=  dsc.priceBelowRange ORDER BY dsc.serviceFee DESC LIMIT 1;
		SET newPrice = 	serviceFees + deliveryPrice;
		RETURN newPrice;
	END IF;

END$$
DELIMITER ;

##############################################################################################
# **Version v.2.10.6| 20 -June-2022**
###############################################################################################

### Code Changes:

1. Production FIX

##############################################################################################
# **Version v.2.10.5| 17 -June-2022**
###############################################################################################

### Code Changes:

1. Bug Fixed Bykea Order Callback

##############################################################################################

# **Version v.2.10.4| 17 -June-2022**

###############################################################################################

### Code Changes:

1. Bug Fixed Bykea Callback

##############################################################################################

# **Version v.2.10.3| 17 -June-2022**

###############################################################################################

### Code Changes:

1. Bug Fixed Bykea Added Callback

##############################################################################################

# **Version v.2.10.2| 17 -June-2022**

###############################################################################################

### Code Changes:

1. Bug Fixed Bykea

##############################################################################################

# **Version v.2.10.1| 17 -June-2022**

###############################################################################################

### Code Changes:

1. Added New Table to store order status

##############################################################################################

# **Version v.2.10.0| 15 -June-2022**

###############################################################################################

### Code Changes:

1. Added New Provider Bykea

##############################################################################################

# **Version v.2.9.9| 07 -June-2022**

###############################################################################################

### Code Changes:

1. Fixed bug Get Price Class for All the provider

##############################################################################################

# **Version v.2.9.8| 07 -June-2022**

###############################################################################################

### Code Changes:

1. Store Id Bug Fixed During Confirm Delivery

##############################################################################################

# **Version v.2.9.7| 07 -June-2022**

###############################################################################################

### Code Changes:

1. Bug Fixed Retry Order
2. Bug Fixed for lat and long

##############################################################################################

# **Version v.2.9.6| 07 -June-2022**

###############################################################################################

### Code Changes:

1. Bug Fixed Retry Order

##############################################################################################

# **Version v.2.9.5| 03 -June-2022**

###############################################################################################

### Code Changes:

1. Store lat and long in db

##############################################################################################

# **Version v.2.9.4| 01-June-2022**

###############################################################################################

### Code Changes:

1.Bug Fixed for missing field to save db

2. easydukan rider submit order bug fixed - missing param to send

##############################################################################################

# **Version v.2.9.3| 01-June-2022**

###############################################################################################

### Code Changes:

Bug Fixed for Easy Dukan Rider

##############################################################################################

# **Version v.2.9.2| 31-May-2022**

###############################################################################################

### Code Changes:

Bug Fixed for Malaysia stores

##############################################################################################

# **Version v.2.9.1| 30-May-2022**

###############################################################################################

### Code Changes:

Version cannot see in staging
##############################################################################################

# **Version v.2.9.0| 30-May-2022**

###############################################################################################

### Code Changes:

1. Added Swyft delivery provider
2. Added Easy Dukan Rider - Still pending. Support only Next Day

ALTER TABLE symplified.delivery_quotation ADD deliveryContactEmail varchar(500) NULL;

##############################################################################################

# **Version v.2.8.9| 25-May-2022**

###############################################################################################

### Code Changes:

1. TCS bug fixed during confirm delivery
2. Pop up date fixed

##############################################################################################

# **Version v.2.8.8| 24-May-2022**

###############################################################################################

### Code Changes:

1. TCS bug fixed during confirm delivery

##############################################################################################

# Version v.2.8.7| 10-May-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - confirm delivery lat and log fixed

##############################################################################################

# Version v.2.8.6| 09-May-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - TCS bug fixed

##############################################################################################

# Version v.2.8.5| 29-April-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - PandaGo Provider
2. Added new function for PandaGo to query order status

##############################################################################################

# Version v.2.8.4| 27-April-2022

###############################################################################################

### Code Changes:

1. Added Panda go Delivery Within 5KM feature added.
2. Added Code Amount Added to During Submit
   ALTER TABLE symplified.delivery_orders ADD codAmount decimal(15,2) NULL;

##############################################################################################

# Version v.2.8.3| 26-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed remove lat and long throws exception in log -during confirm delivery

##############################################################################################

# Version v.2.8.2| 25-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed remove lat and long throws exception in log

##############################################################################################

# Version v.2.8.1| 22-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed remove latitude and longitude for delivery class.

##############################################################################################

# Version v.2.8.0| 21-April-2022

###############################################################################################

### Code Changes:

1. Added Back Split Service Module Back- Staging

##############################################################################################

# Version v.2.7.9| 20-April-2022

###############################################################################################

### Code Changes:

1. Production Lalamove Callback Patch- 0.0.3 - after patch 2.7.8 update the callback url in
   lalamove portal. Once update patch this version. In production.

##############################################################################################

# Version v.2.7.8| 20-April-2022

###############################################################################################

### Code Changes:

1. Production Lalamove Callback Patch- 0.0.2

##############################################################################################

# Version v.2.7.7| 20-April-2022

###############################################################################################

### Code Changes:

1. Production Lalamove Callback Patch- 0.0.1

##############################################################################################

# Version v.2.7.6| 20-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed - callback from the provider
2. Order status update to order-service been normalized;
3. swagger name change according to the endpoint

##############################################################################################

# Version v.2.7.5| 19-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed- Query Order - production
2. Retry Order Bug Fixed
   ##############################################################################################

# Version v.2.7.4| 19-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed- Query Order - production

##############################################################################################

# Version v.2.7.3| 19-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed- Query Order - production

##############################################################################################

# Version v.2.7.2| 18-April-2022

###############################################################################################

### Code Changes:

1. Bug fixed For query order and rider details- production patch

##############################################################################################

# Version v.2.7.1| 15-April-2022

###############################################################################################

### Code Changes:

1. Production Bug Fixed - Rollback V1
2. The one highlighted with "STAGING ONLY" don't patch in production

##############################################################################################

# Version v.2.7.1| 15-April-2022

###############################################################################################

### Code Changes:

1. Rollback V1-delivery-service

##############################################################################################

# Version v.2.7.0| 08-April-2022

###############################################################################################

### Code Changes:

1. Staging Testing

##############################################################################################

# Version v.2.6.25| 08-April-2022

###############################################################################################

### Code Changes:

1. Bug Fixed for Separate Module

##############################################################################################

# Version v.2.6.24| 08-April-2022

###############################################################################################

### Code Changes:

1. Bug Fixed for Separate Module

##############################################################################################

# Version v.2.6.23| 08-April-2022

###############################################################################################

### Code Changes:

1. Bug Fixed for Separate Module

##############################################################################################

# Version v.2.6.22| 08-April-2022 --STAGING ONLY

###############################################################################################

### Code Changes:

1. Testing delivery-v2
   -separate module
2. DB Changes

   ALTER TABLE symplified.delivery_sp ADD externalRequest TINYINT DEFAULT 0 NOT NULL;
   ALTER TABLE symplified.delivery_vehicle_types ADD `view` TINYINT(1) DEFAULT 1 NULL;
   ALTER TABLE symplified.delivery_sp ADD classLoaderName varchar(500) NULL;
   ALTER TABLE symplified.delivery_quotation ADD pickupLatitude varchar(100) NULL;
   ALTER TABLE symplified.delivery_quotation ADD pickupLongitude varchar(100) NULL;
   ALTER TABLE symplified.delivery_quotation ADD deliveryLatitude varchar(100) NULL;
   ALTER TABLE symplified.delivery_quotation ADD deliveryLongitude varchar(100) NULL;

##############################################################################################

# Version v.2.6.21| 07-April-2022

###############################################################################################

### Code Changes:

1. Bug Fixed For Mr Speedy Callback Rider Details Fixed
2. Final Status After Completer the Delivery

##############################################################################################

# Version v.2.6.20| 28-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Parameter type change

##############################################################################################

# Version v.2.6.19| 28-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Get delivery type added new field

ALTER TABLE symplified.delivery_sp ADD dialog TINYINT DEFAULT 0 NOT NULL;

ALTER TABLE symplified.delivery_remarks ADD providerId varchar(50) NOT NULL;
ALTER TABLE symplified.delivery_remarks MODIFY COLUMN providerId varchar(50) CHARACTER SET latin1 COLLATE
latin1_swedish_ci NOT NULL;

##############################################################################################

# Version v.2.6.18| 26-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Submit Order To MrSpeedy

##############################################################################################

# Version v.2.6.17| 24-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Update Status To Order Service

##############################################################################################

# Version v.2.6.16| 24-March-2022

###############################################################################################

### Code Changes:

1. Server Time Convert

##############################################################################################

# Version v.2.6.15| 24-March-2022

###############################################################################################

### Code Changes:

1. Added time at gmt for testing

##############################################################################################

# Version v.2.6.14| 21-March-2022

###############################################################################################

### Code Changes:

1.Added Vehicle Name    
DB Changes :  ALTER TABLE symplified.delivery_vehicle_types ADD name varchar(100) NULL;

##############################################################################################

# Version v.2.6.13| 16-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Submit Order For Lalamove Before Submit Checked Schedule Date More Before the
   current date.
2. Pickupp Added the Air

##############################################################################################

# Version v.2.6.12| 10-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Error Fixed For Production

##############################################################################################

# Version v.2.6.11| 10-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Logic error

##############################################################################################

# Version v.2.6.10| 10-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Get Price Logic Error Fixed- Cannot Read DB Value

##############################################################################################

# Version v.2.6.9| 09-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Jnt Query Order Transaction

##############################################################################################

# Version v.2.6.8| 08-March-2022

###############################################################################################

### Code Changes:

1. Added New Endpoint for Get Vehicle Type;
   ALTER TABLE symplified.delivery_vehicle_types ADD `view` TINYINT(1) DEFAULT 1 NULL;

##############################################################################################

# Version v.2.6.7| 07-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - For Query Order Status

##############################################################################################

# Version v.2.6.6| 07-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed For Bulk Order Processing Endpoint
2. Get Price Bug Fixed Response Body

##############################################################################################

# Version v.2.6.5| 04-March-2022

###############################################################################################

### Code Changes:

ALTER TABLE symplified.delivery_sp ADD retry TINYINT DEFAULT 0 NOT NULL;

1. Bug Fixed For PICKUPP AND Order Status Update to order service

##############################################################################################

# Version v.2.6.4| 04-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Schedule Date Bug Fixed

##############################################################################################

# Version v.2.6.3| 04-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed - Phone No Normalization- Get Price And Submit Order

##############################################################################################

# Version v.2.6.2| 04-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Get Price - Fixed Height And Weight And Length

##############################################################################################

# Version v.2.6.1| 04-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Get Price - Include Tax Price in the get quotation

##############################################################################################

# Version v.2.6.0| 03-March-2022

###############################################################################################

### Code Changes:

1. Production bug fixed- Rider Details and Query Status bug fixed

##############################################################################################

# Version v.2.5.9| 03-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Get Price

##############################################################################################

# Version v.2.5.8| 03-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Submit Order

##############################################################################################

# Version v.2.5.7| 03-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Submit Order Remove The Line

##############################################################################################

# Version v.2.5.6| 03-March-2022

###############################################################################################

### Code Changes:

1. Bug Fixed Get Price

### Release Note Changes:

1. Release Note Update

##############################################################################################

# Version v.2.5.5| 03-March-2022

###############################################################################################

### Code Changes:

1. Confirm Delivery Bug Fixed

- delivery_sp_config
  INSERT INTO SYMPLIFIED.DELIVERY_SP_CONFIG (spId,configField,configValue) VALUES
  (6,'domainUrl','https://gateway.my.pickupp.io/v2'),
  (6,'getprice_connect_timeout','10000'),
  (6,'getprice_url','/merchant/orders/quote'),
  (6,'getprice_wait_timeout','30000'),
  (6,'queryorder_connect_timeout','30000'),
  (6,'queryorder_url','https://gateway.my.pickupp.io/v2/merchant/orders/,?include_history=true'),
  (6,'queryorder_wait_timeout','30000'),
  (6,'serviceType','EXPRESS:express=120;FOURHOURS:four_hours=-1;SAMEDAY:same_day=-1;NEXTDAY:next_day=1;'),
  (6,'submitorder_connect_timeout','20000'),
  (6,'submitorder_url','/merchant/orders/single');
  INSERT INTO SYMPLIFIED.DELIVERY_SP_CONFIG (spId,configField,configValue) VALUES
  (6,'submitorder_wait_timeout','25000'),
  (6,'token','c3ltcGxpZmllZEBrYWxzeW0uY29tOjI1NDZmMjNjYjgxM2E5ZThiNjdmMzFhNWQ5MDk4MWVl'),
  (6,'trackingUrl','https://my.pickupp.io/en/tracking?orderNumber=');

- delivery_sp
- INSERT INTO SYMPLIFIED.DELIVERY_SP (
  ID,NAME,ADDRESS,CONTACTNO,CONTACTPERSON,GETPRICECLASSNAME,SUBMITORDERCLASSNAME,CANCELORDERCLASSNAME,QUERYORDERCLASSNAME,SPCALLBACKCLASSNAME,PICKUPDATECLASSNAME,PICKUPTIMECLASSNAME,LOCATIONIDCLASSNAME,PROVIDERIMAGE,REGIONCOUNTRYID,AIRWAYBILLCLASSNAME,DRIVERDETAILSCLASSNAME,ADDITIONALQUERYCLASSNAME,MINIMUMORDERQUANTITY,ADDPRIORITYCLASSNAME,SCHEDULEDATE,REMARK)
  VALUES
  ('6','PICKUPP',NULL,NULL,NULL,'com.kalsym.deliveryservice.provider.Pickupp.GetPrice','
  com.kalsym.deliveryservice.provider.Pickupp.SubmitOrder',NULL,'
  com.kalsym.deliveryservice.provider.Pickupp.QueryOrder',NULL,NULL,NULL,NULL,'https://symplified.it/delivery-assets/provider-logo/pickupp.png','MYS',NULL,NULL,NULL,1,NULL,0,0)
  ;

-delivery_period
CREATE TABLE `delivery_period` (
`id` varchar(20) NOT NULL,
`name` varchar(50) DEFAULT NULL,
`description` varchar(100) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3

values
INSERT INTO SYMPLIFIED.DELIVERY_PERIOD (ID,NAME,DESCRIPTION) VALUES
('EXPRESS','Express','Pickup between 30 min - 2 hours'),
('FOURDAYS','3-5 Days','Within city 2 days, intercity up to 5 days'),
('FOURHOURS','2-4 Hours','Pickup & Drop-off between 2 - 4 hours'),
('NEXTDAY','Next Day','Pickup & Delivery next day');

##############################################################################################

# Version v.2.5.4| 03-March-2022

###############################################################################################

### Code Changes:

1. Confirm Delivery Bug Fixed

##############################################################################################

# Version v.2.5.3| 02-March-2022

###############################################################################################

### Code Changes:

ALTER TABLE symplified.delivery_quotation ADD intervalTime INT NULL;

1. bug fixed get price

##############################################################################################

# Version v.2.5.2| 02-March-2022

###############################################################################################

### Code Changes:

1. Bug fixed for PICKUPP Query and Place Order Time
2. Get Price sort by merchant choose
3. Pickupp change time zone format

##############################################################################################

# Version v.2.5.1| 02-March-2022

###############################################################################################

### Code Changes:

1. Query Pending Transaction Pending time Changed
2. Pickupp Region country id change

##############################################################################################

# Version v.2.5.0| 01-March-2022

###############################################################################################

### Code Changes:

ALTER TABLE symplified.delivery_quotation ADD pickupTime DATETIME NULL;
ALTER TABLE symplified.delivery_quotation MODIFY COLUMN pickupTime varchar(200) NULL;

1. Bug fixed get price and confirm delivery

##############################################################################################

# Version v.2.4.9| 01-March-2022

###############################################################################################

### Code Changes:

1. patch this version order service in production order-service-3.7.0
2. Bug fixed get price and confirm delivery
3. Get price response body changed

##############################################################################################

# Version v.2.4.8| 25-February-2022

###############################################################################################

### Code Changes:

Bug Fixed - Added Length, Height, Weight, Width Configuration In Table.

ALTER TABLE symplified.delivery_quotation ADD fulfillmentType varchar(100) NULL;

CREATE TABLE symplified.delivery_vehicle_types (
vehicleType enum('MOTORCYCLE','CAR','VAN','PICKUP','LARGEVAN','SMALLLORRY','MEDIUMLORRY','LARGELORRY')CREATE TABLE
symplified.delivery_vehicle_types (
NOT NULL,
height DECIMAL(15,2) NOT NULL,
width DECIMAL(15,2) NOT NULL,
`length` DECIMAL(15,2) NOT NULL,
weight DECIMAL(15,2) NOT NULL
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COLLATE=utf8_general_ci;

INSERT INTO SYMPLIFIED.DELIVERY_VEHICLE_TYPES (VEHICLETYPE,HEIGHT,WIDTH,`length`,WEIGHT) VALUES
('MOTORCYCLE',30.00,30.00,30.00,10.00),
('CAR',50.00,50.00,50.00,40.00),
('PICKUP',120.00,90.00,90.00,200.00),
('VAN',170.00,120.00,120.00,500.00),
('SMALLLORRY',290.00,150.00,150.00,1000.00);

##############################################################################################

# Version v.2.4.7| 25-February-2022

###############################################################################################

### Code Changes:

Get Price Bug Fixed- Added Fulfillment Type in the Response

##############################################################################################

# Version v.2.4.6 | 21-February-2022

###############################################################################################

### Code Changes:

Bulk Order - Bug Fixed

##############################################################################################

# Version v.2.4.5 | 21-February-2022

###############################################################################################

### Code Changes:

Get Price backend logic enhance. Update Get quotaion response
CREATE TABLE `delivery_sp_type` (
`id` int NOT NULL AUTO_INCREMENT,
`deliverySpId` varchar(50) NOT NULL,
`deliveryType` enum('ADHOC','SCHEDULED','SELF') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
`regionCountry` varchar(3) NOT NULL,
`fulfilment` enum('EXPRESS','FOURHOURS','NEXTDAY','FOURDAYS') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT
NULL,
`interval` int DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

values
INSERT INTO s.delivery_sp_type (DELIVERYSPID,DELIVERYTYPE,REGIONCOUNTRY,FULFILMENT,`interval`) VALUES
('1','SCHEDULED','MYS','EXPRESS',0),
('1','SCHEDULED','MYS','FOURHOURS',0),
('3','SCHEDULED','MYS','EXPRESS',0),
('3','SCHEDULED','MYS','FOURHOURS',4),
('4','SCHEDULED','PAK','NEXTDAY',0),
('5','SCHEDULED','MYS','FOURDAYS',0),
('6','SCHEDULED','MYS','NEXTDAY',0),
('6','SCHEDULED','MYS','FOURHOURS',0),
('1','ADHOC','MYS','EXPRESS',NULL),
('3','ADHOC','MYS','FOURHOURS',4);
INSERT INTO SYMPLIFIED.DELIVERY_SP_TYPE (DELIVERYSPID,DELIVERYTYPE,REGIONCOUNTRY,FULFILMENT,`interval`) VALUES
('3','ADHOC','MYS','EXPRESS',0),
('6','ADHOC','MYS','EXPRESS',NULL);


[//]: # (ALTER TABLE symplified.delivery_sp_type ADD `interval` INT NULL;)

##############################################################################################

# Version v.2.4.4 | 21-February-2022

###############################################################################################

### Code Changes:

Bug fixed - Get Price - Fixed- For FNB

##############################################################################################

# Version v.2.4.3 | 18-February-2022

###############################################################################################

### Code Changes:

Bug fixed - Get Price - Fixed

##############################################################################################

# Version v.2.4.2 | 18-February-2022

###############################################################################################

### Code Changes:

Bug fixed - bulk order processing - fixed

##############################################################################################

# Version v.2.4.1 | 18-February-2022

###############################################################################################

### Code Changes:

Bug fixed - bulk order processing

##############################################################################################

# Version v.2.4.0 | 16-February-2022

###############################################################################################

### Code Changes:

1. New provider Integration  "PICKUPP"
    1. Update table delivery_sp_config - production credential
    2. Update table delivery_sp
        - getPriceClassName - com.kalsym.deliveryservice.provider.Pickupp.GetPrice
        - submitOrderClassName - com.kalsym.deliveryservice.provider.Pickupp.SubmitOrder
        - queryOrderClassName - com.kalsym.deliveryservice.provider.Pickupp.QueryOrder
        - spCallbackClassname - com.kalsym.deliveryservice.provider.Pickupp.OrderCallback
        -

ALTER TABLE symplified.delivery_orders ADD pickupTime TIME NULL;
ALTER TABLE symplified.delivery_orders ADD pickupdate TIMESTAMP NULL;

##############################################################################################

# Version v.2.3.20 | 16-February-2022

###############################################################################################

### Code Changes:

1. Bug fixed for schedule and adhoc delivery

##############################################################################################

# Version v.2.3.19 | 14-February-2022

###############################################################################################

### Code Changes:

1. Added new point for delivery type

CREATE TABLE symplified.delivery_main_type (
id BIGINT auto_increment NOT NULL,
`type` enum('SCHEDULED','ADHOC') NOT NULL,
CONSTRAINT delivery_main_type_PK PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COLLATE=utf8_general_ci;

ALTER TABLE symplified.delivery_main_type MODIFY COLUMN `type` enum('SCHEDULED','ADHOC','EXPRESS','4HOURS','SAMEDAY','
NEXTDAY','3-5DAYS') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;
ALTER TABLE symplified.delivery_main_type ADD main_id BIGINT NULL;

-values
INSERT INTO symplified.delivery_main_type (`type`,MAIN_ID) VALUES
('SCHEDULED',NULL),
('ADHOC',NULL),
('EXPRESS',2),
('4HOURS',2),
('SAMEDAY',2),
('NEXTDAY',1),
('3-5DAYS',1);

ALTER TABLE symplified.delivery_quotation CHANGE productCode `type` varchar(100) CHARACTER SET utf8mb4 COLLATE
utf8mb4_0900_ai_ci NULL;

ALTER TABLE symplified.delivery_quotation DROP COLUMN spId;

2. Get Price backend fixed the delivery vehicle type.

##############################################################################################

# Version v.2.3.18 | 10-February-2022

###############################################################################################

### Code Changes:

1. bug fixed
   ALTER TABLE symplified.delivery_sp ADD remark TINYINT NOT NULL;

##############################################################################################

# Version v.2.3.17 | 10-February-2022

###############################################################################################

### Code Changes:

1. Get Delivery Provider details response updated.

ALTER TABLE symplified.delivery_sp MODIFY COLUMN remarks INT NULL;
ALTER TABLE symplified.delivery_sp ADD minimumOrderQuantity INT NULL;

CREATE TABLE symplified.delivery_remarks (
id INT auto_increment NOT NULL,
title varchar(100) NOT NULL,
message varchar(500) NOT NULL,
CONSTRAINT delivery_remarks_PK PRIMARY KEY (id)
)
ENGINE=InnoDB
DEFAULT CHARSET=utf8
COLLATE=utf8_general_ci;

ALTER TABLE symplified.delivery_remarks MODIFY COLUMN id int auto_increment NOT NULL;
ALTER TABLE symplified.delivery_remarks ADD deliveryType varchar(100) NOT NULL;

values
INSERT INTO SYMPLIFIED.DELIVERY_REMARKS (TITLE,MESSAGE,DELIVERYTYPE) VALUES
('Minimum item for pickup does not meet','Please drop your shipment to nearest Logistic.','DROPSHIP'),
('Logistic provider will collect order item ','Logitstic partner will contact to you to for collect the order','PICKUP')
;

##############################################################################################

# Version v.2.3.16 | 09-February-2022

###############################################################################################

### Code Changes:

1. Get Delivery Provider endpoint updated - added remarks
   ALTER TABLE symplified.delivery_sp DROP COLUMN scheduleDate;
   ALTER TABLE symplified.delivery_sp ADD scheduleDate TINYINT DEFAULT 0 NOT NULL;

##############################################################################################

# Version v.2.3.15 | 08-February-2022

###############################################################################################

### Code Changes:

Request body changed for Bulk Order Processing

##############################################################################################

# Version v.2.3.14 | 27-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed- Bug fixed store id when submit delivery
##############################################################################################

# Version v.2.3.13 | 20-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed- Query J&T transaction every 30 minutes

##############################################################################################

# Version v.2.3.12 | 20-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed testing - Update systemStatus

##############################################################################################

# Version v.2.3.11 | 19-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed testing - Reasign rider if still cannot assign rider. return failed
status to order-service
- added rider status to -get rider details
- priority fee handle 2 decimal place

##############################################################################################

# Version v.2.3.10 | 19-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed testing - Reasign rider if still cannot assign rider. return failed
status to order-service

##############################################################################################

# Version v.2.3.9 | 14-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed testing - cron run every five minutes

- update order -service final call after place order

##############################################################################################

# Version v.2.3.8 | 14-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed testing - cron run every five minutes

##############################################################################################

# Version v.2.3.7 | 14-January-2022

###############################################################################################

### Code Changes:

Improved - Bug fixed testing

##############################################################################################

# Version v.2.3.6 | 14-January-2022

###############################################################################################

### Code Changes:

Added New Endpoint - Add Priority Fee

ALTER TABLE symplified.delivery_sp ADD addPriorityClassName varchar(100) NULL;
( for lalamove provider add this "com.kalsym.deliveryservice.provider.LalaMove.AddPriorityFee" )

ALTER TABLE symplified.delivery_orders ADD deliveryFee DECIMAL(15,2) NULL;
ALTER TABLE symplified.delivery_orders ADD priorityFee DECIMAL(15,2) NULL;

CREATE TABLE `delivery_completion_status` (
`status` varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,
`status_description` varchar(100) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,
PRIMARY KEY (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3

INSERT INTO symplified.delivery_completion_status (status,status_description) VALUES
('ASSIGNING_RIDER','Looking for rider '),
('AWAITING_PICKUP','Awaiting for rider to pickup the order'),
('BEING_DELIVERED','Rider on the way delivery the order'),
('CANCELED','Order has been Cancel'),
('COMPLETED','Order Delivery To Customer'),
('DELIVERED_TO_CUSTOMER','Delivered to customer'),
('EXPIRED','Cannot find the rider after 1 hour'),
('NEW_ORDER','Submit new order to delivery provider'),
('REJECTED','Rider rejected the order');

##############################################################################################

# Version v.2.3.5 | 12-January-2022

###############################################################################################

### Code Changes:

- Bug fixed - Callback delivery service
    - ongoing store driver id in the db.
      -if driver changed during ongoing store current driver details in the db.

##############################################################################################

# Version v.2.3.4 | 10-January-2022

###############################################################################################

### Code Changes:

- Added new endpoint - Get Quotation

##############################################################################################

# Version v.2.3.3 | 06-January-2022

###############################################################################################

### Code Changes:

- Testing Cron Job Order Cancel
  1.ALTER TABLE symplified.delivery_orders ADD systemStatus varchar(100) NULL;
  2.ALTER TABLE symplified.delivery_orders ADD CONSTRAINT delivery_orders_FK FOREIGN KEY (systemStatus) REFERENCES
  symplified.delivery_completion_status(status);
  ALTER TABLE symplified.delivery_orders ADD totalRequest BIGINT NULL;

##############################################################################################

# Version v.2.3.2 | 04-January-2022

###############################################################################################

### Code Changes:

1.Lalamove Callback Get 200 OK - testing purposes only - don't patch to production this version

##############################################################################################

# Version v.2.3.1 | 29-December-2021

###############################################################################################

### Code Changes:

1.Bug Fixed Callback Lalamove- Fixed -( lalamove callbak get rider id become null when order become cancel)

##############################################################################################

# Version v.2.3.0 | 27-December-2021

###############################################################################################

### Code Changes:

1.Added new endpoint to create the Cost Center Code for Pakistan Store
ALTER TABLE symplified.store ADD costCenterCode varchar(100) NULL;
ALTER TABLE symplified.delivery_sp ADD additionalQueryClassName varchar(100) NULL; add this value = ("
com.kalsym.deliveryservice.provider.TCS.TCSGenerateCostCenterCode")

##############################################################################################

# Version v.2.2.21 | 21-December-2021

###############################################################################################

### Code Changes:

1. Bug fixed if front end return null value to get price

##############################################################################################

# Version v.2.2.20 | 20-December-2021

###############################################################################################

### Code Changes:

1. Bug fixed for get rider details

##############################################################################################

# Version v.2.2.19 | 16-December-2021

###############################################################################################

### Code Changes:

1. Bug fixed if front end return null value to get price
2. Get delivery provider based on the country.

##############################################################################################

# Version v.2.2.18 | 13-December-2021

###############################################################################################

### Code Changes:

1. Bug fixes for BORZO delivery provider for get price return if return invalid phone, address,
   out of coverage
2. ALTER TABLE symplified.store_delivery_sp ADD storeCostCenterCode varchar(100) NULL;
    1. added the cost center code for Pakistan Store in store delivery sp table
3. Update the delivery sp table change delivery sp name for "Mr Speedy" to "BORZO"

##############################################################################################

# Version v.2.2.17 | 10-December-2021

##############################################################################################

### Code Changes:

1. Submit order fixed cost center code value
2. Delivery Zone City Add Cost Center Code column
   ALTER TABLE symplified.delivery_zone_city ADD costCenterCode varchar(100) NULL;

##################################################

# Version v.2.2.16 | 09-December-2021

##################################################

### Code Changes:

1. Bug fixed schedule delivery date handle null value

##################################################

# Version v.2.2.15 | 06-December-2021

##################################################

### Code Changes:

1. Consignment Note Generate Bug Fixed

##################################################

# Version v.2.2.14 | 06-December-2021

##################################################

### Code Changes:

1. Change Naming rule for consignment note
2. JNT_GET_PRICE bug fixed

##################################################

# Version v.2.2.13 | 03-December-2021

##################################################

### Code Changes:

1. Airway Bill Bug Fixed Can Generate Bill

##################################################

# Version v.2.2.12 | 03-December-2021

##################################################

### Code Changes:

1. Get Rider details bug fixed

##################################################

# Version v.2.2.11 | 03-December-2021

##################################################

### Code Changes:

1. Get Rider details bug fixed
2. JnT bug fixed for query status

##################################################

# Version v.2.2.10 | 02-December-2021

##################################################

### Code Changes:

1. TCS airwayBill bug fixed and add consignment number in the response

##################################################

# Version v.2.2.9 | 01-December-2021

##################################################

### Code Changes:

1. Update Version

##################################################

# Version v.2.2.8 | 01-December-2021

##################################################

### Code Changes:

1. Bug Fix Config From Application Properties

##################################################

# Version v.2.2.7 | 01-December-2021

##################################################

### Code Changes:

1. Bug Fix For JNT delivery Provider
2. Get Airway Bill Save file folder and Save the url into DB
3. Bug fixed for TCS submit order
4. Add upload path in application properties "/home/docker/Software/assets/delivery-assets/"
5. Add url for airway bill in Application Properties "https://symplified.it/delivery-assets/"

##################################################

# Version v.2.2.6 | 29-November-2021

##################################################

### Code Changes:

1. Update the Tcs Integration Code Bug fixed for get price

##################################################

# Version v.2.2.5 | 23-November-2021

##################################################

### Code Changes:

1. Update the Tcs Integration Code Back
2. Add deliveryServiceFee into (Delivery Quotation Table)
3. String pickupCity;
   String deliveryCity;
   String pickupZone;
   String deliveryZone;
   field in delivery quotation table

##################################################

# Version v.2.1.11 | 19-November-2021

##################################################

### Code Changes:

1. Driver Name Remove Space From String - bug

##################################################

# Version v.2.1.10 | 19-November-2021

##################################################

### Code Changes:

1. MrSpeedy Get Rider Details Bug fixed

##################################################
#Version v.2.1.8 | 19-November-2021
##################################################

### Code Changes:

1. Bug Fixed Get Rider Details.
2. Move the query rider details to during callback

##################################################

# Version v.2.1.7 | 18-Nov-2021

##################################################

### Code Changes:

1. Bug Fixed Remove Latest Patch DB Parameter

##################################################
#Version v.2.1.6 | 18-Nov-2021
##################################################

### Code Changes:

1. Production Server Bug Fixed (get rider details)
2. Callback Lalamove status bug fixed
3. Removed latest changes after version 2.2.0

##################################################
#Version v.2.2.4 | 17-Nov-2021
##################################################

### Code Changes:

1. Tcs Bug Fixed

DB:
delivery_quotation

1. deliveryCity varchar(100)
2. pickupCity varchar(100)
3. deliveryZone varchar(100)
4. pickupZone varchar(100)

##################################################
#Version v.2.2.3 | 17-Nov-2021
##################################################

### Code Changes:

1. Get Rider Details Bug Fixed

##################################################
#Version v.2.2.2 | 17-Nov-2021
##################################################

### Code Changes:

1. MarkupPrice added for delivery during get quotation

DB:
delivery_quotation

1. serviceFee DECIMAL(10,2)

CREATE TABLE `delivery_service_charge` (
`id` bigint NOT NULL AUTO_INCREMENT,
`deliverySpId` varchar(50) NOT NULL,
`startTime` varchar(50) DEFAULT NULL,
`endTime` varchar(50) DEFAULT NULL,
`serviceFee` decimal(10,2) NOT NULL,
`priceBelowRange` decimal(10,2) DEFAULT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3

2. Integrated TCS print airway bill

##################################################
#Version v.2.2.1 | 15-Nov-2021
##################################################

### Code Changes:

1. Airway Bill For Jnt Upload Into Server Part Done

##################################################
#Version v.2.2.0 | 12-Nov-2021
##################################################

### Code Changes:

1. Added New TCS Provider Integration
   DB:
   delivery_quotation
1. deliveryPostcode
2. pickupPostcode

provider

1. regionCountryId

CREATE TABLE `delivery_zone_city` (
`city` varchar(200) DEFAULT NULL,
`zone` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3

CREATE TABLE `delivery_zone_price` (
`id` bigint NOT NULL AUTO_INCREMENT,
`spId` varchar(50) NOT NULL,
`weight` decimal(15,2) NOT NULL,
`withInCity` decimal(15,2) NOT NULL,
`sameZone` decimal(15,2) NOT NULL,
`differentZone` decimal(15,2) NOT NULL,
PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3

##################################################
#Version v.2.1.5 | 11-Nov-2021
##################################################

### Code Changes:

1. Added New Parameter in Confirm Delivery Endpoint
2. JnT Provider Merger to Staging K875 with AirwayBill(testing)

##################################################
#Version v.2.1.3 | 10-Nov-2021
##################################################

### Code Changes:

1. Added New Endpoint to query delivery provider details

##################################################
#Version v.2.1.2 | 09-Nov-2021
##################################################

### Code Changes:

1. Added Authentication In Swagger Ui

##################################################
#Version v.2.1.1 | 02-Nov-2021
##################################################

### Code Changes:

1. New Endpoint For Get Driver Details with Consignment Note Url - added Delivery Provider Info

##################################################
#Version v.2.1.0 | 02-Nov-2021
##################################################

### Code Changes:

1. New Endpoint For Get Driver Details with Consignment Note Url

##################################################
#Version v.2.0.15 | 07-Oct-2021
##################################################

### Code Changes:

1. Bug Fixed

##################################################
#Version v.2.0.12 | 07-Oct-2021
##################################################

### Code Changes:

1. Fixed bug for user authentication in (Session Filter Class)
2. Move delivery item type under "ADHOC" validation check

##################################################
#Version v.1.0-FINAL | 22-March-2021
##################################################

### Code Changes:

Final version after internal demo on 19-March-2021