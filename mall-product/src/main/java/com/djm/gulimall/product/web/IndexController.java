package com.djm.gulimall.product.web;

import com.djm.gulimall.product.entity.CategoryEntity;
import com.djm.gulimall.product.feign.wareFeignService;
import com.djm.gulimall.product.service.CategoryService;
import com.djm.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author djm
 * @create 2022-01-22 22:34
 */
@Controller
public class IndexController {
    @Autowired
    CategoryService categoryService;
    @Autowired
    wareFeignService wareFeignService;
    @Autowired
    RedissonClient redission;
    @Autowired
    StringRedisTemplate redisTemplate;
    /**
     * 查询所有一级分类
     * @param model
     * @return
     */
    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){

        // select * from category where parent_id = 0
        //TODO 1、查询所有的一级分类
        List<CategoryEntity> categoryEntityList = categoryService.getLevel1Categorys();

        model.addAttribute("categorys",categoryEntityList);
        // 查询所有的一级分类
        return "index";
    }
    @GetMapping("/hello")
    @ResponseBody
    public String hello(){
        return "index";
    }

    /**
     * 查询完整分类数据
     * @return
     */
    @ResponseBody
    @RequestMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        Map<String, List<Catelog2Vo>> catelogJson = categoryService.getCatelogJson();
        return catelogJson;
    }
    @RequestMapping("/hello1")
    @ResponseBody
    public String hello1(){
        System.out.println("aa");
        // 1、获取一把锁，只要锁得名字一样，就是同一把锁
        RLock lock = redission.getLock("my-lock");
        System.out.println("bb");
        // 2、加锁
//        lock.lock(); // 阻塞式等待，默认加的锁都是30s时间
        // 1、锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s，不用担心业务时间长，锁自动过期后被删掉
        // 2、加锁的业务只要运行完成，即使不给当前锁续期(宕机)，即使不手动解锁，锁默认会在30s以后自动删除

        lock.lock(10, TimeUnit.SECONDS); //10s 后自动删除
        //问题 lock.lock(10, TimeUnit.SECONDS) 在锁时间到了后，不会自动续期
        // 1、如果我们传递了锁的超时时间，就发送给 redis 执行脚本，进行占锁，默认超时就是我们指定的时间
        // 2、如果我们为指定锁的超时时间，就是用 30 * 1000 LockWatchchdogTimeout看门狗的默认时间、
        //      只要占锁成功，就会启动一个定时任务，【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】,每隔10s就自动续期
        //      internalLockLeaseTime【看门狗时间】 /3,10s

        //最佳实践
        // 1、lock.lock(10, TimeUnit.SECONDS);省掉了整个续期操作，手动解锁

        try {
            System.out.println("加锁成功，执行业务..." + Thread.currentThread().getId());
            Thread.sleep(5000);
        } catch (Exception e) {

        } finally {
            // 解锁 将设解锁代码没有运行，reidsson会不会出现死锁
            System.out.println("释放锁...." + Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }
    /**
     * 保证一定能读取到最新数据，修改期间，写锁是一个排他锁（互斥锁，独享锁）读锁是一个共享锁
     * 写锁没释放读锁就必须等待
     * 读 + 读 相当于无锁，并发读，只会在 reids中记录好，所有当前的读锁，他们都会同时加锁成功
     * 写 + 读 等待写锁释放
     * 写 + 写 阻塞方式
     * 读 + 写 有读锁，写也需要等待
     * 只要有写的存在，都必须等待
     * @return String
     */
    @RequestMapping("/write")
    @ResponseBody
    public String writeValue() {

        RReadWriteLock lock = redission.getReadWriteLock("rw_lock");
        String s = "";
        RLock rLock = lock.writeLock();
        try {
            // 1、改数据加写锁，读数据加读锁
            rLock.lock();
            System.out.println("写锁加锁成功..." + Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
            redisTemplate.opsForValue().set("writeValue",s);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("写锁释放..." + Thread.currentThread().getId());
        }
        return s;
    }

    @RequestMapping("/read")
    @ResponseBody
    public String readValue() {
        RReadWriteLock lock = redission.getReadWriteLock("rw_lock");
        RLock rLock = lock.readLock();
        String s = "";
        rLock.lock();
        try {
            System.out.println("读锁加锁成功..." + Thread.currentThread().getId());
            s =  redisTemplate.opsForValue().get("writeValue");
            try { TimeUnit.SECONDS.sleep(3); } catch (InterruptedException e) { e.printStackTrace(); }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("读锁释放..." + Thread.currentThread().getId());
        }
        return s;
    }
    /**
     * 放假锁门
     * 1班没人了
     * 5个班级走完，我们可以锁们了
     * @return
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redission.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();//等待闭锁都完成

        return "放假了....";
    }
    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) {
        RCountDownLatch door = redission.getCountDownLatch("door");
        door.countDown();// 计数器减一

        return id + "班的人走完了.....";
    }
    /**
     * 车库停车
     * 3车位
     * @return
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redission.getSemaphore("park");
        boolean b = park.tryAcquire();//获取一个信号，获取一个值，占用一个车位

        return "ok=" + b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() {
        RSemaphore park = redission.getSemaphore("park");
//    可以一直释放
        park.release(); //释放一个车位

        return "ok";
    }

}
