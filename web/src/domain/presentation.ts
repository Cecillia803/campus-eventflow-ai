import { ClientFailure, type FailureCode, type TimeStatus } from './contracts'

const errors: Record<FailureCode, string> = {
  NOT_CONNECTED: '此功能尚未接入服务。', UNAUTHENTICATED: '请通过正式登录入口登录后再操作。',
  FORBIDDEN: '没有访问这项内容的权限。', INVALID_INPUT: '请检查输入内容。',
  NOT_FOUND: '内容不存在或已移除。', VERSION_CONFLICT: '内容已更新，请刷新后重新确认。',
  IDEMPOTENCY_CONFLICT: '请求内容发生变化，请刷新并重新操作。',
  NOT_OPEN: '尚未开放报名。', CLOSED: '操作时间已截止。', FULL: '名额已满。',
  CANCELLED: '场次或操作已取消。', EXPIRED: '确认信息已过期，请重新选择场次。',
  ALREADY_REGISTERED: '你已有该场次的有效报名或候补。',
  DEPENDENCY_UNAVAILABLE: '服务暂时不可用，操作结果可能尚未确认，请查询记录后重试。',
}
export function errorCode(error: unknown): FailureCode {
  return error instanceof ClientFailure ? error.code : 'DEPENDENCY_UNAVAILABLE'
}
export function errorText(code: FailureCode): string { return errors[code] }
export function statusText(status: TimeStatus, registration = false): string {
  switch (status) {
    case 'UNKNOWN': return '未说明'
    case 'UPCOMING': return registration ? '尚未开放' : '尚未开始'
    case 'ACTIVE': return registration ? '报名中' : '进行中'
    case 'ENDED': return registration ? '已截止' : '已结束'
    case 'CANCELLED': return '已取消'
    case 'POSTPONED': return '已延期'
  }
}
/** Client display protection only. Server-side source ingestion needs independent SSRF controls. */
export function safeExternalLink(raw?: string): string | undefined {
  if (!raw) return undefined
  try {
    const url = new URL(raw)
    return url.protocol === 'https:' && !url.username && !url.password ? url.href : undefined
  } catch { return undefined }
}
export function validDateRange(from: string, untilExclusive: string): boolean {
  const valid = (s: string) => !s || /^\d{4}-\d{2}-\d{2}$/.test(s)
  return valid(from) && valid(untilExclusive) && (!from || !untilExclusive || from < untilExclusive)
}

