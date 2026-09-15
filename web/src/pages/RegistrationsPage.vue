<script setup lang="ts">
import { ref, watch } from 'vue'
import type { EventFlowClient, Registration } from '../domain/contracts'
import { errorCode, errorText } from '../domain/presentation'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
const props = defineProps<{ client?: EventFlowClient }>()
const { state, load } = useResource<Registration[]>()
const selected = ref<Registration>(), busy = ref(false), message = ref('')
const labels: Record<Registration['status'], string> = { PENDING: '受理中', CONFIRMED: '已报名', WAITING: '候补中', CANCELLED: '已取消', CLOSED: '已关闭' }
let command: { target: string; key: string } | undefined
function refresh() { const c = props.client; return load(c ? signal => c.registrations(signal) : undefined) }
watch(() => props.client, refresh, { immediate: true })
async function cancel() {
  if (!props.client || !selected.value || busy.value) return
  const item = selected.value
  const target = item.sessionId + ':' + item.sequence
  if (command?.target !== target) command = { target, key: crypto.randomUUID() }
  busy.value = true; message.value = ''
  try {
    await props.client.cancelRegistration(item.sessionId, item.sequence, command.key)
    selected.value = undefined; command = undefined; await refresh()
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
</script>
<template>
  <header class="page-heading"><h1>我的模拟报名</h1><p>这里的记录仅用于演示，不代表学校官方报名结果。</p></header>
  <p v-if="message" role="alert">{{ message }}</p>
  <ResourcePanel :state="state" @retry="refresh">
    <template v-if="state.kind === 'ready'">
      <p v-if="!state.value.length">已读取，暂无模拟报名记录。</p>
      <article v-for="item in state.value" :key="item.id" class="list-row"><div><h2>{{ item.title }}</h2><p>{{ labels[item.status] }} · {{ item.timeText }}</p></div><button v-if="item.canCancel" class="secondary" :disabled="busy" @click="selected = item">取消报名或退出候补</button></article>
      <button class="secondary" @click="refresh">查询最新结果</button>
    </template>
  </ResourcePanel>
  <section v-if="selected" class="confirm-panel"><h2>确认取消“{{ selected.title }}”？</h2><p>再次报名会按当时余量或队列顺序处理。</p><button :disabled="busy" @click="cancel">确认取消</button><button class="secondary" :disabled="busy" @click="selected = undefined">返回</button></section>
</template>
