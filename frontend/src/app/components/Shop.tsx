import { Sparkles, Lock, ShoppingCart } from "lucide-react";
import { Button } from "./ui/button";

interface ShopItem {
  id: string;
  name: string;
  type: 'sun' | 'nebula' | 'earth' | 'jupiter' | 'mars' | 'saturn' | 'neptune' | 'venus' | 'mercury';
  price: number;
  description: string;
  unlocked: boolean;
}

interface ShopProps {
  items: ShopItem[];
  coinBalance: number;
  onPurchase: (itemId: string) => void;
}

const itemStyles: Record<string, { gradient: string; glow: string; effect?: React.ReactNode }> = {
  sun: {
    gradient: 'from-yellow-300 via-orange-400 to-yellow-500',
    glow: 'rgba(251, 191, 36, 0.7)',
  },
  earth: {
    gradient: 'from-blue-400 via-green-400 to-blue-500',
    glow: 'rgba(59, 130, 246, 0.5)',
  },
  jupiter: {
    gradient: 'from-orange-300 via-amber-200 to-orange-400',
    glow: 'rgba(251, 191, 36, 0.5)',
  },
  mars: {
    gradient: 'from-red-400 via-orange-500 to-red-600',
    glow: 'rgba(239, 68, 68, 0.5)',
  },
  saturn: {
    gradient: 'from-yellow-200 via-amber-300 to-yellow-400',
    glow: 'rgba(251, 191, 36, 0.5)',
  },
  neptune: {
    gradient: 'from-blue-500 via-cyan-400 to-blue-600',
    glow: 'rgba(6, 182, 212, 0.5)',
  },
  venus: {
    gradient: 'from-yellow-300 via-orange-300 to-amber-400',
    glow: 'rgba(251, 146, 60, 0.5)',
  },
  mercury: {
    gradient: 'from-gray-400 via-slate-300 to-gray-500',
    glow: 'rgba(148, 163, 184, 0.5)',
  },
};

export function Shop({ items, coinBalance, onPurchase }: ShopProps) {
  return (
      <div className="w-full h-full bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 overflow-y-auto">
        {/* Header */}
        <div className="sticky top-0 z-30 bg-slate-900/95 backdrop-blur-sm border-b border-slate-700/50 px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl text-white/90">Cosmic Shop</h1>
              <p className="text-sm text-slate-400">Unlock beautiful celestial objects</p>
            </div>
            <div className="bg-yellow-500/20 backdrop-blur-sm border border-yellow-500/30 rounded-full px-6 py-3 flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-yellow-400" />
              <span className="text-xl text-yellow-300">{coinBalance}</span>
              <span className="text-sm text-yellow-200/80">Coins</span>
            </div>
          </div>
        </div>

        {/* Shop Grid */}
        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {items.map((item) => {
              const canAfford = coinBalance >= item.price;
              const isUnlocked = item.unlocked;
              const style = itemStyles[item.type];

              return (
                  <div
                      key={item.id}
                      className={`relative group rounded-xl border overflow-hidden transition-all ${
                          isUnlocked
                              ? 'bg-green-900/20 border-green-700/50'
                              : 'bg-slate-800/60 border-slate-700 hover:border-slate-600'
                      }`}
                  >
                    {/* Image/Preview */}
                    <div
                        className={`relative h-56 flex items-center justify-center bg-gradient-to-br from-slate-900 to-slate-800 overflow-hidden ${
                            !isUnlocked && 'opacity-60'
                        }`}
                    >
                      {/* Background stars */}
                      <div className="absolute inset-0">
                        {[...Array(30)].map((_, i) => (
                            <div
                                key={i}
                                className="absolute bg-white rounded-full"
                                style={{
                                  width: `${Math.random() * 2}px`,
                                  height: `${Math.random() * 2}px`,
                                  top: `${Math.random() * 100}%`,
                                  left: `${Math.random() * 100}%`,
                                  opacity: Math.random() * 0.5 + 0.2,
                                }}
                            />
                        ))}
                      </div>

                      {/* Planet/Celestial Object */}
                      <div className="relative z-10">
                        {item.type === 'nebula' ? (
                            // ציור הערפילית בחנות - אפקט מטושטש וצבעוני
                            <div className="relative w-32 h-32">
                              <div className={`absolute inset-0 bg-gradient-to-br ${style.gradient} rounded-full blur-2xl opacity-80`} />
                              <div className={`absolute inset-4 bg-gradient-to-tr ${style.gradient} rounded-full blur-xl opacity-60`} />
                              <div className="absolute inset-8 bg-white/20 rounded-full blur-lg" />
                            </div>
                        ) : item.type === 'saturn' ? (
                            // Saturn with rings
                            <div className="relative">
                              <div
                                  className={`w-28 h-28 rounded-full bg-gradient-to-br ${style.gradient} shadow-2xl`}
                                  style={{
                                    boxShadow: `0 0 40px ${style.glow}, inset -10px -10px 20px rgba(0, 0, 0, 0.3)`,
                                  }}
                              />
                              <div
                                  className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 border-[6px] border-amber-300/40 rounded-full"
                                  style={{
                                    width: '160px',
                                    height: '45px',
                                    borderTopColor: 'transparent',
                                    borderBottomColor: 'transparent',
                                    boxShadow: '0 0 20px rgba(251, 191, 36, 0.3)',
                                  }}
                              />
                              <div
                                  className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 rounded-full opacity-30 blur-xl -z-10"
                                  style={{
                                    width: '140px',
                                    height: '140px',
                                    background: style.glow,
                                  }}
                              />
                            </div>
                        ) : (
                            // Regular planets
                            <div className="relative">
                              <div
                                  className={`w-28 h-28 rounded-full bg-gradient-to-br ${style.gradient} shadow-2xl ${item.type === 'sun' ? 'animate-pulse' : ''}`}
                                  style={{
                                    boxShadow: `0 0 40px ${style.glow}, inset -10px -10px 20px rgba(0, 0, 0, 0.3)`,
                                  }}
                              >
                                {/* Cloud effect for Earth and Jupiter */}
                                {(item.type === 'earth' || item.type === 'jupiter') && (
                                    <div
                                        className="absolute inset-0 rounded-full"
                                        style={{
                                          background:
                                              'radial-gradient(circle at 30% 30%, rgba(255,255,255,0.3) 0%, transparent 50%)',
                                        }}
                                    />
                                )}
                              </div>
                              {/* Glow effect */}
                              <div
                                  className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 rounded-full opacity-30 blur-xl -z-10"
                                  style={{
                                    width: '140px',
                                    height: '140px',
                                    background: style.glow,
                                  }}
                              />
                            </div>
                        )}
                      </div>

                      {/* התגית "NOT ENOUGH" המעוצבת שלנו */}
                      {!isUnlocked && !canAfford && (
                          <div className="absolute top-3 left-3 bg-red-500/20 border border-red-500/30 text-red-200 text-[10px] font-bold tracking-wider px-2 py-1 rounded-md flex items-center gap-1 backdrop-blur-md z-20">
                            <Lock className="w-3 h-3" />
                            NOT ENOUGH
                          </div>
                      )}

                      {isUnlocked && (
                          <div className="absolute top-3 right-3 bg-green-500/90 text-white px-3 py-1 rounded-full text-xs flex items-center gap-1 z-20">
                            <ShoppingCart className="w-3 h-3" />
                            Owned
                          </div>
                      )}

                      {!isUnlocked && !canAfford && (
                          <div className="absolute inset-0 bg-black/40 backdrop-blur-[2px] flex items-center justify-center z-10">
                            <Lock className="w-12 h-12 text-white/50" />
                          </div>
                      )}
                    </div>

                    {/* Content */}
                    <div className="p-4 space-y-3">
                      <div>
                        <div className="flex items-center justify-between mb-1">
                          <h3 className="text-lg text-white/90">{item.name}</h3>
                          <span className="text-xs text-slate-500 bg-slate-700/50 px-2 py-1 rounded capitalize">
                        {item.type}
                      </span>
                        </div>
                        <p className="text-sm text-slate-400">{item.description}</p>
                      </div>

                      <div className="flex items-center justify-between pt-2">
                        <div className="flex items-center gap-2">
                          <Sparkles className="w-4 h-4 text-yellow-400" />
                          <span className="text-lg text-yellow-300">{item.price}</span>
                          <span className="text-xs text-yellow-200/80">Coins</span>
                        </div>

                        {!isUnlocked && (
                            <Button
                                onClick={() => onPurchase(item.id)}
                                disabled={!canAfford}
                                className={`${
                                    canAfford
                                        ? 'bg-blue-600 hover:bg-blue-700 text-white'
                                        : 'bg-slate-700 text-slate-500 cursor-not-allowed'
                                }`}
                                size="sm"
                            >
                              {canAfford ? 'Unlock' : 'Not enough'}
                            </Button>
                        )}
                      </div>
                    </div>
                  </div>
              );
            })}
          </div>
        </div>
      </div>
  );
}