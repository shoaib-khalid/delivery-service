package com.kalsym.deliveryservice.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class Schedule {
    public String startPickScheduleDate;
    public String endPickScheduleDate;
    public String startPickScheduleTime;
    public String endPickScheduleTime;

    @Override
    public String toString() {
        return "Schedule{" +
                "startPickScheduleDate='" + startPickScheduleDate + '\'' +
                ", endPickScheduleDate='" + endPickScheduleDate + '\'' +
                ", startPickScheduleTime='" + startPickScheduleTime + '\'' +
                ", endPickScheduleTime='" + endPickScheduleTime + '\'' +
                '}';
    }
}
