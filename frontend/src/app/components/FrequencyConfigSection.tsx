import { HabitFrequency, ScheduledTimeType } from "@/services/habitsApi.ts";
import { Check } from "lucide-react";

const DAY_LABELS: { value: number; label: string }[] = [
    { value: 1, label: "Mon" },
    { value: 2, label: "Tue" },
    { value: 3, label: "Wed" },
    { value: 4, label: "Thu" },
    { value: 5, label: "Fri" },
    { value: 6, label: "Sat" },
    { value: 7, label: "Sun" },
];

const INTERVAL_OPTIONS: { value: 7 | 14 | 30; label: string }[] = [
    { value: 7,  label: "Every week" },
    { value: 14, label: "Every 2 weeks" },
    { value: 30, label: "Every month" },
];

/** The chip the user picks. MULTI_DAY is not offered directly — see {@link applyDays}. */
type FrequencyChip = "DAILY" | "WEEKLY" | "CUSTOM";

const FREQUENCY_CHIPS: { value: FrequencyChip; label: string }[] = [
    { value: "DAILY",  label: "Daily" },
    { value: "WEEKLY", label: "Weekly" },
    { value: "CUSTOM", label: "Custom" },
];

/** Seven selected days is just DAILY, so the weekly picker stops at six. */
const MAX_WEEKLY_DAYS = 6;

const TIME_OPTIONS: { value: ScheduledTimeType; label: string }[] = [
    { value: "MORNING",   label: "Morning" },
    { value: "AFTERNOON", label: "Afternoon" },
    { value: "EVENING",   label: "Evening" },
    { value: "CUSTOM",    label: "Custom hour" },
];

export interface FrequencyConfig {
    frequency: HabitFrequency;
    scheduledDayOfWeek: number | null;
    scheduledTimeType: ScheduledTimeType | null;
    scheduledHour: number | null;
    customIntervalDays: 7 | 14 | 30 | null;
    /** ISO days of week (1=Mon…7=Sun). Required for MULTI_DAY; null otherwise. */
    scheduledDaysOfWeek: number[] | null;
}

interface Props {
    value: FrequencyConfig;
    onChange: (next: FrequencyConfig) => void;
}

/** Shared frequency-config section used by AddHabitModal and EditHabitModal. */
export function FrequencyConfigSection({ value, onChange }: Props) {
    const { frequency, scheduledDayOfWeek, scheduledTimeType, scheduledHour, customIntervalDays, scheduledDaysOfWeek } = value;

    const set = (patch: Partial<FrequencyConfig>) => onChange({ ...value, ...patch });

    // WEEKLY and MULTI_DAY are one control to the user; only the day count tells them apart.
    const isWeekly = frequency === "WEEKLY" || frequency === "MULTI_DAY";
    const activeChip: FrequencyChip = frequency === "MULTI_DAY" ? "WEEKLY" : (frequency as FrequencyChip);

    /** Days currently ticked, read back from whichever shape the habit is stored in. */
    const selectedDays: number[] =
        frequency === "MULTI_DAY" ? (scheduledDaysOfWeek ?? [])
            : frequency === "WEEKLY" && scheduledDayOfWeek ? [scheduledDayOfWeek]
                : [];

    /**
     * One control, two backend shapes: a single day is a WEEKLY habit, several days is MULTI_DAY.
     * Streak rules are identical either way — miss the day and the streak resets — so the split is
     * invisible to the user. The only difference is that a WEEKLY habit can still be logged later
     * in the same week for partial credit.
     */
    const applyDays = (days: number[]) => {
        if (days.length > 1) {
            set({
                frequency: "MULTI_DAY",
                scheduledDayOfWeek: null,
                scheduledDaysOfWeek: days,
                customIntervalDays: null,
            });
        } else {
            set({
                frequency: "WEEKLY",
                scheduledDayOfWeek: days[0] ?? null,
                scheduledDaysOfWeek: null,
                customIntervalDays: null,
            });
        }
    };

    const toggleWeekDay = (dayValue: number) => {
        const next = selectedDays.includes(dayValue)
            ? selectedDays.filter(d => d !== dayValue)
            : [...selectedDays, dayValue].sort((a, b) => a - b);
        if (next.length > MAX_WEEKLY_DAYS) return;
        applyDays(next);
    };

    const handleFrequency = (f: FrequencyChip) => {
        if (f === "DAILY") {
            set({ frequency: "DAILY", scheduledDayOfWeek: null, scheduledDaysOfWeek: null, customIntervalDays: null });
        } else if (f === "CUSTOM") {
            set({ frequency: "CUSTOM", scheduledDaysOfWeek: null, customIntervalDays: customIntervalDays ?? 7 });
        } else {
            applyDays(selectedDays); // keep any days already ticked
        }
    };

    const hint =
        activeChip === "DAILY" ? "Once every day."
            : activeChip === "CUSTOM" ? "Once per interval — every week, 2 weeks or month."
                : selectedDays.length > 1
                    ? "On each day you pick. Each day counts on its own, so missing one breaks the streak."
                    : "Once a week on the day you pick. Doing it later that week still counts, but resets the streak.";

    const chipBase = "px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors cursor-pointer select-none";
    const chipActive = "bg-blue-600 border-blue-500 text-white";
    const chipInactive = "bg-slate-800 border-slate-600 text-slate-300 hover:border-slate-400";
    const dayBase = "flex-1 py-1.5 rounded-lg text-xs font-medium border transition-colors cursor-pointer";

    return (
        <div className="space-y-4">
            {/* ── Frequency type ── */}
            <div>
                <label className="block text-sm text-slate-300 mb-2">Frequency</label>
                <div className="flex gap-2 flex-wrap">
                    {FREQUENCY_CHIPS.map(({ value: f, label }) => (
                        <button
                            key={f}
                            type="button"
                            onClick={() => handleFrequency(f)}
                            className={`flex-1 min-w-[70px] ${chipBase} ${activeChip === f ? chipActive : chipInactive}`}
                        >
                            {label}
                        </button>
                    ))}
                </div>
                <p className="text-xs text-slate-400 mt-2">{hint}</p>
            </div>

            {/* ── Custom interval (CUSTOM only) ── */}
            {frequency === "CUSTOM" && (
                <div>
                    <label className="block text-sm text-slate-300 mb-2">Repeat interval</label>
                    <div className="flex gap-2">
                        {INTERVAL_OPTIONS.map(({ value: v, label }) => (
                            <button
                                key={v}
                                type="button"
                                onClick={() => set({ customIntervalDays: v })}
                                className={`flex-1 ${chipBase} ${customIntervalDays === v ? chipActive : chipInactive}`}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {/* ── Weekly day picker (WEEKLY and MULTI_DAY) ── */}
            {isWeekly && (
                <div>
                    <label className="block text-sm text-slate-300 mb-2">
                        Scheduled days <span className="text-slate-500 font-normal">(pick 1–6)</span>
                    </label>
                    <div className="flex gap-1.5">
                        {DAY_LABELS.map(({ value: v, label }) => {
                            const selected = selectedDays.includes(v);
                            return (
                                <button
                                    key={v}
                                    type="button"
                                    onClick={() => toggleWeekDay(v)}
                                    className={`${dayBase} relative ${selected ? chipActive : chipInactive}`}
                                >
                                    {label}
                                    {selected && (
                                        <span className="absolute -top-1 -right-1 w-3 h-3 bg-green-400 rounded-full flex items-center justify-center">
                                            <Check className="w-2 h-2 text-white stroke-[3]" />
                                        </span>
                                    )}
                                </button>
                            );
                        })}
                    </div>
                    {selectedDays.length === 0 && (
                        <p className="text-xs text-amber-400 mt-1">Please select a day.</p>
                    )}
                </div>
            )}

            {/* ── Anchor day (CUSTOM only) ── */}
            {frequency === "CUSTOM" && (
                <div>
                    <label className="block text-sm text-slate-300 mb-2">Anchor day</label>
                    <div className="flex gap-1.5">
                        {DAY_LABELS.map(({ value: v, label }) => (
                            <button
                                key={v}
                                type="button"
                                onClick={() => set({ scheduledDayOfWeek: v })}
                                className={`${dayBase} ${scheduledDayOfWeek === v ? chipActive : chipInactive}`}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                    {!scheduledDayOfWeek && (
                        <p className="text-xs text-amber-400 mt-1">Please select a day.</p>
                    )}
                </div>
            )}

            {/* ── Time of day (all frequencies) ── */}
            <div>
                <label className="block text-sm text-slate-300 mb-2">
                    Time of day <span className="text-slate-500 font-normal">(optional)</span>
                </label>
                <div className="flex gap-2">
                    {TIME_OPTIONS.map(({ value: v, label }) => (
                        <button
                            key={v}
                            type="button"
                            onClick={() => set({
                                scheduledTimeType: scheduledTimeType === v ? null : v,
                                scheduledHour: scheduledTimeType === v || v !== "CUSTOM" ? null : scheduledHour,
                            })}
                            className={`flex-1 ${chipBase} ${scheduledTimeType === v ? chipActive : chipInactive}`}
                        >
                            {label}
                        </button>
                    ))}
                </div>

                {scheduledTimeType === "CUSTOM" && (
                    <div className="mt-2 flex items-center gap-2">
                        <label className="text-sm text-slate-400 whitespace-nowrap">Hour (0–23):</label>
                        <input
                            type="number"
                            min={0}
                            max={23}
                            value={scheduledHour ?? ""}
                            onChange={(e) => set({ scheduledHour: e.target.value === "" ? null : Number(e.target.value) })}
                            placeholder="e.g. 8"
                            className="w-24 bg-slate-800 border border-slate-600 rounded-lg px-3 py-1.5 text-white text-sm focus:outline-none focus:border-blue-500 transition-colors"
                        />
                    </div>
                )}
            </div>
        </div>
    );
}
