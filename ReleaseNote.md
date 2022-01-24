##############################################################################################
# Version v.2.3.12 | 20-January-2022
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
- udpate order -service final call after place order

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
2.ALTER TABLE symplified.delivery_orders ADD CONSTRAINT delivery_orders_FK FOREIGN KEY (systemStatus) REFERENCES symplified.delivery_completion_status(status);
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
    ALTER TABLE symplified.delivery_sp ADD additionalQueryClassName varchar(100) NULL; add this value = ("com.kalsym.deliveryservice.provider.TCS.TCSGenerateCostCenterCode")



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

1.  Driver Name Remove Space From String - bug

##################################################
# Version v.2.1.10 | 19-November-2021
##################################################
### Code Changes:

1.  MrSpeedy Get Rider Details Bug fixed 

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
#Version v.1.0-FINAL |  22-March-2021
##################################################
### Code Changes:

Final version after internal demo on 19-March-2021