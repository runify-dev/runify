<template>
  <el-card shadow="hover" class="card-box">
    <div class="card-header">
      <slot name="header">
        <div class="title flex align-center" :class="$slots.subTitle ? 'mt-4' : ''">
          <slot name="icon">
            <el-avatar
              :size="32"
              class="mr-12"
              shape="square"
              style="flex-shrink: 0"
              v-bind="$attrs"
            >
              <img :src="icon ? icon : '/public/user.jpeg'" alt="" />
            </el-avatar>
          </slot>
          <div style="width: 90%">
            <AutoToolTip :content="title" style="width: 65%; height: 22px">
              {{ title }}
            </AutoToolTip>
            <slot name="subTitle"> </slot>
          </div>
        </div>
      </slot>
    </div>
    <div class="description break-all mt-12">
      <div class="content">
        <slot name="description"> </slot>
      </div>
    </div>
    <div class="tag-content">
      <slot name="tag"></slot>
    </div>
  </el-card>
</template>
<script setup lang="ts">
import AutoToolTip from '@/components/auto-tool-tip/index.vue'
import { t } from '@/locales'
withDefaults(
  defineProps<{
    /**
     * 标题
     */
    title?: string
    /**
     * 是否展示icon
     */
    icon?: string
  }>(),
  { title: t('common.title'), icon: '', border: true }
)
</script>
<style lang="scss" scoped>
.card-box {
  overflow: hidden;

  .tag-content {
    position: absolute;
    right: 12px;
    top: 15px;
    height: auto;
  }

  font-size: 14px;
  position: relative;
  min-height: var(--card-min-height);
  min-width: var(--card-min-width);
  border-radius: 8px;

  .title {
    height: 20px;
  }

  .description {
    color: var(--app-text-color-secondary);
    line-height: 22px;
    font-weight: 400;

    .content {
      display: -webkit-box;
      height: var(--app-card-box-description-height, 110px);
      -webkit-box-orient: vertical;
      -webkit-line-clamp: 5;
      overflow: hidden;
    }
  }
}
</style>
