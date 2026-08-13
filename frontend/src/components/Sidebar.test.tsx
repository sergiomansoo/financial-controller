import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { Sidebar } from './Sidebar'
describe('Sidebar', () => { it('shows a delayed accessible tooltip when collapsed navigation receives focus', async () => { render(<MemoryRouter><Sidebar collapsed /></MemoryRouter>); fireEvent.focus(screen.getByRole('link', { name: /visão geral/i })); expect(screen.queryByRole('tooltip')).not.toBeInTheDocument(); await waitFor(() => expect(screen.getByRole('tooltip')).toHaveTextContent('Visão geral'), { timeout: 500 }) }) })
