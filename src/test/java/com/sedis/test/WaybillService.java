package com.sedis.test;

import java.util.List;
import java.util.Map;

/**
 * Created by yollock on 2016/9/26.
 */
public interface WaybillService {

    Waybill findById(String code);

    List<Waybill> findListById(String code);

    Waybill[] findArrayById(String code);

    Map<String, Waybill> findMapById(String code);

    String findStringById(String code);

    Integer findIntegerById(String code);

}
