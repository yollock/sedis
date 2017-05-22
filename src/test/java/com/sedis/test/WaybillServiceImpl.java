package com.sedis.test;

import com.sedis.cache.annotation.Cache;
import com.sedis.cache.annotation.CacheExpire;

import java.util.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by yollock on 2016/9/26.
 */
public class WaybillServiceImpl implements WaybillService {
    @Override
    @Cache(redisEnable = true, key = "waybill@args0")
    public Waybill findById(String code) {
        return new Waybill(code, 1);
    }

    @Override
    @Cache(redisEnable = true, key = "waybillList@args0")
    public List<Waybill> findListById(String code) {
        List waybills = new ArrayList();
        waybills.add(new Waybill(code, 1));
        waybills.add(new Waybill(code, 2));
        return waybills;
    }

    @Override
    @CacheExpire(key = "waybill@args0")
    public int updateById(String code) {
        return 1;
    }

    @Override
    @Cache(redisEnable = true, key = "waybillArray@args0")
    public Waybill[] findArrayById(String code) {
        Waybill[] waybills = new Waybill[3];
        waybills[0] = new Waybill(code, 1);
        waybills[1] = new Waybill(code, 2);
        waybills[2] = new Waybill(code, 3);
        return waybills;
    }

    @Override
    @Cache(redisEnable = true, key = "waybillMap@args0")
    public Map<String, Waybill> findMapById(String code) {
        Map<String, Waybill> waybillMap = new HashMap<String, Waybill>();
        waybillMap.put(code, new Waybill(code, 1));
        return waybillMap;
    }

    @Override
    @Cache(redisEnable = true, key = "waybillSet@args0")
    public Set<Waybill> findSetById(String code) {
        Set<Waybill> waybillSet = new HashSet<Waybill>();
        waybillSet.add(new Waybill(code, 1));
        waybillSet.add(new Waybill(code, 2));
        waybillSet.add(new Waybill(code, 3));
        return waybillSet;
    }

    @Override
    @Cache(redisEnable = true, key = "waybillString@args0")
    public String findStringById(String code) {
        return "string12312312";
    }

    @Override
    @Cache(redisEnable = true, key = "waybillInteger@args0")
    public Integer findIntegerById(String code) {
        return 12345678;
    }
}
