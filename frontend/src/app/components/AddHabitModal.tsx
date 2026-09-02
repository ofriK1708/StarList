import { useState } from "react";
import { X, Flame, Plus } from "lucide-react";
import { AddHabitRequest, DifficultyLevel } from "@/services/habitsApi.ts";
import { TITLE_MAX_LENGTH } from "@/services/validation.ts";
import { FrequencyConfig, FrequencyConfigSection } from "./FrequencyConfigSection.tsx";

interface AddHabitModalProps {
    isOpen: boolean;
    onClose: () => void;
    onAdd: (habitData: AddHabitRequest) => void;
}

const DEFAULT_FREQ_CONFIG: FrequencyConfig = {
    frequency: "DAILY",
    scheduledDayOfWeek: null,
    scheduledTimeType: null,
    scheduledHour: null,
    customIntervalDays: null,
    scheduledDaysOfWeek: null,
};

export function AddHabitModal({ isOpen, onClose, onAdd }: AddHabitModalProps) {
    const [title, setTitle] = useState("");
    const [difficulty, setDifficulty] = useState<DifficultyLevel>("MEDIUM");
    const [freqConfig, setFreqConfig] = useState<FrequencyConfig>(DEFAULT_FREQ_CONFIG);

    if (!isOpen) return null;

    const isValid = () => {
        if (!title.trim()) return false;
        if (freqConfig.frequency === "WEEKLY" || freqConfig.frequency === "CUSTOM") {
            if (!freqConfig.scheduledDayOfWeek) return false;
        }
        if (freqConfig.frequency === "CUSTOM" && !freqConfig.customIntervalDays) return false;
        if (freqConfig.frequency === "MULTI_DAY") {
            if (!freqConfig.scheduledDaysOfWeek || freqConfig.scheduledDaysOfWeek.length < 2) return false;
        }
        return true;
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!isValid()) return;

        onAdd({
            title,
            difficultyLevel: difficulty,
            frequency: freqConfig.frequency,
            scheduledDayOfWeek: freqConfig.scheduledDayOfWeek ?? undefined,
            scheduledTimeType: freqConfig.scheduledTimeType ?? undefined,
            scheduledHour: freqConfig.scheduledHour ?? undefined,
            customIntervalDays: freqConfig.customIntervalDays ?? undefined,
            scheduledDaysOfWeek: freqConfig.scheduledDaysOfWeek ?? undefined,
        });

        setTitle("");
        setDifficulty("MEDIUM");
        setFreqConfig(DEFAULT_FREQ_CONFIG);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">
                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Flame className="w-5 h-5 text-orange-400" />
                        New Habit
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
                            maxLength={TITLE_MAX_LENGTH}
                            placeholder="e.g., Morning run"
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                        <p className="text-xs text-slate-500 mt-1 text-right">{title.length}/{TITLE_MAX_LENGTH}</p>
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
                            <Plus className="w-4 h-4" />
                            Add Habit
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
