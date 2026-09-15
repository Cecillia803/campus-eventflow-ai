<script setup lang="ts">
import { watch } from 'vue'
import type { EventFlowClient, Notification } from '../domain/contracts'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
const props = defineProps<{ client?: EventFlowClient }>()
const { state, load } = useResource<Notification[]>()
function refresh() { const c = props.client; return load(c ? signal => c.notifications(signal) : undefined) }
watch(() => props.client, refresh, { immediate: true })
</script>
<template>
  <header class="page-heading"><h1>通知中心</h1><p>活动变更、报名截止与候补结果。</p></header>
  <ResourcePanel :state="state" @retry="refresh">
    <template v-if="state.kind === 'ready'">
      <p v-if="!state.value.length">已读取，暂无通知。</p>
      <article v-for="item in state.value" :key="item.id" class="activity-row"><div><h2>{{ item.title }}</h2><p class="preserve">{{ item.body }}</p></div><small>{{ item.timeText }}</small></article>
      <button class="secondary" @click="refresh">刷新通知</button>
    </template>
  </ResourcePanel>
</template>

