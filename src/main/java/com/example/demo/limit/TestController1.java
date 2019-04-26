package com.example.demo.limit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Slf4j
public class TestController1 {

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("/distributedLimit")
    @DistriLimitAnno(limitKey="limit", limit = 10)
    public String distributedLimit(String userId) {
        log.info(userId);
        return "ok";
    }

    @RequestMapping(value = "doTest1",method = RequestMethod.POST)
    public void distrubtedLock() {
        String url = "http://localhost:8080/distributedLimit";
        String uuid = "abcdefg";
//        log.info("uuid:{}", uuid);
        String key = "redisLock";
        String secondsToLive = "1";


        for(int i = 0; i < 100; i++) {
            final int userId = i;
            new Thread(() -> {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("userId", String.valueOf(userId));
                String result = restTemplate.postForObject(url, params, String.class);
                System.out.println("-------------" + result);
            }
            ).start();
        }
    }
}
