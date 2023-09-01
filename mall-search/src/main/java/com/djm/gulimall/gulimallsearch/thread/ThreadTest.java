package com.djm.gulimall.gulimallsearch.thread;

import javax.annotation.security.RunAs;
import java.util.concurrent.*;

/**
 * @author djm
 * @create 2022-02-01 21:49
 */
public class ThreadTest {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        ExecutorService service = Executors.newFixedThreadPool(3);
//或者
//        new ThreadPoolExecutor(corePoolSize,maximumPoolSize,keepAliveTime,TimeUnit,unit,workQueue,threadFactory,handler);
//        默认interger最大值队列
//        new LinkedBlockingQueue<>(123);
//        Executors.defaultThreadFactory();
//       默认
//        new ThreadPoolExecutor.AbortPolicy();
//        asd a = new asd();
//        FutureTask<Integer> futureTask = new FutureTask(a);
//        new Thread(futureTask).start();
//        Integer integer = futureTask.get();

//        区别;
//          1、2不能得到返回值。
//         3可以获取返回值
//         1、2、3都不能控制资源
//        4可以控制资源,性能稳定。
//        //我们以后再业务代码里面，以上三种启动线程的方式都不用。【将所有的多线程异步任务都交给线程池执行】
//        new Thread(()->System.out.println( "helLo" ) ).start();
//         当前系统中池只有一两个，每个异步任务，提交给线程池让他自己去执行就行**
//        *七大参数
//         * corePoolSize:[5]核心线程数[一直存在除非（alLowCoreThreadTimeDut)];线程池,创建好以后就准备就绪的线程数量，等待异步任务去执行
//                      5个Thread thread = new Thread();thread.start();
//        *maximumPooLSize: [200]最大线程数量;控制资源
//        * keepAliveTime:存活时间。如果当前的线程数量大于core数量。释放空闲的线程(maximumPoolSize-corePoolSize)。
//         只要线程空闲大于指定的keepALiveTime;
//        * unit:时间单位
//        * BLockingQueue<Runnable> workQueue:阻塞队列。如果任务有很多，就会将目前多的任务放在队列里面。
//        只要有线程空闲,就会去队列里面取出新的任务继续执行。
//        *threadFactory :线程的创建工厂。
//        * RejectedExecutionHandLer handLer:如果队列满了，按照我们指定的拒绝策略拒绝执行任务
        service.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("asd");
            }
        });
        System.out.println("main");

    }
   static class asd implements Callable<Integer>{

        @Override
        public Integer call() throws Exception {
            System.out.println("运行了call()");
            return 123;
        }
    }
    static class asda implements Runnable{


        @Override
        public void run() {

        }
    }
}
