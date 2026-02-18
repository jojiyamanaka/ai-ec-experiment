package com.example.aiec.config;

import com.example.aiec.modules.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!production-internal")
@RequestMapping("/test")
@Hidden
public class TestController {

    @GetMapping("/slow")
    public ApiResponse<String> slow() throws InterruptedException {
        Thread.sleep(10000);  // 10秒遅延
        return ApiResponse.success("Slow response");
    }
}
