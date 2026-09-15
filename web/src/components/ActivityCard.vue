<script setup lang="ts">
import type { Activity } from '../domain/contracts'
import { statusText } from '../domain/presentation'
defineProps<{ activity: Activity }>()
defineEmits<{ open: [id: string] }>()
</script>
<template>
  <article class="activity-row">
    <div>
      <p class="muted">{{ activity.category || '类别未说明' }} · {{ activity.campus || '校区未说明' }}</p>
      <h2><button class="text-button title-button" @click="$emit('open', activity.id)">{{ activity.title }}</button></h2>
      <p>{{ activity.summary }}</p>
      <p class="muted">{{ activity.organizer || '主办方未说明' }}</p>
    </div>
    <dl class="time-summary">
      <dt>活动时间 · {{ statusText(activity.eventStatus) }}</dt><dd>{{ activity.eventTimeText }}</dd>
      <dt>报名时间 · {{ statusText(activity.registrationStatus, true) }}</dt><dd>{{ activity.registrationTimeText }}</dd>
    </dl>
  </article>
</template>

