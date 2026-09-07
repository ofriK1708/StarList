import { Plus, MessageSquare, Circle, CheckCircle2, Clock, Trash2, Edit2, Calendar, ArrowUpDown } from "lucide-react";
import { Button } from "./ui/button";
import { useState } from "react";
import { TaskResponse } from "@/services/taskApi.ts";
import { formatDuration } from "@/lib/duration.ts";

interface TaskDashboardProps {
  tasks: TaskResponse[];
  onTaskToggle: (taskId: number) => void;
  onQuickAdd: () => void;
  onOpenChat: () => void;
  onTaskEdit: (task: TaskResponse) => void;
  onTaskDelete: (taskId: number) => void;
}

const difficultyColors = {
  EASY: 'bg-green-500/20 border-green-500/40 text-green-300',
  MEDIUM: 'bg-yellow-500/20 border-yellow-500/40 text-yellow-300',
  HARD: 'bg-red-500/20 border-red-500/40 text-red-300',
};

const difficultyLabels = {
  EASY: 'Easy',
  MEDIUM: 'Medium',
  HARD: 'Hard',
};

export function TaskDashboard({ tasks, onTaskToggle, onQuickAdd, onOpenChat, onTaskEdit, onTaskDelete }: TaskDashboardProps) {
  const [filter, setFilter] = useState<'all' | 'active' | 'completed'>('active');
  const [sortBy, setSortBy] = useState<'date' | 'difficulty' | 'name' | 'duration'>('date');

  const filteredAndSortedTasks = tasks
      .filter(task => {
        const isCompleted = task.status === 'COMPLETED';
        if (filter === 'all') return true;
        if (filter === 'active') return !isCompleted;
        if (filter === 'completed') return isCompleted;
        return true;
      })
      .sort((a, b) => {
        if (sortBy === 'date') {
          const dateA = a.dueDate ? new Date(a.dueDate).getTime() : Infinity;
          const dateB = b.dueDate ? new Date(b.dueDate).getTime() : Infinity;
          return dateA - dateB;
        }
        if (sortBy === 'difficulty') {
          const weights = { HARD: 3, MEDIUM: 2, EASY: 1 };
          return weights[b.difficultyLevel] - weights[a.difficultyLevel];
        }
        if (sortBy === 'name') {
          return a.title.localeCompare(b.title);
        }
        if (sortBy === 'duration') {
          return (a.durationMinutes ?? 0) - (b.durationMinutes ?? 0);
        }
        return 0;
      });

  const isTaskOverdue = (dateString: string | null, isCompleted: boolean) => {
    if (isCompleted || !dateString) return false;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const taskDate = new Date(dateString);
    taskDate.setHours(0, 0, 0, 0);
    return taskDate < today;
  };

  return (
      <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 overflow-y-auto scrollbar-hide">
        {/* Header */}
        <div className="sticky top-0 z-10 bg-slate-900/95 backdrop-blur-sm border-b border-slate-700/50 px-6 py-4">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h1 className="text-2xl text-white/90">Task Dashboard</h1>
              <p className="text-sm text-slate-400">Manage your cosmic missions</p>
            </div>
            <div className="flex gap-3">
              <Button
                  onClick={onOpenChat}
                  className="bg-purple-600 hover:bg-purple-700 text-white flex items-center gap-2"
              >
                <MessageSquare className="w-4 h-4" />
                AI Chat
              </Button>
              <Button
                  onClick={onQuickAdd}
                  className="bg-blue-600 hover:bg-blue-700 text-white flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Add Task
              </Button>
            </div>
          </div>

          {/* Filters and Sorting Container */}
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
                  onChange={(e) => setSortBy(e.target.value as any)}
                  className="bg-transparent text-sm text-slate-300 focus:outline-none cursor-pointer appearance-none pr-4"
              >
                <option value="date" className="bg-slate-800">Sort by Date</option>
                <option value="difficulty" className="bg-slate-800">Sort by Difficulty</option>
                <option value="name" className="bg-slate-800">Sort by Name</option>
                <option value="duration" className="bg-slate-800">Sort by Duration</option>
              </select>
            </div>
          </div>
        </div>

        {/* Task List */}
        <div className="p-6 space-y-3 pb-24">
          {filteredAndSortedTasks.length === 0 ? (
              <div className="text-center py-12 text-slate-400">
                <Clock className="w-12 h-12 mx-auto mb-3 opacity-50" />
                <p>No tasks found</p>
              </div>
          ) : (
              filteredAndSortedTasks.map((task) => {
                const isCompleted = task.status === 'COMPLETED';
                const isOverdue = isTaskOverdue(task.dueDate, isCompleted);

                return (
                    <div
                        key={task.taskId}
                        className={`relative group rounded-lg border backdrop-blur-sm p-4 transition-all hover:scale-[1.01] cursor-pointer ${
                            isCompleted
                                ? 'bg-slate-800/40 border-slate-700/50 opacity-60'
                                : isOverdue
                                    ? 'bg-red-950/20 border-red-900/30'
                                    : 'bg-slate-800/60 border-slate-700'
                        }`}
                        onClick={() => onTaskToggle(task.taskId)}
                    >
                      <div className="flex items-start gap-4">
                        <button className="mt-1">
                          {isCompleted ? (
                              <CheckCircle2 className="w-6 h-6 text-green-400" />
                          ) : (
                              <Circle className={`w-6 h-6 group-hover:text-slate-300 ${isOverdue ? 'text-red-400/50' : 'text-slate-500'}`} />
                          )}
                        </button>

                        <div className="flex-1 min-w-0">
                          <div className="flex items-start justify-between gap-3 mb-2">
                            <h3 className={`text-lg ${isCompleted ? 'line-through text-slate-500' : 'text-white/90'}`}>
                              {task.title}
                            </h3>
                            <div className="flex items-center gap-2 shrink-0">
                                <span className={`px-3 py-1 rounded-full text-xs border ${difficultyColors[task.difficultyLevel]}`}>
                                  {difficultyLabels[task.difficultyLevel]}
                                </span>
                              {!isCompleted && (
                                  <>
                                    <button
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          onTaskEdit(task);
                                        }}
                                        className="text-slate-500 hover:text-blue-400 transition-colors p-1.5 rounded-md hover:bg-blue-500/10"
                                        title="Edit task"
                                    >
                                      <Edit2 className="w-4 h-4" />
                                    </button>
                                    <button
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          onTaskDelete(task.taskId);
                                        }}
                                        className="text-slate-500 hover:text-red-500 transition-colors p-1.5 rounded-md hover:bg-red-500/10"
                                        title="Delete task"
                                    >
                                      <Trash2 className="w-4 h-4" />
                                    </button>
                                  </>
                              )}
                            </div>
                          </div>

                          {task.description && (
                              <p className={`text-sm mb-3 ${isCompleted ? 'text-slate-600' : 'text-slate-400'}`}>
                                {task.description}
                              </p>
                          )}

                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              {task.durationMinutes && (
                                  <div className="flex items-center gap-1.5 text-slate-400 bg-slate-700/50 px-2 py-1 rounded">
                                    <Clock className="w-3.5 h-3.5" />
                                    <span className="text-xs">{formatDuration(task.durationMinutes)}</span>
                                  </div>
                              )}

                              {task.dueDate && (
                                  <div className={`flex items-center gap-1.5 px-2 py-1 rounded ${
                                      isCompleted ? 'text-slate-500 bg-slate-700/30' :
                                          isOverdue ? 'text-red-400 bg-red-500/10 border border-red-500/20' : 'text-slate-400 bg-slate-700/50'
                                  }`}>
                                    <Calendar className="w-3.5 h-3.5" />
                                    <span className="text-xs">{new Date(task.dueDate).toLocaleDateString('en-GB')}</span>
                                  </div>
                              )}
                            </div>

                            <div className="flex items-center gap-1 text-yellow-400">
                              <span className="text-sm">+{task.coinReward}</span>
                              <span className="text-xs">coins</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                );
              })
          )}
        </div>
      </div>
  );
}