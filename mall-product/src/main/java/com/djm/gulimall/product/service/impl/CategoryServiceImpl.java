package com.djm.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.djm.gulimall.product.service.CategoryBrandRelationService;
import com.djm.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.product.dao.CategoryDao;
import com.djm.gulimall.product.entity.CategoryEntity;
import com.djm.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2、组装成父子的树形结构

        //2.1）、找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrens(menu,entities));
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());


        return level1Menus;
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> list = new ArrayList();
        findParentId(catelogId,list);
        Collections.reverse(list);

        return list.toArray(new Long[list.size()]);
    }

    /**
     * 级联更新所有的关联数据
     * @CacheEvict 失效模式
     * 1、同时进行多种缓存操作 @Caching
     * 2、指定删除某个分区下的所有数据 @CacheEvict(value = {"category"},allEntries = true)
     * 3、存储同一类型的数据，都可以指定成同一分区，分区名默认就是缓存的前缀
     *
     * @param category
     */
    @Caching(evict = {
            @CacheEvict(value = {"category"},key = "'getLevel1Categorys'"),
            @CacheEvict(value = {"category"},key = "'getCatelogJson'")
    })
//    @CacheEvict(value = {"category"},allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }
    /**
     * TODO 产生堆外内存溢出 OutOfDirectMemoryError
     * 1、SpringBoot2.0以后默认使用 Lettuce作为操作redis的客户端，它使用 netty进行网络通信
     * 2、lettuce 的bug导致netty堆外内存溢出，-Xmx300m netty 如果没有指定堆内存移除，默认使用 -Xmx300m
     *      可以通过-Dio.netty.maxDirectMemory 进行设置
     *   解决方案 不能使用 -Dio.netty.maxDirectMemory调大内存
     *   1、升级 lettuce客户端，2、 切换使用jedis
     *   redisTemplate:
     *   lettuce、jedis 操作redis的底层客户端，Spring再次封装
     * @return
     */



    /**
     * 1、每一个需要缓存的数据我们都需要指定放到那个名字的缓存【缓存分区的划分【按照业务类型划分】】
     * 2、@Cacheable({"category"})
     *      代表当前方法的结果需要缓存，如果缓存中有，方法不调用
     *      如果缓存中没有，调用方法，最后将方法的结果放入缓存
     * 3、默认行为:
     *      1、如果缓存中有，方法不用调用
     *      2、key默自动生成，缓存的名字:SimpleKey[](自动生成的key值)
     *      3、缓存中value的值，默认使用jdk序列化，将序列化后的数据存到redis
     *      3、默认的过期时间，-1
     *
     *    自定义操作
     *      1、指定缓存使用的key     key属性指定，接收一个SpEl
     *      2、指定缓存数据的存活时间  配置文件中修改ttl
     *      3、将数据保存为json格式
     *4、Spring-Cache的不足：
     *      1、读模式：
     *          缓存穿透:查询一个null数据，解决 缓存空数据：ache-null-values=true
     *          缓存击穿:大量并发进来同时查询一个正好过期的数据，解决:加锁 ？ 默认是无加锁(sync=true)加锁,解决击穿
     *          缓存雪崩:大量的key同时过期，解决：加上随机时间，Spring-cache-redis-time-to-live
     *       2、写模式：（缓存与数据库库不一致）
     *          1、读写加锁
     *          2、引入canal，感知到MySQL的更新去更新数据库
     *          3、读多写多，直接去数据库查询就行
     *
     *    总结：
     *      常规数据（读多写少，即时性，一致性要求不高的数据）完全可以使用SpringCache 写模式（ 只要缓存数据有过期时间就足够了）
     *
     *    特殊数据：特殊设计
     *      原理：
     *          CacheManager(RedisManager) -> Cache(RedisCache) ->Cache负责缓存的读写
     * @return
     */
    //value 缓存的别名
    // key redis中key的名称，默认是方法名称
    @Cacheable(value = {"category"},key = "#root.method.name",sync = false)
    //    @Cacheable(value = {"category"},key = "'getLevel1Category'")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        long l = System.currentTimeMillis();
        // parent_cid为0则是一级目录
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        System.out.println("耗费时间：" + (System.currentTimeMillis() - l));
        return categoryEntities;
    }
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        // 给缓存中放 json 字符串、拿出的是 json 字符串，还要逆转为能用的对象类型【序列化和反序列化】

        // 1、加入缓存逻辑，缓存中放的数据是 json 字符串
        // JSON 跨语言，跨平台兼容
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");

        if (StringUtils.isEmpty(catelogJSON)) {
            // 2、缓存没有，从数据库中查询
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatelogJsonFromDbWithRedisLock();

            return catelogJsonFromDb;
        }
        // 转换为我们指定的对象
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {});

        return result;
    }
    //map
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithLocalLock () {

        synchronized (this){
            String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
            if (!StringUtils.isEmpty(catelogJSON)) {
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
            return result;
            }
        }
        System.out.println("查询了数据库");

        List<CategoryEntity> categoryEntitiesAll = baseMapper.selectList(null);

        // 1、查询所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(categoryEntitiesAll,0L);
        // 2、封装数据封装成 map类型  key为 catId,value List<Catelog2Vo>
        Map<String, List<Catelog2Vo>> categoryList = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 1、每一个的一级分类，查询到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(categoryEntitiesAll,v.getCatId());
            // 2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    List<CategoryEntity> categoryEntities1 = getParent_cid(categoryEntitiesAll,l2.getCatId());
                    if (categoryEntities1 != null ){
                        // 2、分装成指定格式
                        List<Catelog2Vo.catelog3Vo> catelog3VoList = categoryEntities1.stream().map(l3 -> {
                            Catelog2Vo.catelog3Vo catelog3Vo = new Catelog2Vo.catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        // 3、设置分类数据
                        catelog2Vo.setCatalog3List(catelog3VoList);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        // 3、查询到数据，将数据转成 JSON 后放入缓存中
        String s = JSON.toJSONString(categoryList);
        redisTemplate.opsForValue().set("catelogJSON",s,1, TimeUnit.DAYS);
        return categoryList;
        }
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDbWithRedisLock () {
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "0",300,TimeUnit.SECONDS));
//        if (lock) {
//            // 加锁成功..执行业务
//            // 设置过期时间
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
//            Map<String,List<Catelog2Vo>> dataFromDb = getDataFromDB();
//            redisTemplate.delete("lock"); // 删除锁
//            return dataFromDb;
//        } else {
//            // 加锁失败，重试 synchronized()
//            // 休眠100ms重试
//            return getCatelogJsonFromDbWithRedisLock();
//        }



        String uuid = UUID.randomUUID().toString();
        // 设置值同时设置过期时间
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock",uuid,300,TimeUnit.SECONDS);
        if (lock) {
            System.out.println("获取分布式锁成功");
            // 加锁成功..执行业务
            // 设置过期时间,必须和加锁是同步的，原子的
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
            Map<String,List<Catelog2Vo>> dataFromDb;
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (lockValue.equals(uuid)) {
//                // 删除我自己的锁
//                redisTemplate.delete("lock"); // 删除锁
//            }
            try {
                //解决锁自动续期
                dataFromDb = getDataFromDB();
            } finally {
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                //删除锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDb;
        } else {
            // 加锁失败，重试 synchronized()
            // 休眠200ms重试
            System.out.println("获取分布式锁失败，等待重试");
            try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
            return getCatelogJsonFromDbWithRedisLock();
        }

    }

    private Map<String, List<Catelog2Vo>> getDataFromDB() {
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (!StringUtils.isEmpty(catelogJSON)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catelogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
        return result;
    }
        System.out.println("查询了数据库");

        List<CategoryEntity> categoryEntitiesAll = baseMapper.selectList(null);

        // 1、查询所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(categoryEntitiesAll,0L);
        // 2、封装数据封装成 map类型  key为 catId,value List<Catelog2Vo>
        Map<String, List<Catelog2Vo>> categoryList = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 1、每一个的一级分类，查询到这个一级分类的二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(categoryEntitiesAll,v.getCatId());
            // 2、封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    List<CategoryEntity> categoryEntities1 = getParent_cid(categoryEntitiesAll,l2.getCatId());
                    if (categoryEntities1 != null ){
                        // 2、分装成指定格式
                        List<Catelog2Vo.catelog3Vo> catelog3VoList = categoryEntities1.stream().map(l3 -> {
                            Catelog2Vo.catelog3Vo catelog3Vo = new Catelog2Vo.catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        // 3、设置分类数据
                        catelog2Vo.setCatalog3List(catelog3VoList);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        // 3、查询到数据，将数据转成 JSON 后放入缓存中
        String s = JSON.toJSONString(categoryList);
        redisTemplate.opsForValue().set("catelogJSON",s,1, TimeUnit.DAYS);
        return categoryList;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> categoryEntitiesAll,Long id) {

    return categoryEntitiesAll.stream().filter(m -> m.getParentCid()==id).collect(Collectors.toList());
    }





    private void findParentId(Long catelogId,List<Long> list) {
        list.add(catelogId);
        CategoryEntity categoryEntity = baseMapper.selectById(catelogId);
        if (categoryEntity.getParentCid()!=0){
            findParentId(categoryEntity.getParentCid(),list);
        }
    }


    private List<CategoryEntity> getChildrens(CategoryEntity menu, List<CategoryEntity> entities) {
        List<CategoryEntity> collect = entities.stream().filter(m -> {
            return menu.getCatId() == m.getParentCid();
        }).map(m ->{
            m.setChildren(getChildrens(m,entities));
            return m;
        }).sorted((menu1,menu2)->{
            //2、菜单的排序
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());
        return collect;
    }

}