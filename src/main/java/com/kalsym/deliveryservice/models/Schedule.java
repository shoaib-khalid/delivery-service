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
}
