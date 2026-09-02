import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { FrequencyConfigSection, type FrequencyConfig } from '../FrequencyConfigSection'

const DEFAULT: FrequencyConfig = {
  frequency: 'DAILY',
  scheduledDayOfWeek: null,
  scheduledTimeType: null,
  scheduledHour: null,
  customIntervalDays: null,
  scheduledDaysOfWeek: null,
}

function Harness({ initial = DEFAULT, onValue }: { initial?: FrequencyConfig; onValue?: (v: FrequencyConfig) => void }) {
  const [value, setValue] = useState<FrequencyConfig>(initial)
  return (
    <FrequencyConfigSection
      value={value}
      onChange={(next) => {
        setValue(next)
        onValue?.(next)
      }}
    />
  )
}

describe('FrequencyConfigSection', () => {
  beforeEach(() => vi.clearAllMocks())

  it('DAILY hides interval, day-of-week and multi-day pickers', () => {
    render(<Harness />)
    expect(screen.queryByText('Repeat interval')).not.toBeInTheDocument()
    expect(screen.queryByText('Anchor day')).not.toBeInTheDocument()
    expect(screen.queryByText(/Scheduled days/)).not.toBeInTheDocument()
  })

  it('switching to CUSTOM reveals the interval options and defaults to weekly', async () => {
    const onValue = vi.fn()
    render(<Harness onValue={onValue} />)
    await userEvent.click(screen.getByRole('button', { name: 'Custom' }))
    expect(screen.getByText('Repeat interval')).toBeInTheDocument()
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({ frequency: 'CUSTOM', customIntervalDays: 7 }),
    )
  })

  it('WEEKLY requires a day and reports the chosen ISO day', async () => {
    const onValue = vi.fn()
    render(<Harness onValue={onValue} />)
    await userEvent.click(screen.getByRole('button', { name: 'Weekly' }))
    expect(screen.getByText('Please select a day.')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Fri' }))
    expect(onValue).toHaveBeenLastCalledWith(expect.objectContaining({ scheduledDayOfWeek: 5 }))
  })

  it('picking a second day under Weekly switches the habit to MULTI_DAY', async () => {
    const onValue = vi.fn()
    render(<Harness onValue={onValue} />)
    await userEvent.click(screen.getByRole('button', { name: 'Weekly' }))
    expect(screen.getByText('Please select a day.')).toBeInTheDocument()

    // One day is an ordinary WEEKLY habit.
    await userEvent.click(screen.getByRole('button', { name: 'Sat' })) // 6
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({ frequency: 'WEEKLY', scheduledDayOfWeek: 6, scheduledDaysOfWeek: null }),
    )
    expect(screen.queryByText('Please select a day.')).not.toBeInTheDocument()

    // A second day promotes it to MULTI_DAY, sorted.
    await userEvent.click(screen.getByRole('button', { name: 'Tue' })) // 2
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({
        frequency: 'MULTI_DAY',
        scheduledDaysOfWeek: [2, 6],
        scheduledDayOfWeek: null,
      }),
    )

    // Dropping back to one day demotes it to WEEKLY again.
    await userEvent.click(screen.getByRole('button', { name: 'Tue' }))
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({ frequency: 'WEEKLY', scheduledDayOfWeek: 6, scheduledDaysOfWeek: null }),
    )
  })

  it('an existing MULTI_DAY habit opens with its days already ticked', async () => {
    const onValue = vi.fn()
    render(
      <Harness
        onValue={onValue}
        initial={{ ...DEFAULT, frequency: 'MULTI_DAY', scheduledDaysOfWeek: [1, 4] }}
      />,
    )
    // The Weekly chip represents both shapes, so it must read as selected.
    expect(screen.getByRole('button', { name: 'Weekly' })).toHaveClass('bg-blue-600')
    expect(screen.getByRole('button', { name: 'Mon' })).toHaveClass('bg-blue-600')
    expect(screen.getByRole('button', { name: 'Thu' })).toHaveClass('bg-blue-600')
    expect(screen.getByRole('button', { name: 'Tue' })).not.toHaveClass('bg-blue-600')
  })

  it('"Custom hour" time option toggles the hour input and clears on re-click', async () => {
    const onValue = vi.fn()
    render(<Harness onValue={onValue} />)
    await userEvent.click(screen.getByRole('button', { name: 'Custom hour' }))
    expect(screen.getByPlaceholderText('e.g. 8')).toBeInTheDocument()

    await userEvent.type(screen.getByPlaceholderText('e.g. 8'), '9')
    expect(onValue).toHaveBeenLastCalledWith(expect.objectContaining({ scheduledHour: 9 }))

    await userEvent.click(screen.getByRole('button', { name: 'Custom hour' }))
    expect(screen.queryByPlaceholderText('e.g. 8')).not.toBeInTheDocument()
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({ scheduledTimeType: null, scheduledHour: null }),
    )
  })
})
