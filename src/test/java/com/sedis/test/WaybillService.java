package com.sedis.test;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by yollock on 2016/9/26.
 */
public interface WaybillService {

    Waybill findById(String code);

    int updateById(String code);

    int deleteById(String code);

    List<Waybill> findListById(String code);

    Waybill[] findArrayById(String code);

    Map<String, Waybill> findMapById(String code);

    Set<Waybill> findSetById(String code);

    String findStringById(String code);

    Integer findIntegerById(String code);

}
