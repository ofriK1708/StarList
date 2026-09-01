import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { EditHabitModal } from '../EditHabitModal'
import type { HabitResponse } from '@/services/habitsApi.ts'

const habit = (over: Partial<HabitResponse> = {}): HabitResponse => ({
  habitId: 21,
  title: 'Existing habit',
  description: null,
  frequency: 'WEEKLY',
  difficultyLevel: 'HARD',
  coinReward: 10,
  coinPenalty: null,
  currentStreak: 0,
  bestStreak: 0,
  totalCompletions: 0,
  lastCompletedDate: null,
  createdAt: '2026-01-01T00:00:00Z',
  isActive: true,
  scheduledDayOfWeek: 2,
  scheduledTimeType: null,
  scheduledHour: null,
  customIntervalDays: null,
  scheduledDaysOfWeek: null,
  streakUnit: 'week',
  ...over,
})

const props = (over = {}) => ({
  isOpen: true,
  onClose: vi.fn(),
  habitToEdit: habit(),
  onUpdate: vi.fn(),
  ...over,
})

const titleInput = () => screen.getByDisplayValue('Existing habit') as HTMLInputElement

describe('EditHabitModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders nothing when closed or without a habit', () => {
    const { container: c1 } = render(<EditHabitModal {...props()} isOpen={false} />)
    expect(c1).toBeEmptyDOMElement()
    const { container: c2 } = render(<EditHabitModal {...props()} habitToEdit={null} />)
    expect(c2).toBeEmptyDOMElement()
  })

  it('prefills title and difficulty from the habit', () => {
    render(<EditHabitModal {...props()} />)
    expect(titleInput().value).toBe('Existing habit')
    expect((screen.getByRole('combobox') as HTMLSelectElement).value).toBe('HARD')
  })

  it('is immediately valid for a habit that already has its schedule set', () => {
    render(<EditHabitModal {...props()} />)
    expect(screen.getByRole('button', { name: /Save Changes/i })).toBeEnabled()
  })

  it('calls onUpdate with the habit id and edited fields', async () => {
    const p = props()
    render(<EditHabitModal {...p} />)

    const input = titleInput()
    await userEvent.clear(input)
    await userEvent.type(input, 'Renamed habit')
    await userEvent.click(screen.getByRole('button', { name: /Save Changes/i }))

    expect(p.onUpdate).toHaveBeenCalledTimes(1)
    expect(p.onUpdate.mock.calls[0][0]).toBe(21)
    expect(p.onUpdate.mock.calls[0][1]).toMatchObject({
      title: 'Renamed habit',
      frequency: 'WEEKLY',
      difficultyLevel: 'HARD',
      scheduledDayOfWeek: 2,
    })
  })

  it('blocks saving when the title is cleared', async () => {
    const p = props()
    render(<EditHabitModal {...p} />)
    await userEvent.clear(titleInput())
    expect(screen.getByRole('button', { name: /Save Changes/i })).toBeDisabled()
  })
})
