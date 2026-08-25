import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { ApiError, askAssistant } from '../lib/api'
import { AssistantPage } from './AssistantPage'

vi.mock('../lib/api', async (importOriginal) => {
  const original = await importOriginal<typeof import('../lib/api')>()

  return { ...original, askAssistant: vi.fn() }
})

const askAssistantMock = vi.mocked(askAssistant)

describe('AssistantPage', () => {
  afterEach(() => vi.resetAllMocks())

  it('sends the selected month and renders the assistant answer', async () => {
    askAssistantMock.mockResolvedValue({ message: 'Transporte representa R$ 180,00.' })
    render(<AssistantPage />)

    fireEvent.change(screen.getByLabelText('Mês de análise'), { target: { value: '2026-07' } })
    fireEvent.change(screen.getByLabelText('Pergunta'), {
      target: { value: 'Qual foi a maior despesa?' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))

    expect(askAssistantMock).toHaveBeenCalledWith({
      message: 'Qual foi a maior despesa?',
      month: '2026-07',
      history: [],
    })
    expect(await screen.findByText('Transporte representa R$ 180,00.')).toBeInTheDocument()
    expect(screen.getByText('Qual foi a maior despesa?')).toBeInTheDocument()
    expect(screen.getByLabelText('Pergunta')).toHaveValue('')
  })

  it('sends a question with Enter and keeps Shift+Enter available for a line break', async () => {
    askAssistantMock.mockResolvedValue({ message: 'Resposta.' })
    render(<AssistantPage />)

    const question = screen.getByLabelText('Pergunta')
    fireEvent.change(question, { target: { value: 'Posso enviar com Enter?' } })
    fireEvent.keyDown(question, { key: 'Enter' })

    await waitFor(() => expect(askAssistantMock).toHaveBeenCalledTimes(1))

    fireEvent.change(question, { target: { value: 'Uma pergunta\ncom duas linhas' } })
    fireEvent.keyDown(question, { key: 'Enter', shiftKey: true })

    expect(askAssistantMock).toHaveBeenCalledTimes(1)
  })

  it('keeps the question visible and retries the last unsent request after a provider error', async () => {
    const unavailable = new ApiError(
      503,
      'AI_UNAVAILABLE',
      'O assistente de IA está indisponível no momento. Tente novamente em instantes.',
    )
    askAssistantMock
      .mockRejectedValueOnce(unavailable)
      .mockResolvedValueOnce({ message: 'Agora consigo responder.' })
    render(<AssistantPage />)

    fireEvent.change(screen.getByLabelText('Mês de análise'), { target: { value: '2026-06' } })
    fireEvent.change(screen.getByLabelText('Pergunta'), {
      target: { value: 'Quanto gastei?' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(unavailable.message)
    expect(screen.getByLabelText('Pergunta')).toHaveValue('Quanto gastei?')
    expect(within(screen.getByRole('region', { name: 'Conversa com o assistente' }))
      .queryByText('Quanto gastei?')).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Pergunta'), {
      target: { value: 'Um rascunho diferente' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    await waitFor(() => expect(askAssistantMock).toHaveBeenCalledTimes(2))
    expect(askAssistantMock).toHaveBeenLastCalledWith({
      message: 'Quanto gastei?',
      month: '2026-06',
      history: [],
    })
    expect(await screen.findByText('Agora consigo responder.')).toBeInTheDocument()
    expect(screen.getByLabelText('Pergunta')).toHaveValue('Um rascunho diferente')
  })

  it('shows the privacy notice and renders assistant content as plain text', async () => {
    askAssistantMock.mockResolvedValue({ message: '<img src=x onerror=alert(1)> resposta' })
    const { container } = render(<AssistantPage />)

    expect(screen.getByText(
      'Ao enviar uma pergunta, os dados financeiros necessários para a análise são processados pelo provedor de IA Groq. Não envie senhas, chaves ou dados que não estejam no seu controle financeiro.',
    )).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Pergunta'), { target: { value: 'Analise meus dados' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))

    expect(await screen.findByText('<img src=x onerror=alert(1)> resposta')).toBeInTheDocument()
    expect(container.querySelector('img')).not.toBeInTheDocument()
  })

  it('renders emphasis returned in a general assistant answer', async () => {
    askAssistantMock.mockResolvedValue({ message: '**Importante:** mantenha uma reserva antes de investir.' })
    render(<AssistantPage />)

    fireEvent.change(screen.getByLabelText('Pergunta'), { target: { value: 'Me dê uma dica de investimento' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))

    expect(await screen.findByText('Importante:', { selector: 'strong' })).toBeInTheDocument()
  })

  it('scrolls only the transcript to the newest assistant response', async () => {
    const originalScrollTo = HTMLElement.prototype.scrollTo
    const scrollTo = vi.fn()
    Object.defineProperty(HTMLElement.prototype, 'scrollTo', { configurable: true, value: scrollTo })
    askAssistantMock.mockResolvedValue({ message: 'Resposta nova.' })
    render(<AssistantPage />)

    fireEvent.change(screen.getByLabelText('Pergunta'), { target: { value: 'Pergunta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))

    await screen.findByText('Resposta nova.')
    expect(scrollTo).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }))
    Object.defineProperty(HTMLElement.prototype, 'scrollTo', { configurable: true, value: originalScrollTo })
  })

  it('keeps only the latest ten completed transcript messages', async () => {
    askAssistantMock.mockImplementation(async ({ message }) => ({ message: `Resposta ${message}` }))
    render(<AssistantPage />)

    for (let index = 1; index <= 6; index += 1) {
      fireEvent.change(screen.getByLabelText('Pergunta'), { target: { value: `Pergunta ${index}` } })
      fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))
      await screen.findByText(`Resposta Pergunta ${index}`)
    }

    expect(screen.queryByText('Pergunta 1')).not.toBeInTheDocument()
    expect(screen.queryByText('Resposta Pergunta 1')).not.toBeInTheDocument()
    expect(screen.getByText('Pergunta 2')).toBeInTheDocument()
    expect(screen.getByText('Resposta Pergunta 6')).toBeInTheDocument()
    const lastRequest = askAssistantMock.mock.calls[askAssistantMock.mock.calls.length - 1]?.[0]
    expect(lastRequest?.history).toHaveLength(10)
  })

  it('keeps a detailed assistant answer available as clean history for a follow-up question', async () => {
    const detailedAnswer = 'a'.repeat(1001)
    askAssistantMock
      .mockResolvedValueOnce({ message: detailedAnswer, visualType: 'budget_summary', visualData: { categories: [] } })
      .mockResolvedValueOnce({ message: 'Suas metas est\u00e3o em dia.' })
    render(<AssistantPage />)

    const question = screen.getByLabelText('Pergunta')
    fireEvent.change(question, { target: { value: 'Me explique o carro' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))
    await screen.findByText(detailedAnswer)

    fireEvent.change(question, { target: { value: 'Quais s\u00e3o minhas metas?' } })
    fireEvent.click(screen.getByRole('button', { name: 'Enviar pergunta' }))

    await waitFor(() => expect(askAssistantMock).toHaveBeenCalledTimes(2))
    expect(askAssistantMock).toHaveBeenLastCalledWith({
      message: 'Quais s\u00e3o minhas metas?',
      month: expect.any(String),
      history: [
        { role: 'user', content: 'Me explique o carro' },
        { role: 'assistant', content: detailedAnswer },
      ],
    })
  })
})
