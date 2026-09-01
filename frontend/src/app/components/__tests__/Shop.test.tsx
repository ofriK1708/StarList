import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Shop } from '../Shop'

interface ShopItem {
  id: string
  name: string
  type: string
  price: number
  description: string
  unlocked: boolean
  imageUrl?: string
}

const makeItem = (over: Partial<ShopItem> = {}): ShopItem => ({
  id: '1',
  name: 'Earth',
  type: 'earth',
  price: 50,
  description: 'A blue marble',
  unlocked: false,
  ...over,
})

const headings = () =>
  screen.getAllByRole('heading', { level: 3 }).map((h) => h.textContent)

describe('Shop', () => {
  beforeEach(() => vi.clearAllMocks())

  it('defaults to the "available" filter (locked items only)', () => {
    render(
      <Shop
        items={[
          makeItem({ id: '1', name: 'MoonRock', unlocked: false }),
          makeItem({ id: '2', name: 'StarDust', unlocked: true }),
        ]}
        coinBalance={0}
        onPurchase={vi.fn()}
      />,
    )
    expect(screen.getByRole('heading', { name: 'MoonRock' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'StarDust' })).not.toBeInTheDocument()
  })

  it('"Owned" filter shows only unlocked items', async () => {
    render(
      <Shop
        items={[
          makeItem({ id: '1', name: 'MoonRock', unlocked: false }),
          makeItem({ id: '2', name: 'StarDust', unlocked: true }),
        ]}
        coinBalance={0}
        onPurchase={vi.fn()}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Owned' }))
    expect(screen.getByRole('heading', { name: 'StarDust' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'MoonRock' })).not.toBeInTheDocument()
  })

  it('sorts by price ascending by default and descending on demand', async () => {
    render(
      <Shop
        items={[
          makeItem({ id: '1', name: 'Pricey', price: 200 }),
          makeItem({ id: '2', name: 'Cheap', price: 20 }),
          makeItem({ id: '3', name: 'Mid', price: 100 }),
        ]}
        coinBalance={0}
        onPurchase={vi.fn()}
      />,
    )
    expect(headings()).toEqual(['Cheap', 'Mid', 'Pricey'])

    await userEvent.selectOptions(screen.getByRole('combobox'), 'price-desc')
    expect(headings()).toEqual(['Pricey', 'Mid', 'Cheap'])
  })

  it('sorts by name', async () => {
    render(
      <Shop
        items={[
          makeItem({ id: '1', name: 'Zeta', price: 10 }),
          makeItem({ id: '2', name: 'Alpha', price: 99 }),
        ]}
        coinBalance={0}
        onPurchase={vi.fn()}
      />,
    )
    await userEvent.selectOptions(screen.getByRole('combobox'), 'name')
    expect(headings()).toEqual(['Alpha', 'Zeta'])
  })

  it('enables "Unlock" only when the balance covers the price', () => {
    render(
      <Shop
        items={[
          makeItem({ id: '1', name: 'Affordable', price: 50 }),
          makeItem({ id: '2', name: 'TooDear', price: 500 }),
        ]}
        coinBalance={100}
        onPurchase={vi.fn()}
      />,
    )
    expect(screen.getByRole('button', { name: 'Unlock' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Not enough' })).toBeDisabled()
  })

  it('clicking "Unlock" calls onPurchase with the item id', async () => {
    const onPurchase = vi.fn()
    render(
      <Shop
        items={[makeItem({ id: 'planet-7', name: 'Buyable', price: 10 })]}
        coinBalance={100}
        onPurchase={onPurchase}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Unlock' }))
    expect(onPurchase).toHaveBeenCalledWith('planet-7')
  })

  it('shows the coin balance in the header', () => {
    render(<Shop items={[]} coinBalance={1234} onPurchase={vi.fn()} />)
    expect(screen.getByText('1234')).toBeInTheDocument()
  })

  it('marks owned items with an OWNED badge and no purchase button', async () => {
    render(
      <Shop
        items={[makeItem({ id: '1', name: 'Mine', unlocked: true })]}
        coinBalance={0}
        onPurchase={vi.fn()}
      />,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Owned' }))
    expect(screen.getByText('OWNED')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Unlock|Not enough/ })).not.toBeInTheDocument()
  })
})
