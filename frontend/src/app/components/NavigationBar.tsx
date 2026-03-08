import { ListTodo, Globe, MessageSquare, User, ShoppingBag, BarChart3, Flame } from "lucide-react";

interface NavigationBarProps {
  currentScreen: 'tasks' | 'habits' | 'galaxy' | 'chat' | 'profile' | 'shop' | 'statistics' ;
  onNavigate: (screen: 'tasks' | 'habits' | 'galaxy' | 'chat' | 'profile' | 'shop' | 'statistics' ) => void;
}

export function NavigationBar({ currentScreen, onNavigate }: NavigationBarProps) {
  const navItems = [
    { id: 'tasks' as const, icon: ListTodo, label: 'Tasks' },
    { id: 'habits' as const, icon: Flame, label: 'Habits' },
    { id: 'chat' as const, icon: MessageSquare, label: 'Chat' },
    { id: 'galaxy' as const, icon: Globe, label: 'Galaxy' },
    { id: 'shop' as const, icon: ShoppingBag, label: 'Shop' },
    { id: 'statistics' as const, icon: BarChart3, label: 'Stats' },
    { id: 'profile' as const, icon: User, label: 'Profile' },

  ];

  return (
      <div className="bg-slate-900/95 backdrop-blur-sm border-t border-slate-700/50 px-4 py-3">
        <div className="flex items-center justify-around max-w-2xl mx-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = currentScreen === item.id;

            return (
                <button
                    key={item.id}
                    onClick={() => onNavigate(item.id)}
                    className={`flex flex-col items-center gap-1 px-4 py-2 rounded-lg transition-all ${
                        isActive
                            ? 'bg-blue-600/20 text-blue-400'
                            : 'text-slate-400 hover:text-slate-300 hover:bg-slate-800/50'
                    }`}
                >
                  <Icon className={`w-6 h-6 ${isActive ? 'scale-110' : ''} transition-transform`} />
                  <span className="text-xs">{item.label}</span>
                </button>
            );
          })}
        </div>
      </div>
  );
}