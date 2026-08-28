import { useState, useEffect } from "react";
import { X, Edit2, Check } from "lucide-react";
import { HabitResponse, UpdateHabitRequest, DifficultyLevel } from "@/services/habitsApi.ts";
import { FrequencyConfig, FrequencyConfigSection } from "./FrequencyConfigSection.tsx";

interface EditHabitModalProps {
    isOpen: boolean;
    onClose: () => void;
    habitToEdit: HabitResponse | null;
    onUpdate: (habitId: number, habitData: UpdateHabitRequest) => void;
}

function habitToFreqConfig(habit: HabitResponse): FrequencyConfig {
    return {
        frequency: habit.frequency,
        scheduledDayOfWeek: habit.scheduledDayOfWeek,
        scheduledTimeType: habit.scheduledTimeType,
        scheduledHour: habit.scheduledHour,
        customIntervalDays: (habit.customIntervalDays as 7 | 14 | 30 | null) ?? null,
    };
}

export function EditHabitModal({ isOpen, onClose, habitToEdit, onUpdate }: EditHabitModalProps) {
    const [title, setTitle] = useState("");
    const [difficulty, setDifficulty] = useState<DifficultyLevel>("MEDIUM");
    const [freqConfig, setFreqConfig] = useState<FrequencyConfig>({
        frequency: "DAILY",
        scheduledDayOfWeek: null,
        scheduledTimeType: null,
        scheduledHour: null,
        customIntervalDays: null,
    });

    useEffect(() => {
        if (habitToEdit) {
            setTitle(habitToEdit.title);
            setDifficulty(habitToEdit.difficultyLevel);
            setFreqConfig(habitToFreqConfig(habitToEdit));
        }
    }, [habitToEdit]);

    if (!isOpen || !habitToEdit) return null;

    const isValid = () => {
        if (!title.trim()) return false;
        if (freqConfig.frequency === "WEEKLY" || freqConfig.frequency === "CUSTOM") {
            if (!freqConfig.scheduledDayOfWeek) return false;
        }
        if (freqConfig.frequency === "CUSTOM" && !freqConfig.customIntervalDays) return false;
        return true;
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!isValid()) return;

        onUpdate(habitToEdit.habitId, {
            title,
            difficultyLevel: difficulty,
            frequency: freqConfig.frequency,
            scheduledDayOfWeek: freqConfig.scheduledDayOfWeek ?? undefined,
            scheduledTimeType: freqConfig.scheduledTimeType ?? undefined,
            scheduledHour: freqConfig.scheduledHour ?? undefined,
            customIntervalDays: freqConfig.customIntervalDays ?? undefined,
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Edit2 className="w-5 h-5 text-blue-400" />
                        Edit Habit
                    </h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Habit Title</label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Difficulty</label>
                        <select
                            value={difficulty}
                            onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                        >
                            <option value="EASY">Easy</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HARD">Hard</option>
                        </select>
                    </div>

                    <FrequencyConfigSection value={freqConfig} onChange={setFreqConfig} />

                    <div className="pt-2 flex gap-3">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 py-2 rounded-lg font-medium bg-slate-700 hover:bg-slate-600 text-white transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={!isValid()}
                            className="flex-1 py-2 rounded-lg font-medium bg-blue-600 hover:bg-blue-700 disabled:opacity-40 disabled:cursor-not-allowed text-white flex items-center justify-center gap-2 transition-colors"
                        >
                            <Check className="w-4 h-4" />
                            Save Changes
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
