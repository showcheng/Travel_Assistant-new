package com.travel.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.common.enums.ErrorCode;
import com.travel.common.exception.BusinessException;
import com.travel.product.dto.ProductCreateRequest;
import com.travel.product.dto.ProductUpdateRequest;
import com.travel.product.entity.Product;
import com.travel.product.mapper.ProductMapper;
import com.travel.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 商品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    public Long createProduct(ProductCreateRequest request) {
        log.info("创建商品: {}", request.getName());

        // 1. 验证商品类型
        validateProductType(request.getType());

        // 2. 验证价格
        if (request.getCurrentPrice().compareTo(request.getOriginalPrice()) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE);
        }

        // 3. 创建商品实体
        Product product = new Product();
        BeanUtils.copyProperties(request, product);

        // 4. 保存到数据库
        productMapper.insert(product);

        log.info("商品创建成功: ID={}, Name={}", product.getId(), product.getName());
        return product.getId();
    }

    @Override
    public void updateProduct(ProductUpdateRequest request) {
        log.info("更新商品: ID={}", request.getId());

        // 1. 检查商品是否存在
        Product existingProduct = productMapper.selectById(request.getId());
        if (existingProduct == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 验证商品类型（如果提供）
        if (StringUtils.hasText(request.getType())) {
            validateProductType(request.getType());
        }

        // 3. 更新商品信息
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setId(request.getId());

        productMapper.updateById(product);

        log.info("商品更新成功: ID={}", request.getId());
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("删除商品: ID={}", id);

        // 1. 检查商品是否存在
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 删除商品
        productMapper.deleteById(id);

        log.info("商品删除成功: ID={}", id);
    }

    @Override
    public Product getProductById(Long id) {
        log.info("查询商品: ID={}", id);

        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return product;
    }

    @Override
    public Page<Product> getProducts(Integer page, Integer size, String type, Integer status) {
        log.info("分页查询商品: page={}, size={}, type={}, status={}", page, size, type, status);

        // 1. 构建查询条件
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();

        if (StringUtils.hasText(type)) {
            queryWrapper.eq("type", type);
        }

        if (status != null) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("create_time");

        // 2. 分页查询
        Page<Product> pageParam = new Page<>(page, size);
        Page<Product> resultPage = productMapper.selectPage(pageParam, queryWrapper);

        log.info("查询到 {} 个商品", resultPage.getTotal());
        return resultPage;
    }

    @Override
    public void decreaseStock(Long productId, Integer quantity) {
        log.info("扣减库存: productId={}, quantity={}", productId, quantity);

        // 1. 检查商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 检查库存是否足够
        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.PRODUCT_OUT_OF_STOCK);
        }

        // 3. 使用乐观锁扣减库存
        int rows = productMapper.decreaseStock(productId, quantity, product.getVersion());
        if (rows == 0) {
            log.warn("库存扣减失败，可能存在并发操作: productId={}", productId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_UPDATE_FAILED);
        }

        log.info("库存扣减成功: productId={}, 扣减数量={}", productId, quantity);
    }

    @Override
    public void increaseStock(Long productId, Integer quantity) {
        log.info("增加库存: productId={}, quantity={}", productId, quantity);

        // 1. 检查商品是否存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // 2. 增加库存
        int rows = productMapper.increaseStock(productId, quantity);
        if (rows == 0) {
            log.warn("库存增加失败: productId={}", productId);
            throw new BusinessException(ErrorCode.PRODUCT_STOCK_UPDATE_FAILED);
        }

        log.info("库存增加成功: productId={}, 增加数量={}", productId, quantity);
    }

    /**
     * 验证商品类型
     */
    private void validateProductType(String type) {
        if (!"TICKET".equals(type) && !"PRODUCT".equals(type) && !"HOTEL".equals(type)) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_TYPE);
        }
    }
}
