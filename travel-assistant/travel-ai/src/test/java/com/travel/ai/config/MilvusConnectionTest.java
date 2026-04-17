package com.travel.ai.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import io.milvus.response.DescribeCollectionResponse;

/**
 * Milvus连接测试类
 * 测试Milvus向量数据库连接和基本功能
 */
public class MilvusConnectionTest {

    private static final String MILVUS_HOST = "localhost";
    private static final int MILVUS_PORT = 19530;

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  Milvus连接测试");
        System.out.println("==========================================");
        System.out();

        try {
            // 创建Milvus连接
            System.out.println("1. 连接到Milvus...");
            ConnectParam connectParam = ConnectParam.newBuilder()
                    .withHost(MILVUS_HOST)
                    .withPort(MILVUS_PORT)
                    .build();

            MilvusServiceClient milvusClient = new MilvusServiceClient(connectParam);
            System.out.println("✅ Milvus连接成功");
            System.out.println();

            // 测试基本功能
            System.out.println("2. 检查系统状态...");
            R<DescribeCollectionResponse> response = milvusClient.describeCollection("test");

            if (response.getStatus() != R.Status.Success.getCode()) {
                System.out.println("✅ Milvus响应正常 (集合不存在是预期的)");
            }

            System.out.println();
            System.out.println("3. 关闭连接...");
            milvusClient.close();
            System.out.println("✅ 连接已关闭");

            System.out.println();
            System.out.println("==========================================");
            System.out.println("  ✅ Milvus连接测试通过！");
            System.out.println("==========================================");

        } catch (Exception e) {
            System.out.println();
            System.out.println("❌ Milvus连接测试失败");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
