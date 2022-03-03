package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SubmitDelivery {
    public String startPickScheduleDate;
    public String endPickScheduleDate;
    public String startPickScheduleTime;
    public String endPickScheduleTime;
    public boolean bulkOrder;

    @Override
    public String toString() {
        return "SubmitDelivery{" +
                "startPickScheduleDate='" + startPickScheduleDate + '\'' +
                ", endPickScheduleDate='" + endPickScheduleDate + '\'' +
                ", startPickScheduleTime='" + startPickScheduleTime + '\'' +
                ", endPickScheduleTime='" + endPickScheduleTime + '\'' +
                '}';
    }

}
