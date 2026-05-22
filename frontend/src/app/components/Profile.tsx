import { User as UserIcon, Mail, Trophy, Sparkles, Crown, Settings, LogOut } from "lucide-react";
import { Button } from "./ui/button";
import { useUser } from "../../context/UserContext";

interface ProfileProps {
  user: {
    name: string;
    email: string;
    achievements: Array<{ id: string; name: string; icon: string; description: string; unlocked: boolean }>;
  };
  coinBalance: number;
}

export function Profile({ user, coinBalance }: ProfileProps) {
  const { logout } = useUser();

  return (
      <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 overflow-y-auto relative">
        {/* Header with gradient background */}
        <div className="relative h-48 bg-gradient-to-br from-purple-600 via-blue-600 to-indigo-600 overflow-hidden">

          {/* Logout Button in Top Right */}
          <button
              onClick={logout}
              className="absolute top-6 right-6 z-20 flex items-center gap-2 from-slate-900 hover:text-white hover:bg-white/10 px-3 py-2 rounded-lg transition-all duration-200"
              title="Disconnect from Galaxy"
          >
            <LogOut className="w-5 h-5" />
            <span className="text-base font-medium">Logout</span>
          </button>

          {/* Decorative stars */}
          {[...Array(20)].map((_, i) => (
              <div
                  key={i}
                  className="absolute w-1 h-1 bg-white rounded-full animate-pulse z-0"
                  style={{
                    top: `${Math.random() * 100}%`,
                    left: `${Math.random() * 100}%`,
                    animationDelay: `${Math.random() * 2}s`,
                    opacity: Math.random() * 0.7 + 0.3,
                  }}
              />
          ))}
        </div>

        <div className="relative px-6 -mt-16 z-10">
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

          {/* Coins */}
          <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 text-center mb-6">
            <div className="flex items-center justify-center gap-2 mb-2">
              <Sparkles className="w-5 h-5 text-yellow-400" />
              <span className="text-sm text-slate-400">Coins</span>
            </div>
            <p className="text-3xl text-white">{coinBalance}</p>
          </div>

          {/* Achievements */}
          <div className="bg-slate-800/60 border border-slate-700 rounded-lg p-4 pb-20">
            <h2 className="text-lg text-white/90 mb-4 flex items-center gap-2">
              <Trophy className="w-5 h-5 text-yellow-400" />
              Achievements
            </h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
              {user.achievements.map((achievement) => (
                  <div
                      key={achievement.id}
                      className={`aspect-square flex flex-col items-center justify-center gap-2 p-3 rounded-lg text-center ${
                          achievement.unlocked
                              ? 'bg-gradient-to-br from-yellow-500/20 to-orange-500/20 border border-yellow-500/40'
                              : 'bg-slate-700/30 border border-slate-600 opacity-40'
                      }`}
                  >
                    <span className="text-4xl">{achievement.icon}</span>
                    <p className="text-sm text-white font-medium leading-tight">{achievement.name}</p>
                    <p className="text-xs text-slate-400 leading-tight">{achievement.description}</p>
                  </div>
              ))}
            </div>
          </div>
        </div>
      </div>
  );
}
