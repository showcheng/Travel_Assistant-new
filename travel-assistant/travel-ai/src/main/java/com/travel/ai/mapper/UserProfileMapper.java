package com.travel.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travel.ai.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户画像Mapper
 *
 * 提供用户画像的基础CRUD操作，继承自MyBatis-Plus BaseMapper。
 * 乐观锁并发控制由MyBatis-Plus OptimisticLockerInnerInterceptor处理。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
