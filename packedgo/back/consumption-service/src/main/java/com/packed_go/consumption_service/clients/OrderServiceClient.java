package com.packed_go.consumption_service.clients;

import com.packed_go.consumption_service.dtos.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", url = "${order.service.url:http://localhost:8084}")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{orderId}")
    OrderDTO getOrderById(@PathVariable("orderId") Long orderId);
}