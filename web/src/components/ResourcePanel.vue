<script setup lang="ts">
import type { LoadState } from '../domain/contracts'
import { errorText } from '../domain/presentation'
defineProps<{ state: LoadState<unknown> }>()
defineEmits<{ retry: [] }>()
</script>

<template>
  <section v-if="state.kind === 'unconnected'" class="resource-state" role="status">
    <h2>等待接入</h2>
    <p>当前仅交付页面与规则源代码。此处没有连接业务服务，也没有加载模拟数据。</p>
    <p class="muted">这不代表查询结果为空。请在主要开发电脑完成接入后查看。</p>
  </section>
  <section v-else-if="state.kind === 'loading'" class="resource-state" role="status" aria-live="polite">正在读取…</section>
  <section v-else-if="state.kind === 'error'" class="resource-state" role="alert">
    <h2>暂时无法显示</h2><p>{{ errorText(state.code) }}</p>
    <button type="button" @click="$emit('retry')">重新读取</button>
  </section>
  <slot v-else />
</template>

