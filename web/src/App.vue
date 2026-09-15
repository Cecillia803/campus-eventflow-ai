<script setup lang="ts">
import { ref, watch } from 'vue'
import type { EventFlowClient } from './domain/contracts'
import DiscoverPage from './pages/DiscoverPage.vue'
import DetailPage from './pages/DetailPage.vue'
import SubscriptionsPage from './pages/SubscriptionsPage.vue'
import NotificationsPage from './pages/NotificationsPage.vue'
import RegistrationsPage from './pages/RegistrationsPage.vue'
import AssistantPage from './pages/AssistantPage.vue'
import AdminPage from './pages/AdminPage.vue'
import './styles.css'
const props = defineProps<{ client?: EventFlowClient }>()
type View = 'discover' | 'subscriptions' | 'notifications' | 'registrations' | 'assistant' | 'admin'
const active = ref<View>('discover'), activityId = ref<string>()
const clientEpoch = ref(0)
watch(() => props.client, () => { clientEpoch.value++; active.value = 'discover'; activityId.value = undefined })
const nav: { id: View; label: string }[] = [
  { id: 'discover', label: '发现活动' }, { id: 'subscriptions', label: '我的订阅' },
  { id: 'notifications', label: '通知' }, { id: 'registrations', label: '模拟报名' },
  { id: 'assistant', label: '活动助手' }, { id: 'admin', label: '管理工作台' },
]
function navigate(view: View) { active.value = view; activityId.value = undefined }
</script>
<template>
  <a class="skip-link" href="#main">跳到主要内容</a>
  <header class="site-header"><button class="brand" @click="navigate('discover')">Campus EventFlow<span>校园活动智约</span></button>
    <nav aria-label="主导航"><button v-for="item in nav" :key="item.id" :aria-current="active === item.id ? 'page' : undefined" @click="navigate(item.id)">{{ item.label }}</button></nav>
  </header>
  <div v-if="!client" class="connection-notice" role="status">源代码交付阶段 · 业务服务未接入 · 不含演示数据</div>
  <main id="main" :key="clientEpoch" class="workspace">
    <DetailPage v-if="activityId" :key="activityId" :client="client" :activity-id="activityId" @back="activityId = undefined" @records="navigate('registrations')" />
    <DiscoverPage v-else-if="active === 'discover'" :client="client" @open="activityId = $event" />
    <SubscriptionsPage v-else-if="active === 'subscriptions'" :client="client" />
    <NotificationsPage v-else-if="active === 'notifications'" :client="client" />
    <RegistrationsPage v-else-if="active === 'registrations'" :client="client" />
    <AssistantPage v-else-if="active === 'assistant'" :client="client" />
    <AdminPage v-else :client="client" />
  </main>
  <footer>资讯以主办方原始通知为准。模拟报名不产生官方报名资格。</footer>
</template>
