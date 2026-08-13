import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { StatementUpload } from './StatementUpload'

describe('StatementUpload', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows the server message when the uploaded statement is rejected', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'UNSUPPORTED_STATEMENT_FORMAT',
          message: 'Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.',
        }),
        { status: 400, headers: { 'Content-Type': 'application/json' } },
      ),
    )
    vi.stubGlobal('fetch', fetchMock)

    render(<StatementUpload />)
    fireEvent.change(screen.getByLabelText(/statement file/i), {
      target: { files: [new File(['content'], 'statement.csv', { type: 'text/csv' })] },
    })
    fireEvent.submit(screen.getByRole('button', { name: /upload statement/i }).closest('form')!)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.',
    )

    const request = fetchMock.mock.calls[0][1] as RequestInit
    expect(new Headers(request.headers).get('Content-Type')).toBeNull()
  })
})
