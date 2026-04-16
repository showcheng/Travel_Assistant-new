package com.travel.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 使用乐观锁扣减库存
     *
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @param version 版本号
     * @return 影响的行数
     */
    @Update("UPDATE product SET stock = stock - #{quantity}, version = version + 1 " +
            "WHERE id = #{productId} AND stock >= #{quantity} AND version = #{version}")
    int decreaseStock(@Param("productId") Long productId,
                     @Param("quantity") Integer quantity,
                     @Param("version") Integer version);

    /**
     * 增加库存
     *
     * @param productId 商品ID
     * @param quantity 增加数量
     * @return 影响的行数
     */
    @Update("UPDATE product SET stock = stock + #{quantity} WHERE id = #{productId}")
    int increaseStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
