package com.sedis.test;

import com.sedis.util.JsonUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.text.MessageFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class CurrencyCacheTest {

    public static void main(String[] args) {
        String configPath = "classpath:spring-context.xml";
        ApplicationContext context = new ClassPathXmlApplicationContext(configPath);
        final WaybillService service = (WaybillService) context.getBean("waybillService");
//        oneKey(service);
//        everySingleKey(service);
        randomKey(service);
    }

    private static void oneKey(final WaybillService service) {
        final int currency = 50;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(currency);
        final AtomicLong time = new AtomicLong(0L);
        final String code = "1";
        int count = 10000;
        final AtomicLong countless = new AtomicLong(count);
        service.findListById(code);
        for (int i = 0; i < count; i++) {
            final int index = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    List<Waybill> waybills = service.findListById(code);
                    long end = System.currentTimeMillis();
                    time.getAndAdd(end - start);
                    System.out.println(MessageFormat.format("current task is {0}, time is {1}, val is {2}", index, end - start, JsonUtils.beanToJson(waybills)));
                    countless.decrementAndGet();
                }
            });
        }
        while (true) {
            if (countless.get() == 0) {
                System.out.println(MessageFormat.format("oneKey.findListById with currency is {0} and taskcount is {1} and total_time is {2} and avg_time is {3}", currency, count, time.get(), time.get() / count));
                break;
            }
            LockSupport.parkNanos(service, 1000L * 1000L * 1000L);
            continue;
        }
        executor.shutdown();
    }

    private static void randomKey(final WaybillService service) {
        final int currency = 100;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(currency);
        final AtomicLong time = new AtomicLong(0L);
        int count = 10000;
        for (int i = 0; i < count; i++) {
            service.findListById(Integer.toString(i));
        }
        final Random random = new Random();
        final AtomicLong countless = new AtomicLong(count);
        for (int i = 0; i < count; i++) {
            final int index = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    List<Waybill> waybills = service.findListById(Integer.toString(random.nextInt(9999)));
                    long end = System.currentTimeMillis();
                    time.getAndAdd(end - start);
                    System.out.println(MessageFormat.format("current task is {0}, time is {1}, val is {2}", index, end - start, JsonUtils.beanToJson(waybills)));
                    countless.decrementAndGet();
                }
            });
        }
        while (true) {
            if (countless.get() == 0) {
                System.out.println(MessageFormat.format("randomKey.findListById with currency is {0} and taskcount is {1} and total_time is {2} and avg_time is {3}", currency, count, time.get(), time.get() / count));
                break;
            }
            LockSupport.parkNanos(service, 1000L * 1000L * 1000L);
            continue;
        }
        executor.shutdown();
    }

    private static void everySingleKey(final WaybillService service) {
        final int currency = 100;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(currency);
        final AtomicLong time = new AtomicLong(0L);
        int count = 10000;
        for (int i = 0; i < count; i++) {
            service.findListById(Integer.toString(i));
        }
        final AtomicLong countless = new AtomicLong(count);
        for (int i = 0; i < count; i++) {
            final int index = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    List<Waybill> waybills = service.findListById(Integer.toString(index));
                    long end = System.currentTimeMillis();
                    time.getAndAdd(end - start);
                    System.out.println(MessageFormat.format("current task is {0}, time is {1}, val is {2}", index, end - start, JsonUtils.beanToJson(waybills)));
                    countless.decrementAndGet();
                }
            });
        }
        while (true) {
            if (countless.get() == 0) {
                System.out.println(MessageFormat.format("everySingleKey.findListById with currency is {0} and taskcount is {1} and total_time is {2} and avg_time is {3}", currency, count, time.get(), time.get() / count));
                break;
            }
            LockSupport.parkNanos(service, 1000L * 1000L * 1000L);
            continue;
        }
        executor.shutdown();
    }

}
