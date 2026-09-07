import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Profile } from '../Profile'

const logout = vi.fn()
vi.mock('../../../context/UserContext', () => ({
  useUser: () => ({ logout }),
}))

const baseUser = {
  name: 'Nova Explorer',
  email: 'nova@galaxy.dev',
  achievements: [
    { id: 'a1', name: 'First Light', icon: '⭐', description: 'Complete your first task', unlocked: true },
    { id: 'a2', name: 'Streak Master', icon: '🔥', description: 'Keep a 7-day streak', unlocked: false },
  ],
}

describe('Profile', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows the user name, email and coin balance', () => {
    render(<Profile user={baseUser} coinBalance={777} />)
    expect(screen.getByRole('heading', { name: 'Nova Explorer' })).toBeInTheDocument()
    expect(screen.getByText('nova@galaxy.dev')).toBeInTheDocument()
    expect(screen.getByText('777')).toBeInTheDocument()
  })

  it('lists achievements with their descriptions', () => {
    render(<Profile user={baseUser} coinBalance={0} />)
    expect(screen.getByText('First Light')).toBeInTheDocument()
    expect(screen.getByText('Complete your first task')).toBeInTheDocument()
    expect(screen.getByText('Streak Master')).toBeInTheDocument()
  })

  it('no longer renders an Edit button', () => {
    render(<Profile user={baseUser} coinBalance={0} />)
    expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument()
  })

  it('Logout triggers the context logout', async () => {
    render(<Profile user={baseUser} coinBalance={0} />)
    await userEvent.click(screen.getByRole('button', { name: /logout/i }))
    expect(logout).toHaveBeenCalledTimes(1)
  })
})
