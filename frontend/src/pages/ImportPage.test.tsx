import { render, screen } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import { ImportPage } from './ImportPage'
import { getImports } from '../lib/api'
vi.mock('../lib/api', () => ({ getImports: vi.fn(), previewStatement: vi.fn(), uploadStatement: vi.fn() }))
it('lists imported CSV files with localized date and row count', async () => { vi.mocked(getImports).mockResolvedValue([{ originalFilename: 'agosto.csv', importedAt: '2026-08-14T15:30:00Z', rowCount: 42 }]); render(<ImportPage />); expect(await screen.findByText('agosto.csv')).toBeInTheDocument(); expect(screen.getByText(/42 linhas/)).toBeInTheDocument(); expect(screen.getByText(/14\/08\/2026/)).toBeInTheDocument() })
