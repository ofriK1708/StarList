import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { TaskDashboard } from '../TaskDashboard'
import type { TaskResponse } from '@/services/taskApi.ts'

const makeTask = (over: Partial<TaskResponse> = {}): TaskResponse => ({
  taskId: 1,
  title: 'Task',
  description: '',
  difficultyLevel: 'MEDIUM',
  durationMinutes: 30,
  coinReward: 10,
  coinPenalty: null,
  status: 'PENDING',
  dueDate: null,
  createdAt: '2026-01-01T00:00:00Z',
  ...over,
})

const baseProps = (tasks: TaskResponse[]) => ({
  tasks,
  onTaskToggle: vi.fn(),
  onQuickAdd: vi.fn(),
  onOpenChat: vi.fn(),
  onTaskEdit: vi.fn(),
  onTaskDelete: vi.fn(),
})

describe('TaskDashboard', () => {
  beforeEach(() => vi.clearAllMocks())

  it('defaults to the "active" filter and hides completed tasks', () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'Open task' }),
          makeTask({ taskId: 2, title: 'Done task', status: 'COMPLETED' }),
        ])}
      />,
    )
    expect(screen.getByText('Open task')).toBeInTheDocument()
    expect(screen.queryByText('Done task')).not.toBeInTheDocument()
  })

  it('"Completed" filter shows only completed tasks', async () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'Open task' }),
          makeTask({ taskId: 2, title: 'Done task', status: 'COMPLETED' }),
        ])}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Completed' }))
    expect(screen.getByText('Done task')).toBeInTheDocument()
    expect(screen.queryByText('Open task')).not.toBeInTheDocument()
  })

  it('"All" filter shows both', async () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'Open task' }),
          makeTask({ taskId: 2, title: 'Done task', status: 'COMPLETED' }),
        ])}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'All' }))
    expect(screen.getByText('Open task')).toBeInTheDocument()
    expect(screen.getByText('Done task')).toBeInTheDocument()
  })

  it('shows the empty state when the filter matches nothing', () => {
    render(<TaskDashboard {...baseProps([makeTask({ status: 'COMPLETED' })])} />)
    expect(screen.getByText('No tasks found')).toBeInTheDocument()
  })

  it('sorts by name', async () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'Zebra' }),
          makeTask({ taskId: 2, title: 'Apple' }),
        ])}
      />,
    )
    await userEvent.selectOptions(screen.getByRole('combobox'), 'name')
    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)
    expect(headings).toEqual(['Apple', 'Zebra'])
  })

  it('sorts by difficulty (hardest first)', async () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'Easy one', difficultyLevel: 'EASY' }),
          makeTask({ taskId: 2, title: 'Hard one', difficultyLevel: 'HARD' }),
        ])}
      />,
    )
    await userEvent.selectOptions(screen.getByRole('combobox'), 'difficulty')
    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)
    expect(headings).toEqual(['Hard one', 'Easy one'])
  })

  it('sorts by due date (earliest first, undated last)', async () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'No date', dueDate: null }),
          makeTask({ taskId: 2, title: 'Later', dueDate: '2999-02-01T00:00:00Z' }),
          makeTask({ taskId: 3, title: 'Sooner', dueDate: '2999-01-01T00:00:00Z' }),
        ])}
      />,
    )
    // 'date' is the default sort
    const headings = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)
    expect(headings).toEqual(['Sooner', 'Later', 'No date'])
  })

  it('sorts by duration (shortest first)', async () => {
    render(
      <TaskDashboard
        {...baseProps([
          makeTask({ taskId: 1, title: 'Long', durationMinutes: 120 }),
          makeTask({ taskId: 2, title: 'Quick', durationMinutes: 10 }),
          makeTask({ taskId: 3, title: 'Medium', durationMinutes: 45 }),
        ])}
      />,
    )
    await userEvent.selectOptions(screen.getByRole('combobox'), 'duration')
    const order = screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)
    expect(order).toEqual(['Quick', 'Medium', 'Long'])
  })

  it('shows the duration in the unit it reads best as', () => {
    render(<TaskDashboard {...baseProps([makeTask({ title: 'Two hours', durationMinutes: 120 })])} />)
    expect(screen.getByText('2 hr')).toBeInTheDocument()
  })

  it('shows an awkward duration in minutes', () => {
    render(<TaskDashboard {...baseProps([makeTask({ title: 'Odd', durationMinutes: 45 })])} />)
    expect(screen.getByText('45 min')).toBeInTheDocument()
  })

  it('clicking a task card toggles completion', async () => {
    const props = baseProps([makeTask({ taskId: 5, title: 'Toggle me' })])
    render(<TaskDashboard {...props} />)
    await userEvent.click(screen.getByText('Toggle me'))
    expect(props.onTaskToggle).toHaveBeenCalledWith(5)
  })

  it('edit button calls onTaskEdit without toggling the task', async () => {
    const props = baseProps([makeTask({ taskId: 5, title: 'Editable' })])
    render(<TaskDashboard {...props} />)
    await userEvent.click(screen.getByTitle('Edit task'))
    expect(props.onTaskEdit).toHaveBeenCalledWith(expect.objectContaining({ taskId: 5 }))
    expect(props.onTaskToggle).not.toHaveBeenCalled()
  })

  it('delete button calls onTaskDelete without toggling the task', async () => {
    const props = baseProps([makeTask({ taskId: 8, title: 'Removable' })])
    render(<TaskDashboard {...props} />)
    await userEvent.click(screen.getByTitle('Delete task'))
    expect(props.onTaskDelete).toHaveBeenCalledWith(8)
    expect(props.onTaskToggle).not.toHaveBeenCalled()
  })

  it('completed tasks expose no edit/delete controls', async () => {
    render(<TaskDashboard {...baseProps([makeTask({ status: 'COMPLETED', title: 'Locked' })])} />)
    await userEvent.click(screen.getByRole('button', { name: 'All' }))
    expect(screen.queryByTitle('Edit task')).not.toBeInTheDocument()
    expect(screen.queryByTitle('Delete task')).not.toBeInTheDocument()
  })

  it('header actions call their handlers', async () => {
    const props = baseProps([makeTask()])
    render(<TaskDashboard {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /AI Chat/i }))
    await userEvent.click(screen.getByRole('button', { name: /Add Task/i }))
    expect(props.onOpenChat).toHaveBeenCalledTimes(1)
    expect(props.onQuickAdd).toHaveBeenCalledTimes(1)
  })

  it('renders the coin reward for a task', () => {
    render(<TaskDashboard {...baseProps([makeTask({ title: 'Rewarded', coinReward: 42 })])} />)
    const card = screen.getByText('Rewarded').closest('div')!.parentElement!.parentElement!
    expect(within(card).getByText('+42')).toBeInTheDocument()
  })
})
