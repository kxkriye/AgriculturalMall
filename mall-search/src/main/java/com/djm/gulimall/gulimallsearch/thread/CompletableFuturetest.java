package com.djm.gulimall.gulimallsearch.thread;

import java.util.concurrent.*;

/**
 * @author djm
 * @create 2022-02-01 22:24
 */
public class CompletableFuturetest {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
//        System.out.println("main....start.....");
//        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//        }, executor);
//
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor);
//        Integer integer = future.get();
//
//        System.out.println("main....stop....." );

        //方法完成后的感知
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).whenComplete((res,exception) ->{
//            // 虽然能得到异常信息，但是没法修改返回的数据
//            System.out.println("异步任务成功完成了...结果是：" +res + "异常是：" + exception);
//        }).exceptionally(throwable -> {
//            // 可以感知到异常，同时返回默认值
//            return 10;
//        });
//        System.out.println(future.get());

        /**
         方法执行完成后的处理 ，和 complete 一样，可以对结果做最后的处理（可处理异常），可改变返回值
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).handle((res,thr) ->{
//            if (res != null ) {
//                return res * 2;
//            }
//            if (thr != null) {
//                return 0;
//            }
//            return 0;
//        });

        /**
         * 线程串行化，
         * 1、thenRun:不能获取到上一步的执行结果，无返回值
         * .thenRunAsync(() ->{
         *             System.out.println("任务2启动了....");
         *         },executor);
         * 2、能接受上一步结果，但是无返回值 thenAcceptAsync
         * 3、thenApplyAsync 能收受上一步结果，有返回值
         *
         */
//        CompletableFuture<String> stringCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).thenApplyAsync(res -> {
//            System.out.println("任务2启动了..." + res);
//            return "hello";
//        }, executor);
//
//        System.out.println("main....stop....."+stringCompletableFuture.get());

        /**
         * 两个都完成，最好同一类型，不同类型也不会报错
         */
        CompletableFuture<Integer> future01 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1当前线程：" + Thread.currentThread().getId());
            int i = 10 / 4;
            System.out.println("任务1结束：" + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return i;
        }, executor);

        CompletableFuture<Integer> future02 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2当前线程：" + Thread.currentThread().getId());
            System.out.println("任务2结束：");
            return 123;
        }, executor);

        // f1 和 f2 执行完成后在执行这个
//        future01.runAfterBothAsync(future02,() -> {
//            System.out.println("任务3开始");
//        },executor);

//         返回f1 和 f2 的运行结果
//        future01.thenAcceptBothAsync(future02,(f1,f2) -> {
//            System.out.println("任务3开始....之前的结果:" + f1 + "==>" + f2);
//        },executor);

//        // f1 和 f2 单独定义返回结果
//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
//            return f1 + ":" + f2 + "-> Haha";
//        }, executor);

//        System.out.println("main....end....." + future.get());

/**
 * 两个任务，只要有一个完成，我们就执行任务,两个CompletableFuture都要一个类型：报错
 * runAfterEnitherAsync：不感知结果，自己没有返回值
 * acceptEitherAsync：感知结果，自己没有返回值
 *  applyToEitherAsync：感知结果，自己有返回值
 */
//        future01.runAfterEitherAsync(future02,() ->{
//            System.out.println("任务3开始...之前的结果:");
//        },executor);
//
//        future01.acceptEitherAsync(future02,(res) -> {
//            System.out.println("任务3开始...之前的结果:" + res);
//        },executor);
//
//        CompletableFuture<String> future = future01.applyToEitherAsync(future02, res -> {
//            System.out.println("任务3开始...之前的结果：" + res);
//            return res.toString() + "->哈哈";
//        }, executor);
//        System.out.println(future.get());
//        System.out.println("main");

        //多任务组合
        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息");
            return "hello.jpg";
        },executor);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性");
            return "黑色+256G";
        },executor);

        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
            System.out.println("查询商品介绍");
            return "华为";
        },executor);

//        // 等待全部执行完
//        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
//        allOf.get();

        // 只需要有一个执行完
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
        anyOf.get();
        System.out.println("main....end....." + anyOf.get());

    }

}
