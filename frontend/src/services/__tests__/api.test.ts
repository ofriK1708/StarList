import { describe, it, expect, afterEach } from 'vitest'
import api, { setAuthToken } from '../api'

describe('api / setAuthToken', () => {
  afterEach(() => {
    setAuthToken(null)
  })

  it('exposes an axios-like instance with verb methods', () => {
    expect(typeof api.get).toBe('function')
    expect(typeof api.post).toBe('function')
    expect(typeof api.put).toBe('function')
    expect(typeof api.delete).toBe('function')
  })

  it('sets a Bearer Authorization header when given a token', () => {
    setAuthToken('jwt-abc')
    expect(api.defaults.headers.common['Authorization']).toBe('Bearer jwt-abc')
  })

  it('removes the Authorization header when passed null', () => {
    setAuthToken('jwt-abc')
    setAuthToken(null)
    expect(api.defaults.headers.common['Authorization']).toBeUndefined()
  })
})
