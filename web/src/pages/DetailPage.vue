<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Detail, Draft, EventFlowClient, Registration } from '../domain/contracts'
import { errorCode, errorText, safeExternalLink, statusText } from '../domain/presentation'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
const props = defineProps<{ client?: EventFlowClient; activityId: string }>()
defineEmits<{ back: []; records: [] }>()
const { state, load } = useResource<Detail>()
const draft = ref<Draft>()
const result = ref<Registration>()
const busy = ref(false), message = ref('')
let draftKey: string | undefined
let generation = 0
function refresh() {
  generation++; draft.value = undefined; result.value = undefined; draftKey = undefined; message.value = ''
  const client = props.client
  return load(client ? signal => client.detail(props.activityId, signal) : undefined)
}
watch(() => [props.client, props.activityId], refresh, { immediate: true })
async function prepare() {
  if (!props.client || state.value.kind !== 'ready' || !state.value.value.demoSession || busy.value) return
  const session = state.value.value.demoSession, current = generation
  busy.value = true; message.value = ''; draftKey ??= crypto.randomUUID()
  try {
    const value = await props.client.prepareDraft(session.id, session.ruleVersion, draftKey)
    if (current === generation) draft.value = value
  } catch (e) {
    if (current === generation) {
      const code = errorCode(e)
      if (code === 'VERSION_CONFLICT') await refresh()
      message.value = errorText(code)
    }
  }
  finally { busy.value = false }
}
async function confirm() {
  if (!props.client || !draft.value || busy.value) return
  const current = generation
  busy.value = true; message.value = ''
  try {
    const value = await props.client.confirmDraft(draft.value.id)
    if (current === generation) result.value = value
  } catch (e) {
    if (current === generation) {
      const code = errorCode(e); message.value = errorText(code)
      if (code === 'EXPIRED' || code === 'VERSION_CONFLICT' || code === 'CANCELLED') { draft.value = undefined; draftKey = undefined }
      if (code === 'VERSION_CONFLICT') { await refresh(); message.value = errorText(code) }
    }
  } finally { busy.value = false }
}
</script>
<template>
  <button class="text-button" @click="$emit('back')">返回活动列表</button>
  <ResourcePanel :state="state" @retry="refresh">
    <template v-if="state.kind === 'ready'">
      <header class="page-heading"><h1>{{ state.value.title }}</h1><p>{{ state.value.summary }}</p></header>
      <div class="detail-layout">
        <article>
          <h2>活动内容</h2><p class="preserve">{{ state.value.content }}</p>
          <h2>参与资格</h2><p>{{ state.value.eligibility || '来源未注明，请向主办方确认。' }}</p>
          <h2>来源与核查时间</h2>
          <ul class="source-list"><li v-for="source in state.value.sources" :key="source.id">
            <a v-if="safeExternalLink(source.url)" :href="safeExternalLink(source.url)" target="_blank" rel="noopener noreferrer">{{ source.label }}</a>
            <span v-else>{{ source.label }}</span><p class="muted">{{ source.checkedAtText }}</p>
          </li></ul>
          <h2>更新记录</h2><p v-if="!state.value.revisions.length">未提供修订记录。</p>
          <ol><li v-for="revision in state.value.revisions" :key="revision.id"><p>{{ revision.explanation }}</p><small>{{ revision.changedAtText }} · {{ revision.sourceLabel }}</small></li></ol>
        </article>
        <aside class="side-panel">
          <h2>时间与报名</h2>
          <p>活动：{{ statusText(state.value.eventStatus) }}</p><p>{{ state.value.eventTimeText }}</p>
          <p>报名：{{ statusText(state.value.registrationStatus, true) }}</p><p>{{ state.value.registrationTimeText }}</p>
          <template v-if="state.value.externalRegistration">
            <h3>官方报名方式</h3><p class="preserve">{{ state.value.externalRegistration.instructions }}</p>
            <a v-if="safeExternalLink(state.value.externalRegistration.url)" :href="safeExternalLink(state.value.externalRegistration.url)" target="_blank" rel="noopener noreferrer">查看官方入口</a>
          </template>
          <section v-if="state.value.demoSession" class="demo-section">
            <h3>模拟报名体验</h3><p>仅演示本平台流程，不产生学校真实报名资格。</p><p>{{ state.value.demoSession.description }}</p>
            <button v-if="!draft" :disabled="busy" @click="prepare">查看报名确认</button>
            <div v-if="draft && !result">
              <p>{{ draft.title }}</p><p>{{ draft.sessionText }}</p><p>确认有效期：{{ draft.expiresAtText }}</p>
              <p>确认草稿不预留名额，结果以服务端处理为准。</p>
              <button :disabled="busy || draft.status === 'REVOKED'" @click="confirm">确认模拟报名</button>
              <button class="secondary" :disabled="busy" @click="draft = undefined">暂不确认</button>
            </div>
            <div v-if="result" role="status"><p>服务端返回状态：{{ result.status }}</p><button @click="$emit('records')">查看我的报名</button></div>
          </section>
          <p v-if="message" role="alert">{{ message }}</p>
        </aside>
      </div>
    </template>
  </ResourcePanel>
</template>
