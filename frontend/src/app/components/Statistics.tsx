import { BarChart3, PieChart, TrendingUp, Award, CheckCircle2, Zap } from "lucide-react";

interface StatisticsProps {
  tasks: any[];
  habits: any[];
  totalCoinsEarned: number;
  currentStreak: number;
}

export function Statistics({ tasks, habits, totalCoinsEarned, currentStreak }: StatisticsProps) {
  const completedTasks = tasks.filter(t => t.completed).length;
  const completionRate = tasks.length > 0 ? Math.round((completedTasks / tasks.length) * 100) : 0;

  const difficultyData = {
    easy: tasks.filter(t => t.completed && t.difficulty === 'easy').length,
    medium: tasks.filter(t => t.completed && t.difficulty === 'medium').length,
    hard: tasks.filter(t => t.completed && t.difficulty === 'hard').length,
  };

  return (
      <div className="h-full flex flex-col w-full animate-in fade-in duration-500 overflow-y-auto bg-slate-900/50">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-white mb-1">Performance Analytics</h1>
          <p className="text-slate-400 text-sm">Tracking your journey through the galaxy 🚀</p>
        </div>

        <div className="p-6 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard icon={<CheckCircle2 className="text-green-400" />} label="Tasks Done" value={completedTasks} subValue={`of ${tasks.length}`} />
          <StatCard icon={<Zap className="text-blue-400" />} label="Completion" value={`${completionRate}%`} />
          <StatCard icon={<TrendingUp className="text-orange-400" />} label="Best Streak" value={currentStreak} subValue="Days" />
          <StatCard icon={<Award className="text-yellow-400" />} label="Total Balance" value={totalCoinsEarned} subValue="Coins" />
        </div>

        <div className="p-6 grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-slate-800/50 border border-slate-700/50 rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-6">
              <PieChart className="w-5 h-5 text-purple-400" />
              <h3 className="text-white font-medium">Task Difficulty Distribution</h3>
            </div>
            <div className="space-y-4">
              <DifficultyBar label="Hard" count={difficultyData.hard} color="bg-red-500" total={completedTasks} />
              <DifficultyBar label="Medium" count={difficultyData.medium} color="bg-yellow-500" total={completedTasks} />
              <DifficultyBar label="Easy" count={difficultyData.easy} color="bg-green-500" total={completedTasks} />
            </div>
          </div>

          {/* Upcoming Achievements */}
          <div className="bg-slate-800/50 border border-slate-700/50 rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-6">
              <Award className="w-5 h-5 text-yellow-400" />
              <h3 className="text-white font-medium">Next Milestones</h3>
            </div>
            <div className="space-y-4">
              <Milestone label="Habit Master" desc="Reach a 7-day streak" progress={Math.min(100, (currentStreak/7)*100)} />
            </div>
          </div>
        </div>
      </div>
  );
}

function StatCard({ icon, label, value, subValue }: any) {
  return (
      <div className="bg-slate-800/40 border border-slate-700/50 p-5 rounded-2xl hover:bg-slate-800/60 transition-colors">
        <div className="flex items-center gap-3 mb-3">{icon} <span className="text-slate-400 text-sm font-medium">{label}</span></div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-white">{value}</span>
          <span className="text-slate-500 text-xs">{subValue}</span>
        </div>
      </div>
  );
}

function DifficultyBar({ label, count, color, total }: any) {
  const percentage = total > 0 ? (count / total) * 100 : 0;
  return (
      <div>
        <div className="flex justify-between text-xs mb-1">
          <span className="text-slate-300">{label}</span>
          <span className="text-slate-500">{count} tasks</span>
        </div>
        <div className="w-full bg-slate-700 h-2 rounded-full overflow-hidden">
          <div className={`${color} h-full transition-all duration-1000`} style={{ width: `${percentage}%` }} />
        </div>
      </div>
  );
}

function Milestone({ label, desc, progress }: any) {
  return (
      <div className="p-3 rounded-xl bg-slate-900/30 border border-slate-700/30">
        <div className="flex justify-between items-start mb-2">
          <div>
            <p className="text-sm text-white font-medium">{label}</p>
            <p className="text-xs text-slate-500">{desc}</p>
          </div>
          <span className="text-xs font-bold text-blue-400">{Math.round(progress)}%</span>
        </div>
        <div className="w-full bg-slate-700 h-1.5 rounded-full overflow-hidden">
          <div className="bg-blue-500 h-full transition-all duration-1000" style={{ width: `${progress}%` }} />
        </div>
      </div>
  );
}