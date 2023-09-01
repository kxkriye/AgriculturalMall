package com.djm.gulimall.product.feign;

import com.djm.common.exception.BizCodeEnume;
import com.djm.common.utils.R;
import org.springframework.stereotype.Component;

/**
 * @author djm
 * @create 2022-03-10 22:25
 */
@Component
public class SeckillFeignServiceFallBack implements SeckillFeignService{

    @Override
    public R getSkuSeckilInfo(Long skuId) {
        System.out.println("熔断方法调用。。");
        return R.error(BizCodeEnume.TO_MANY_REQUEST.getCode(),BizCodeEnume.TO_MANY_REQUEST.getMsg());
    }
}
