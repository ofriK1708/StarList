import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { NavigationBar } from '../NavigationBar'

describe('NavigationBar', () => {
  beforeEach(() => vi.clearAllMocks())

  it('renders every destination', () => {
    render(<NavigationBar currentScreen="tasks" onNavigate={vi.fn()} />)
    for (const label of ['Tasks', 'Habits', 'Chat', 'Galaxy', 'Shop', 'Stats', 'Profile']) {
      expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
    }
  })

  it('calls onNavigate with the destination id', async () => {
    const onNavigate = vi.fn()
    render(<NavigationBar currentScreen="tasks" onNavigate={onNavigate} />)
    await userEvent.click(screen.getByRole('button', { name: 'Habits' }))
    expect(onNavigate).toHaveBeenCalledWith('habits')
    await userEvent.click(screen.getByRole('button', { name: 'Shop' }))
    expect(onNavigate).toHaveBeenCalledWith('shop')
  })

  it('highlights the active destination', () => {
    render(<NavigationBar currentScreen="profile" onNavigate={vi.fn()} />)
    expect(screen.getByRole('button', { name: 'Profile' }).className).toMatch(/text-blue-400/)
    expect(screen.getByRole('button', { name: 'Tasks' }).className).not.toMatch(/text-blue-400/)
  })
})
