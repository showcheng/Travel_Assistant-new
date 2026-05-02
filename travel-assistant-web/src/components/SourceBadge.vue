<template>
  <div v-if="sources && sources.length > 0" class="source-badge">
    <div class="source-title">信息来源：</div>
    <div class="source-list">
      <span v-for="(source, index) in sources" :key="index" class="source-item">
        <el-tooltip :content="'相似度: ' + (source.score * 100).toFixed(1) + '%'">
          <el-tag :type="getTagType(source.relevance)" size="small" effect="plain">
            {{ source.docTitle }}
          </el-tag>
        </el-tooltip>
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface SourceInfo {
  docTitle: string
  score: number
  category: string
  relevance: string
}

defineProps<{
  sources: SourceInfo[]
}>()

const getTagType = (relevance: string): string => {
  switch (relevance) {
    case '高度相关': return 'success'
    case '一般相关': return 'warning'
    case '低相关': return 'info'
    default: return 'info'
  }
}
</script>

<style scoped>
.source-badge {
  margin-top: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 12px;
  color: #909399;
}
.source-title {
  margin-bottom: 4px;
  font-weight: 500;
}
.source-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
