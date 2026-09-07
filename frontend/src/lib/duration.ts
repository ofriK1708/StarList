/**
 * Task durations are stored on the backend as a whole number of minutes.
 * These helpers let the UI express that value in minutes, hours or days.
 */
export type DurationUnit = 'minutes' | 'hours' | 'days'

export const DURATION_UNITS: { value: DurationUnit; label: string; minutes: number }[] = [
  { value: 'minutes', label: 'Minutes', minutes: 1 },
  { value: 'hours', label: 'Hours', minutes: 60 },
  { value: 'days', label: 'Days', minutes: 60 * 24 },
]

const factor = (unit: DurationUnit): number =>
  DURATION_UNITS.find((u) => u.value === unit)!.minutes

/** Converts a value + unit into a whole number of minutes. */
export function toMinutes(value: number, unit: DurationUnit): number {
  if (!Number.isFinite(value) || value <= 0) return 0
  return Math.round(value * factor(unit))
}

/**
 * Converts a stored minute count into the largest unit that divides it evenly,
 * so 120 -> {2, hours} and 2880 -> {2, days} while 90 stays {90, minutes}.
 */
export function splitMinutes(minutes: number): { value: number; unit: DurationUnit } {
  if (!Number.isFinite(minutes) || minutes <= 0) return { value: 0, unit: 'minutes' }
  if (minutes % (60 * 24) === 0) return { value: minutes / (60 * 24), unit: 'days' }
  if (minutes % 60 === 0) return { value: minutes / 60, unit: 'hours' }
  return { value: minutes, unit: 'minutes' }
}

/** Short human label for a stored minute count, in the unit it reads best as ("2 hr", "90 min", "3 days"). */
export function formatDuration(minutes: number | null | undefined): string {
  const { value, unit } = splitMinutes(minutes ?? 0)
  const label = unit === 'minutes' ? 'min' : unit === 'hours' ? 'hr' : value === 1 ? 'day' : 'days'
  return `${value} ${label}`
}
