import { Flame, CheckCircle2, Circle, Plus } from "lucide-react";

interface Habit {
    id: string;
    title: string;
    streak: number;
    completedToday: boolean;
    baseReward: number;
}

interface HabitTrackerProps {
    habits: Habit[];
    onHabitCheck: (id: string) => void;
    onAddHabitClick: () => void; // הפונקציה החדשה שתפתח את החלון
}

export function HabitTracker({ habits, onHabitCheck, onAddHabitClick }: HabitTrackerProps) {
    return (
        <div className="h-full flex flex-col w-full animate-in fade-in duration-300">

            {/* אזור הכותרת והכפתור - עכשיו נמצאים כאן בצורה מסודרת */}
            <div className="p-6 pb-2 flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-white mb-1">Daily Habits</h1>
                    <p className="text-slate-400 text-sm">Build your streaks and earn bonus coins! 🔥</p>
                </div>
                <button
                    onClick={onAddHabitClick}
                    className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center gap-2 transition-colors font-medium text-sm"
                >
                    <Plus className="w-4 h-4" />
                    Add Habit
                </button>
            </div>

            {/* רשימת ההרגלים */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {habits.map(habit => (
                    <div key={habit.id} className="bg-slate-800/80 border border-slate-700 rounded-xl p-4 flex items-center justify-between hover:bg-slate-700/50 transition-colors">
                        <div className="flex items-center gap-4">
                            <button
                                onClick={() => onHabitCheck(habit.id)}
                                disabled={habit.completedToday}
                                className={`rounded-full transition-all duration-300 ${
                                    habit.completedToday
                                        ? 'text-green-500 scale-110'
                                        : 'text-slate-500 hover:text-green-400 hover:scale-110'
                                }`}
                            >
                                {habit.completedToday ? <CheckCircle2 className="w-7 h-7" /> : <Circle className="w-7 h-7" />}
                            </button>
                            <div>
                                <h3 className={`font-medium transition-colors ${
                                    habit.completedToday ? 'text-slate-400 line-through' : 'text-slate-100'
                                }`}>
                                    {habit.title}
                                </h3>
                                <div className={`flex items-center gap-1.5 text-sm mt-1 ${
                                    habit.streak > 0 ? 'text-orange-400' : 'text-slate-500'
                                }`}>
                                    <Flame className={`w-4 h-4 ${habit.streak > 0 ? 'animate-pulse' : ''}`} />
                                    <span>{habit.streak} Day Streak</span>
                                </div>
                            </div>
                        </div>
                        <div className="flex flex-col items-end">
              <span className="text-yellow-400 font-medium text-sm">
                +{habit.baseReward + (habit.streak * 2)} coins
              </span>
                            {habit.streak > 0 && !habit.completedToday && (
                                <span className="text-xs text-orange-400/80">Includes streak bonus!</span>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}