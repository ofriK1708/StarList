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

function HabitRadialCard({ habit, onCheck, onEdit, onDelete, index }: { habit: HabitResponse; onCheck: () => void; onEdit: () => void; onDelete: () => void; index: number }) {
    const now = new Date();
    const todayStr = now.toISOString().split('T')[0];
    const todayDate = now.getDate();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();
    const isCompletedToday = Boolean(habit.lastCompletedDate && habit.lastCompletedDate.startsWith(todayStr));

    const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();

    // Work out which days of this month are part of the current streak
    const completedDaysInMonth = new Set<number>();
    if (habit.lastCompletedDate && habit.currentStreak > 0) {
        const lastCompleted = new Date(habit.lastCompletedDate + 'T00:00:00');
        const monthStart = new Date(currentYear, currentMonth, 1);
        for (let i = 0; i < habit.currentStreak; i++) {
            const d = new Date(lastCompleted);
            d.setDate(d.getDate() - i);
            if (d.getFullYear() === currentYear && d.getMonth() === currentMonth) {
                completedDaysInMonth.add(d.getDate());
            } else if (d < monthStart) {
                break;
            }
        }
    }

    const radius = 90;
    const center = 120;
    const circumference = 2 * Math.PI * radius;
    const segmentLength = circumference / daysInMonth;
    const gap = 4;
    const strokeWidth = 12;

    const colors = [
        { stroke: 'text-blue-400', glow: 'rgba(96, 165, 250, 0.5)', bg: 'bg-blue-400' },
        { stroke: 'text-purple-400', glow: 'rgba(192, 132, 252, 0.5)', bg: 'bg-purple-400' },
        { stroke: 'text-emerald-400', glow: 'rgba(52, 211, 153, 0.5)', bg: 'bg-emerald-400' },
        { stroke: 'text-orange-400', glow: 'rgba(251, 146, 60, 0.5)', bg: 'bg-orange-400' },
    ];
    const color = colors[index % colors.length];

    return (
        <div className="bg-slate-800/40 border border-slate-700/50 rounded-[2.5rem] p-8 flex flex-col items-center group hover:bg-slate-800/60 transition-all duration-300 relative overflow-hidden">

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

            <div className="text-center mb-6 z-10">
                <h3 className="text-xl font-bold text-white group-hover:text-blue-200 transition-colors">{habit.title}</h3>
                <div className="flex items-center justify-center gap-1.5 text-orange-400 text-sm mt-1 font-medium">
                    <Flame className="w-4 h-4 fill-orange-400/20" />
                    <span>{habit.currentStreak} day streak</span>
                </div>
            </div>

            <div className="relative w-64 h-64 flex items-center justify-center z-10 my-4">
                {/* One arc segment per day of the month */}
                <svg viewBox="0 0 240 240" className="w-full h-full transform -rotate-90 drop-shadow-2xl">
                    {Array.from({ length: daysInMonth }, (_, i) => {
                        const dayNum = i + 1;
                        const isCompleted = completedDaysInMonth.has(dayNum);
                        const isFuture = dayNum > todayDate;
                        const segActualLength = segmentLength - gap;
                        return (
                            <circle
                                key={dayNum}
                                cx={center}
                                cy={center}
                                r={radius}
                                fill="transparent"
                                stroke="currentColor"
                                strokeWidth={strokeWidth}
                                strokeDasharray={`${segActualLength} ${circumference - segActualLength}`}
                                strokeDashoffset={-(i * segmentLength)}
                                className={isCompleted ? color.stroke : isFuture ? 'text-slate-800' : 'text-slate-700'}
                                style={isCompleted ? { filter: `drop-shadow(0 0 6px ${color.glow})` } : undefined}
                            />
                        );
                    })}
                </svg>

                {/* Day number labels — separate unrotated SVG so numbers stay upright */}
                <svg viewBox="0 0 240 240" className="w-full h-full absolute inset-0 pointer-events-none">
                    {Array.from({ length: daysInMonth }, (_, i) => {
                        const dayNum = i + 1;
                        const theta = (i + 0.5) / daysInMonth * 2 * Math.PI;
                        const x = center + radius * Math.sin(theta);
                        const y = center - radius * Math.cos(theta);
                        const isCompleted = completedDaysInMonth.has(dayNum);
                        const isFuture = dayNum > todayDate;
                        return (
                            <text
                                key={dayNum}
                                x={x}
                                y={y}
                                textAnchor="middle"
                                dominantBaseline="central"
                                fill={isCompleted ? 'white' : isFuture ? '#1e293b' : '#475569'}
                                fontSize="7"
                                fontWeight={isCompleted ? '700' : '400'}
                            >
                                {dayNum}
                            </text>
                        );
                    })}
                </svg>

                <div className="absolute inset-0 flex items-center justify-center">
                    <button
                        onClick={(e) => {
                            e.stopPropagation();
                            onCheck();
                        }}
                        disabled={isCompletedToday}
                        className={`w-32 h-32 rounded-full flex flex-col items-center justify-center transition-all duration-500 relative overflow-hidden ${
                            isCompletedToday
                                ? 'bg-transparent text-green-400 cursor-default'
                                : 'bg-slate-900 text-white hover:scale-105 border border-slate-700 hover:border-slate-500 shadow-2xl'
                        }`}
                    >
                        {isCompletedToday ? (
                            <>
                                <Check className="w-12 h-12 animate-in zoom-in" />
                                <span className="text-[11px] mt-2 uppercase font-black tracking-tighter">Day Logged</span>
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

            <div className="absolute top-6 right-8 flex items-center gap-1 opacity-40">
                <span className="text-xs font-bold text-yellow-500">+{habit.coinReward}</span>
            </div>

            <div className={`absolute -bottom-10 -left-10 w-40 h-40 rounded-full blur-[80px] opacity-10 ${color.bg}`} />
        </div>
    );
}