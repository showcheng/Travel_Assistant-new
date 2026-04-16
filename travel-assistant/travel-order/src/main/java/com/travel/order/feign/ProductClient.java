package com.travel.order.feign;

import com.travel.common.response.Result;
import com.travel.order.feign.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 商品服务Feign客户端
 */
@FeignClient(name = "travel-product", url = "http://localhost:8082")
public interface ProductClient {

    /**
     * 根据ID获取商品信息
     */
    @GetMapping("/api/products/{id}")
    Result<ProductDTO> getProductById(@PathVariable("id") Long id);

    /**
     * 批量获取商品信息
     */
    @PostMapping("/api/products/batch")
    Result<List<ProductDTO>> getProductsByIds(@RequestBody List<Long> ids);

    /**
     * 扣减库存
     */
    @PostMapping("/api/products/{id}/decrease-stock")
    Result<Void> decreaseStock(@PathVariable("id") Long id, @RequestBody StockDecreaseRequest request);

    /**
     * 增加库存
     */
    @PostMapping("/api/products/{id}/increase-stock")
    Result<Void> increaseStock(@PathVariable("id") Long id, @RequestBody StockIncreaseRequest request);

    /**
     * 库存扣减请求
     */
    class StockDecreaseRequest {
        private Integer quantity;

        public StockDecreaseRequest() {
        }

        public StockDecreaseRequest(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    /**
     * 库存增加请求
     */
    class StockIncreaseRequest {
        private Integer quantity;

        public StockIncreaseRequest() {
        }

        public StockIncreaseRequest(Integer quantity) {
            this.quantity = quantity;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}