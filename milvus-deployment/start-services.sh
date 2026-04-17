#!/bin/bash

# 启动MinIO和Milvus服务

echo "=========================================="
echo "  启动MinIO和Milvus服务"
echo "=========================================="
echo ""

# 启动MinIO
echo "1. 启动MinIO..."
podman run -d --name milvus-minio \
  --pod milvus-pod \
  -v minio-data:/minio_data \
  -e MINIO_ACCESS_KEY=minioadmin \
  -e MINIO_SECRET_KEY=minioadmin \
  m.daocloud.io/docker.io/minio/minio:RELEASE.2023-03-20T20-16-18Z \
  server /minio_data

if [ $? -eq 0 ]; then
    echo "✅ MinIO启动成功"
else
    echo "❌ MinIO启动失败"
    exit 1
fi

echo ""
echo "等待MinIO启动（10秒）..."
sleep 10

echo ""

# 启动Milvus
echo "2. 启动Milvus..."
podman run -d --name milvus-standalone \
  --pod milvus-pod \
  -v milvus-data:/var/lib/milvus \
  -e ETCD_ENDPOINTS=etcd:2379 \
  -e MINIO_ADDRESS=minio:9000 \
  m.daocloud.io/docker.io/milvusdb/milvus:v2.4.4

if [ $? -eq 0 ]; then
    echo "✅ Milvus启动成功"
else
    echo "❌ Milvus启动失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "  服务启动完成！"
echo "=========================================="
echo ""
echo "查看所有容器:"
podman ps --filter "pod=milvus-pod" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo ""
echo "等待Milvus完全启动（约30秒）..."
sleep 30
echo ""
echo "检查Milvus日志:"
podman logs --tail 10 milvus-standalone
echo ""
