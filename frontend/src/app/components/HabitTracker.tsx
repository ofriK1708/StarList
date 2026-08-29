import { Check, Flame, Plus, Trash2, Edit2 } from "lucide-react";
import { Button } from "./ui/button";
import { HabitResponse } from "@/services/habitsApi.ts";

interface HabitTrackerProps {
    habits: HabitResponse[];
    onHabitCheck: (id: number) => void;
    onAddHabitClick: () => void;
    onEditHabitClick: (habit: HabitResponse) => void;
    onDeleteHabit: (id: number) => void;
}

export function HabitTracker({ habits, onHabitCheck, onAddHabitClick, onEditHabitClick, onDeleteHabit }: HabitTrackerProps) {
    return (
        <div className="w-full h-full bg-slate-900/50 overflow-y-auto p-6 animate-in fade-in duration-500">
            <div className="max-w-5xl mx-auto">
                <div className="flex justify-between items-center mb-10">
                    <div>
                        <h1 className="text-2xl font-bold text-white tracking-tight">Habits</h1>
                        <p className="text-slate-400 text-sm">Maintain consistency to power the galaxy's core</p>
                    </div>
                    <Button
                        onClick={onAddHabitClick}
                        className="bg-indigo-600 hover:bg-indigo-700 text-white gap-2 shadow-lg shadow-indigo-900/20"
                    >
                        <Plus className="w-4 h-4" /> New Habit
                    </Button>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
                    {habits.map((habit, index) => (
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
            </div>
        </div>
    );
}

// ── Helpers ────────────────────────────────────────────────────────────────────

/** Returns true when the current period has already been completed. */
function isCurrentPeriodDone(habit: HabitResponse): boolean {
    if (!habit.lastCompletedDate) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const last = new Date(habit.lastCompletedDate + "T00:00:00");

    switch (habit.frequency) {
        case "DAILY":
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

        case "MULTI_DAY":
            // Each slot is a single day — done if completed today
            return habit.lastCompletedDate === new Date().toISOString().split("T")[0];
    }
}

/** Derives a label for each radial segment based on frequency. */
function segmentLabel(frequency: HabitResponse["frequency"], index: number): string {
    switch (frequency) {
        case "DAILY":     return String(index + 1);
        case "WEEKLY":    return `W${index + 1}`;
        case "CUSTOM":    return `P${index + 1}`;
        case "MULTI_DAY": return String(index + 1);
    }
}

/** Label shown in the center button when the period is already done. */
function doneLabelFor(frequency: HabitResponse["frequency"]): string {
    switch (frequency) {
        case "DAILY":     return "Day Logged";
        case "WEEKLY":    return "Week Logged";
        case "CUSTOM":    return "Period Logged";
        case "MULTI_DAY": return "Day Logged";
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
                <h3 className="text-xl font-bold text-white group-hover:text-blue-200 transition-colors">{habit.title}</h3>
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
                            strokeClass = "text-rose-900";
                            glowStyle = undefined;
                        } else {
                            strokeClass = "text-slate-800";
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
                            status === "MISSED" ? "#881337" :
                                                  "#1e293b";
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
