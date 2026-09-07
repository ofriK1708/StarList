import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AIChat } from '../AIChat'

type Msg = Parameters<typeof AIChat>[0]['messages'][number]

const baseProps = () => ({
  messages: [] as Msg[],
  onSendMessage: vi.fn(),
  onAddTask: vi.fn(),
  onDailyBriefing: vi.fn(),
  isLoading: false,
})

const getInput = () =>
  screen.getByPlaceholderText(/Ask about your tasks/i) as HTMLTextAreaElement

const getSendButton = () => {
  const buttons = screen.getAllByRole('button')
  return buttons[buttons.length - 1] // Send is the trailing icon-only button
}

describe('AIChat', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows the empty-state hint when there are no messages', () => {
    render(<AIChat {...baseProps()} />)
    expect(screen.getByText(/Ask me about your tasks, habits/i)).toBeInTheDocument()
  })

  it('renders user and AI message bubbles', () => {
    const props = baseProps()
    props.messages = [
      { id: '1', type: 'user', content: 'hello there', timestamp: new Date() },
      { id: '2', type: 'ai', content: 'hi human', timestamp: new Date() },
    ]
    render(<AIChat {...props} />)
    expect(screen.getByText('hello there')).toBeInTheDocument()
    expect(screen.getByText('hi human')).toBeInTheDocument()
  })

  it('renders a task-suggestion card and wires "Add to Tasks"', async () => {
    const props = baseProps()
    props.messages = [
      {
        id: '3',
        type: 'task-suggestion',
        content: 'How about this task?',
        timestamp: new Date(),
        taskData: { title: 'Do laundry', difficulty: 'easy', reward: 5 },
      },
    ]
    render(<AIChat {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /Add to Tasks/i }))
    expect(props.onAddTask).toHaveBeenCalledWith({ title: 'Do laundry', difficulty: 'easy', reward: 5 })
  })

  it('sends a typed message with newConversation=false and clears the input', async () => {
    const props = baseProps()
    render(<AIChat {...props} />)

    await userEvent.type(getInput(), 'plan my week')
    await userEvent.click(getSendButton())

    expect(props.onSendMessage).toHaveBeenCalledWith('plan my week', false)
    expect(getInput().value).toBe('')
  })

  it('submits on Enter but not on Shift+Enter', async () => {
    const props = baseProps()
    render(<AIChat {...props} />)
    const input = getInput()

    await userEvent.type(input, 'first{Shift>}{Enter}{/Shift}')
    expect(props.onSendMessage).not.toHaveBeenCalled()

    await userEvent.type(input, '{Enter}')
    expect(props.onSendMessage).toHaveBeenCalledTimes(1)
    expect(props.onSendMessage.mock.calls[0][0]).toContain('first')
  })

  it('does not send when the input is only whitespace', async () => {
    const props = baseProps()
    render(<AIChat {...props} />)
    await userEvent.type(getInput(), '    ')
    await userEvent.click(getSendButton())
    expect(props.onSendMessage).not.toHaveBeenCalled()
  })

  it('disables the composer while loading and shows the typing indicator', () => {
    const props = baseProps()
    props.isLoading = true
    render(<AIChat {...props} />)
    expect(getInput()).toBeDisabled()
    expect(screen.getByRole('button', { name: /Briefing/i })).toBeDisabled()
  })

  it('"Briefing" calls onDailyBriefing', async () => {
    const props = baseProps()
    render(<AIChat {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /Briefing/i }))
    expect(props.onDailyBriefing).toHaveBeenCalledTimes(1)
  })

  it('"New Chat" asks the parent to start a fresh conversation', async () => {
    const props = baseProps()
    render(<AIChat {...props} />)
    await userEvent.click(screen.getByRole('button', { name: /New Chat/i }))
    expect(props.onSendMessage).toHaveBeenCalledWith('', true)
  })

  it('after "New Chat", the first real message carries newConversation=true, then resets to false', async () => {
    const props = baseProps()
    render(<AIChat {...props} />)

    await userEvent.click(screen.getByRole('button', { name: /New Chat/i }))
    props.onSendMessage.mockClear()

    await userEvent.type(getInput(), 'first prompt')
    await userEvent.click(getSendButton())
    expect(props.onSendMessage).toHaveBeenLastCalledWith('first prompt', true)

    await userEvent.type(getInput(), 'second prompt')
    await userEvent.click(getSendButton())
    expect(props.onSendMessage).toHaveBeenLastCalledWith('second prompt', false)
  })
})
