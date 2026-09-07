import { describe, it, expect } from 'vitest'
import { toMinutes, splitMinutes, formatDuration, DURATION_UNITS } from '../duration'

describe('toMinutes', () => {
  it('keeps minutes as-is', () => {
    expect(toMinutes(45, 'minutes')).toBe(45)
  })

  it('multiplies hours by 60', () => {
    expect(toMinutes(2, 'hours')).toBe(120)
    expect(toMinutes(1.5, 'hours')).toBe(90)
  })

  it('multiplies days by 1440', () => {
    expect(toMinutes(3, 'days')).toBe(3 * 24 * 60)
  })

  it('rounds fractional results to whole minutes', () => {
    expect(toMinutes(0.001, 'hours')).toBe(0)
    expect(toMinutes(1.019, 'hours')).toBe(61)
  })

  it('clamps non-positive / non-finite input to 0', () => {
    expect(toMinutes(0, 'hours')).toBe(0)
    expect(toMinutes(-5, 'minutes')).toBe(0)
    expect(toMinutes(NaN, 'days')).toBe(0)
  })
})

describe('splitMinutes', () => {
  it('leaves an awkward minute count in minutes', () => {
    expect(splitMinutes(90)).toEqual({ value: 90, unit: 'minutes' })
    expect(splitMinutes(25)).toEqual({ value: 25, unit: 'minutes' })
  })

  it('promotes a round-hour count to hours', () => {
    expect(splitMinutes(120)).toEqual({ value: 2, unit: 'hours' })
  })

  it('promotes a round-day count to days', () => {
    expect(splitMinutes(2880)).toEqual({ value: 2, unit: 'days' })
  })

  it('treats zero / negative as zero minutes', () => {
    expect(splitMinutes(0)).toEqual({ value: 0, unit: 'minutes' })
    expect(splitMinutes(-10)).toEqual({ value: 0, unit: 'minutes' })
  })

  it('round-trips through toMinutes', () => {
    for (const mins of [15, 30, 45, 60, 90, 120, 1440, 2880, 4320]) {
      const { value, unit } = splitMinutes(mins)
      expect(toMinutes(value, unit)).toBe(mins)
    }
  })
})

describe('formatDuration', () => {
  it('shows an even hour count in hours', () => {
    expect(formatDuration(120)).toBe('2 hr')
    expect(formatDuration(60)).toBe('1 hr')
  })

  it('shows an even day count in days and pluralizes', () => {
    expect(formatDuration(1440)).toBe('1 day')
    expect(formatDuration(2880)).toBe('2 days')
  })

  it('falls back to minutes for awkward values', () => {
    expect(formatDuration(45)).toBe('45 min')
    expect(formatDuration(90)).toBe('90 min')
  })

  it('handles null / zero gracefully', () => {
    expect(formatDuration(null)).toBe('0 min')
    expect(formatDuration(0)).toBe('0 min')
  })
})

describe('DURATION_UNITS', () => {
  it('exposes minutes, hours and days with the right factors', () => {
    expect(DURATION_UNITS.map((u) => [u.value, u.minutes])).toEqual([
      ['minutes', 1],
      ['hours', 60],
      ['days', 1440],
    ])
  })
})
