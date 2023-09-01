package com.djm.gulimall.product;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.djm.gulimall.product.dao.AttrGroupDao;
import com.djm.gulimall.product.dao.SkuSaleAttrValueDao;
import com.djm.gulimall.product.entity.AttrEntity;
import com.djm.gulimall.product.service.AttrGroupService;
import com.djm.gulimall.product.service.AttrService;
import com.djm.gulimall.product.vo.Attr;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallProductApplicationTests {

	@Autowired
    AttrGroupDao AttrGroupDao;
	@Autowired
    SkuSaleAttrValueDao s;
	@Test
	public void contextLoads() {

	}

    @Test
    public void  asd(){


        System.out.println(AttrGroupDao.getSpuItem(6L,225L ));
    };


}
