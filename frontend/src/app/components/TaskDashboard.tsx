import { Plus, MessageSquare, Circle, CheckCircle2, Clock, Trash2 } from "lucide-react";
import { Button } from "./ui/button";
import { useState } from "react";

interface Task {
  id: string;
  title: string;
  description: string;
  difficulty: 'easy' | 'medium' | 'hard';
  completed: boolean;
  reward: number;
  duration: string;
}

interface TaskDashboardProps {
  tasks: Task[];
  onTaskToggle: (id: string) => void;
  onQuickAdd: () => void;
  onOpenChat: () => void;
  onTaskDelete: (id: string) => void;
}

const difficultyColors = {
  easy: 'bg-green-500/20 border-green-500/40 text-green-300',
  medium: 'bg-yellow-500/20 border-yellow-500/40 text-yellow-300',
  hard: 'bg-red-500/20 border-red-500/40 text-red-300',
};

const difficultyLabels = {
  easy: 'Easy',
  medium: 'Medium',
  hard: 'Hard',
};

export function TaskDashboard({ tasks, onTaskToggle, onQuickAdd, onOpenChat, onTaskDelete }: TaskDashboardProps) {
  const [filter, setFilter] = useState<'all' | 'active' | 'completed'>('active');

  const filteredTasks = tasks.filter(task => {
    if (filter === 'all') return true;
    if (filter === 'active') return !task.completed;
    if (filter === 'completed') return task.completed;
    return true;
  });

  return (
    <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 overflow-y-auto">
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

        {/* Filter tabs */}
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
      </div>

      {/* Task List */}
      <div className="p-6 space-y-3">
        {filteredTasks.length === 0 ? (
          <div className="text-center py-12 text-slate-400">
            <Clock className="w-12 h-12 mx-auto mb-3 opacity-50" />
            <p>No tasks found</p>
          </div>
        ) : (
          filteredTasks.map((task) => (
            <div
              key={task.id}
              className={`relative group rounded-lg border backdrop-blur-sm p-4 transition-all hover:scale-[1.01] cursor-pointer ${
                task.completed
                  ? 'bg-slate-800/40 border-slate-700/50 opacity-60'
                  : 'bg-slate-800/60 border-slate-700'
              }`}
              onClick={() => onTaskToggle(task.id)}
            >
              <div className="flex items-start gap-4">
                {/* Checkbox */}
                <button className="mt-1">
                  {task.completed ? (
                    <CheckCircle2 className="w-6 h-6 text-green-400" />
                  ) : (
                    <Circle className="w-6 h-6 text-slate-500 group-hover:text-slate-400" />
                  )}
                </button>

                {/* Content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <h3 className={`text-lg ${task.completed ? 'line-through text-slate-500' : 'text-white/90'}`}>
                      {task.title}
                    </h3>
                    <div className="flex items-center gap-2 shrink-0">
                      <span className={`px-3 py-1 rounded-full text-xs border ${difficultyColors[task.difficulty]}`}>
                        {difficultyLabels[task.difficulty]}
                      </span>
                      {!task.completed && (
                          <button
                              onClick={(e) => {
                                e.stopPropagation();
                                onTaskDelete(task.id);
                              }}
                              className="text-slate-500 hover:text-red-500 transition-colors p-1.5 rounded-md hover:bg-red-500/10"
                              title="Delete task"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                      )}
                    </div>
                  </div>
                  <p className={`text-sm mb-3 ${task.completed ? 'text-slate-600' : 'text-slate-400'}`}>
                    {task.description}
                  </p>
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5 text-slate-400 bg-slate-700/50 px-2 py-1 rounded">
                      <Clock className="w-3.5 h-3.5" />
                      <span className="text-xs">{task.duration}</span>
                    </div>
                    <div className="flex items-center gap-1 text-yellow-400">
                      <span className="text-sm">+{task.reward}</span>
                      <span className="text-xs">coins</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}