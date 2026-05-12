import { BarChart3, PieChart, TrendingUp, Award, UserCheck, CheckCircle2, Zap } from "lucide-react";

interface StatisticsProps {
  tasks: any[];
  habits: any[];
  currentCoins: number;
  totalCoinsEarned: number;
}

function isCompleted(task: { status: string }): boolean {
  return task.status === 'COMPLETED';
}

export function Statistics({ tasks, habits, currentCoins, totalCoinsEarned }: StatisticsProps) {
  const completedTasks = tasks.filter(t => isCompleted(t)).length;
  const completionRate = tasks.length > 0 ? Math.round((completedTasks / tasks.length) * 100) : 0;

  const difficultyData = {
    easy: tasks.filter(t => isCompleted(t) && t.difficultyLevel === 'EASY').length,
    medium: tasks.filter(t => isCompleted(t) && t.difficultyLevel === 'MEDIUM').length,
    hard: tasks.filter(t => isCompleted(t) && t.difficultyLevel === 'HARD').length,
  };

  return (
      <div className="h-full flex flex-col w-full animate-in fade-in duration-500 overflow-y-auto bg-slate-900/50">
        <div className="p-6">
          <h1 className="text-2xl font-bold text-white mb-1">Performance Analytics</h1>
          <p className="text-slate-400 text-sm">Tracking your journey through the galaxy 🚀</p>
        </div>

        <div className="p-6 grid grid-cols-2 gap-4">
          <StatCard icon={<Award className="text-yellow-400" />} label="Current Coins" value={currentCoins} subValue="Coins" />
          <StatCard icon={<Award className="text-yellow-600" />} label="Overall Coins Earned" value={totalCoinsEarned} subValue="Coins" />
        </div>

        <br />

        <div className="pl-6 flex items-center gap-2">
          <UserCheck className="text-blue-500" />
          <h1 className="text-white font-bold">Tasks</h1>
        </div>

        <div className="grid grid-cols-2 gap-4 p-6">
          <div className="grid grid-cols-1 gap-4">
            <StatCard icon={<CheckCircle2 className="text-green-300" />} label="Tasks Done" value={completedTasks} subValue={`of ${tasks.length}`} />
            <StatCard icon={<Zap className="text-blue-400" />} label="Task Completion %" value={`${completionRate}%`} />
          </div>

          <div>
            <div className="bg-slate-800/50 border border-slate-700/50 rounded-2xl p-6 h-full">
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