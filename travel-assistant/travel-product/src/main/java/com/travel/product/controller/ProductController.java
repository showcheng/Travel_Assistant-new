package com.travel.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.response.Result;
import com.travel.product.dto.ProductCreateRequest;
import com.travel.product.dto.ProductUpdateRequest;
import com.travel.product.entity.Product;
import com.travel.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 商品控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "商品管理", description = "商品的增删改查接口")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "创建商品", description = "创建新的商品")
    public Result<Long> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        log.info("创建商品请求: {}", request.getName());
        Long productId = productService.createProduct(request);
        return Result.success(productId);
    }

    @PutMapping
    @Operation(summary = "更新商品", description = "更新商品信息")
    public Result<Void> updateProduct(@Valid @RequestBody ProductUpdateRequest request) {
        log.info("更新商品请求: ID={}", request.getId());
        productService.updateProduct(request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品", description = "根据ID删除商品")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        log.info("删除商品请求: ID={}", id);
        productService.deleteProduct(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取商品详情", description = "根据ID获取商品详情")
    public Result<Product> getProductById(@PathVariable Long id) {
        log.info("查询商品请求: ID={}", id);
        Product product = productService.getProductById(id);
        return Result.success(product);
    }

    @GetMapping
    @Operation(summary = "分页查询商品", description = "分页查询商品列表")
    public Result<Page<Product>> getProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        log.info("分页查询商品请求: page={}, size={}, type={}, status={}", page, size, type, status);
        Page<Product> products = productService.getProducts(page, size, type, status);
        return Result.success(products);
    }

    @PostMapping("/{id}/decrease-stock")
    @Operation(summary = "扣减库存", description = "扣减商品库存")
    public Result<Void> decreaseStock(
            @PathVariable Long id,
            @RequestBody StockDecreaseRequest request) {
        log.info("扣减库存请求: ID={}, quantity={}", id, request.getQuantity());
        productService.decreaseStock(id, request.getQuantity());
        return Result.success();
    }

    @PostMapping("/{id}/increase-stock")
    @Operation(summary = "增加库存", description = "增加商品库存")
    public Result<Void> increaseStock(
            @PathVariable Long id,
            @RequestBody StockIncreaseRequest request) {
        log.info("增加库存请求: ID={}, quantity={}", id, request.getQuantity());
        productService.increaseStock(id, request.getQuantity());
        return Result.success();
    }

    /**
     * 库存扣减请求
     */
    public static class StockDecreaseRequest {
        private Integer quantity;

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
    public static class StockIncreaseRequest {
        private Integer quantity;

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}
