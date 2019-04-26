package com.example.demo.miaosha;

import com.example.demo.limit.DistriLimitAnno;
import com.example.demo.limit.LimitAspect;
import com.example.demo.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
public class FlashSaleController {

    @Autowired
    OrderService orderService;
    @Autowired
    DistributedLock distributedLock;
    @Autowired
    LimitAspect limitAspect;
    //注意RedisTemplate用的String,String，后续所有用到的key和value都是String的
    @Autowired
    RedisTemplate<String, String> redisTemplate;
    @Autowired
    RestTemplate restTemplate;

    private static final String LOCK_PRE = "LOCK_ORDER";

    @PostMapping("/initCatalog")
    @ResponseBody
    public String initCatalog()  {
        try {
            orderService.initCatalog();
        } catch (Exception e) {
            log.error("error", e);
        }

        return "init is ok";
    }

    @PostMapping("/placeOrder")
    @ResponseBody
    @DistriLimitAnno(limitKey = "limit", limit = 100, seconds = "1")
    public String  placeOrder(Long orderId) {
        Long saleOrderId = 0L;
        boolean locked = false;
        String key = LOCK_PRE + orderId;
        String uuid = String.valueOf(orderId);
        try {
            locked = distributedLock.distributedLock(key, uuid,
                    "10" );
            if(locked) {
                //直接操作数据库
//                saleOrderId = orderService.placeOrder(orderId);
                //操作缓存 异步操作数据库
                saleOrderId = orderService.placeOrderWithQueue(orderId);
            }
            log.info("saleOrderId:{}", saleOrderId);
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            if(locked) {
                distributedLock.distributedUnlock(key, uuid);
            }
        }
        return saleOrderId.toString();
    }

    @PostMapping("/miaosha")
    public void flashsaleTest() {
        String url = "http://localhost:8080/placeOrder";
        for(int i = 0; i < 3000; i++) {
            try {
                TimeUnit.MILLISECONDS.sleep(30);
                new Thread(() -> {
                    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                    params.add("orderId", "3");
                    String result = restTemplate.postForObject(url, params, String.class);
                    if(!result.equals("0")) {
                        System.out.println("-------------" + result);
                    }
                }
                ).start();
            } catch (Exception e) {
                log.info("error:{}", e.getMessage());
            }

        }
    }

}
