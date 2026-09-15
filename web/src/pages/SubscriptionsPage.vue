<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import type { EventFlowClient, Subscription } from '../domain/contracts'
import { errorCode, errorText } from '../domain/presentation'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
const props = defineProps<{ client?: EventFlowClient }>()
const form = reactive({ campus: '', category: '', source: '' })
const { state, load } = useResource<Subscription[]>()
const busy = ref(false), message = ref(''), selected = ref<Subscription>()
let command: { payload: string; key: string } | undefined
function key(payload: string) {
  if (command?.payload !== payload) command = { payload, key: crypto.randomUUID() }
  return command.key
}
function refresh() { const c = props.client; return load(c ? signal => c.subscriptions(signal) : undefined) }
watch(() => props.client, refresh, { immediate: true })
async function subscribe() {
  if (!props.client || busy.value) return
  if (!Object.values(form).some(s => s.trim())) { message.value = '请至少填写一个订阅条件。'; return }
  busy.value = true; message.value = ''
  try {
    await props.client.subscribe({ ...form }, key(JSON.stringify(form)))
    message.value = '订阅已由服务端确认。'; command = undefined; await refresh()
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
async function unsubscribe() {
  if (!props.client || !selected.value || busy.value) return
  const item = selected.value
  busy.value = true; message.value = ''
  try {
    await props.client.unsubscribe(item.id, item.version, key('unsubscribe:' + item.id + ':' + item.version))
    selected.value = undefined; command = undefined; await refresh()
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
</script>
<template>
  <header class="page-heading"><h1>我的订阅</h1><p>关注感兴趣的渠道和类别，重要更新集中查看。</p></header>
  <form class="filters" @submit.prevent="subscribe">
    <label>校区<input v-model.trim="form.campus" /></label><label>类别<input v-model.trim="form.category" /></label>
    <label>来源标识<input v-model.trim="form.source" /></label><button :disabled="!client || busy">添加订阅</button>
  </form>
  <p class="muted">多个条件同时满足才匹配；暂未接入站外推送。字段由服务端进一步校验。</p>
  <p v-if="message" role="status">{{ message }}</p>
  <ResourcePanel :state="state" @retry="refresh">
    <template v-if="state.kind === 'ready'">
      <p v-if="!state.value.length">尚无订阅记录。</p>
      <article v-for="item in state.value" :key="item.id" class="list-row"><div><h2>{{ item.label }}</h2><p>{{ item.active ? '订阅中' : '已取消' }}</p></div>
        <button v-if="item.active" class="secondary" :disabled="busy" @click="selected = item">取消订阅</button>
      </article>
    </template>
  </ResourcePanel>
  <section v-if="selected" class="confirm-panel" aria-label="取消订阅确认"><h2>取消“{{ selected.label }}”？</h2><p>尚未发送的提醒将失效。</p><button :disabled="busy" @click="unsubscribe">确认取消</button><button class="secondary" :disabled="busy" @click="selected = undefined">保留订阅</button></section>
</template>

