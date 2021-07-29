package com.kalsym.deliveryservice.models;

import com.kalsym.deliveryservice.provider.PriceResult;

import java.util.Set;

public class ScheduledDelivery {
    public Set<PriceResult> scheduledDelivery;
    public String scheduleDate;
    public String scheduleTime;
}
