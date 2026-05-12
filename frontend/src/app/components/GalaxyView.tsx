import { Sparkles, ShoppingCart } from "lucide-react";

const IMAGE_BASE_URL = "";

interface GalaxyViewProps {
  coinBalance: number;
  planets: Array<{
    id: string;
    name: string;
    type: string;
    size: number;
    position: { x: number; y: number };
    imageUrl?: string;
  }>;
}

const getVisuals = (type: string) => {
  const visuals: Record<string, any> = {
    sun: { glow: 'rgba(251, 191, 36, 0.6)', gradient: 'from-yellow-300 to-orange-500' },
    star: { glow: 'rgba(251, 191, 36, 0.6)', gradient: 'from-yellow-300 to-orange-500' },
    mercury: { glow: 'rgba(148, 163, 184, 0.3)', gradient: 'from-gray-400 to-gray-600' },
    venus: { glow: 'rgba(251, 146, 60, 0.4)', gradient: 'from-orange-300 to-amber-500' },
    earth: { glow: 'rgba(59, 130, 246, 0.4)', gradient: 'from-blue-400 to-blue-600' },
    planet: { glow: 'rgba(59, 130, 246, 0.4)', gradient: 'from-blue-400 to-blue-600' },
    mars: { glow: 'rgba(239, 68, 68, 0.4)', gradient: 'from-red-400 to-red-600' },
    jupiter: { glow: 'rgba(251, 191, 36, 0.4)', gradient: 'from-orange-200 to-orange-400' },
    saturn: { glow: 'rgba(251, 191, 36, 0.5)', gradient: 'from-yellow-200 to-yellow-400' },
    neptune: { glow: 'rgba(6, 182, 212, 0.4)', gradient: 'from-blue-500 to-cyan-500' },
    nebula: { glow: 'rgba(168, 85, 247, 0.4)', gradient: 'from-purple-500 to-pink-500' }
  };
  return visuals[type?.toLowerCase()] || { glow: 'rgba(59, 130, 246, 0.4)', gradient: 'from-slate-400 to-slate-600' };
};

export function GalaxyView({ coinBalance, planets }: GalaxyViewProps) {
  return (
      <div className="relative w-full h-full flex flex-col bg-slate-950">
        {/* Header */}
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
          {/* Space Background */}
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

          {/* Empty State */}
          {planets.length === 0 && (
              <div className="absolute inset-0 flex items-center justify-center z-20 pointer-events-none">
                <div className="bg-slate-900/80 backdrop-blur-md border border-slate-700 p-8 rounded-2xl text-center max-w-sm mt-32">
                  <div className="bg-blue-500/20 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4 border border-blue-500/30">
                    <ShoppingCart className="w-8 h-8 text-blue-400" />
                  </div>
                  <h2 className="text-xl font-bold text-white mb-2">Your Galaxy is Empty!</h2>
                  <p className="text-slate-400 mb-2">
                    Complete tasks to earn coins, then visit the Shop to build your system.
                  </p>
                </div>
              </div>
          )}

          {/* Planets Area */}
          <div className="absolute inset-0 z-10 overflow-x-auto overflow-y-hidden">
            <div className="relative min-w-[800px] h-full">

              {/* Orbital Plane */}
              {planets.length > 0 && (
                  <div className="absolute top-1/2 left-0 right-0 h-[1px] bg-slate-700/50 z-0" />
              )}

              {planets.map((planet) => {
                const visuals = getVisuals(planet.type);
                const isSun = planet.type.toLowerCase() === 'sun' || planet.type.toLowerCase() === 'star';
                const currentSize = planet.size;

                const layerIndex = planet.type.toLowerCase() === 'planet' ? 20 : 10;

                return (
                    <div
                        key={planet.id}
                        className="absolute flex flex-col items-center justify-center transform -translate-x-1/2 -translate-y-1/2 group"
                        style={{ left: `${planet.position.x}%`, top: `${planet.position.y}%`, zIndex: layerIndex }}
                    >
                      <div className="relative mb-2 cursor-pointer transition-transform duration-300 group-hover:scale-150 z-10">

                        {/* Render Image if exists, else render CSS Circle */}
                        {planet.imageUrl ? (
                            <div className="relative flex items-center justify-center" style={{ width: currentSize, height: currentSize }}>
                              <img
                                  src={`${IMAGE_BASE_URL}${planet.imageUrl}`}
                                  alt={planet.name}
                                  className={`w-full h-full object-contain relative z-20 scale-150`}
                                  style={{
                                    filter: `drop-shadow(0px 0px ${isSun ? '40px' : '20px'} ${visuals.glow})`
                                  }}
                                  onError={(e) => {
                                    (e.target as HTMLImageElement).style.display = 'none';
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
                                }}
                            />
                        )}

                        {/* Background Glow Layer */}
                        <div
                            className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 rounded-full opacity-30 blur-xl -z-10 pointer-events-none"
                            style={{ width: currentSize * 1.5, height: currentSize * 1.5, background: visuals.glow }}
                        />
                      </div>

                      {/* Name Label */}
                      <div
                          className="absolute text-center bg-slate-900/60 px-3 py-1 rounded-lg border border-slate-700/50 backdrop-blur-sm whitespace-nowrap z-50 pointer-events-none"
                          style={{
                            top: 'calc(85% + 8px)',
                            left: '50%',
                            transform: 'translateX(-50%)'
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