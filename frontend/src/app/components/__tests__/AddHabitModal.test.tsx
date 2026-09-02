import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AddHabitModal } from '../AddHabitModal'
import { TITLE_MAX_LENGTH } from '@/services/validation.ts'

const props = () => ({ isOpen: true, onClose: vi.fn(), onAdd: vi.fn() })
const titleInput = () => screen.getByPlaceholderText('e.g., Morning run') as HTMLInputElement
const submit = () => screen.getByRole('button', { name: /Add Habit/i })

describe('AddHabitModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders nothing when closed', () => {
    const { container } = render(<AddHabitModal {...props()} isOpen={false} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('limits the title length and shows a counter', async () => {
    render(<AddHabitModal {...props()} />)
    expect(titleInput()).toHaveAttribute('maxlength', String(TITLE_MAX_LENGTH))
    await userEvent.type(titleInput(), 'abc')
    expect(screen.getByText(`3/${TITLE_MAX_LENGTH}`)).toBeInTheDocument()
  })

  it('keeps submit disabled until a title is entered (DAILY is otherwise valid)', async () => {
    render(<AddHabitModal {...props()} />)
    expect(submit()).toBeDisabled()
    await userEvent.type(titleInput(), 'Morning run')
    expect(submit()).toBeEnabled()
  })

  it('submits a DAILY habit payload', async () => {
    const p = props()
    render(<AddHabitModal {...p} />)
    await userEvent.type(titleInput(), 'Morning run')
    await userEvent.click(submit())
    expect(p.onAdd).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Morning run', frequency: 'DAILY', difficultyLevel: 'MEDIUM' }),
    )
  })

  it('requires a scheduled day for a WEEKLY habit', async () => {
    const p = props()
    render(<AddHabitModal {...p} />)
    await userEvent.type(titleInput(), 'Weekly review')
    await userEvent.click(screen.getByRole('button', { name: 'Weekly' }))
    expect(submit()).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Wed' }))
    expect(submit()).toBeEnabled()

    await userEvent.click(submit())
    expect(p.onAdd).toHaveBeenCalledWith(
      expect.objectContaining({ frequency: 'WEEKLY', scheduledDayOfWeek: 3 }),
    )
  })

  it('picking two days under Weekly submits a MULTI_DAY habit', async () => {
    const p = props()
    render(<AddHabitModal {...p} />)
    await userEvent.type(titleInput(), 'Gym days')
    await userEvent.click(screen.getByRole('button', { name: 'Weekly' }))
    expect(submit()).toBeDisabled() // no day picked yet

    // One day is already a valid WEEKLY habit.
    await userEvent.click(screen.getByRole('button', { name: 'Mon' }))
    expect(submit()).toBeEnabled()
    await userEvent.click(screen.getByRole('button', { name: 'Thu' }))
    expect(submit()).toBeEnabled()

    await userEvent.click(submit())
    expect(p.onAdd).toHaveBeenCalledWith(
      expect.objectContaining({ frequency: 'MULTI_DAY', scheduledDaysOfWeek: [1, 4] }),
    )
  })

  it('Cancel closes the modal', async () => {
    const p = props()
    render(<AddHabitModal {...p} />)
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(p.onClose).toHaveBeenCalledTimes(1)
  })
})
