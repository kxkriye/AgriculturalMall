package com.djm.gulimall.order.service.impl;
import static com.djm.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.djm.common.to.OrderTo;
import com.djm.common.to.mq.SeckillOrderTo;
import com.djm.common.utils.R;
import com.djm.common.vo.MemberRespVo;
import com.djm.gulimall.order.constant.OrderConstant;
import com.djm.gulimall.order.constant.PayConstant;
import com.djm.gulimall.order.entity.OrderItemEntity;
import com.djm.gulimall.order.entity.PaymentInfoEntity;
import com.djm.gulimall.order.enume.OrderStatusEnum;
import com.djm.gulimall.order.feign.CartFeignService;
import com.djm.gulimall.order.feign.MemberFeignService;
import com.djm.gulimall.order.feign.ProductFeignService;
import com.djm.gulimall.order.feign.WmsFeignService;
import com.djm.gulimall.order.interceptor.LoginUserInterceptor;
import com.djm.gulimall.order.service.OrderItemService;
import com.djm.gulimall.order.service.PaymentInfoService;
import com.djm.gulimall.order.to.OrderCreateTo;
import com.djm.gulimall.order.to.SpuInfoVo;
import com.djm.gulimall.order.vo.*;
//import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.djm.common.utils.PageUtils;
import com.djm.common.utils.Query;

import com.djm.gulimall.order.dao.OrderDao;
import com.djm.gulimall.order.entity.OrderEntity;
import com.djm.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

        private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();
        @Autowired
        WmsFeignService wmsFeignService;
        @Autowired
        CartFeignService cartFeignService;
        @Autowired
        MemberFeignService memberFeignService;
        @Autowired
        ThreadPoolExecutor threadPoolExecutor;
        @Autowired
        StringRedisTemplate redisTemplate;
        @Autowired
        ProductFeignService productFeignService;
        @Autowired
        OrderItemService orderItemService;
        @Autowired
        RabbitTemplate rabbitTemplate;
        @Autowired
        PaymentInfoService paymentInfoService;

        @Override
        public PageUtils queryPage(Map<String, Object> params) {
            IPage<OrderEntity> page = this.page(
                    new Query<OrderEntity>().getPage(params),
                    new QueryWrapper<OrderEntity>()
            );

            return new PageUtils(page);
        }

        @Override
        public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
            MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
            OrderConfirmVo confirmVo = new OrderConfirmVo();
            //TODO :获取当前线程请求头信息(解决Feign异步调用丢失请求头问题)
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

            //开启第一个异步任务
            CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {

                //每一个线程都来共享之前的请求数据
                RequestContextHolder.setRequestAttributes(requestAttributes);

                //1、远程查询所有的收获地址列表
                List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
                confirmVo.setMemberAddressVos(address);
            }, threadPoolExecutor);

            //开启第二个异步任务
            CompletableFuture<Void> cartInfoFuture = CompletableFuture.runAsync(() -> {

                //每一个线程都来共享之前的请求数据
                RequestContextHolder.setRequestAttributes(requestAttributes);

                //2、远程查询购物车所有选中的购物项
                List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
                confirmVo.setItems(currentCartItems);
                //feign在远程调用之前要构造请求，调用很多的拦截器
            }, threadPoolExecutor).thenRunAsync(() -> {
                List<OrderItemVo> items = confirmVo.getItems();
                //获取全部商品的id
                List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());

                //远程查询商品库存信息
                R skuHasStock = wmsFeignService.getSkuHasStock(skuIds);
                List<SkuStockVo> skuStockVos = skuHasStock.getData("data", new TypeReference<List<SkuStockVo>>() {});

                if (skuStockVos != null && skuStockVos.size() > 0) {
                    //将skuStockVos集合转换为map
                    Map<Long, Boolean> skuHasStockMap = skuStockVos.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                    confirmVo.setStocks(skuHasStockMap);
                }
            },threadPoolExecutor);

            //3、查询用户积分
            Integer integration = memberRespVo.getIntegration();
            confirmVo.setIntegration(integration);

            //4、价格数据自动计算

            //TODO 5、防重令牌(防止表单重复提交)
            /**
             * 接口幂等性就是用户对同一操作发起的一次请求和多次请求结果是一致的
             * 不会因为多次点击而产生了副作用，比如支付场景，用户购买了商品，支付扣款成功，
             * 但是返回结果的时候出现了网络异常，此时钱已经扣了，用户再次点击按钮，
             * 此时就会进行第二次扣款，返回结果成功，用户查询余额发现多扣钱了，
             * 流水记录也变成了两条。。。这就没有保证接口幂等性
             */
            // 先是再页面中生成一个随机码把他叫做token先存到redis中，然后放到对象中在页面进行渲染。
            // 用户提交表单的时候，带着这个token和redis里面去匹配如果一直那么可以执行下面流程。
            // 匹配成功后再redis中删除这个token，下次请求再过来的时候就匹配不上直接返回
            // 生成防重令牌
            String token = UUID.randomUUID().toString().replace("-", "");
            redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX+memberRespVo.getId(),token,30, TimeUnit.MINUTES);
            confirmVo.setOrderToken(token);


            CompletableFuture.allOf(addressFuture,cartInfoFuture).get();

            return confirmVo;
        }
//        @GlobalTransactional
        @Transactional
        @Override
        public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
            // 先将参数放到共享变量中，方便之后方法使用该参数
            confirmVoThreadLocal.set(vo);
            // 接收返回数据
            SubmitOrderResponseVo response = new SubmitOrderResponseVo();
            response.setCode(0);
            // 通过拦截器拿到用户的数据
            MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
            /**
             * 不使用原子性验证令牌
             *      1、用户带着两个订单，提交速度非常快，两个订单的令牌都是123，去redis里面查查到的也是123。
             *          两个对比都通过，然后来删除令牌，那么就会出现用户重复提交的问题，
             *      2、第一次差的快，第二次查的慢，只要没删就会出现这些问题
             *      3、因此令牌的【验证和删除必须保证原子性】
             *      String orderToken = vo.getOrderToken();
             *      String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
             *         if (orderToken != null && orderToken.equals(redisToken)) {
             *             // 令牌验证通过 进行删除
             *             redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
             *         } else {
             *             // 不通过
             *         }
             */
            // 验证令牌【令牌的对比和删除必须保证原子性】
            // 因此使用redis中脚本来进行验证并删除令牌
            // 0【删除失败/验证失败】 1【删除成功】
            String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            /**
             * redis lur脚本命令解析
             * if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end
             *  1、redis调用get方法来获取一个key的值，如果这个get出来的值等于我们传过来的值
             *  2、然后就执行删除，根据这个key进行删除，删除成功返回1，验证失败返回0
             *  3、删除否则就是0
             *  总结：相同的进行删除，不相同的返回0
             * 脚本大致意思
             * 0都是换取失败
             */
            // 拿到令牌
            String orderToken = vo.getOrderToken();
            /**
             * 	public <T> T execute(RedisScript<T> script // redis的脚本
             * 	    , List<K> keys // 对应的key 参数中使用了Array.asList 将参数转成list集合
             * 	    , Object... args) { // 要删除的值
             */
            // 原子验证和删除
            Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                    , Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId())
                    , orderToken);
            if (result  == 0L) { // 验证令牌验证失败
                // 验证失败直接返回结果
                response.setCode(1);
                return response;
            } else { // 原子验证令牌成功
                // 下单 创建订单、验证令牌、验证价格、验证库存
                // 1、创建订单、订单项信息
                OrderCreateTo order = createOrder();
                // 2、应付总额
                BigDecimal payAmount = order.getOrder().getPayAmount();
                // 应付价格
                BigDecimal payPrice = vo.getPayPrice();
                /**
                 * 电商项目对付款的金额精确到小数点后面两位
                 * 订单创建好的应付总额 和购物车中计算好的应付价格求出绝对值。
                 */
                if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {// 金额对比成功 保存订单
                    saveOrder(order);
                    // 创建锁定库存Vo
                    WareSkuLockVo wareSkuLockedVo = new WareSkuLockVo();
                    // 准备好商品项
                    List<OrderItemVo> lock = order.getOrderItems().stream().map(orderItemEntity -> {
                        OrderItemVo orderItemVo = new OrderItemVo();
                        // 商品购买数量
                        orderItemVo.setCount(orderItemEntity.getSkuQuantity());
                        // skuid 用来查询商品信息
                        orderItemVo.setSkuId(orderItemEntity.getSkuId());
                        // 商品标题
                        orderItemVo.setTitle(orderItemEntity.getSkuName());
                        return orderItemVo;
                    }).collect(Collectors.toList());
                    // 订单号
                    wareSkuLockedVo.setOrderSn(order.getOrder().getOrderSn());
                    // 商品项
                    wareSkuLockedVo.setLocks(lock);
                    // 远程调用库存服务锁定库存
                    R r = wmsFeignService.orderLockStock(wareSkuLockedVo);
//                    int i = 10 /0;
                    if (r.getCode() == 0) { // 库存锁定成功
                        // 将订单对象放到返回Vo中
                        response.setOrder(order.getOrder());
                        // 设置状态码
                        response.setCode(0);
//                         订单创建成功发送消息给MQ
                        rabbitTemplate.convertAndSend("order-event-exchange"
                                ,"order.create.order"
                                ,order.getOrder());
                        return response;
                    } else {
                        // 远程锁定库存失败
                        response.setCode(3);
//                        throw new NoStockException("没有库存");  没有库存不应该 创建订单不删，应该抛异常回滚之前操作
                        return response;
                    }
                } else {
                    // 商品价格比较失败
                    response.setCode(2);
                    return response;
                }
            }
        }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));

        return orderEntity;

    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {

        OrderEntity orderInfo = this.getOne(new QueryWrapper<OrderEntity>().
                eq("order_sn",orderEntity.getOrderSn()));
        //关闭订单之前先查询一下数据库，判断此订单状态是否已支付
        //害怕刚刚关单就支付完成，保证幂等性，库存服务也要做相应的验证
        //设置关单时间要比支付时间慢
        if (orderInfo.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            //代付款状态进行关单
            //不能用之前队列传过来的对象，过了30分钟很多值可能不一致
            OrderEntity orderUpdate = new OrderEntity();
            orderUpdate.setId(orderInfo.getId());
            orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderUpdate);

            // 发送消息给MQ
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderInfo, orderTo);

            try {
                //TODO 确保每个消息发送成功，给每个消息做好日志记录，(给数据库保存每一个详细信息)保存每个消息的详细信息
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                //TODO 定期扫描数据库，重新发送失败的消息
            }
        }


    }

    @Override
    public PayVo getOrderPay(String orderSn) {

        PayVo payVo = new PayVo();
        OrderEntity orderInfo = this.getOrderByOrderSn(orderSn);

        //保留两位小数点，向上取值
        BigDecimal payAmount = orderInfo.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());
        payVo.setOut_trade_no(orderInfo.getOrderSn());

        //查询订单项的数据
        List<OrderItemEntity> orderItemInfo = orderItemService.list(
                new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity orderItemEntity = orderItemInfo.get(0);

        payVo.setBody(orderItemEntity.getSkuAttrsVals());

        payVo.setSubject(orderItemEntity.getSkuName());

        return payVo;


    }

    @Override
    public PageUtils listwithItem(Map<String, Object> params) {

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId())
        );

        page.setRecords(page.getRecords().stream().map(m ->{
            m.setOrderItemEntityList(orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn",m.getOrderSn())));
            return m;
        }).collect(Collectors.toList()));

        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {

        //保存交易流水信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        //添加到数据库中
        this.paymentInfoService.save(paymentInfo);

        //修改订单状态
        //获取当前状态
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            //支付成功状态
            String orderSn = asyncVo.getOut_trade_no(); //获取订单号
            this.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }

        return "success";

    }

    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        //TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        //保存订单
        this.save(orderEntity);

        //保存订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);

        orderItem.setSkuQuantity(orderTo.getNum());

        //保存商品的spu信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuBrand(spuInfoData.getBrandName());
        orderItem.setCategoryId(spuInfoData.getCatalogId());
        orderItem.setSkuPic(orderTo.getSkuPic());

        //保存订单项数据
        orderItemService.save(orderItem);
    }

    private void updateOrderStatus(String orderSn, Integer code, Integer alipay) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setModifyTime(new Date());
        orderEntity.setStatus(code);
        orderEntity.setPayType(alipay);
        this.baseMapper.update(orderEntity, new UpdateWrapper<OrderEntity>().eq("order_sn", orderSn));
    }


    private void saveOrder(OrderCreateTo orderCreateTo) {
        //获取订单信息
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        //保存订单
        this.baseMapper.insert(order);

        //获取订单项信息
        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        //批量保存订单项数据
        orderItemService.saveBatch(orderItems);
    }


        private OrderCreateTo createOrder() {
            OrderCreateTo orderCreateTo = new OrderCreateTo();
            // 1、生成订单号
            String orderSn = IdWorker.getTimeId();
            // 2、构建订单
            OrderEntity orderEntity = buildOrder(orderSn);
            // 3、构建订单项
            List<OrderItemEntity> itemEntities = builderOrderItems(orderSn);
            // 4、设置价格、积分相关信息
            computPrice(orderEntity,itemEntities);
            // 5、设置订单项
            orderCreateTo.setOrderItems(itemEntities);
            // 6、设置订单
            orderCreateTo.setOrder(orderEntity);
            return orderCreateTo;
        }

    private void computPrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        //总价
        BigDecimal total = new BigDecimal("0.0");
        //优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        //积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        //订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity orderItem : orderItemEntities) {
            //优惠价格信息
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());

            //总价
            total = total.add(orderItem.getRealAmount());

            //积分信息和成长值信息
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();

        }
        //1、订单价格相关的
        orderEntity.setTotalAmount(total);
        //设置应付总额(总额+运费)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);

        //设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);

        //设置删除状态(0-未删除，1-已删除)
        orderEntity.setDeleteStatus(0);


    }

    private OrderEntity buildOrder(String orderSn) {
            // 拿到共享数据
            OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
            // 用户登录登录数据
            MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

            OrderEntity orderEntity = new OrderEntity();
            // 设置订单号
            orderEntity.setOrderSn(orderSn);
            // 用户id
            orderEntity.setMemberId(memberRespVo.getId());
            // 根据用户收货地址id查询出用户的收获地址信息
            R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
            FareVo data = fare.getData(new TypeReference<FareVo>() {
            });
            //将查询到的会员收货地址信息设置到订单对象中
            // 运费金额
            orderEntity.setFreightAmount(data.getFare());
            // 城市
            orderEntity.setReceiverCity(data.getAddress().getCity());
            // 详细地区
            orderEntity.setReceiverDetailAddress(data.getAddress().getDetailAddress());
            // 收货人姓名
            orderEntity.setReceiverName(data.getAddress().getName());
            // 收货人手机号
            orderEntity.setReceiverPhone(data.getAddress().getPhone());
            // 区
            orderEntity.setReceiverRegion(data.getAddress().getRegion());
            // 省份直辖市
            orderEntity.setReceiverProvince(data.getAddress().getProvince());
            // 订单刚创建状态设置为 待付款，用户支付成功后将该该状态改成已付款
            orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
            // 自动确认时间
            orderEntity.setAutoConfirmDay(7);
            orderEntity.setConfirmStatus(0);
            orderEntity.setModifyTime(new Date());
            return orderEntity;


        }
        private List<OrderItemEntity> builderOrderItems(String orderSn) {
                // 获取购物车中选中的商品
                List<OrderItemVo> currentUserCartItem = cartFeignService.getCurrentCartItems();
                if (currentUserCartItem != null && currentUserCartItem.size() > 0) {
                    List<OrderItemEntity> collect = currentUserCartItem.stream().map(orderItemVo -> {
                        // 构建订单项
                        OrderItemEntity itemEntity = builderOrderItem(orderItemVo);
                        itemEntity.setOrderSn(orderSn);
                        return itemEntity;
                    }).collect(Collectors.toList());
                    return collect;
                }
                return null;
            }

    private OrderItemEntity builderOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1、根据skuid查询关联的spuinfo信息
        Long skuId = cartItem.getSkuId();
        R spuinfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoVo = spuinfo.getData(new TypeReference<SpuInfoVo>() {
        });
        // 2、设置商品项spu信息
        // 品牌信息
        itemEntity.setSpuBrand(spuInfoVo.getBrandId().toString());
        // 商品分类信息
        itemEntity.setCategoryId(spuInfoVo.getCatalogId());
        // spuid
        itemEntity.setSpuId(spuInfoVo.getId());
        // spu_name 商品名字
        itemEntity.setSpuName(spuInfoVo.getSpuName());

        // 3、设置商品sku信息
        // skuid
        itemEntity.setSkuId(skuId);
        // 商品标题
        itemEntity.setSkuName(cartItem.getTitle());
        // 商品图片
        itemEntity.setSkuPic(cartItem.getImage());
        // 商品sku价格
        itemEntity.setSkuPrice(cartItem.getPrice());
        // 商品属性以 ; 拆分
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        // 商品购买数量
        itemEntity.setSkuQuantity(cartItem.getCount());

        // 4、设置商品优惠信息【不做】
        // 5、设置商品积分信息
        // 赠送积分 移弃小数值
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        // 赠送成长值
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // 6、订单项的价格信息
        // 这里需要计算商品的分解信息
        // 商品促销分解金额
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        // 优惠券优惠分解金额
        itemEntity.setCouponAmount(new BigDecimal("0"));
        // 积分优惠分解金额
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 商品价格乘以商品购买数量=总金额(未包含优惠信息)
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        // 总价格减去优惠卷-积分优惠-商品促销金额 = 总金额
        origin.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        // 该商品经过优惠后的分解金额
        itemEntity.setRealAmount(origin);
        return itemEntity;

    }
    public void a(){
        //无效
        this.c();
        this.b();
        OrderServiceImpl o =  (OrderServiceImpl) AopContext.currentProxy();
            o.c();
            o.b();
    }
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void  c(){}

@Transactional(propagation = Propagation.REQUIRED)
public void  b(){}

}




























