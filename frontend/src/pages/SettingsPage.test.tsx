import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AuthProvider } from '../lib/auth'
import { MovementFilterProvider } from '../lib/movement-filter'
import { SettingsPage } from './SettingsPage'
it('persists the chosen settings filter', () => { localStorage.setItem('financial-controller.session', JSON.stringify({ token: 'token', user: { id: 1, name: 'Ana', email: 'ana@example.com' } })); render(<AuthProvider><MovementFilterProvider><SettingsPage /></MovementFilterProvider></AuthProvider>); fireEvent.click(screen.getByLabelText('Receitas')); expect(localStorage.getItem('financial-controller.movement-filter')).toBe('income') })
