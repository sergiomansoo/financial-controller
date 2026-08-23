import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, askAssistant, login } from './api'

describe('API client', () => {
  afterEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('sends the persisted session token as a bearer authorization header', async () => {
    localStorage.setItem(
      'financial-controller.session',
      JSON.stringify({ token: 'persisted-token', user: { id: '1', name: 'Ada', email: 'ada@example.com' } }),
    )
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          accessToken: 'new-token',
          tokenType: 'Bearer',
          user: { id: '1', name: 'Ada', email: 'ada@example.com' },
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    await login({ email: 'ada@example.com', password: 'secret' })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/auth/login',
      expect.any(Object),
    )
    const request = fetchMock.mock.calls[0][1] as RequestInit
    expect(new Headers(request.headers).get('Authorization')).toBe('Bearer persisted-token')
  })

  it('converts a JSON problem response into a typed ApiError', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({ code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' }),
          { status: 401, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    await expect(login({ email: 'ada@example.com', password: 'wrong' })).rejects.toMatchObject({
      name: 'ApiError',
      status: 401,
      code: 'INVALID_CREDENTIALS',
      message: 'Invalid email or password.',
    } satisfies Partial<ApiError>)
  })

  it('posts a typed assistant request with the persisted bearer session', async () => {
    localStorage.setItem(
      'financial-controller.session',
      JSON.stringify({ token: 'assistant-token', user: { id: '1', name: 'Ada', email: 'ada@example.com' } }),
    )
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ message: 'Resposta financeira.' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const input = {
      message: 'Quanto gastei?',
      month: '2026-08',
      history: [{ role: 'user' as const, content: 'Olá' }],
    }

    await expect(askAssistant(input)).resolves.toEqual({ message: 'Resposta financeira.' })
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/v1/assistant/chat',
      expect.objectContaining({ method: 'POST', body: JSON.stringify(input) }),
    )
    const request = fetchMock.mock.calls[0][1] as RequestInit
    expect(new Headers(request.headers).get('Authorization')).toBe('Bearer assistant-token')
  })
})
