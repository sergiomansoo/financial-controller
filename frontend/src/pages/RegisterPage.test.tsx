import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { AuthProvider } from '../lib/auth'
import { RegisterPage } from './RegisterPage'

describe('RegisterPage', () => {
  afterEach(() => {
    localStorage.clear()
    vi.unstubAllGlobals()
  })

  it('uses ledger auth controls with readable token styles', () => {
    render(<MemoryRouter><AuthProvider><RegisterPage /></AuthProvider></MemoryRouter>)
    expect(screen.getByLabelText('Nome')).toHaveClass('ledger-auth-control')
    expect(screen.getByLabelText('Senha')).toHaveClass('ledger-auth-control')
  })

  it('persists the returned session and redirects to the dashboard after registration', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            accessToken: 'registered-access-token',
            tokenType: 'Bearer',
            user: { id: '2', name: 'Grace Hopper', email: 'grace@example.com' },
          }),
          { status: 201, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    )

    render(
      <MemoryRouter initialEntries={['/register']}>
        <AuthProvider>
          <Routes>
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/dashboard" element={<h1>Dashboard</h1>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText(/^nome$/i), {
      target: { value: 'Grace Hopper' },
    })
    fireEvent.change(screen.getByLabelText(/email/i), {
      target: { value: 'grace@example.com' },
    })
    fireEvent.change(screen.getByLabelText(/senha/i), {
      target: { value: 'correct-horse-battery-staple' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    })
    expect(JSON.parse(localStorage.getItem('financial-controller.session')!)).toEqual({
      token: 'registered-access-token',
      user: { id: '2', name: 'Grace Hopper', email: 'grace@example.com' },
    })
  })
})
