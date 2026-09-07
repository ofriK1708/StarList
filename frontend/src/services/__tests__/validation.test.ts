import { describe, it, expect } from 'vitest'
import { TITLE_MAX_LENGTH, DESCRIPTION_MAX_LENGTH } from '../validation'

describe('validation limits', () => {
  it('title limit is a positive integer', () => {
    expect(Number.isInteger(TITLE_MAX_LENGTH)).toBe(true)
    expect(TITLE_MAX_LENGTH).toBeGreaterThan(0)
  })

  it('description limit is larger than the title limit', () => {
    expect(DESCRIPTION_MAX_LENGTH).toBeGreaterThan(TITLE_MAX_LENGTH)
  })

  it('stays within the backend column sizes (title <= 255, description <= 2000)', () => {
    expect(TITLE_MAX_LENGTH).toBeLessThanOrEqual(255)
    expect(DESCRIPTION_MAX_LENGTH).toBeLessThanOrEqual(2000)
  })
})
