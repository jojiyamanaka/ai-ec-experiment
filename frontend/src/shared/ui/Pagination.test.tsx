import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import Pagination from './Pagination'

describe('Pagination', () => {
  it('disables Prev on first page and calls next page on Next click', () => {
    const onPageChange = vi.fn()

    render(<Pagination currentPage={1} totalPages={3} onPageChange={onPageChange} />)

    expect(screen.getByRole('button', { name: 'Prev' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: 'Next' }))
    expect(onPageChange).toHaveBeenCalledWith(2)
  })

  it('renders ellipsis when total pages are large', () => {
    const onPageChange = vi.fn()

    render(<Pagination currentPage={5} totalPages={12} onPageChange={onPageChange} />)

    expect(screen.getAllByText('...').length).toBeGreaterThanOrEqual(1)
    fireEvent.click(screen.getByRole('button', { name: '12' }))
    expect(onPageChange).toHaveBeenCalledWith(12)
  })
})
