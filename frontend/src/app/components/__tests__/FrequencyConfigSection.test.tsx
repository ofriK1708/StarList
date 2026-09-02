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
    expect(screen.queryByText('Scheduled day')).not.toBeInTheDocument()
    expect(screen.queryByText(/Select days/)).not.toBeInTheDocument()
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

  it('MULTI_DAY accumulates selected days in sorted order and warns below two', async () => {
    const onValue = vi.fn()
    render(<Harness onValue={onValue} />)
    await userEvent.click(screen.getByRole('button', { name: 'Specific days' }))
    expect(screen.getByText('Please select at least 2 days.')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Sat' })) // 6
    await userEvent.click(screen.getByRole('button', { name: 'Tue' })) // 2
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({ scheduledDaysOfWeek: [2, 6] }),
    )
    expect(screen.queryByText('Please select at least 2 days.')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Tue' })) // toggle off -> [6]
    expect(onValue).toHaveBeenLastCalledWith(
      expect.objectContaining({ scheduledDaysOfWeek: [6] }),
    )
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
