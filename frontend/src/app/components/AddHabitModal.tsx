import { useState } from "react";
import { X, Flame, Plus } from "lucide-react";

interface AddHabitModalProps {
    isOpen: boolean;
    onClose: () => void;
    onAdd: (habitData: { title: string; baseReward: number }) => void;
}

export function AddHabitModal({ isOpen, onClose, onAdd }: AddHabitModalProps) {
    const [title, setTitle] = useState("");
    const [difficulty, setDifficulty] = useState<'easy' | 'medium' | 'hard'>('medium');

    if (!isOpen) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        const rewardMap = { easy: 10, medium: 20, hard: 30 };
        const baseReward = rewardMap[difficulty];

        onAdd({ title, baseReward });

        setTitle("");
        setDifficulty("medium");
        onClose();
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
                            placeholder="e.g., Drink 2L of water"
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Difficulty (Base Reward)</label>
                        <select
                            value={difficulty}
                            onChange={(e) => setDifficulty(e.target.value as any)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                        >
                            <option value="easy">Easy (+10 coins/day)</option>
                            <option value="medium">Medium (+20 coins/day)</option>
                            <option value="hard">Hard (+30 coins/day)</option>
                        </select>
                    </div>

                    <div className="pt-4 flex gap-3">
                        <button type="button" onClick={onClose} className="flex-1 py-2 rounded-lg font-medium bg-slate-700 hover:bg-slate-600 text-white transition-colors">
                            Cancel
                        </button>
                        <button type="submit" className="flex-1 py-2 rounded-lg font-medium bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2 transition-colors">
                            <Plus className="w-4 h-4" />
                            Add Habit
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}