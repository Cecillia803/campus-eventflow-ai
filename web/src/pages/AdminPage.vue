<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { AdminRow, AdminSection, EventFlowClient, ReviewField } from '../domain/contracts'
import { errorCode, errorText } from '../domain/presentation'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
const props = defineProps<{ client?: EventFlowClient }>()
const section = ref<AdminSection>('sources')
const sections: { id: AdminSection; label: string }[] = [
  { id: 'sources', label: '来源状态' }, { id: 'reviews', label: '提取与去重审核' },
  { id: 'revisions', label: '活动修订' }, { id: 'sessions', label: '模拟场次' },
  { id: 'registrations', label: '报名与候补' }, { id: 'tasks', label: '失败任务' }, { id: 'audit', label: '操作审计' },
]
const { state, load } = useResource<AdminRow[]>()
const busy = ref(false), message = ref(''), reason = ref('')
const selection = ref<{ row: AdminRow; action: string }>()
const edits = ref<ReviewField[]>([])
function select(row: AdminRow, action: string) {
  selection.value = { row, action }; reason.value = ''
  edits.value = (row.fields ?? []).map(field => ({ ...field }))
}
const form = reactive({ activityId: '', title: '', capacity: '', opensAt: '', closesAt: '', cancellationDeadline: '', promotionDeadline: '' })
let pending: { payload: string; key: string } | undefined
function key(payload: string) {
  if (pending?.payload !== payload) pending = { payload, key: crypto.randomUUID() }
  return pending.key
}
function refresh() { const c = props.client; return load(c ? signal => c.admin(section.value, signal) : undefined) }
watch(() => [props.client, section.value], () => { selection.value = undefined; reason.value = ''; message.value = ''; return refresh() }, { immediate: true })
async function act() {
  if (!props.client || !selection.value || !reason.value.trim() || busy.value) return
  const { row, action } = selection.value
  busy.value = true; message.value = ''
  try {
    const changes = action === 'APPROVE' ? edits.value.map(field => ({ ...field })) : undefined
    await props.client.adminAction(section.value, row.id, row.version, action, reason.value, key(JSON.stringify([section.value, row.id, row.version, action, reason.value, changes])), changes)
    selection.value = undefined; reason.value = ''; pending = undefined; await refresh()
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
async function create() {
  if (!props.client || busy.value) return
  const capacity = Number(form.capacity)
  if (!Number.isSafeInteger(capacity) || capacity <= 0 || !form.activityId.trim() || !form.title.trim()
      || !form.opensAt || !form.closesAt || !form.cancellationDeadline || !form.promotionDeadline
      || form.opensAt >= form.closesAt || form.promotionDeadline < form.closesAt
      || form.cancellationDeadline < form.opensAt) { message.value = '请检查名额、时间顺序和必填字段。'; return }
  busy.value = true; message.value = ''
  try {
    await props.client.createSession({ ...form, capacity, zone: props.client.zone }, key(JSON.stringify(form)))
    pending = undefined; message.value = '场次已由服务端确认创建。'
    Object.assign(form, { activityId: '', title: '', capacity: '', opensAt: '', closesAt: '', cancellationDeadline: '', promotionDeadline: '' })
    await refresh()
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
const actionLabels: Record<string, string> = { APPROVE: '确认通过', REJECT: '退回审核', RETRY: '重新处理' }
</script>
<template>
  <header class="page-heading"><h1>管理工作台</h1><p>审核来源、追踪变更、管理模拟报名。实际权限由服务端校验。</p></header>
  <nav class="tabs" aria-label="管理模块"><button v-for="item in sections" :key="item.id" :class="{ selected: section === item.id }" :disabled="busy" @click="section = item.id">{{ item.label }}</button></nav>
  <p v-if="message" role="status">{{ message }}</p>
  <ResourcePanel :state="state" @retry="refresh">
    <template v-if="state.kind === 'ready'">
      <p v-if="!state.value.length">已读取，当前模块没有记录。</p>
      <div v-else class="table-scroll"><table><thead><tr><th>项目</th><th>状态</th><th>说明与证据</th><th>更新时间</th><th>操作</th></tr></thead><tbody>
        <tr v-for="row in state.value" :key="row.id"><td>{{ row.title }}</td><td>{{ row.status }}</td><td class="preserve">{{ row.detail }}</td><td>{{ row.updatedAtText }}</td><td><button v-for="action in row.actions" :key="action" class="secondary" :disabled="busy" @click="select(row, action)">{{ actionLabels[action] }}</button></td></tr>
      </tbody></table></div>
    </template>
  </ResourcePanel>
  <form v-if="selection" class="confirm-panel" @submit.prevent="act">
    <h2>{{ actionLabels[selection.action] }}：{{ selection.row.title }}</h2><p class="preserve">{{ selection.row.detail }}</p>
    <div v-if="selection.action === 'APPROVE'" class="form-grid"><label v-for="field in edits" :key="field.key">{{ field.key }}<textarea v-model="field.value" required maxlength="4000" /><small>依据：{{ field.sourceId }} / {{ field.sourceRevision }} / {{ field.locator }}</small></label></div>
    <label>审核或重试理由<textarea v-model="reason" required maxlength="2000" /></label><div class="actions"><button :disabled="busy || !reason.trim()">确认提交</button><button type="button" class="secondary" :disabled="busy" @click="selection = undefined">返回</button></div>
  </form>
  <details v-if="section === 'sessions'" class="side-panel">
    <summary>创建模拟场次</summary><p>不产生真实学校报名资格。时间使用接入方提供的时区：{{ client?.zone || '未接入' }}。</p>
    <form class="form-grid" @submit.prevent="create">
      <label>关联活动标识<input v-model.trim="form.activityId" required /></label><label>场次名称<input v-model.trim="form.title" required /></label>
      <label>名额<input v-model="form.capacity" type="number" min="1" step="1" required /></label>
      <label>开放报名<input v-model="form.opensAt" type="datetime-local" required /></label><label>截止报名<input v-model="form.closesAt" type="datetime-local" required /></label>
      <label>取消截止<input v-model="form.cancellationDeadline" type="datetime-local" required /></label><label>递补截止<input v-model="form.promotionDeadline" type="datetime-local" required /></label>
      <button :disabled="!client || busy">创建演示场次</button>
    </form>
  </details>
</template>
