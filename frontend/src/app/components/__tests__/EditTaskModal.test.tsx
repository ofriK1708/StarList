import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { EditTaskModal } from '../EditTaskModal'
import type { TaskResponse } from '@/services/taskApi.ts'

const task = (over: Partial<TaskResponse> = {}): TaskResponse => ({
  taskId: 12,
  title: 'Original title',
  description: 'Original description',
  difficultyLevel: 'EASY',
  durationMinutes: 25,
  coinReward: 10,
  coinPenalty: null,
  status: 'PENDING',
  dueDate: null,
  createdAt: '2026-01-01T00:00:00Z',
  ...over,
})

const props = (over = {}) => ({
  isOpen: true,
  onClose: vi.fn(),
  taskToEdit: task(),
  onUpdate: vi.fn(),
  ...over,
})

const titleInput = () => screen.getByPlaceholderText('e.g., Finish OS Assignment') as HTMLInputElement

describe('EditTaskModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders nothing when closed or when there is no task', () => {
    const { container: c1 } = render(<EditTaskModal {...props()} isOpen={false} />)
    expect(c1).toBeEmptyDOMElement()
    const { container: c2 } = render(<EditTaskModal {...props()} taskToEdit={null} />)
    expect(c2).toBeEmptyDOMElement()
  })

  it('prefills the form from the task being edited', () => {
    render(<EditTaskModal {...props()} />)
    expect(titleInput().value).toBe('Original title')
    expect((screen.getByPlaceholderText('Add some details...') as HTMLTextAreaElement).value).toBe(
      'Original description',
    )
    expect((screen.getByRole('combobox', { name: 'Difficulty' }) as HTMLSelectElement).value).toBe('EASY')
    expect((screen.getByRole('spinbutton') as HTMLInputElement).value).toBe('25')
    expect((screen.getByRole('combobox', { name: 'Duration unit' }) as HTMLSelectElement).value).toBe('minutes')
  })

  it('prefills a round-hours duration as hours', () => {
    render(<EditTaskModal {...props({ taskToEdit: task({ durationMinutes: 120 }) })} />)
    expect((screen.getByRole('spinbutton') as HTMLInputElement).value).toBe('2')
    expect((screen.getByRole('combobox', { name: 'Duration unit' }) as HTMLSelectElement).value).toBe('hours')
  })

  it('re-converts an hours duration back to minutes on save', async () => {
    const p = props({ taskToEdit: task({ durationMinutes: 120 }) })
    render(<EditTaskModal {...p} />)
    await userEvent.click(screen.getByRole('button', { name: /Save Changes/i }))
    expect(p.onUpdate.mock.calls[0][1].durationMinutes).toBe(120)
  })

  it('prefills the due date into the datetime-local control', () => {
    render(<EditTaskModal {...props({ taskToEdit: task({ dueDate: '2999-03-04T07:08:00Z' }) })} />)
    const due = document.querySelector('input[type="datetime-local"]') as HTMLInputElement
    const d = new Date('2999-03-04T07:08:00Z')
    const pad = (n: number) => String(n).padStart(2, '0')
    expect(due.value).toBe(
      `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`,
    )
  })

  it('calls onUpdate with the task id and edited fields', async () => {
    const p = props()
    render(<EditTaskModal {...p} />)

    await userEvent.clear(titleInput())
    await userEvent.type(titleInput(), 'Renamed')
    await userEvent.click(screen.getByRole('button', { name: /Save Changes/i }))

    expect(p.onUpdate).toHaveBeenCalledTimes(1)
    expect(p.onUpdate.mock.calls[0][0]).toBe(12)
    expect(p.onUpdate.mock.calls[0][1]).toMatchObject({ title: 'Renamed', difficultyLevel: 'EASY' })
  })

  it('does not submit if the title is cleared', async () => {
    const p = props()
    render(<EditTaskModal {...p} />)
    await userEvent.clear(titleInput())
    await userEvent.click(screen.getByRole('button', { name: /Save Changes/i }))
    expect(p.onUpdate).not.toHaveBeenCalled()
  })

  it('allows a duration of 0', async () => {
    const p = props()
    render(<EditTaskModal {...p} />)
    const duration = screen.getByRole('spinbutton') as HTMLInputElement
    await userEvent.clear(duration)
    await userEvent.type(duration, '0')
    await userEvent.click(screen.getByRole('button', { name: /Save Changes/i }))
    expect(p.onUpdate.mock.calls[0][1].durationMinutes).toBe(0)
  })
})
