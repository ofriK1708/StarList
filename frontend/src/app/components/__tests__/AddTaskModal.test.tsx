import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AddTaskModal } from '../AddTaskModal'
import { TITLE_MAX_LENGTH, DESCRIPTION_MAX_LENGTH } from '@/services/validation.ts'

const props = () => ({ isOpen: true, onClose: vi.fn(), onAdd: vi.fn() })

const title = () => screen.getByPlaceholderText('e.g., Finish OS Assignment') as HTMLInputElement
const desc = () => screen.getByPlaceholderText('Add some details...') as HTMLTextAreaElement

describe('AddTaskModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders nothing when closed', () => {
    const { container } = render(<AddTaskModal {...props()} isOpen={false} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('enforces the shared character limits on title and description', () => {
    render(<AddTaskModal {...props()} />)
    expect(title()).toHaveAttribute('maxlength', String(TITLE_MAX_LENGTH))
    expect(desc()).toHaveAttribute('maxlength', String(DESCRIPTION_MAX_LENGTH))
  })

  it('shows a live character counter for the title', async () => {
    render(<AddTaskModal {...props()} />)
    expect(screen.getByText(`0/${TITLE_MAX_LENGTH}`)).toBeInTheDocument()
    await userEvent.type(title(), 'abcde')
    expect(screen.getByText(`5/${TITLE_MAX_LENGTH}`)).toBeInTheDocument()
  })

  it('does not submit an empty title', async () => {
    const p = props()
    render(<AddTaskModal {...p} />)
    await userEvent.click(screen.getByRole('button', { name: /Add Mission/i }))
    expect(p.onAdd).not.toHaveBeenCalled()
  })

  it('submits a well-formed payload and closes', async () => {
    const p = props()
    render(<AddTaskModal {...p} />)

    await userEvent.type(title(), 'Write report')
    await userEvent.type(desc(), 'the quarterly one')
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Difficulty' }), 'HARD')

    const duration = screen.getByRole('spinbutton') as HTMLInputElement
    await userEvent.clear(duration)
    await userEvent.type(duration, '45')

    await userEvent.click(screen.getByRole('button', { name: /Add Mission/i }))

    expect(p.onAdd).toHaveBeenCalledTimes(1)
    const payload = p.onAdd.mock.calls[0][0]
    expect(payload).toMatchObject({
      title: 'Write report',
      description: 'the quarterly one',
      difficultyLevel: 'HARD',
      durationMinutes: 45,
    })
    expect(payload.dueDate).toBeUndefined()
    expect(p.onClose).toHaveBeenCalledTimes(1)
  })

  it('converts a duration entered in hours to minutes', async () => {
    const p = props()
    render(<AddTaskModal {...p} />)
    await userEvent.type(title(), 'Long task')

    const duration = screen.getByRole('spinbutton') as HTMLInputElement
    await userEvent.clear(duration)
    await userEvent.type(duration, '2')
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Duration unit' }), 'hours')
    await userEvent.click(screen.getByRole('button', { name: /Add Mission/i }))

    expect(p.onAdd.mock.calls[0][0].durationMinutes).toBe(120)
  })

  it('converts a duration entered in days to minutes', async () => {
    const p = props()
    render(<AddTaskModal {...p} />)
    await userEvent.type(title(), 'Multi-day task')

    const duration = screen.getByRole('spinbutton') as HTMLInputElement
    await userEvent.clear(duration)
    await userEvent.type(duration, '3')
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Duration unit' }), 'days')
    await userEvent.click(screen.getByRole('button', { name: /Add Mission/i }))

    expect(p.onAdd.mock.calls[0][0].durationMinutes).toBe(3 * 24 * 60)
  })

  it('converts a picked due date to an ISO string', async () => {
    const p = props()
    render(<AddTaskModal {...p} />)
    await userEvent.type(title(), 'With deadline')

    const due = document.querySelector('input[type="datetime-local"]') as HTMLInputElement
    await userEvent.type(due, '2999-05-01T09:30')

    await userEvent.click(screen.getByRole('button', { name: /Add Mission/i }))

    const payload = p.onAdd.mock.calls[0][0]
    expect(payload.dueDate).toBe(new Date('2999-05-01T09:30').toISOString())
  })

  it('Cancel closes without submitting', async () => {
    const p = props()
    render(<AddTaskModal {...p} />)
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(p.onClose).toHaveBeenCalledTimes(1)
    expect(p.onAdd).not.toHaveBeenCalled()
  })
})
