package com.example.demo.lock;

import com.example.demo.base.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class TestController {

    @Autowired
    private DistributedLock lock;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private RedisUtil redisUtil;

    @PostMapping("/distributedLock")
    public String distributedLock(String key, String uuid, String secondsToLock, String userId) throws Exception{
//        String uuid = UUID.randomUUID().toString();
        Boolean locked = false;
        try {
            locked = lock.distributedLock(key, uuid, secondsToLock);
            if(locked) {
                log.info("userId:{} is locked - uuid:{}", userId, uuid);
                log.info("do business logic");
//                Integer stock=Integer.valueOf(String.valueOf(redisUtil.get("stock",1)));
//                if (stock>0&&stock<=500){
//                    redisUtil.set("stock",String.valueOf(stock-1),1);
//                }
                TimeUnit.MICROSECONDS.sleep(500);
            } else {
                log.info("userId:{} is not locked - uuid:{}", userId, uuid);
            }
        } catch (Exception e) {
            log.error("error", e);
        } finally {
            if(locked) {
                lock.distributedUnlock(key, uuid);
            }
        }
        return "ok";
    }

    @RequestMapping(value = "doTest",method = RequestMethod.POST)
    public void distrubtedLock() {
        String url = "http://localhost:8080/distributedLock";
        String uuid = "abcdefg";
//        log.info("uuid:{}", uuid);
        String key = "redisLock";
        String secondsToLive = "1";


        for(int i = 0; i < 1000; i++) {
            final int userId = i;
            new Thread(() -> {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("uuid", uuid);
                params.add("key", key);
                params.add("secondsToLock", secondsToLive);
                params.add("userId", String.valueOf(userId));
                String result = restTemplate.postForObject(url, params, String.class);
                System.out.println("-------------" + result);
            }
            ).start();
        }
    }

    @RequestMapping(value = "setStock",method = RequestMethod.POST)
    public void setStock() {
        redisUtil.set("stock","500",1);
    }


}
