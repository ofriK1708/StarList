import { Check, Flame, Plus, Trash2, Edit2, ArrowUpDown } from "lucide-react";
import { useState } from "react";
import { Button } from "./ui/button";
import { HabitResponse } from "@/services/habitsApi.ts";

interface HabitTrackerProps {
    habits: HabitResponse[];
    onHabitCheck: (id: number) => void;
    onAddHabitClick: () => void;
    onEditHabitClick: (habit: HabitResponse) => void;
    onDeleteHabit: (id: number) => void;
}

type HabitSort = 'date' | 'name' | 'difficulty' | 'streak';
type HabitFilter = 'all' | 'active' | 'completed';

const DIFFICULTY_WEIGHT: Record<HabitResponse["difficultyLevel"], number> = { HARD: 3, MEDIUM: 2, EASY: 1 };

export function HabitTracker({ habits, onHabitCheck, onAddHabitClick, onEditHabitClick, onDeleteHabit }: HabitTrackerProps) {
    const [sortBy, setSortBy] = useState<HabitSort>('date');
    const [filter, setFilter] = useState<HabitFilter>('all');

    const sortedHabits = [...habits]
        .filter((h) => {
            if (filter === 'active') return !isCurrentPeriodDone(h);
            if (filter === 'completed') return isCurrentPeriodDone(h);
            return true;
        })
        .sort((a, b) => {
            switch (sortBy) {
                case 'name':
                    return a.title.localeCompare(b.title);
                case 'difficulty':
                    return DIFFICULTY_WEIGHT[b.difficultyLevel] - DIFFICULTY_WEIGHT[a.difficultyLevel];
                case 'streak':
                    return b.currentStreak - a.currentStreak;
                case 'date':
                default:
                    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
            }
        });

    return (
        <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 overflow-y-auto scrollbar-hide">
            {/* Header */}
            <div className="sticky top-0 z-30 bg-slate-900/95 backdrop-blur-sm border-b border-slate-700/50 px-6 py-4">
                <div className="flex items-center justify-between mb-4">
                    <div>
                        <h1 className="text-2xl text-white/90">Habit Tracker</h1>
                        <p className="text-sm text-slate-400">Maintain consistency to power the galaxy's core</p>
                    </div>
                    <div className="flex gap-3">
                        <Button
                            onClick={onAddHabitClick}
                            className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2"
                        >
                            <Plus className="w-4 h-4" />
                            Add Habit
                        </Button>
                    </div>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-4">
                    <div className="flex gap-2">
                        {(['all', 'active', 'completed'] as const).map((f) => (
                            <button
                                key={f}
                                onClick={() => setFilter(f)}
                                className={`px-4 py-2 rounded-lg text-sm transition-colors ${
                                    filter === f
                                        ? 'bg-slate-700 text-white'
                                        : 'bg-slate-800/50 text-slate-400 hover:bg-slate-700/50 hover:text-slate-300'
                                }`}
                            >
                                {f.charAt(0).toUpperCase() + f.slice(1)}
                            </button>
                        ))}
                    </div>

                    <div className="flex items-center gap-2 bg-slate-800/50 px-3 py-1.5 rounded-lg border border-slate-700/50">
                        <ArrowUpDown className="w-4 h-4 text-slate-400" />
                        <select
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value as HabitSort)}
                            className="bg-transparent text-sm text-slate-300 focus:outline-none cursor-pointer appearance-none pr-4"
                        >
                            <option value="date" className="bg-slate-800">Sort by Newest</option>
                            <option value="name" className="bg-slate-800">Sort by Name</option>
                            <option value="difficulty" className="bg-slate-800">Sort by Difficulty</option>
                            <option value="streak" className="bg-slate-800">Sort by Streak</option>
                        </select>
                    </div>
                </div>
            </div>

            {/* Habit Grid */}
            <div className="p-6 pb-24">
                {sortedHabits.length === 0 ? (
                    <div className="text-center py-12 text-slate-400">
                        <Flame className="w-12 h-12 mx-auto mb-3 opacity-50" />
                        <p>{habits.length === 0 ? 'No habits yet' : 'No habits match this filter'}</p>
                    </div>
                ) : (
                    <div className="grid grid-cols-[repeat(auto-fill,320px)] justify-center gap-8">
                        {sortedHabits.map((habit, index) => (
                            <HabitRadialCard
                                key={habit.habitId}
                                habit={habit}
                                onCheck={() => onHabitCheck(habit.habitId)}
                                onEdit={() => onEditHabitClick(habit)}
                                onDelete={() => onDeleteHabit(habit.habitId)}
                                index={index}
                            />
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

// ── Helpers ────────────────────────────────────────────────────────────────────

const TITLE_LINE_MAX = 20;

/**
 * Splits a habit title so it renders on at most two lines, each ≤ 20 chars.
 * Prefers breaking on whitespace; falls back to a hard cut at 20 characters.
 */
function splitHabitTitle(title: string): string[] {
    const t = title.trim();
    if (t.length <= TITLE_LINE_MAX) return [t];

    // Latest space within the first line's budget.
    let breakAt = t.lastIndexOf(" ", TITLE_LINE_MAX);
    // Use it only if the remainder also fits on one line; otherwise hard-cut.
    if (breakAt <= 0 || t.length - breakAt - 1 > TITLE_LINE_MAX) {
        return [t.slice(0, TITLE_LINE_MAX), t.slice(TITLE_LINE_MAX, TITLE_LINE_MAX * 2)];
    }
    return [t.slice(0, breakAt), t.slice(breakAt + 1, breakAt + 1 + TITLE_LINE_MAX)];
}

/** Returns true when the current period has already been completed. */
function isCurrentPeriodDone(habit: HabitResponse): boolean {
    if (!habit.lastCompletedDate) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const last = new Date(habit.lastCompletedDate + "T00:00:00");

    switch (habit.frequency) {
        case "DAILY":
        case "MULTI_DAY":
            return habit.lastCompletedDate === new Date().toISOString().split("T")[0];

        case "WEEKLY": {
            // Monday of the current ISO week
            const dow = today.getDay(); // 0=Sun
            const offsetToMon = dow === 0 ? -6 : 1 - dow;
            const monday = new Date(today);
            monday.setDate(today.getDate() + offsetToMon);
            return last >= monday;
        }

        case "CUSTOM": {
            // lastCompletedDate is within the last customIntervalDays window
            const interval = habit.customIntervalDays ?? 30;
            const windowStart = new Date(today);
            windowStart.setDate(today.getDate() - interval + 1);
            return last >= windowStart;
        }
    }
}

/** Derives a label for each radial segment based on frequency. */
function segmentLabel(frequency: HabitResponse["frequency"], index: number): string {
    switch (frequency) {
        case "DAILY":   return String(index + 1);
        case "WEEKLY":  return `W${index + 1}`;
        case "CUSTOM":  return `P${index + 1}`;
    }
}

/** Label shown in the center button when the period is already done. */
function doneLabelFor(frequency: HabitResponse["frequency"]): string {
    switch (frequency) {
        case "DAILY":  return "Day Logged";
        case "WEEKLY": return "Week Logged";
        case "CUSTOM": return "Period Logged";
    }
}

// ── HabitRadialCard ────────────────────────────────────────────────────────────

const CARD_COLORS = [
    { stroke: "text-blue-400",    glow: "rgba(96, 165, 250, 0.5)",  bg: "bg-blue-400"    },
    { stroke: "text-purple-400",  glow: "rgba(192, 132, 252, 0.5)", bg: "bg-purple-400"  },
    { stroke: "text-emerald-400", glow: "rgba(52, 211, 153, 0.5)",  bg: "bg-emerald-400" },
    { stroke: "text-orange-400",  glow: "rgba(251, 146, 60, 0.5)",  bg: "bg-orange-400"  },
];

function HabitRadialCard({
    habit,
    onCheck,
    onEdit,
    onDelete,
    index,
}: {
    habit: HabitResponse;
    onCheck: () => void;
    onEdit: () => void;
    onDelete: () => void;
    index: number;
}) {
    const color = CARD_COLORS[index % CARD_COLORS.length];
    const isDone = isCurrentPeriodDone(habit);

    // monthCompletions drives the segment count; fall back to days-in-month for bare DAILY habits.
    const now = new Date();
    const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
    const completions: Array<"DONE" | "MISSED" | "NA"> =
        habit.monthCompletions && habit.monthCompletions.length > 0
            ? (habit.monthCompletions as Array<"DONE" | "MISSED" | "NA">)
            : Array.from({ length: daysInMonth }, (_, i) => {
                  // Fallback for when monthCompletions isn't returned (e.g. after a mutation)
                  const dayNum = i + 1;
                  const todayNum = now.getDate();
                  if (dayNum > todayNum) return "NA";
                  if (dayNum === todayNum) return isDone ? "DONE" : "NA";
                  return "MISSED";
              });

    const segCount = completions.length;
    const radius = 90;
    const center = 120;
    const circumference = 2 * Math.PI * radius;
    const segLen = circumference / segCount;
    const gap = segCount <= 6 ? 8 : segCount <= 12 ? 6 : 4;
    const strokeWidth = segCount <= 6 ? 16 : 12;

    const streakUnit = habit.streakUnit ?? "day";

    return (
        <div className="bg-slate-800/40 border border-slate-700/50 rounded-[2.5rem] p-8 flex flex-col items-center group hover:bg-slate-800/60 transition-all duration-300 relative overflow-hidden">

            {/* ── Edit / Delete buttons (hover) ── */}
            <div className="absolute top-6 left-6 flex gap-2 z-20 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                    onClick={(e) => { e.stopPropagation(); onEdit(); }}
                    className="p-1.5 bg-slate-700/50 hover:bg-slate-600 rounded-lg text-slate-300 hover:text-white transition-colors"
                    title="Edit Habit"
                >
                    <Edit2 className="w-4 h-4" />
                </button>
                <button
                    onClick={(e) => { e.stopPropagation(); onDelete(); }}
                    className="p-1.5 bg-red-900/30 hover:bg-red-500/80 rounded-lg text-red-400 hover:text-white transition-colors"
                    title="Delete Habit"
                >
                    <Trash2 className="w-4 h-4" />
                </button>
            </div>

            {/* ── Title + streak ── */}
            <div className="text-center mb-6 z-10">
                <h3 className="text-xl font-bold text-white group-hover:text-blue-200 transition-colors leading-tight">
                    {splitHabitTitle(habit.title).map((line, i) => (
                        <span key={i} className="block">{line}</span>
                    ))}
                </h3>
                <div className="flex items-center justify-center gap-1.5 text-orange-400 text-sm mt-1 font-medium">
                    <Flame className="w-4 h-4 fill-orange-400/20" />
                    <span>{habit.currentStreak} {streakUnit} streak</span>
                </div>
            </div>

            {/* ── Radial ring ── */}
            <div className="relative w-64 h-64 flex items-center justify-center z-10 my-4">

                {/* Arcs — one per period */}
                <svg viewBox="0 0 240 240" className="w-full h-full transform -rotate-90 drop-shadow-2xl">
                    {completions.map((status, i) => {
                        const segActual = segLen - gap;
                        let strokeClass: string;
                        let glowStyle: React.CSSProperties | undefined;
                        if (status === "DONE") {
                            strokeClass = color.stroke;
                            glowStyle = { filter: `drop-shadow(0 0 6px ${color.glow})` };
                        } else if (status === "MISSED") {
                            strokeClass = "text-rose-800";
                            glowStyle = undefined;
                        } else {
                            strokeClass = "text-slate-600";
                            glowStyle = undefined;
                        }
                        return (
                            <circle
                                key={i}
                                cx={center}
                                cy={center}
                                r={radius}
                                fill="transparent"
                                stroke="currentColor"
                                strokeWidth={strokeWidth}
                                strokeDasharray={`${segActual} ${circumference - segActual}`}
                                strokeDashoffset={-(i * segLen)}
                                className={strokeClass}
                                style={glowStyle}
                            />
                        );
                    })}
                </svg>

                {/* Segment labels — unrotated overlay */}
                <svg viewBox="0 0 240 240" className="w-full h-full absolute inset-0 pointer-events-none">
                    {completions.map((status, i) => {
                        const theta = (i + 0.5) / segCount * 2 * Math.PI;
                        const x = center + radius * Math.sin(theta);
                        const y = center - radius * Math.cos(theta);
                        const fill =
                            status === "DONE"   ? "white"   :
                            status === "MISSED" ? "#fb7185" :
                                                  "#94a3b8";
                        const label = segmentLabel(habit.frequency, i);
                        // Show labels only when segments are large enough to read
                        const minSegForLabel = 8;
                        if (segCount > 31 || (segCount > minSegForLabel && label.length > 2)) return null;
                        return (
                            <text
                                key={i}
                                x={x}
                                y={y}
                                textAnchor="middle"
                                dominantBaseline="central"
                                fill={fill}
                                fontSize={segCount <= 6 ? "9" : segCount <= 12 ? "8" : "7"}
                                fontWeight={status === "DONE" ? "700" : "400"}
                            >
                                {label}
                            </text>
                        );
                    })}
                </svg>

                {/* Center complete button */}
                <div className="absolute inset-0 flex items-center justify-center">
                    <button
                        onClick={(e) => { e.stopPropagation(); onCheck(); }}
                        disabled={isDone}
                        className={`w-32 h-32 rounded-full flex flex-col items-center justify-center transition-all duration-500 relative overflow-hidden ${
                            isDone
                                ? "bg-transparent text-green-400 cursor-default"
                                : "bg-slate-900 text-white hover:scale-105 border border-slate-700 hover:border-slate-500 shadow-2xl"
                        }`}
                    >
                        {isDone ? (
                            <>
                                <Check className="w-12 h-12 animate-in zoom-in" />
                                <span className="text-[11px] mt-2 uppercase font-black tracking-tighter">
                                    {doneLabelFor(habit.frequency)}
                                </span>
                            </>
                        ) : (
                            <>
                                <Plus className="w-12 h-12 group-hover:rotate-90 transition-transform duration-300" />
                                <span className="text-[11px] mt-2 uppercase font-black tracking-tighter">Complete</span>
                            </>
                        )}
                    </button>
                </div>
            </div>

            {/* Coin reward badge */}
            <div className="absolute top-6 right-8 flex items-center gap-1 opacity-40">
                <span className="text-xs font-bold text-yellow-500">+{habit.coinReward}</span>
            </div>

            {/* Background glow */}
            <div className={`absolute -bottom-10 -left-10 w-40 h-40 rounded-full blur-[80px] opacity-10 ${color.bg}`} />
        </div>
    );
}
