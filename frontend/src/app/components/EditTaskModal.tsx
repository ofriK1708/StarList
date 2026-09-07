import { useState, useEffect } from "react";
import { X, Edit2, Check } from "lucide-react";
import { TaskResponse, UpdateTaskRequest, DifficultyLevel } from "@/services/taskApi.ts";
import { TITLE_MAX_LENGTH, DESCRIPTION_MAX_LENGTH } from "@/services/validation.ts";
import { DURATION_UNITS, DurationUnit, toMinutes, splitMinutes } from "@/lib/duration.ts";

interface EditTaskModalProps {
    isOpen: boolean;
    onClose: () => void;
    taskToEdit: TaskResponse | null;
    onUpdate: (taskId: number, taskData: UpdateTaskRequest) => void;
}

/** ISO string -> value accepted by <input type="datetime-local"> (local time, no seconds). */
function toLocalInputValue(iso: string | null): string {
    if (!iso) return "";
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return "";
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function EditTaskModal({ isOpen, onClose, taskToEdit, onUpdate }: EditTaskModalProps) {
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [difficulty, setDifficulty] = useState<DifficultyLevel>("MEDIUM");
    const [durationValue, setDurationValue] = useState<number>(30);
    const [durationUnit, setDurationUnit] = useState<DurationUnit>('minutes');
    const [dueDate, setDueDate] = useState<string>("");

    useEffect(() => {
        if (taskToEdit) {
            setTitle(taskToEdit.title);
            setDescription(taskToEdit.description ?? "");
            setDifficulty(taskToEdit.difficultyLevel);
            const { value, unit } = splitMinutes(taskToEdit.durationMinutes ?? 30);
            setDurationValue(value);
            setDurationUnit(unit);
            setDueDate(toLocalInputValue(taskToEdit.dueDate));
        }
    }, [taskToEdit]);

    if (!isOpen || !taskToEdit) return null;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        onUpdate(taskToEdit.taskId, {
            title,
            description,
            difficultyLevel: difficulty,
            durationMinutes: toMinutes(durationValue, durationUnit),
            dueDate: dueDate ? new Date(dueDate).toISOString() : undefined,
        });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
            <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-in fade-in zoom-in duration-200">

                <div className="flex items-center justify-between p-4 border-b border-slate-700/50 bg-slate-800/50">
                    <h2 className="text-lg text-white font-medium flex items-center gap-2">
                        <Edit2 className="w-5 h-5 text-blue-400" />
                        Edit Mission
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
                            maxLength={TITLE_MAX_LENGTH}
                            placeholder="e.g., Finish OS Assignment"
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors"
                            required
                        />
                        <p className="text-xs text-slate-500 mt-1 text-right">{title.length}/{TITLE_MAX_LENGTH}</p>
                    </div>

                    <div>
                        <label className="block text-sm text-slate-300 mb-1">Description (Optional)</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            maxLength={DESCRIPTION_MAX_LENGTH}
                            placeholder="Add some details..."
                            className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 transition-colors resize-none h-20"
                        />
                        <p className="text-xs text-slate-500 mt-1 text-right">{description.length}/{DESCRIPTION_MAX_LENGTH}</p>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm text-slate-300 mb-1">Difficulty</label>
                            <select
                                aria-label="Difficulty"
                                value={difficulty}
                                onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
                                className="w-full bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                            >
                                <option value="EASY">Easy</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HARD">Hard</option>
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm text-slate-300 mb-1">Duration</label>
                            <div className="flex gap-2">
                                <input
                                    type="number"
                                    min="0"
                                    step="1"
                                    aria-label="Duration amount"
                                    value={durationValue}
                                    onChange={(e) => setDurationValue(Number(e.target.value))}
                                    className="w-full min-w-0 bg-slate-800 border border-slate-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-blue-500"
                                />
                                <select
                                    aria-label="Duration unit"
                                    value={durationUnit}
                                    onChange={(e) => setDurationUnit(e.target.value as DurationUnit)}
                                    className="bg-slate-800 border border-slate-600 rounded-lg px-2 py-2 text-white focus:outline-none focus:border-blue-500 appearance-none"
                                >
                                    {DURATION_UNITS.map((u) => (
                                        <option key={u.value} value={u.value}>{u.label}</option>
                                    ))}
                                </select>
                            </div>
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
                            <Check className="w-4 h-4" />
                            Save Changes
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
