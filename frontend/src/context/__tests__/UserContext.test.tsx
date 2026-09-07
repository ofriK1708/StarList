import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { UserProvider, useUser } from '../UserContext'
import { authService } from '../../services/authService'
import { usersApi } from '../../services/usersApi'
import { setAuthToken } from '../../services/api'

vi.mock('../../services/authService', () => ({
  authService: {
    getIdToken: vi.fn(),
    getCognitoSub: vi.fn(),
    getEmail: vi.fn(),
    handleSignUp: vi.fn(),
    handleConfirmSignUp: vi.fn(),
    handleSignIn: vi.fn(),
    handleSignOut: vi.fn(),
  },
}))
vi.mock('../../services/usersApi', () => ({
  usersApi: {
    getUserByCognitoSub: vi.fn(),
    createUser: vi.fn(),
  },
}))
vi.mock('../../services/api', () => ({ setAuthToken: vi.fn() }))

const mAuth = authService as unknown as Record<string, ReturnType<typeof vi.fn>>
const mUsers = usersApi as unknown as Record<string, ReturnType<typeof vi.fn>>

function Harness() {
  const { user, isLoading, login, logout, spendCoins } = useUser()
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="name">{user?.displayName ?? 'none'}</span>
      <span data-testid="coins">{user?.totalCoins ?? -1}</span>
      <button onClick={() => login('a@b.c', 'pw')}>login</button>
      <button onClick={() => logout()}>logout</button>
      <button onClick={() => spendCoins(30)}>spend30</button>
    </div>
  )
}

const renderCtx = () =>
  render(
    <UserProvider>
      <Harness />
    </UserProvider>,
  )

const makeUser = (over: Partial<Record<string, unknown>> = {}) => ({
  id: 1,
  email: 'a@b.c',
  displayName: 'Alice',
  totalCoins: 100,
  lifetimeCoinsEarned: 100,
  currentGalaxyCycle: 1,
  galaxyResetDate: '2026-01-01',
  createdAt: '2026-01-01T00:00:00Z',
  ...over,
})

describe('UserContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mAuth.getIdToken.mockResolvedValue(null)
    mAuth.getCognitoSub.mockResolvedValue(null)
    mAuth.getEmail.mockResolvedValue(null)
    mAuth.handleSignOut.mockResolvedValue(undefined)
    mAuth.handleSignIn.mockResolvedValue(undefined)
  })

  it('useUser throws when used outside a provider', () => {
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {})
    function Bare() {
      useUser()
      return null
    }
    expect(() => render(<Bare />)).toThrow(/useUser must be used within a UserProvider/)
    spy.mockRestore()
  })

  it('finishes the restore session with no user when there is no Cognito session', async () => {
    renderCtx()
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))
    expect(screen.getByTestId('name')).toHaveTextContent('none')
  })

  it('restores an existing session and fetches the backend user', async () => {
    mAuth.getIdToken.mockResolvedValue('jwt')
    mAuth.getCognitoSub.mockResolvedValue('sub-1')
    mAuth.getEmail.mockResolvedValue('a@b.c')
    mUsers.getUserByCognitoSub.mockResolvedValue(makeUser({ displayName: 'Restored' }))

    renderCtx()

    await waitFor(() => expect(screen.getByTestId('name')).toHaveTextContent('Restored'))
    expect(setAuthToken).toHaveBeenCalledWith('jwt')
  })

  it('login sets the token and loads the backend user', async () => {
    mAuth.getIdToken.mockResolvedValue(null) // restore -> no user
    renderCtx()
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))

    mAuth.getIdToken.mockResolvedValue('jwt-2')
    mAuth.getCognitoSub.mockResolvedValue('sub-2')
    mUsers.getUserByCognitoSub.mockResolvedValue(makeUser({ displayName: 'LoggedIn', totalCoins: 40 }))

    await userEvent.click(screen.getByText('login'))

    await waitFor(() => expect(screen.getByTestId('name')).toHaveTextContent('LoggedIn'))
    expect(setAuthToken).toHaveBeenLastCalledWith('jwt-2')
  })

  it('self-heals a missing backend user on login (404 -> createUser)', async () => {
    renderCtx()
    await waitFor(() => expect(screen.getByTestId('loading')).toHaveTextContent('false'))

    mAuth.getIdToken.mockResolvedValue('jwt-3')
    mAuth.getCognitoSub.mockResolvedValue('sub-3')
    mUsers.getUserByCognitoSub.mockRejectedValue({ response: { status: 404 } })
    mUsers.createUser.mockResolvedValue(makeUser({ displayName: 'Healed' }))

    await userEvent.click(screen.getByText('login'))

    await waitFor(() => expect(screen.getByTestId('name')).toHaveTextContent('Healed'))
    expect(mUsers.createUser).toHaveBeenCalledWith(
      expect.objectContaining({ cognitoUserId: 'sub-3', email: 'a@b.c' }),
    )
  })

  it('logout clears the user and the auth token', async () => {
    mAuth.getIdToken.mockResolvedValue('jwt')
    mAuth.getCognitoSub.mockResolvedValue('sub-1')
    mAuth.getEmail.mockResolvedValue('a@b.c')
    mUsers.getUserByCognitoSub.mockResolvedValue(makeUser())
    renderCtx()
    await waitFor(() => expect(screen.getByTestId('name')).toHaveTextContent('Alice'))

    await userEvent.click(screen.getByText('logout'))

    await waitFor(() => expect(screen.getByTestId('name')).toHaveTextContent('none'))
    expect(setAuthToken).toHaveBeenLastCalledWith(null)
  })

  describe('spendCoins', () => {
    beforeEach(async () => {
      mAuth.getIdToken.mockResolvedValue('jwt')
      mAuth.getCognitoSub.mockResolvedValue('sub-1')
      mAuth.getEmail.mockResolvedValue('a@b.c')
      mUsers.getUserByCognitoSub.mockResolvedValue(makeUser({ totalCoins: 50 }))
    })

    it('deducts coins when the balance is sufficient', async () => {
      renderCtx()
      await waitFor(() => expect(screen.getByTestId('coins')).toHaveTextContent('50'))
      await userEvent.click(screen.getByText('spend30'))
      await waitFor(() => expect(screen.getByTestId('coins')).toHaveTextContent('20'))
    })

    it('does not deduct when the balance is insufficient', async () => {
      mUsers.getUserByCognitoSub.mockResolvedValue(makeUser({ totalCoins: 10 }))
      renderCtx()
      await waitFor(() => expect(screen.getByTestId('coins')).toHaveTextContent('10'))
      await userEvent.click(screen.getByText('spend30'))
      // unchanged
      expect(screen.getByTestId('coins')).toHaveTextContent('10')
    })
  })
})
