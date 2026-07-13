<template>
  <div class="skill-reference-panel" v-if="skills && skills.length > 0">
    <div class="panel-header">
      <el-icon><Collection /></el-icon>
      <span>引用的Skill ({{ skills.length }})</span>
    </div>
    <div class="skill-list">
      <div
        v-for="skill in skills"
        :key="skill.id"
        class="skill-item"
        @click="handleSkillClick(skill)"
      >
        <div class="skill-header">
          <el-tag size="small" :type="getSkillTypeTag(skill.skillType)">
            {{ getSkillTypeLabel(skill.skillType) }}
          </el-tag>
          <span class="skill-name">{{ skill.skillName }}</span>
          <span class="skill-version">v{{ skill.version }}</span>
        </div>
        <div class="skill-description" v-if="skill.content">
          {{ extractDescription(skill.content) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { Collection } from '@element-plus/icons-vue'

const props = defineProps({
  skills: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['on-skill-click'])

const getSkillTypeTag = (type) => {
  const map = {
    'PLATFORM_METADATA': 'primary',
    'PLATFORM_QUALITY': 'success',
    'TABLE': 'warning',
    'DATABASE': 'primary',
    'MULTI_TABLE': 'success',
    'REPORT_TEMPLATE': 'danger',
    'METRIC': 'danger'
  }
  return map[type] || 'info'
}

const getSkillTypeLabel = (type) => {
  const map = {
    'PLATFORM_METADATA': '元数据',
    'PLATFORM_QUALITY': '质量',
    'TABLE': '表级',
    'DATABASE': '整库',
    'MULTI_TABLE': '多表',
    'REPORT_TEMPLATE': '报告模板',
    'METRIC': '指标'
  }
  return map[type] || type
}

const extractDescription = (content) => {
  const match = content.match(/description:\s*(.+)/)
  if (match) {
    return match[1].substring(0, 80) + (match[1].length > 80 ? '...' : '')
  }
  return content.substring(0, 80) + '...'
}

const handleSkillClick = (skill) => {
  emit('on-skill-click', skill)
}
</script>

<style lang="scss" scoped>
.skill-reference-panel {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  border: 1px solid #e9ecef;

  .panel-header {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    font-weight: 500;
    color: #606266;
    margin-bottom: 10px;

    .el-icon {
      color: #409eff;
    }
  }

  .skill-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .skill-item {
    background: #ffffff;
    border-radius: 6px;
    padding: 10px 12px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      transform: translateY(-1px);
    }

    .skill-header {
      display: flex;
      align-items: center;
      gap: 8px;

      .skill-name {
        font-size: 13px;
        font-weight: 500;
        color: #303133;
      }

      .skill-version {
        font-size: 11px;
        color: #909399;
      }
    }

    .skill-description {
      font-size: 12px;
      color: #909399;
      margin-top: 6px;
      line-height: 1.5;
    }
  }
}
</style>
