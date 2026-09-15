<script setup lang="ts">
import { ref, watch } from 'vue'
import type { Chat, EventFlowClient } from '../domain/contracts'
import { errorCode, errorText, safeExternalLink } from '../domain/presentation'
import { useResource } from '../composables/useResource'
import ResourcePanel from '../components/ResourcePanel.vue'
const props = defineProps<{ client?: EventFlowClient }>()
const { state, load } = useResource<Chat>()
const text = ref(''), busy = ref(false), message = ref(''), chatId = ref<string>()
let creationKey: string | undefined
let pending: { question: string; id: string } | undefined
function refresh() {
  const c = props.client, id = chatId.value
  if (!c || !id) return load()
  return load(signal => c.chat(id, signal))
}
watch(() => props.client, () => { chatId.value = undefined; creationKey = undefined; pending = undefined; return refresh() }, { immediate: true })
async function open() {
  if (!props.client || busy.value) return
  busy.value = true; message.value = ''; creationKey ??= crypto.randomUUID()
  try {
    const chat = await props.client.openChat(creationKey)
    chatId.value = chat.id; state.value = { kind: 'ready', value: chat }; creationKey = undefined
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
async function ask() {
  if (!props.client || state.value.kind !== 'ready' || !text.value.trim() || busy.value) return
  const chat = state.value.value, question = text.value.trim()
  if (pending?.question !== question) pending = { question, id: crypto.randomUUID() }
  busy.value = true; message.value = ''
  try {
    const answer = await props.client.ask(chat.id, chat.version, pending.id, question)
    state.value = { kind: 'ready', value: answer }; text.value = ''; pending = undefined
  } catch (e) { message.value = errorText(errorCode(e)) }
  finally { busy.value = false }
}
</script>
<template>
  <header class="page-heading"><h1>活动助手</h1><p>按时间和兴趣查找活动，核对规则与来源。不会自动替你报名。</p></header>
  <button v-if="client && !chatId" :disabled="busy" @click="open">创建我的会话</button>
  <p v-if="client && !chatId" class="muted">会话由服务端创建并绑定当前登录用户。</p>
  <ResourcePanel v-if="!client || chatId" :state="state" @retry="refresh">
    <template v-if="state.kind === 'ready'">
      <div class="conversation" aria-live="polite" aria-relevant="additions">
        <p v-if="!state.value.messages.length" class="muted">会话已建立，可以输入问题。</p>
        <article v-for="item in state.value.messages" :key="item.id" :class="['chat-message', item.role]">
          <h2>{{ item.role === 'user' ? '你' : '活动助手' }}</h2><p class="preserve">{{ item.text }}</p>
          <details v-if="item.citations.length"><summary>查看回答依据</summary>
            <div v-for="cite in item.citations" :key="cite.id"><a v-if="safeExternalLink(cite.url)" :href="safeExternalLink(cite.url)" target="_blank" rel="noopener noreferrer">{{ cite.label }}</a><strong v-else>{{ cite.label }}</strong><p>{{ cite.excerpt }}</p></div>
          </details>
        </article>
      </div>
      <form class="composer" @submit.prevent="ask"><label for="question">你的问题</label><textarea id="question" v-model="text" maxlength="4000" rows="3" :disabled="busy" /><div class="actions"><button :disabled="busy || !text.trim()">发送问题</button><button class="secondary" type="button" :disabled="busy" @click="refresh">刷新会话</button></div></form>
    </template>
  </ResourcePanel>
  <p v-if="message" role="alert">{{ message }}</p>
</template>

