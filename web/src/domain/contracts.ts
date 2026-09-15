export type FailureCode = 'NOT_CONNECTED' | 'UNAUTHENTICATED' | 'FORBIDDEN' | 'INVALID_INPUT'
  | 'NOT_FOUND' | 'VERSION_CONFLICT' | 'IDEMPOTENCY_CONFLICT' | 'NOT_OPEN' | 'CLOSED'
  | 'FULL' | 'CANCELLED' | 'EXPIRED' | 'ALREADY_REGISTERED' | 'DEPENDENCY_UNAVAILABLE'

export class ClientFailure extends Error {
  constructor(public readonly code: FailureCode) { super(code) }
}
export type LoadState<T> =
  | { kind: 'unconnected' } | { kind: 'loading' }
  | { kind: 'ready'; value: T }
  | { kind: 'error'; code: FailureCode }

export interface Page<T> { items: T[]; nextCursor?: string; coverage: string }
export type TimeStatus = 'UNKNOWN' | 'UPCOMING' | 'ACTIVE' | 'ENDED' | 'CANCELLED' | 'POSTPONED'
export interface Activity {
  id: string; version: number; title: string; summary: string
  campus?: string; category?: string; organizer?: string
  eventTimeText: string; registrationTimeText: string
  eventStatus: TimeStatus; registrationStatus: TimeStatus
}
export interface Source { id: string; label: string; url?: string; checkedAtText: string }
export interface Revision { id: string; changedAtText: string; explanation: string; sourceLabel: string }
export interface Detail extends Activity {
  content: string; eligibility?: string; sources: Source[]; revisions: Revision[]
  externalRegistration?: { instructions: string; url?: string }
  demoSession?: { id: string; ruleVersion: number; description: string }
}
export interface Query {
  text: string; campus: string; category: string; from: string; untilExclusive: string
  registrationOpenOnly: boolean; zone: string; cursor?: string
}
export interface Subscription {
  id: string; version: number; label: string; active: boolean
}
export interface SubscriptionInput { category: string; campus: string; source: string }
export interface Notification { id: string; title: string; body: string; timeText: string }
export interface Registration {
  id: string; sessionId: string; sequence: string; title: string; status: 'PENDING' | 'CONFIRMED' | 'WAITING' | 'CANCELLED' | 'CLOSED'
  timeText: string; canCancel: boolean
}
export interface Draft { id: string; title: string; sessionText: string; expiresAtText: string; status: 'PENDING' | 'SUBMITTED' | 'REVOKED' }
export interface Citation { id: string; label: string; excerpt: string; url?: string }
export interface Message { id: string; role: 'user' | 'assistant'; text: string; citations: Citation[] }
export interface Chat { id: string; version: number; messages: Message[] }
export type AdminSection = 'sources' | 'reviews' | 'revisions' | 'sessions' | 'registrations' | 'tasks' | 'audit'
export interface ReviewField { key: string; value: string; sourceId: string; sourceRevision: string; locator: string }
export interface AdminRow { id: string; version: number; title: string; status: string; detail: string; updatedAtText: string; fields?: ReviewField[]; actions: ('APPROVE' | 'REJECT' | 'RETRY')[] }
export interface SessionInput { activityId: string; title: string; capacity: number; opensAt: string; closesAt: string; cancellationDeadline: string; promotionDeadline: string; zone: string }

/** Supplied by the next computer's authenticated transport adapter. No default implementation. */
export interface EventFlowClient {
  readonly zone: string
  search(query: Query, signal: AbortSignal): Promise<Page<Activity>>
  detail(id: string, signal: AbortSignal): Promise<Detail>
  subscriptions(signal: AbortSignal): Promise<Subscription[]>
  subscribe(input: SubscriptionInput, commandKey: string): Promise<Subscription>
  unsubscribe(id: string, expectedVersion: number, commandKey: string): Promise<void>
  notifications(signal: AbortSignal): Promise<Notification[]>
  registrations(signal: AbortSignal): Promise<Registration[]>
  cancelRegistration(sessionId: string, expectedSequence: string, commandKey: string): Promise<Registration>
  prepareDraft(sessionId: string, ruleVersion: number, commandKey: string): Promise<Draft>
  confirmDraft(draftId: string): Promise<Registration>
  openChat(commandKey: string): Promise<Chat>
  chat(id: string, signal: AbortSignal): Promise<Chat>
  ask(chatId: string, expectedVersion: number, messageId: string, question: string): Promise<Chat>
  admin(section: AdminSection, signal: AbortSignal): Promise<AdminRow[]>
  adminAction(section: AdminSection, id: string, expectedVersion: number, action: string, reason: string, commandKey: string, changes?: ReviewField[]): Promise<void>
  createSession(input: SessionInput, commandKey: string): Promise<void>
}
