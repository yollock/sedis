package com.sedis.test;

import com.sedis.cache.annotation.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by yollock on 2016/9/26.
 */
public class WaybillServiceImpl implements WaybillService {
    @Override
    @Cache(memoryEnable = true, redisEnable = true, key = "waybill@args0")
    public Waybill findById(String code) {
        LockSupport.parkNanos(this, 1000000000L);
        return new Waybill(code, 1);
    }

    @Override
    @Cache(redisEnable = true, key = "waybillList@args0")
    public List<Waybill> findListById(String code) {
        //LockSupport.parkNanos(this, 1000000000L);
        List waybills = new ArrayList();
        waybills.add(new Waybill(code, 1));
        waybills.add(new Waybill(code, 2));
        return waybills;
    }

    @Override
    @Cache(redisEnable = true, key = "waybillArray@args0")
    public Waybill[] findArrayById(String code) {
        //LockSupport.parkNanos(this, 1000000000L);
        Waybill[] waybills = new Waybill[3];
        waybills[0] = new Waybill(code, 1);
        waybills[1] = new Waybill(code, 2);
        waybills[2] = new Waybill(code, 3);
        return waybills;
    }

    @Override
    @Cache(redisEnable = true, key = "waybillMap@args0")
    public Map<String, Waybill> findMapById(String code) {
        //LockSupport.parkNanos(this, 1000000000L);
        Map<String, Waybill> waybillMap = new HashMap<String, Waybill>();
        waybillMap.put(code, new Waybill(code, 1));
        return waybillMap;
    }

    @Override
    @Cache(memoryEnable = true, redisEnable = true, key = "waybillString@args0")
    public String findStringById(String code) {
        LockSupport.parkNanos(this, 1000000000L);
        return "qawsxzcde2131231";
    }

    @Override
    @Cache(memoryEnable = true, redisEnable = true, key = "waybillInteger@args0")
    public Integer findIntegerById(String code) {
        LockSupport.parkNanos(this, 1000000000L);
        return 123123;
    }
}
