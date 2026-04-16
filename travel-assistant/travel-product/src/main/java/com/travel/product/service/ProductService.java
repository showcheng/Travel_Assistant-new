package com.travel.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.product.dto.ProductCreateRequest;
import com.travel.product.dto.ProductUpdateRequest;
import com.travel.product.entity.Product;

/**
 * 商品服务接口
 */
public interface ProductService {

    /**
     * 创建商品
     */
    Long createProduct(ProductCreateRequest request);

    /**
     * 更新商品
     */
    void updateProduct(ProductUpdateRequest request);

    /**
     * 删除商品
     */
    void deleteProduct(Long id);

    /**
     * 根据ID获取商品
     */
    Product getProductById(Long id);

    /**
     * 分页查询商品
     */
    Page<Product> getProducts(Integer page, Integer size, String type, Integer status);

    /**
     * 扣减库存
     */
    void decreaseStock(Long productId, Integer quantity);

    /**
     * 增加库存
     */
    void increaseStock(Long productId, Integer quantity);
}
