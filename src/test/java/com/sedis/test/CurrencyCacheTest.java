package com.sedis.test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.text.MessageFormat;
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
                    service.findListById(code);
                    long end = System.currentTimeMillis();
                    time.getAndAdd(end - start);
                    System.out.println(MessageFormat.format("current task is {0} and time is {1}", index, end - start));
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
                    service.findListById(Integer.toString(random.nextInt(9999)));
                    long end = System.currentTimeMillis();
                    time.getAndAdd(end - start);
                    System.out.println(MessageFormat.format("current task is {0} and time is {1}", index, end - start));
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
                    service.findListById(Integer.toString(index));
                    long end = System.currentTimeMillis();
                    time.getAndAdd(end - start);
                    System.out.println(MessageFormat.format("current task is {0} and time is {1}", index, end - start));
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


    private static void findIntegerById(WaybillService service, String code) {
        long s9 = System.currentTimeMillis();
        System.out.println("findIntegerById, " + service.findIntegerById(code));
        long e9 = System.currentTimeMillis();
        System.out.println("f9 is " + (e9 - s9));
        long s10 = System.currentTimeMillis();
        System.out.println("findIntegerById, " + service.findIntegerById(code));
        long e10 = System.currentTimeMillis();
        System.out.println("f10 is " + (e10 - s10));

        System.out.println("==================================");
    }

    private static void findStringById(WaybillService service, String code) {
        long s7 = System.currentTimeMillis();
        System.out.println("findStringById, " + service.findStringById(code));
        long e7 = System.currentTimeMillis();
        System.out.println("f7 is " + (e7 - s7));
        long s8 = System.currentTimeMillis();
        System.out.println("findStringById, " + service.findStringById(code));
        long e8 = System.currentTimeMillis();
        System.out.println("f8 is " + (e8 - s8));

        System.out.println("==================================");
    }

    private static void findArrayById(WaybillService service, String code) {
        long s1 = System.currentTimeMillis();
        System.out.println("findArrayById, " + service.findArrayById(code));
        long e1 = System.currentTimeMillis();
        System.out.println("f1 is " + (e1 - s1));
        long s2 = System.currentTimeMillis();
        System.out.println("findArrayById, " + service.findArrayById(code));
        long e2 = System.currentTimeMillis();
        System.out.println("f2 is " + (e2 - s2));

        System.out.println("==================================");
    }

    private static void findMapById(WaybillService service, String code) {
        long s5 = System.currentTimeMillis();
        System.out.println("findMapById, " + service.findMapById(code));
        long e5 = System.currentTimeMillis();
        System.out.println("f5 is " + (e5 - s5));
        long s6 = System.currentTimeMillis();
        System.out.println("findMapById, " + service.findMapById(code));
        long e6 = System.currentTimeMillis();
        System.out.println("f6 is " + (e6 - s6));

        System.out.println("==================================");
    }

    private static void findListById(WaybillService service, String code) {
        long s3 = System.currentTimeMillis();
        System.out.println("findListById, " + service.findListById(code));
        long e3 = System.currentTimeMillis();
        System.out.println("f3 is " + (e3 - s3));
        long s4 = System.currentTimeMillis();
        System.out.println("findListById, " + service.findListById(code));
        long e4 = System.currentTimeMillis();
        System.out.println("f4 is " + (e4 - s4));
        System.out.println("==================================");
    }


}
