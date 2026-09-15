<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { Activity, EventFlowClient, Page } from '../domain/contracts'
import { validDateRange } from '../domain/presentation'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
import ActivityCard from '../components/ActivityCard.vue'
const props = defineProps<{ client?: EventFlowClient }>()
defineEmits<{ open: [id: string] }>()
const form = reactive({ text: '', campus: '', category: '', from: '', untilExclusive: '', registrationOpenOnly: false })
const { state, load, fail } = useResource<Page<Activity>>()
async function search(cursor?: string) {
  if (!validDateRange(form.from, form.untilExclusive)) { fail('INVALID_INPUT'); return }
  const client = props.client
  await load(client ? signal => client.search({ ...form, zone: client.zone, cursor }, signal) : undefined)
}
watch(() => props.client, () => search(), { immediate: true })
</script>
<template>
  <header class="page-heading"><h1>校园里的下一件事</h1><p>集中发现活动，核对来源，不再错过重要时间。</p></header>
  <form class="filters" @submit.prevent="search()">
    <label class="wide">关键词<input v-model.trim="form.text" maxlength="300" type="search" /></label>
    <label>校区<input v-model.trim="form.campus" /></label>
    <label>类别<input v-model.trim="form.category" /></label>
    <label>举办日期，从<input v-model="form.from" type="date" /></label>
    <label>到（不含当天）<input v-model="form.untilExclusive" type="date" /></label>
    <label class="check"><input v-model="form.registrationOpenOnly" type="checkbox" />只看报名中</label>
    <button :disabled="!client || state.kind === 'loading'">查找活动</button>
  </form>
  <ResourcePanel :state="state" @retry="search()">
    <template v-if="state.kind === 'ready'">
      <p class="muted">{{ state.value.coverage }}</p>
      <p v-if="!state.value.items.length" class="resource-state">已查询的收录范围内，没有符合条件的活动。</p>
      <ActivityCard v-for="activity in state.value.items" :key="activity.id" :activity="activity" @open="$emit('open', $event)" />
      <div class="actions"><button v-if="state.value.nextCursor" @click="search(state.value.nextCursor)">下一页</button><button class="secondary" @click="search()">回到第一页</button></div>
    </template>
  </ResourcePanel>
</template>

