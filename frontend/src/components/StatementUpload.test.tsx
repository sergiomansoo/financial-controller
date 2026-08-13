import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { StatementUpload } from './StatementUpload'

describe('StatementUpload', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requires preview confirmation before uploading', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ previewCount: 1, duplicateCount: 0, rows: [{ date: '2026-08-01', history: 'Mercado', description: null, amount: 10, type: 'EXPENSE', duplicate: false }] }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ importedCount: 1, duplicateCount: 0, transactions: [] }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)
    render(<StatementUpload />)
    fireEvent.change(screen.getByLabelText(/arquivo csv/i), { target: { files: [new File(['csv'], 'statement.csv', { type: 'text/csv' })] } })
    fireEvent.submit(screen.getByRole('button', { name: /ver prévia/i }).closest('form')!)
    expect(await screen.findByRole('button', { name: /confirmar importação/i })).toBeEnabled()
    expect(fetchMock).toHaveBeenCalledTimes(1)
    fireEvent.click(screen.getByRole('button', { name: /confirmar importação/i }))
    expect(await screen.findByRole('status')).toHaveTextContent('1 movimentações importadas')
    expect(fetchMock).toHaveBeenCalledTimes(2)
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
    fireEvent.change(screen.getByLabelText(/arquivo csv/i), {
      target: { files: [new File(['content'], 'statement.csv', { type: 'text/csv' })] },
    })
    fireEvent.submit(screen.getByRole('button', { name: /ver prévia/i }).closest('form')!)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.',
    )

    const request = fetchMock.mock.calls[0][1] as RequestInit
    expect(new Headers(request.headers).get('Content-Type')).toBeNull()
  })
})
