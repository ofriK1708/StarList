import { Sparkles, ShoppingCart } from "lucide-react";
import { useMemo } from "react";

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

type Planet = GalaxyViewProps['planets'][0];

// ─── visuals ─────────────────────────────────────────────────────────────────

const GLOW_MAP: Record<string, string> = {
  sun:     'rgba(251,191,36,0.75)', star:    'rgba(251,191,36,0.75)',
  mercury: 'rgba(148,163,184,0.4)', venus:   'rgba(251,146,60,0.55)',
  earth:   'rgba(59,130,246,0.55)', planet:  'rgba(59,130,246,0.55)',
  mars:    'rgba(239,68,68,0.55)',  jupiter: 'rgba(251,191,36,0.55)',
  saturn:  'rgba(251,191,36,0.6)',  neptune: 'rgba(6,182,212,0.55)',
  nebula:  'rgba(168,85,247,0.55)',
};

const GRAD_MAP: Record<string, string> = {
  sun:     'from-yellow-300 to-orange-500', star:    'from-yellow-300 to-orange-500',
  mercury: 'from-gray-400 to-gray-600',     venus:   'from-orange-300 to-amber-500',
  earth:   'from-blue-400 to-blue-600',     planet:  'from-blue-400 to-blue-600',
  mars:    'from-red-400 to-red-600',       jupiter: 'from-orange-200 to-orange-400',
  saturn:  'from-yellow-200 to-yellow-400', neptune: 'from-blue-500 to-cyan-500',
  nebula:  'from-purple-500 to-pink-500',
};

const getGlow = (type: string) => GLOW_MAP[type?.toLowerCase()] ?? 'rgba(59,130,246,0.4)';
const getGrad = (type: string) => GRAD_MAP[type?.toLowerCase()] ?? 'from-slate-400 to-slate-600';

// ─── orbital geometry ─────────────────────────────────────────────────────────

// [rx, ry] — slightly elliptical rings for a tilted-orbit perspective
const ORBIT_RINGS: [number, number][] = [
  [ 90,  80],  // inner
  [150, 130],  // middle
  [200, 175],  // outer
];

// Central star radius per system — edit index 0 to resize star1, etc.
const SYSTEM_STAR_R: [number, number, number] = [38, 52, 52];
const ORBIT_R: [number, number, number] = [30, 25, 20]; // planet radii per orbit level

const PLACEMENT_OVERRIDES: Record<string, { system: 0|1|2; orbit: 0|1|2 }> = {
};

// Extra angle offset per planet (radians, lowercase keys). Positive = clockwise / rightward.
const ANGLE_OVERRIDES: Record<string, number> = {
  'planet 13': 1.2,
  'planet 15': 1.2,
};

// ─── data distribution ────────────────────────────────────────────────────────

interface SystemData {
  star: Planet | null;
  orbits: [Planet[], Planet[], Planet[]];
}

const FALLBACK_STAR: Planet = {
  id: '__fallback__', name: '', type: 'star',
  size: 84, position: { x: 50, y: 50 },
};

function buildSystems(planets: Planet[]): [SystemData, SystemData, SystemData] {
  const isStar = (p: Planet) => ['sun', 'star'].includes(p.type.toLowerCase());
  const stars = planets.filter(isStar);
  const rest  = planets.filter(p => !isStar(p));

  const systems: SystemData[] = [0, 1, 2].map(i => ({
    star: stars[i] ?? null,
    orbits: [[], [], []] as [Planet[], Planet[], Planet[]],
  }));

  // Pinned planets go to their designated system+orbit first.
  const pinned = new Set<string>();
  rest.forEach(p => {
    const override = PLACEMENT_OVERRIDES[p.name.toLowerCase()];
    if (override) {
      systems[override.system].orbits[override.orbit].push(p);
      pinned.add(p.id);
    }
  });

  // Round-robin the remaining planets across the three systems.
  // Within each system: first 2 go to inner orbit, next 3 to middle, rest outer.
  const buckets: Planet[][] = [[], [], []];
  rest.filter(p => !pinned.has(p.id)).forEach((p, i) => buckets[i % 3].push(p));

  buckets.forEach((group, si) => {
    group.forEach((p, pi) => {
      const oi: 0 | 1 | 2 = pi < 2 ? 0 : pi < 5 ? 1 : 2;
      systems[si].orbits[oi].push(p);
    });
  });

  return [systems[0], systems[1], systems[2]];
}

function orbitXY(rx: number, ry: number, idx: number, total: number, startAngle: number, extraOffset = 0) {
  const angle = startAngle + (idx / Math.max(total, 1)) * 2 * Math.PI + extraOffset;
  return {
    x: Math.round(Math.cos(angle) * rx),
    y: Math.round(Math.sin(angle) * ry),
  };
}

// Each orbit ring gets a distinct starting angle so planets don't all cluster at the top.
// Offset varies by both orbit index and system index for maximum spread.
function orbitStartAngle(systemIndex: number, orbitIndex: number) {
  return -Math.PI / 2 + systemIndex * 1.3 + orbitIndex * 2.1;
}

// ─── sub-components ───────────────────────────────────────────────────────────

interface CelestialProps {
  planet: Planet;
  r: number;
  cx?: number;
  cy?: number;
  isStar?: boolean;
}

function Celestial({ planet, r, cx = 0, cy = 0, isStar = false }: CelestialProps) {
  const glowColor = getGlow(planet.type);
  const grad      = getGrad(planet.type);
  const d         = r * 2;

  return (
    <div
      className="absolute group"
      style={{
        left: `calc(50% + ${cx}px)`,
        top:  `calc(50% + ${cy}px)`,
        transform: 'translate(-50%, -50%)',
        zIndex: isStar ? 20 : 10,
      }}
    >
      <div
        className="relative cursor-pointer transition-transform duration-300 hover:scale-110"
        style={{ width: d, height: d }}
      >
        {planet.imageUrl ? (
          <img
            src={`${IMAGE_BASE_URL}${planet.imageUrl}`}
            alt={planet.name}
            className="w-full h-full object-contain"
            style={{ filter: `drop-shadow(0 0 ${isStar ? 28 : 12}px ${glowColor})` }}
            onError={e => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
        ) : (
          <div
            className={`w-full h-full rounded-full bg-gradient-to-br ${grad} ${isStar ? 'animate-pulse' : ''}`}
            style={{
              boxShadow: isStar
                ? `0 0 ${r * 1.6}px ${glowColor}, 0 0 ${r * 3}px ${glowColor.replace(/[\d.]+\)$/, '0.3)')}`
                : `0 0 ${r}px ${glowColor}, inset -${r / 5}px -${r / 5}px ${r / 4}px rgba(0,0,0,0.3)`,
            }}
          />
        )}
        {/* ambient halo */}
        <div
          className="absolute inset-0 rounded-full -z-10 blur-xl pointer-events-none"
          style={{ transform: 'scale(2.8)', background: glowColor, opacity: 0.15 }}
        />
      </div>

      {/* name tooltip on hover */}
      {planet.name && (
        <div
          className="absolute whitespace-nowrap bg-slate-900/85 px-2 py-0.5 rounded-md border border-slate-700/50 backdrop-blur-sm pointer-events-none opacity-0 group-hover:opacity-100 transition-opacity duration-200 text-white text-[10px] tracking-wide"
          style={{ top: d + 4, left: '50%', transform: 'translateX(-50%)' }}
        >
          {planet.name}
        </div>
      )}
    </div>
  );
}

// Deterministic per-system mini-starfield (avoids Math.random on every render)
function SystemStars({ seed }: { seed: number }) {
  const stars = useMemo(() =>
    Array.from({ length: 45 }, (_, i) => {
      const a = Math.sin(seed * 9301 + i * 49297 + 233) * 0.5 + 0.5;
      const b = Math.sin(seed * 49297 + i * 9301 + 751) * 0.5 + 0.5;
      const c = Math.sin(seed * 233 + i * 751 + 9301) * 0.5 + 0.5;
      const dd = Math.sin(seed * 751 + i * 233 + 49297) * 0.5 + 0.5;
      return { top: b * 100, left: a * 100, size: c * 1.6 + 0.4, opacity: dd * 0.55 + 0.2 };
    }),
    [seed],
  );

  return (
    <>
      {stars.map((s, i) => (
        <div
          key={i}
          className="absolute bg-white rounded-full animate-pulse pointer-events-none"
          style={{
            top: `${s.top}%`, left: `${s.left}%`,
            width: s.size, height: s.size,
            opacity: s.opacity,
            animationDelay: `${(i * 0.13) % 3}s`,
          }}
        />
      ))}
    </>
  );
}

interface SolarSystemProps {
  system: SystemData;
  index: number;
  starR: number;
}

function SolarSystem({ system, index, starR }: SolarSystemProps) {
  const star = system.star ?? FALLBACK_STAR;

  return (
    <div className="relative w-full h-full">
      <SystemStars seed={index + 1} />

      {/* Orbital rings */}
      <svg
        className="absolute inset-0 w-full h-full pointer-events-none"
        style={{ overflow: 'visible' }}
      >
        {ORBIT_RINGS.map(([rx, ry], oi) => (
          <ellipse
            key={oi}
            cx="50%" cy="50%"
            rx={rx} ry={ry}
            fill="none"
            stroke={system.orbits[oi].length > 0
              ? 'rgba(148,163,184,0.20)'
              : 'rgba(148,163,184,0.07)'}
            strokeWidth="1.5"
            strokeDasharray={oi === 0 ? '3 6' : oi === 1 ? '4 8' : '6 11'}
          />
        ))}
      </svg>

      {/* Planets on orbits */}
      {system.orbits.map((orbitPlanets, oi) =>
        orbitPlanets.map((planet, pi) => {
          const [rx, ry] = ORBIT_RINGS[oi];
          const { x, y } = orbitXY(rx, ry, pi, orbitPlanets.length, orbitStartAngle(index, oi), ANGLE_OVERRIDES[planet.name.toLowerCase()] ?? 0);
          return (
            <Celestial key={planet.id} planet={planet} r={ORBIT_R[oi]} cx={x} cy={y} />
          );
        })
      )}

      {/* Central star */}
      <Celestial planet={star} r={starR} isStar />
    </div>
  );
}

// ─── main export ─────────────────────────────────────────────────────────────

export function GalaxyView({ coinBalance, planets }: GalaxyViewProps) {
  const systems = useMemo(() => buildSystems(planets), [planets]);

  return (
    <div
      className="relative w-full h-full overflow-hidden border border-slate-700/60 rounded-lg"
      style={{ background: 'linear-gradient(to bottom, #020617, #1e1b4b 40%, #020617)' }}
    >
      {/* Header — floats over the space content */}
      <div
        className="absolute top-0 left-0 right-0 z-30 px-6 py-4 flex items-center justify-between"
      >
        <div className="flex-1" />
        <h1 className="text-2xl text-white font-sans tracking-wide drop-shadow-lg">MY GALAXY</h1>
        <div className="flex-1 flex justify-end">
          <div className="bg-yellow-500/20 border border-yellow-500/30 rounded-full px-5 py-2 flex items-center gap-2 backdrop-blur-sm">
            <Sparkles className="w-4 h-4 text-yellow-400" />
            <span className="text-lg text-yellow-300">{coinBalance}</span>
            <span className="text-sm text-yellow-400 hidden sm:inline">Coins</span>
          </div>
        </div>
      </div>

      {/* Content area: scrollable vertically on small screens, fixed on wide */}
      <div className="w-full h-full overflow-y-auto xl:overflow-hidden [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">

        {/* Empty state */}
        {planets.length === 0 && (
          <div className="h-full flex items-center justify-center">
            <div className="bg-slate-900/80 backdrop-blur-md border border-slate-700 p-8 rounded-2xl text-center max-w-sm">
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

        {/* Three solar systems */}
        {planets.length > 0 && (
          <div className="flex flex-col xl:flex-row xl:h-full">
            {systems.map((system, i) => (
              <div key={i} className="relative h-[420px] xl:h-full xl:flex-1">
                <SolarSystem system={system} index={i} starR={SYSTEM_STAR_R[i]} />
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
