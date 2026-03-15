import { Sparkles, ShoppingCart } from "lucide-react";

interface GalaxyViewProps {
  coinBalance: number;
  planets: Array<{ id: string; name: string; type: string; size: number; position: { x: number; y: number } }>;
}

const getVisuals = (type: string) => {
  const visuals: Record<string, any> = {
    sun: { gradient: 'from-yellow-300 via-orange-400 to-yellow-500', glow: 'rgba(251, 191, 36, 0.6)' },
    mercury: { gradient: 'from-gray-400 via-slate-300 to-gray-500', glow: 'rgba(148, 163, 184, 0.3)' },
    venus: { gradient: 'from-yellow-300 via-orange-300 to-amber-400', glow: 'rgba(251, 146, 60, 0.4)' },
    earth: { gradient: 'from-blue-400 via-green-400 to-blue-500', glow: 'rgba(59, 130, 246, 0.4)', hasClouds: true },
    mars: { gradient: 'from-red-400 via-orange-500 to-red-600', glow: 'rgba(239, 68, 68, 0.4)' },
    jupiter: { gradient: 'from-orange-300 via-amber-200 to-orange-400', glow: 'rgba(251, 191, 36, 0.4)', hasClouds: true },
    saturn: { gradient: 'from-yellow-200 via-amber-300 to-yellow-400', glow: 'rgba(251, 191, 36, 0.5)', hasRings: true },
    neptune: { gradient: 'from-blue-500 via-cyan-400 to-blue-600', glow: 'rgba(6, 182, 212, 0.4)' },
    nebula: { gradient: 'from-purple-500 via-fuchsia-400 to-pink-500', glow: 'rgba(168, 85, 247, 0.4)', isNebula: true }
  };
  return visuals[type] || visuals.earth;
};

export function GalaxyView({ coinBalance, planets }: GalaxyViewProps) {
  return (
      <div className="relative w-full h-full flex flex-col bg-slate-950">

        <div className="bg-slate-950/80 backdrop-blur-sm px-6 py-4 flex items-center justify-between border-b border-slate-800/50 relative z-30">
          <div className="flex-1" />
          <h1 className="text-2xl text-white font-sans tracking-wide">MY GALAXY</h1>
          <div className="flex-1 flex justify-end">
            <div className="bg-yellow-500/20 border border-yellow-500/30 rounded-full px-5 py-2 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-yellow-400" />
              <span className="text-lg text-yellow-300">{coinBalance}</span>
              <span className="text-sm text-yellow-400 hidden sm:inline">Coins</span>
            </div>
          </div>
        </div>

        <div className="flex-1 relative overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-b from-slate-950 via-indigo-950 to-slate-950 z-0">
            {[...Array(150)].map((_, i) => (
                <div
                    key={i}
                    className="absolute bg-white rounded-full animate-pulse"
                    style={{
                      width: `${Math.random() * 2 + 0.5}px`,
                      height: `${Math.random() * 2 + 0.5}px`,
                      top: `${Math.random() * 100}%`,
                      left: `${Math.random() * 100}%`,
                      animationDelay: `${Math.random() * 3}s`,
                      opacity: Math.random() * 0.8 + 0.2,
                    }}
                />
            ))}
          </div>

          <div className="absolute top-20 right-1/4 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '8s' }} />
          <div className="absolute bottom-1/4 left-1/3 w-[500px] h-[500px] bg-blue-600/10 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '10s' }} />

          {/* Message when the galaxy is empty */}
          {planets.length === 0 && (
              <div className="absolute inset-0 flex items-center justify-center z-20 pointer-events-none">
                <div className="bg-slate-900/80 backdrop-blur-md border border-slate-700 p-8 rounded-2xl text-center max-w-sm mt-32">
                  <div className="bg-blue-500/20 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border border-blue-500/30">
                    <ShoppingCart className="w-8 h-8 text-blue-400" />
                  </div>
                  <h2 className="text-xl font-bold text-white mb-2">Your Galaxy is Empty!</h2>
                  <p className="text-slate-400 mb-2">
                    Complete tasks to earn coins, then visit the Shop to buy objects and build your solar system from scratch.
                  </p>
                </div>
              </div>
          )}

          {/* The area where all the stars are */}
          <div className="absolute inset-0 z-10 overflow-x-auto overflow-y-hidden">
            <div className="relative min-w-[800px] h-full">

              {/* Orbital Plane */}
              {planets.some(p => p.type !== 'sun' && p.type !== 'nebula') && (
                  <div className="absolute top-1/2 left-0 right-0 h-[1px] bg-slate-700/50 z-0" />
              )}

              {/* All the stars (and the sun) in one smart routine */}
              {planets.map((planet) => {
                const visuals = getVisuals(planet.type);
                const isSun = planet.type === 'sun';

                const positionStyle = isSun
                    ? { left: '-100px', top: '50%' }
                    : { left: `${planet.position.x}%`, top: `${planet.position.y}%` };

                const currentSize = isSun ? 250 : planet.size;

                return (
                    <div
                        key={planet.id}
                        className={`absolute flex flex-col items-center justify-center transform -translate-y-1/2 group ${isSun ? '' : '-translate-x-1/2'}`}
                        style={positionStyle}
                    >
                      <div className="relative mb-2 cursor-pointer transition-transform duration-300 group-hover:scale-105 z-10">
                        {visuals.isNebula ? (
                            <div
                                className={`rounded-full bg-gradient-to-br ${visuals.gradient} blur-md opacity-80 animate-pulse`}
                                style={{ width: currentSize, height: currentSize, animationDuration: '4s' }}
                            />
                        ) : visuals.hasRings ? (
                            <div className="relative">
                              <div
                                  className={`rounded-full bg-gradient-to-br ${visuals.gradient} shadow-2xl`}
                                  style={{ width: currentSize, height: currentSize, boxShadow: `0 0 30px ${visuals.glow}, inset -8px -8px 15px rgba(0,0,0,0.3)` }}
                              />
                              <div
                                  className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 border-4 border-amber-300/40 rounded-full"
                                  style={{
                                    width: currentSize * 2,
                                    height: currentSize * 0.4,
                                    borderTopColor: 'transparent',
                                    borderBottomColor: 'transparent',
                                    boxShadow: '0 0 15px rgba(251, 191, 36, 0.3)',
                                  }}
                              />
                            </div>
                        ) : (
                            <div
                                className={`rounded-full bg-gradient-to-br ${visuals.gradient} shadow-2xl ${isSun ? 'animate-pulse' : ''}`}
                                style={{
                                  width: currentSize,
                                  height: currentSize,
                                  boxShadow: `0 0 ${isSun ? '80px' : '30px'} ${visuals.glow}, inset -${currentSize/5}px -${currentSize/5}px ${currentSize/4}px rgba(0, 0, 0, 0.3)`,
                                  animationDuration: isSun ? '4s' : 'auto'
                                }}
                            >
                              {visuals.hasClouds && (
                                  <div
                                      className="absolute inset-0 rounded-full"
                                      style={{ background: 'radial-gradient(circle at 30% 30%, rgba(255,255,255,0.3) 0%, transparent 50%)' }}
                                  />
                              )}
                            </div>
                        )}

                        <div
                            className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 rounded-full opacity-30 blur-xl -z-10 pointer-events-none"
                            style={{ width: currentSize * 1.5, height: currentSize * 1.5, background: visuals.glow }}
                        />
                      </div>

                      <div
                          className="absolute text-center bg-slate-900/60 px-3 py-1 rounded-lg border border-slate-700/50 backdrop-blur-sm whitespace-nowrap z-50 pointer-events-none"
                          style={{
                            top: 'calc(100% + 8px)',
                            left: '50%',
                            transform: isSun ? 'none' : 'translateX(-50%)'
                          }}
                      >
                        <span className="text-white font-sans text-xs tracking-wide">{planet.name}</span>
                      </div>
                    </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
  );
}