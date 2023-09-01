package com.djm.gulimall.thirdparty;

import com.djm.gulimall.thirdparty.service.MsmService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallThirdPartyApplicationTests {
	@Autowired
MsmService msmService;
	@Test
	void contextLoads() {
		msmService.send("1234", "18023869735");
	}

}
