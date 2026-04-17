#!/bin/bash

# Milvus向量数据库部署脚本 (使用Podman)

echo "=========================================="
echo "  Milvus向量数据库部署"
echo "  使用Podman - 单机版"
echo "=========================================="
echo ""

# 检查Podman
if ! command -v podman &> /dev/null; then
    echo "❌ Podman未安装，请先安装Podman"
    exit 1
fi

echo "✅ Podman版本: $(podman --version)"
echo ""

# 创建Pod
echo "1. 创建Milvus Pod..."
podman pod create --name milvus-pod -p 19530:19530 -p 9091:9091 -p 2379:2379 -p 9000:9000

if [ $? -eq 0 ]; then
    echo "✅ Pod创建成功"
else
    echo "⚠️  Pod可能已存在，继续执行..."
fi

echo ""

# 创建数据卷
echo "2. 创建数据卷..."
podman volume create etcd-data
podman volume create minio-data
podman volume create milvus-data
echo "✅ 数据卷创建完成"
echo ""

# 启动Etcd
echo "3. 启动Etcd..."
podman run -d --name milvus-etcd \
  --pod milvus-pod \
  -v etcd-data:/etcd \
  -e ETCD_AUTO_COMPACTION_MODE=revision \
  -e ETCD_AUTO_COMPACTION_RETENTION=1000 \
  -e ETCD_QUOTA_BACKEND_BYTES=4294967296 \
  -e ETCD_SNAPSHOT_COUNT=50000 \
  quay.io/coreos/etcd:v3.5.5 \
  etcd -advertise-client-urls=http://127.0.0.1:2379 \
       -listen-client-urls http://0.0.0.0:2379 \
       -data-dir /etcd

if [ $? -eq 0 ]; then
    echo "✅ Etcd启动成功"
else
    echo "❌ Etcd启动失败"
    exit 1
fi

# 等待Etcd启动
echo "等待Etcd启动..."
sleep 5

echo ""

# 启动MinIO
echo "4. 启动MinIO..."
podman run -d --name milvus-minio \
  --pod milvus-pod \
  -v minio-data:/minio_data \
  -e MINIO_ACCESS_KEY=minioadmin \
  -e MINIO_SECRET_KEY=minioadmin \
  minio/minio:RELEASE.2023-03-20T20-16-18Z \
  server /minio_data

if [ $? -eq 0 ]; then
    echo "✅ MinIO启动成功"
else
    echo "❌ MinIO启动失败"
    exit 1
fi

# 等待MinIO启动
echo "等待MinIO启动..."
sleep 5

echo ""

# 启动Milvus
echo "5. 启动Milvus..."
podman run -d --name milvus-standalone \
  --pod milvus-pod \
  -v milvus-data:/var/lib/milvus \
  -e ETCD_ENDPOINTS=etcd:2379 \
  -e MINIO_ADDRESS=minio:9000 \
  milvusdb/milvus:v2.3.0

if [ $? -eq 0 ]; then
    echo "✅ Milvus启动成功"
else
    echo "❌ Milvus启动失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "  Milvus部署完成！"
echo "=========================================="
echo ""
echo "服务信息:"
echo "  - Milvus: localhost:19530"
echo "  - Etcd:   localhost:2379"
echo "  - MinIO:  localhost:9000"
echo ""
echo "查看日志:"
echo "  podman logs -f milvus-standalone"
echo ""
echo "查看状态:"
echo "  podman pod ps"
echo "  podman ps"
echo ""

# 等待Milvus完全启动
echo "等待Milvus完全启动（约30秒）..."
sleep 30

# 检查Milvus健康状态
echo "检查Milvus健康状态..."
podman exec milvus-standalone ls /var/lib/milvus > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Milvus看起来运行正常"
else
    echo "⚠️  Milvus可能需要更多时间启动"
    echo "请运行: podman logs milvus-standalone"
fi

echo ""
echo "=========================================="
