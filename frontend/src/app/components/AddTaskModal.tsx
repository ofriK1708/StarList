import { useState } from "react";
import { X, Target, Plus } from "lucide-react";
import { AddTaskRequest } from "@/services/taskApi.ts";

interface AddTaskModalProps {
    isOpen: boolean;
    onClose: () => void;
    onAdd: (taskData: AddTaskRequest) => void;
}

export function AddTaskModal({ isOpen, onClose, onAdd }: AddTaskModalProps) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [difficulty, setDifficulty] = useState<'EASY' | 'MEDIUM' | 'HARD'>('MEDIUM');
    const [durationMinutes, setDurationMinutes] = useState<number>(30);
    const [dueDate, setDueDate] = useState<string>("");

    if (!isOpen) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        onAdd({
            title,
            description,
            difficultyLevel: difficulty,
            durationMinutes,
            dueDate: dueDate ? new Date(dueDate).toISOString() : undefined,
        });

        setTitle("");
        setDescription("");
        setDifficulty("MEDIUM");
        setDurationMinutes(30);
        setDueDate("");
        onClose();
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">

                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Target className="w-5 h-5 text-blue-400" />
                        New Mission
                    </h2>
                    <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                        <X className="w-5 h-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="p-5 space-y-4">
                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Task Title</label>
                        <input
                            type="text"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="e.g., Finish OS Assignment"
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
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors resize-none h-20"
                        />
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm text-slate-300 mb-1">Difficulty</label>
                            <select
                                value={difficulty}
                                onChange={(e) => setDifficulty(e.target.value as 'EASY' | 'MEDIUM' | 'HARD')}
                                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                            >
                                <option value="EASY">Easy</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HARD">Hard</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm text-slate-300 mb-1">Duration (Min)</label>
                            <input
                                type="number"
                                min="5"
                                step="5"
                                value={durationMinutes}
                                onChange={(e) => setDurationMinutes(Number(e.target.value))}
                                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Due Date (Optional)</label>
                        <input
                            type="datetime-local"
                            value={dueDate}
                            onChange={(e) => setDueDate(e.target.value)}
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 [color-scheme:dark]"
                        />
                    </div>

                    <div className="pt-4 flex gap-3">
                        <button type="button" onClick={onClose} className="flex-1 py-2 rounded-lg font-medium bg-slate-700 hover:bg-slate-600 text-white transition-colors">
                            Cancel
                        </button>
                        <button type="submit" className="flex-1 py-2 rounded-lg font-medium bg-blue-600 hover:bg-blue-700 text-white flex items-center justify-center gap-2 transition-colors">
                            <Plus className="w-4 h-4" />
                            Add Mission
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}