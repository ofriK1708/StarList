import { useState } from "react";
import { X, Star, Rocket } from "lucide-react";
import { Button } from "./ui/button";

interface AddTaskModalProps {
    isOpen: boolean;
    onClose: () => void;
    onAdd: (taskData: any) => void;
}

export function AddTaskModal({ isOpen, onClose, onAdd }: AddTaskModalProps) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [difficulty, setDifficulty] = useState<'easy' | 'medium' | 'hard'>('medium');
    const [duration, setDuration] = useState("30 min");

    if (!isOpen) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        // המרת רמת הקושי למטבעות בצורה אוטומטית
        const rewardMap = { easy: 10, medium: 30, hard: 50 };
        const reward = rewardMap[difficulty];

        onAdd({
            title,
            description,
            difficulty,
            reward,
            duration,
        });

        // איפוס הטופס וסגירת החלון
        setTitle("");
        setDescription("");
        setDifficulty("medium");
        setDuration("30 min");
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">

                {/* כותרת החלון */}
                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Rocket className="w-5 h-5 text-blue-400" />
                        New Mission
                    </h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* טופס הוספת המשימה */}
                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Task Title</label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="What needs to be done?"
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Description (Optional)</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="Add some details..."
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 resize-none h-20 focus:outline-none focus:border-blue-500 transition-colors"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm text-slate-300 mb-1">Difficulty</label>
                            <select
                                value={difficulty}
                                onChange={(e) => setDifficulty(e.target.value as any)}
                                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                            >
                                <option value="easy">Easy (+10 coins)</option>
                                <option value="medium">Medium (+30 coins)</option>
                                <option value="hard">Hard (+50 coins)</option>
                            </select>
                        </div>

                        <div>
                            <label className="block text-sm text-slate-300 mb-1">Estimated Time</label>
                            <select
                                value={duration}
                                onChange={(e) => setDuration(e.target.value)}
                                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                            >
                                <option value="15 min">15 min</option>
                                <option value="30 min">30 min</option>
                                <option value="1 hour">1 hour</option>
                                <option value="2+ hours">2+ hours</option>
                            </select>
                        </div>
                    </div>

                    <div className="pt-4 flex gap-3">
                        <Button type="button" onClick={onClose} className="flex-1 bg-slate-700 hover:bg-slate-600 text-white">
                            Cancel
                        </Button>
                        <Button type="submit" className="flex-1 bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2">
                            <Star className="w-4 h-4" />
                            Add Task
                        </Button>
                    </div>
                </form>
            </div>
        </div>
    );
}