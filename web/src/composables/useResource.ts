import { onScopeDispose, shallowRef } from 'vue'
import { type LoadState, type FailureCode } from '../domain/contracts'
import { errorCode } from '../domain/presentation'

export function useResource<T>() {
  const state = shallowRef<LoadState<T>>({ kind: 'unconnected' })
  let epoch = 0
  let controller: AbortController | undefined
  async function load(reader?: (signal: AbortSignal) => Promise<T>) {
    const current = ++epoch
    controller?.abort()
    if (!reader) { state.value = { kind: 'unconnected' }; return }
    controller = new AbortController()
    state.value = { kind: 'loading' }
    try {
      const value = await reader(controller.signal)
      if (current === epoch) state.value = { kind: 'ready', value }
    } catch (error) {
      if (current === epoch) state.value = { kind: 'error', code: errorCode(error) }
    }
  }
  function fail(code: FailureCode) { ++epoch; controller?.abort(); state.value = { kind: 'error', code } }
  onScopeDispose(() => { ++epoch; controller?.abort() })
  return { state, load, fail }
}
