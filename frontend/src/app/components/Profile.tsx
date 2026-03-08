import { User as UserIcon, Mail, Trophy, Sparkles, Crown, Settings } from "lucide-react";
import { Button } from "./ui/button";

interface ProfileProps {
  user: {
    name: string;
    email: string;
    level: number;
    xp: number;
    xpToNextLevel: number;
    tasksCompleted: number;
    achievements: Array<{ id: string; name: string; icon: string; unlocked: boolean }>;
  };
  coinBalance: number;
}

export function Profile({ user, coinBalance }: ProfileProps) {
  const progressPercent = (user.xp / user.xpToNextLevel) * 100;

  return (
    <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 overflow-y-auto">
      {/* Header with gradient background */}
      <div className="relative h-48 bg-gradient-to-br from-purple-600 via-blue-600 to-indigo-600 overflow-hidden">
        {/* Decorative stars */}
        {[...Array(20)].map((_, i) => (
          <div
            key={i}
            className="absolute w-1 h-1 bg-white rounded-full animate-pulse"
            style={{
              top: `${Math.random() * 100}%`,
              left: `${Math.random() * 100}%`,
              animationDelay: `${Math.random() * 2}s`,
              opacity: Math.random() * 0.7 + 0.3,
            }}
          />
        ))}
      </div>

      <div className="relative px-6 -mt-16">
        {/* Profile Picture */}
        <div className="flex items-end gap-4 mb-6">
          <div className="w-28 h-28 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center border-4 border-slate-900 shadow-xl">
            <UserIcon className="w-14 h-14 text-white" />
          </div>
          <div className="pb-2 flex-1">
            <div className="flex items-center gap-2 mb-1">
              <h1 className="text-2xl text-white">{user.name}</h1>
              <Crown className="w-5 h-5 text-yellow-400" />
            </div>
            <div className="flex items-center gap-2 text-sm text-slate-400">
              <Mail className="w-4 h-4" />
              <span>{user.email}</span>
            </div>
          </div>
          <Button className="bg-slate-800 hover:bg-slate-700 text-white border border-slate-700">
            <Settings className="w-4 h-4 mr-2" />
            Edit
          </Button>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-2 gap-4 mb-6">
          <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 text-center">
            <div className="flex items-center justify-center gap-2 mb-2">
              <Trophy className="w-5 h-5 text-yellow-400" />
              <span className="text-sm text-slate-400">Level</span>
            </div>
            <p className="text-3xl text-white">{user.level}</p>
          </div>

          <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 text-center">
            <div className="flex items-center justify-center gap-2 mb-2">
              <Sparkles className="w-5 h-5 text-yellow-400" />
              <span className="text-sm text-slate-400">Coins</span>
            </div>
            <p className="text-3xl text-white">{coinBalance}</p>
          </div>
        </div>

        {/* XP Progress */}
        <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm text-slate-400">Experience</span>
            <span className="text-sm text-slate-300">
              {user.xp} / {user.xpToNextLevel} XP
            </span>
          </div>
          <div className="w-full bg-slate-700 rounded-full h-3 overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-blue-500 to-purple-500 transition-all duration-500"
              style={{ width: `${progressPercent}%` }}
            />
          </div>
          <p className="text-xs text-slate-500 mt-2 text-center">
            {user.xpToNextLevel - user.xp} XP to Level {user.level + 1}
          </p>
        </div>

        {/* Tasks Completed */}
        <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between">
            <span className="text-sm text-slate-400">Total Tasks Completed</span>
            <span className="text-2xl text-white">{user.tasksCompleted}</span>
          </div>
        </div>

        {/* Achievements */}
        <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 mb-6">
          <h2 className="text-lg text-white/90 mb-4 flex items-center gap-2">
            <Trophy className="w-5 h-5 text-yellow-400" />
            Achievements
          </h2>
          <div className="grid grid-cols-4 gap-3">
            {user.achievements.map((achievement) => (
              <div
                key={achievement.id}
                className={`aspect-square rounded-lg flex items-center justify-center text-3xl ${
                  achievement.unlocked
                    ? 'bg-gradient-to-br from-yellow-500/20 to-orange-500/20 border border-yellow-500/40'
                    : 'bg-slate-700/30 border border-slate-600 opacity-40'
                }`}
                title={achievement.name}
              >
                {achievement.icon}
              </div>
            ))}
          </div>
        </div>

        {/* Activity Section */}
        <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 mb-6">
          <h2 className="text-lg text-white/90 mb-3">Recent Activity</h2>
          <div className="space-y-3">
            <div className="flex items-center gap-3 text-sm">
              <div className="w-8 h-8 rounded-full bg-green-500/20 flex items-center justify-center">
                <Trophy className="w-4 h-4 text-green-400" />
              </div>
              <div className="flex-1">
                <p className="text-slate-300">Completed 5 tasks today</p>
                <p className="text-xs text-slate-500">2 hours ago</p>
              </div>
            </div>
            <div className="flex items-center gap-3 text-sm">
              <div className="w-8 h-8 rounded-full bg-purple-500/20 flex items-center justify-center">
                <Crown className="w-4 h-4 text-purple-400" />
              </div>
              <div className="flex-1">
                <p className="text-slate-300">Reached Level {user.level}</p>
                <p className="text-xs text-slate-500">Yesterday</p>
              </div>
            </div>
            <div className="flex items-center gap-3 text-sm">
              <div className="w-8 h-8 rounded-full bg-blue-500/20 flex items-center justify-center">
                <Sparkles className="w-4 h-4 text-blue-400" />
              </div>
              <div className="flex-1">
                <p className="text-slate-300">Unlocked "Nebula Effect"</p>
                <p className="text-xs text-slate-500">2 days ago</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
