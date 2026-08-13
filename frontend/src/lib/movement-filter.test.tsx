import { fireEvent, render, screen } from '@testing-library/react'
import { MovementFilterProvider, useMovementFilter } from './movement-filter'

function Probe() {
  const { filter, setFilter } = useMovementFilter()
  return <button onClick={() => setFilter('income')}>{filter}</button>
}

test('persists the selected movement filter', () => {
  localStorage.clear()
  render(<MovementFilterProvider><Probe /></MovementFilterProvider>)
  fireEvent.click(screen.getByRole('button', { name: 'both' }))
  expect(screen.getByRole('button')).toHaveTextContent('income')
  expect(localStorage.getItem('financial-controller.movement-filter')).toBe('income')
})
