package com.sedis.test;

/**
 * Created by yangbo12 on 2016/9/26.
 */
public class Waybill {

    private String waybillCode;
    private Integer waybillStatus;

    public Waybill() {
    }

    public Waybill(String waybillCode, Integer waybillStatus) {
        this.waybillCode = waybillCode;
        this.waybillStatus = waybillStatus;
    }

    public String getWaybillCode() {
        return waybillCode;
    }

    public void setWaybillCode(String waybillCode) {
        this.waybillCode = waybillCode;
    }

    public Integer getWaybillStatus() {
        return waybillStatus;
    }

    public void setWaybillStatus(Integer waybillStatus) {
        this.waybillStatus = waybillStatus;
    }

    @Override
    public String toString() {
        return "Waybill{" +
                "waybillCode='" + waybillCode + '\'' +
                ", waybillStatus=" + waybillStatus +
                '}';
    }
}
