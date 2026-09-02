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

const FREQUENCY_OPTIONS: { value: HabitFrequency; label: string; hint: string }[] = [
    { value: "DAILY", label: "Daily", hint: "Once every day." },
    { value: "WEEKLY", label: "Weekly", hint: "Once a week, on the day you pick. Finishing it any day that week counts." },
    { value: "CUSTOM", label: "Custom", hint: "Once per interval — every week, 2 weeks or month." },
    {
        value: "MULTI_DAY",
        label: "Specific days",
        // The distinction users miss: this is not "weekly with extra days" — every selected
        // day is its own obligation, so missing one breaks the streak.
        hint: "Every week, on each day you pick. Each day counts on its own, so missing one breaks the streak.",
    },
];

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

    const handleFrequency = (f: HabitFrequency) => {
        // Reset sub-fields that don't apply to the newly selected frequency
        set({
            frequency: f,
            scheduledDayOfWeek: f === "DAILY" || f === "MULTI_DAY" ? null : scheduledDayOfWeek,
            customIntervalDays: f === "CUSTOM" ? (customIntervalDays ?? 7) : null,
            scheduledDaysOfWeek: f === "MULTI_DAY" ? (scheduledDaysOfWeek ?? []) : null,
        });
    };

    const toggleMultiDay = (dayValue: number) => {
        const current = scheduledDaysOfWeek ?? [];
        const next = current.includes(dayValue)
            ? current.filter(d => d !== dayValue)
            : [...current, dayValue].sort((a, b) => a - b);
        set({ scheduledDaysOfWeek: next });
    };

    const chipBase = "px-3 py-1.5 rounded-lg text-sm font-medium border transition-colors cursor-pointer select-none";
    const chipActive = "bg-blue-600 border-blue-500 text-white";
    const chipInactive = "bg-slate-800 border-slate-600 text-slate-300 hover:border-slate-400";

    return (
        <div className="space-y-4">
            {/* ── Frequency type ── */}
            <div>
                <label className="block text-sm text-slate-300 mb-2">Frequency</label>
                <div className="flex gap-2 flex-wrap">
                    {FREQUENCY_OPTIONS.map(({ value: f, label }) => (
                        <button
                            key={f}
                            type="button"
                            onClick={() => handleFrequency(f)}
                            className={`flex-1 min-w-[70px] ${chipBase} ${frequency === f ? chipActive : chipInactive}`}
                        >
                            {label}
                        </button>
                    ))}
                </div>
                <p className="text-xs text-slate-400 mt-2">
                    {FREQUENCY_OPTIONS.find((o) => o.value === frequency)?.hint}
                </p>
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

            {/* ── Multi-day selection (MULTI_DAY only) ── */}
            {frequency === "MULTI_DAY" && (
                <div>
                    <label className="block text-sm text-slate-300 mb-2">
                        Select days <span className="text-slate-500 font-normal">(pick 2–6)</span>
                    </label>
                    <div className="flex gap-1.5">
                        {DAY_LABELS.map(({ value: v, label }) => {
                            const selected = (scheduledDaysOfWeek ?? []).includes(v);
                            return (
                                <button
                                    key={v}
                                    type="button"
                                    onClick={() => toggleMultiDay(v)}
                                    className={`flex-1 py-1.5 rounded-lg text-xs font-medium border transition-colors cursor-pointer relative
                                        ${selected ? chipActive : chipInactive}`}
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
                    {(scheduledDaysOfWeek ?? []).length < 2 && (
                        <p className="text-xs text-amber-400 mt-1">Please select at least 2 days.</p>
                    )}
                </div>
            )}

            {/* ── Day of week (WEEKLY and CUSTOM) ── */}
            {(frequency === "WEEKLY" || frequency === "CUSTOM") && (
                <div>
                    <label className="block text-sm text-slate-300 mb-2">
                        {frequency === "WEEKLY" ? "Scheduled day" : "Anchor day"}
                    </label>
                    <div className="flex gap-1.5">
                        {DAY_LABELS.map(({ value: v, label }) => (
                            <button
                                key={v}
                                type="button"
                                onClick={() => set({ scheduledDayOfWeek: v })}
                                className={`flex-1 py-1.5 rounded-lg text-xs font-medium border transition-colors cursor-pointer
                                    ${scheduledDayOfWeek === v ? chipActive : chipInactive}`}
                            >
                                {label}
                            </button>
                        ))}
                    </div>
                    {(frequency === "WEEKLY" || frequency === "CUSTOM") && !scheduledDayOfWeek && (
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
