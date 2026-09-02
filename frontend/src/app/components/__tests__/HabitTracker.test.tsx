import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HabitTracker } from '../HabitTracker'
import type { HabitResponse } from '@/services/habitsApi.ts'

const todayStr = () => new Date().toISOString().split('T')[0]

const makeHabit = (over: Partial<HabitResponse> = {}): HabitResponse => ({
  habitId: 1,
  title: 'Habit',
  description: null,
  frequency: 'DAILY',
  difficultyLevel: 'MEDIUM',
  coinReward: 10,
  coinPenalty: null,
  currentStreak: 0,
  bestStreak: 0,
  totalCompletions: 0,
  lastCompletedDate: null,
  createdAt: '2026-01-01T00:00:00Z',
  isActive: true,
  scheduledDayOfWeek: null,
  scheduledTimeType: null,
  scheduledHour: null,
  customIntervalDays: null,
  scheduledDaysOfWeek: null,
  streakUnit: 'day',
  ...over,
})

const baseProps = (habits: HabitResponse[]) => ({
  habits,
  onHabitCheck: vi.fn(),
  onAddHabitClick: vi.fn(),
  onEditHabitClick: vi.fn(),
  onDeleteHabit: vi.fn(),
})

const headings = () =>
  screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent?.replace(/\s+/g, ' ').trim())

describe('HabitTracker', () => {
  beforeEach(() => vi.clearAllMocks())

  it('disables Complete for a habit not scheduled today', async () => {
    const habit = makeHabit({
      habitId: 9,
      title: 'Gym days',
      frequency: 'MULTI_DAY',
      scheduledDaysOfWeek: [1, 4],
      periodStatus: { periodStart: null, periodEnd: null, dueDate: null, scheduledToday: false, completedThisPeriod: false, daysLate: 0, daysUntilDue: 0 },
    })
    const p = baseProps([habit])
    render(<HabitTracker {...p} />)

    const button = screen.getByText('Not today').closest('button')!
    expect(button).toBeDisabled()

    await userEvent.click(button)
    expect(p.onHabitCheck).not.toHaveBeenCalled()
  })

  it('keeps Complete enabled on a scheduled day', () => {
    const habit = makeHabit({
      habitId: 9,
      title: 'Gym days',
      frequency: 'MULTI_DAY',
      scheduledDaysOfWeek: [1, 4],
      periodStatus: { periodStart: null, periodEnd: null, dueDate: null, scheduledToday: true, completedThisPeriod: false, daysLate: 0, daysUntilDue: 0 },
    })
    render(<HabitTracker {...baseProps([habit])} />)
    expect(screen.getByText('Complete').closest('button')).toBeEnabled()
  })

  it('shows "No habits yet" with an empty list', () => {
    render(<HabitTracker {...baseProps([])} />)
    expect(screen.getByText('No habits yet')).toBeInTheDocument()
  })

  it('shows a filter-specific empty message when a filter hides everything', async () => {
    render(<HabitTracker {...baseProps([makeHabit({ title: 'Unfinished', lastCompletedDate: null })])} />)
    await userEvent.click(screen.getByRole('button', { name: 'Completed' }))
    expect(screen.getByText('No habits match this filter')).toBeInTheDocument()
  })

  it('"Completed" filter keeps only habits done for the current period', async () => {
    render(
      <HabitTracker
        {...baseProps([
          makeHabit({ habitId: 1, title: 'Done today', lastCompletedDate: todayStr() }),
          makeHabit({ habitId: 2, title: 'Not yet', lastCompletedDate: null }),
        ])}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Completed' }))
    expect(screen.getByText('Done today')).toBeInTheDocument()
    expect(screen.queryByText('Not yet')).not.toBeInTheDocument()
  })

  it('"Active" filter keeps only habits not done for the current period', async () => {
    render(
      <HabitTracker
        {...baseProps([
          makeHabit({ habitId: 1, title: 'Done today', lastCompletedDate: todayStr() }),
          makeHabit({ habitId: 2, title: 'Not yet', lastCompletedDate: null }),
        ])}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Active' }))
    expect(screen.getByText('Not yet')).toBeInTheDocument()
    expect(screen.queryByText('Done today')).not.toBeInTheDocument()
  })

  it('default sort is newest-created first', () => {
    render(
      <HabitTracker
        {...baseProps([
          makeHabit({ habitId: 1, title: 'Older', createdAt: '2026-01-01T00:00:00Z' }),
          makeHabit({ habitId: 2, title: 'Newer', createdAt: '2026-06-01T00:00:00Z' }),
        ])}
      />,
    )
    expect(headings()).toEqual(['Newer', 'Older'])
  })

  it('sorts by name', async () => {
    render(
      <HabitTracker
        {...baseProps([
          makeHabit({ habitId: 1, title: 'Yoga' }),
          makeHabit({ habitId: 2, title: 'Abs' }),
        ])}
      />,
    )
    await userEvent.selectOptions(screen.getByRole('combobox'), 'name')
    expect(headings()).toEqual(['Abs', 'Yoga'])
  })

  it('sorts by streak (highest first)', async () => {
    render(
      <HabitTracker
        {...baseProps([
          makeHabit({ habitId: 1, title: 'Low', currentStreak: 2 }),
          makeHabit({ habitId: 2, title: 'High', currentStreak: 30 }),
        ])}
      />,
    )
    await userEvent.selectOptions(screen.getByRole('combobox'), 'streak')
    expect(headings()).toEqual(['High', 'Low'])
  })

  it('wraps a long title onto two <=20 char lines', () => {
    render(
      <HabitTracker
        {...baseProps([makeHabit({ title: 'Drink water every single hour today' })])}
      />,
    )
    const h3 = screen.getByRole('heading', { level: 3 })
    const spans = h3.querySelectorAll('span')
    expect(spans.length).toBe(2)
    spans.forEach((s) => expect((s.textContent ?? '').length).toBeLessThanOrEqual(20))
  })

  it('keeps a short title on one line', () => {
    render(<HabitTracker {...baseProps([makeHabit({ title: 'Short one' })])} />)
    const h3 = screen.getByRole('heading', { level: 3 })
    expect(h3.querySelectorAll('span').length).toBe(1)
  })

  it('renders the streak with its unit', () => {
    render(<HabitTracker {...baseProps([makeHabit({ currentStreak: 4, streakUnit: 'week' })])} />)
    expect(screen.getByText(/4 week streak/i)).toBeInTheDocument()
  })

  it('the center button completes an unfinished habit', async () => {
    const props = baseProps([makeHabit({ habitId: 9, lastCompletedDate: null })])
    render(<HabitTracker {...props} />)
    await userEvent.click(screen.getByRole('button', { name: 'Complete' }))
    expect(props.onHabitCheck).toHaveBeenCalledWith(9)
  })

  it('the center button is disabled once the period is done', () => {
    render(<HabitTracker {...baseProps([makeHabit({ lastCompletedDate: todayStr() })])} />)
    expect(screen.getByRole('button', { name: /Day Logged/i })).toBeDisabled()
  })

  it('edit and delete controls call their handlers', async () => {
    const props = baseProps([makeHabit({ habitId: 3, title: 'Manage me' })])
    render(<HabitTracker {...props} />)
    await userEvent.click(screen.getByTitle('Edit Habit'))
    await userEvent.click(screen.getByTitle('Delete Habit'))
    expect(props.onEditHabitClick).toHaveBeenCalledWith(expect.objectContaining({ habitId: 3 }))
    expect(props.onDeleteHabit).toHaveBeenCalledWith(3)
  })

  it('"Add Habit" calls onAddHabitClick', async () => {
    const props = baseProps([])
    render(<HabitTracker {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /Add Habit/i }))
    expect(props.onAddHabitClick).toHaveBeenCalledTimes(1)
  })
})
