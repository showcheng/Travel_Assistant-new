#!/bin/bash

# Milvus停止和清理脚本

echo "=========================================="
echo "  停止Milvus服务"
echo "=========================================="
echo ""

# 停止并删除容器
echo "停止容器..."
podman stop milvus-standalone milvus-minio milvus-etcd 2>/dev/null
podman rm milvus-standalone milvus-minio milvus-etcd 2>/dev/null

# 删除Pod
echo "删除Pod..."
podman pod stop milvus-pod 2>/dev/null
podman pod rm milvus-pod 2>/dev/null

# 删除数据卷（可选）
read -p "是否删除数据卷？(y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "删除数据卷..."
    podman volume rm etcd-data minio-data milvus-data 2>/dev/null
    echo "✅ 数据卷已删除"
else
    echo "保留数据卷"
fi

echo ""
echo "✅ 清理完成"
echo ""
