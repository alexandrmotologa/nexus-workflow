const fs = require('fs');
const path = require('path');
const { Resvg } = require(path.resolve(__dirname, '../../node_modules/@resvg/resvg-js'));

const outDir = path.resolve(__dirname, '../docs/images');
if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir, { recursive: true });
}

// -----------------------------------------------------------------------------
// CONCEPT A: The Origami Peregrine Falcon & Hexagonal DAG Constellation
// -----------------------------------------------------------------------------
const svgConceptA = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <clipPath id="badge-clip-a">
      <rect x="24" y="24" width="976" height="976" rx="220" />
    </clipPath>
    <filter id="amber-pulse-a" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
    <linearGradient id="blue-facet" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#3b82f6" />
      <stop offset="100%" stop-color="#1d4ed8" />
    </linearGradient>
    <linearGradient id="cyan-facet" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#60a5fa" />
      <stop offset="100%" stop-color="#2563eb" />
    </linearGradient>
  </defs>

  <!-- Luxury White Squircle Container -->
  <rect x="24" y="24" width="976" height="976" rx="220" fill="#ffffff" stroke="#e2e8f0" stroke-width="16" />

  <g clip-path="url(#badge-clip-a)">
    <!-- Subtle Background DAG Grid Ticks -->
    <g stroke="#f1f5f9" stroke-width="2" stroke-dasharray="8,12">
      <line x1="200" y1="200" x2="200" y2="824" />
      <line x1="512" y1="150" x2="512" y2="874" />
      <line x1="824" y1="200" x2="824" y2="824" />
      <line x1="150" y1="512" x2="874" y2="512" />
    </g>

    <!-- Outer Hexagonal Gateway Frame (Hexagonal Architecture) -->
    <!-- Center (512, 512), R=340 -->
    <polygon points="806,682 512,852 218,682 218,342 512,172 806,342" 
             fill="none" stroke="#18181b" stroke-width="12" stroke-linejoin="round" />

    <!-- Secondary Dotted Inner Orbit -->
    <circle cx="512" cy="512" r="275" fill="none" stroke="#cbd5e1" stroke-width="3" stroke-dasharray="8,8" />

    <!-- Directed DAG Trajectory Paths between Nodes -->
    <!-- Path 1: Start -> Signal Node -->
    <path d="M 218 512 C 240 320, 360 210, 512 172" fill="none" stroke="#2563eb" stroke-width="8" stroke-linecap="round" />
    
    <!-- Path 2: Signal -> Completed Terminal Node -->
    <path d="M 512 172 C 670 172, 780 300, 806 512" fill="none" stroke="#10b981" stroke-width="8" stroke-linecap="round" />

    <!-- Path 3: Backward Saga Compensation Arc (Reverse Rollback) -->
    <path d="M 806 512 C 770 680, 650 780, 512 852 C 380 780, 260 680, 218 512" 
          fill="none" stroke="#ef4444" stroke-width="5" stroke-dasharray="10,8" opacity="0.65" />

    <!-- DAG Waypoint Nodes -->
    <!-- Node 1: Origin / Activity Ingest (Sapphire) -->
    <circle cx="218" cy="512" r="26" fill="#18181b" stroke="#ffffff" stroke-width="6" />
    <circle cx="218" cy="512" r="14" fill="#2563eb" />
    <circle cx="218" cy="512" r="6" fill="#ffffff" />

    <!-- Node 2: External Signal Waiting / Human-in-the-loop (Amber Pulse) -->
    <circle cx="512" cy="172" r="32" fill="none" stroke="#f59e0b" stroke-width="4" stroke-dasharray="6,4" opacity="0.8" />
    <circle cx="512" cy="172" r="26" fill="#18181b" stroke="#ffffff" stroke-width="6" />
    <circle cx="512" cy="172" r="14" fill="#f59e0b" filter="url(#amber-pulse-a)" />
    <circle cx="512" cy="172" r="6" fill="#ffffff" />

    <!-- Node 3: Completed Terminal State (Emerald) -->
    <circle cx="806" cy="512" r="26" fill="#18181b" stroke="#ffffff" stroke-width="6" />
    <circle cx="806" cy="512" r="14" fill="#10b981" />
    <circle cx="806" cy="512" r="6" fill="#ffffff" />

    <!-- Node 4: Saga Rollback Gate (Crimson Anchor) -->
    <circle cx="512" cy="852" r="20" fill="#18181b" stroke="#ffffff" stroke-width="5" />
    <circle cx="512" cy="852" r="10" fill="#ef4444" />

    <!-- ================================================================= -->
    <!-- CENTRAL MASCOT: The Origami Peregrine Falcon (Soaring Trajectory) -->
    <!-- ================================================================= -->
    <g transform="translate(512, 515)">
      <!-- Left Upper Wing (Faceted Low-Poly Origami) -->
      <polygon points="0,-75 -180,-190 -115,-40 -20,-15" fill="#18181b" />
      <polygon points="-180,-190 -230,-120 -115,-40" fill="#27272a" />
      <polygon points="-230,-120 -200,-30 -115,-40" fill="#3f3f46" />
      <polygon points="-200,-30 -130,25 -90,-10" fill="#18181b" />

      <!-- Left Wing Dynamic Color Accents (Sapphire & Cyan) -->
      <polygon points="-115,-40 -180,-190 -120,-110" fill="url(#blue-facet)" />
      <polygon points="-115,-40 -120,-110 -60,-65" fill="url(#cyan-facet)" />
      <polygon points="-115,-40 -60,-65 -20,-15" fill="#1d4ed8" />

      <!-- Right Upper Wing (High-Speed Swept Wing) -->
      <polygon points="0,-75 195,-225 125,-45 20,-15" fill="#18181b" />
      <polygon points="195,-225 245,-145 125,-45" fill="#27272a" />
      <polygon points="245,-145 205,-40 125,-45" fill="#3f3f46" />
      <polygon points="205,-40 135,30 95,-10" fill="#18181b" />

      <!-- Right Wing Dynamic Color Accents (Sapphire & Cyan) -->
      <polygon points="125,-45 195,-225 130,-125" fill="url(#blue-facet)" />
      <polygon points="125,-45 130,-125 65,-70" fill="url(#cyan-facet)" />
      <polygon points="125,-45 65,-70 20,-15" fill="#1d4ed8" />

      <!-- Central Dorsal Spine & Chest (Charcoal / Low-Poly) -->
      <polygon points="0,-75 -25,-15 0,110 25,-15" fill="#18181b" />
      <polygon points="0,-75 0,110 25,-15" fill="#27272a" />
      
      <!-- Negative Space Chevron Execution Arrow on Chest (>) -->
      <polygon points="-18,20 0,42 18,20 0,65" fill="#38bdf8" />
      <polygon points="-14,60 0,78 14,60 0,98" fill="#60a5fa" />

      <!-- Tail Feathers (Split Vector Rudder) -->
      <polygon points="0,110 -45,210 -15,160 0,140" fill="#18181b" />
      <polygon points="0,110 45,210 15,160 0,140" fill="#27272a" />
      <polygon points="0,140 -15,160 0,185 15,160" fill="#2563eb" />

      <!-- Falcon Head & Sharp Beak -->
      <polygon points="0,-75 -22,-120 0,-165 22,-120" fill="#18181b" />
      <polygon points="0,-75 0,-165 22,-120" fill="#27272a" />
      <polygon points="0,-165 0,-195 14,-155" fill="#f59e0b" />
      <polygon points="0,-165 0,-195 -14,-155" fill="#d97706" />

      <!-- Falcon Alert Eyes (Amber Signal Beacons) -->
      <circle cx="-10" cy="-135" r="4.5" fill="#f59e0b" />
      <circle cx="10" cy="-135" r="4.5" fill="#f59e0b" />
      <circle cx="-9" cy="-136" r="1.5" fill="#ffffff" />
      <circle cx="11" cy="-136" r="1.5" fill="#ffffff" />
    </g>
  </g>
</svg>`;

// -----------------------------------------------------------------------------
// CONCEPT B: The Hexagonal Dragonfly & 4-Way Virtual Thread Mesh
// -----------------------------------------------------------------------------
const svgConceptB = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <clipPath id="badge-clip-b">
      <rect x="24" y="24" width="976" height="976" rx="220" />
    </clipPath>
    <filter id="cyan-glow-b" x="-20%" y="-20%" width="140%" height="140%">
      <feGaussianBlur stdDeviation="8" result="blur" />
      <feComposite in="SourceGraphic" in2="blur" operator="over" />
    </filter>
    <linearGradient id="indigo-wing" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#6366f1" />
      <stop offset="100%" stop-color="#4338ca" />
    </linearGradient>
    <linearGradient id="cyan-wing" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#38bdf8" />
      <stop offset="100%" stop-color="#0284c7" />
    </linearGradient>
  </defs>

  <!-- Luxury White Squircle Container -->
  <rect x="24" y="24" width="976" height="976" rx="220" fill="#ffffff" stroke="#e2e8f0" stroke-width="16" />

  <g clip-path="url(#badge-clip-b)">
    <!-- Background Directed Acyclic Graph Lines (Subtle) -->
    <g stroke="#e2e8f0" stroke-width="2.5" stroke-dasharray="6,8">
      <circle cx="512" cy="512" r="320" fill="none" />
      <circle cx="512" cy="512" r="180" fill="none" />
      <line x1="512" y1="100" x2="512" y2="924" />
      <line x1="100" y1="512" x2="924" y2="512" />
      <line x1="220" y1="220" x2="804" y2="804" />
      <line x1="220" y1="804" x2="804" y2="220" />
    </g>

    <!-- Outer Concentric Hexagon Portals (Ports & Adapters) -->
    <polygon points="789,672 512,832 235,672 235,352 512,192 789,352" 
             fill="none" stroke="#18181b" stroke-width="11" stroke-linejoin="round" />

    <!-- 4 Parallel Execution Waypoint Hubs (Virtual Threads) -->
    <!-- Top-Left Hub -->
    <circle cx="280" cy="280" r="22" fill="#18181b" stroke="#ffffff" stroke-width="5" />
    <circle cx="280" cy="280" r="11" fill="#6366f1" />
    <!-- Top-Right Hub -->
    <circle cx="744" cy="280" r="22" fill="#18181b" stroke="#ffffff" stroke-width="5" />
    <circle cx="744" cy="280" r="11" fill="#6366f1" />
    <!-- Bottom-Left Hub -->
    <circle cx="280" cy="744" r="22" fill="#18181b" stroke="#ffffff" stroke-width="5" />
    <circle cx="280" cy="744" r="11" fill="#0284c7" />
    <!-- Bottom-Right Hub -->
    <circle cx="744" cy="744" r="22" fill="#18181b" stroke="#ffffff" stroke-width="5" />
    <circle cx="744" cy="744" r="11" fill="#10b981" />

    <!-- ================================================================= -->
    <!-- CENTRAL MASCOT: The Hexagonal Origami Dragonfly (Parallel Wings)  -->
    <!-- ================================================================= -->
    <g transform="translate(512, 500)">
      <!-- Top-Left Wing (Virtual Thread 1) -->
      <polygon points="-15,-30 -240,-160 -170,-45" fill="#18181b" />
      <polygon points="-240,-160 -310,-120 -170,-45" fill="url(#indigo-wing)" />
      <polygon points="-170,-45 -310,-120 -185,-15 -25,-10" fill="#27272a" />
      <polygon points="-170,-45 -185,-15 -100,-15" fill="url(#cyan-wing)" />

      <!-- Top-Right Wing (Virtual Thread 2) -->
      <polygon points="15,-30 240,-160 170,-45" fill="#27272a" />
      <polygon points="240,-160 310,-120 170,-45" fill="url(#indigo-wing)" />
      <polygon points="170,-45 310,-120 185,-15 25,-10" fill="#18181b" />
      <polygon points="170,-45 185,-15 100,-15" fill="url(#cyan-wing)" />

      <!-- Bottom-Left Wing (Virtual Thread 3 - Saga Rollback Arc) -->
      <polygon points="-15,10 -220,130 -140,40" fill="#18181b" />
      <polygon points="-220,130 -280,110 -140,40" fill="url(#cyan-wing)" />
      <polygon points="-140,40 -280,110 -150,15 -20,15" fill="#3f3f46" />

      <!-- Bottom-Right Wing (Virtual Thread 4) -->
      <polygon points="15,10 220,130 140,40" fill="#27272a" />
      <polygon points="220,130 280,110 140,40" fill="url(#cyan-wing)" />
      <polygon points="140,40 280,110 150,15 20,15" fill="#18181b" />

      <!-- Long Slender Abdomen / DAG Event Stream Spine -->
      <polygon points="0,-40 -16,-10 0,260 16,-10" fill="#18181b" />
      
      <!-- Segmentation Ticks on Abdomen (Immutable Events Log) -->
      <line x1="-12" y1="30" x2="12" y2="30" stroke="#38bdf8" stroke-width="3.5" />
      <line x1="-10" y1="70" x2="10" y2="70" stroke="#38bdf8" stroke-width="3.5" />
      <line x1="-8" y1="110" x2="8" y2="110" stroke="#38bdf8" stroke-width="3.5" />
      <line x1="-7" y1="150" x2="7" y2="150" stroke="#6366f1" stroke-width="3.5" />
      <line x1="-5" y1="190" x2="5" y2="190" stroke="#6366f1" stroke-width="3.5" />
      
      <!-- Tail Beacon (Signal Waiting Node at tip of Abdomen) -->
      <circle cx="0" cy="265" r="14" fill="#f59e0b" filter="url(#cyan-glow-b)" />
      <circle cx="0" cy="265" r="6" fill="#ffffff" />

      <!-- Thorax (Engine Hub Core) -->
      <polygon points="0,-60 -24,-25 0,15 24,-25" fill="#2563eb" />
      <circle cx="0" cy="-22" r="8" fill="#ffffff" />

      <!-- Head & Dual Compound Eyes (360 Observability Dashboard) -->
      <polygon points="0,-60 -20,-85 0,-105 20,-85" fill="#18181b" />
      <circle cx="-16" cy="-85" r="12" fill="#18181b" stroke="#38bdf8" stroke-width="3" />
      <circle cx="-16" cy="-85" r="5" fill="#38bdf8" />
      <circle cx="16" cy="-85" r="12" fill="#18181b" stroke="#38bdf8" stroke-width="3" />
      <circle cx="16" cy="-85" r="5" fill="#38bdf8" />
    </g>
  </g>
</svg>`;

// Render both via resvg
console.log('Rendering Option A (Falcon)...');
const resvgA = new Resvg(svgConceptA, { fitTo: { mode: 'width', value: 1024 } });
const pngA = resvgA.render().asPng();
fs.writeFileSync(path.join(outDir, 'logo-falcon.png'), pngA);
fs.writeFileSync(path.join(outDir, 'logo-falcon.svg'), svgConceptA);

console.log('Rendering Option B (Dragonfly)...');
const resvgB = new Resvg(svgConceptB, { fitTo: { mode: 'width', value: 1024 } });
const pngB = resvgB.render().asPng();
fs.writeFileSync(path.join(outDir, 'logo-dragonfly.png'), pngB);
fs.writeFileSync(path.join(outDir, 'logo-dragonfly.svg'), svgConceptB);

// By default set Option A as official logo.png & logo.svg
fs.writeFileSync(path.join(outDir, 'logo.png'), pngA);
fs.writeFileSync(path.join(outDir, 'logo.svg'), svgConceptA);

console.log('Done! All assets written to:', outDir);
