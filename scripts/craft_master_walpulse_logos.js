const fs = require('fs');
const path = require('path');
const { Resvg } = require(path.resolve(__dirname, '../../node_modules/@resvg/resvg-js'));

const outDir = path.resolve(__dirname, '../docs/images');

function renderSvg(svgString, pngFilename, svgFilename) {
  const svgPath = path.join(outDir, svgFilename);
  const pngPath = path.join(outDir, pngFilename);
  fs.writeFileSync(svgPath, svgString, 'utf8');

  const resvg = new Resvg(svgString, {
    fitTo: { mode: 'width', value: 1024 },
    font: { loadSystemFonts: true },
    shapeRendering: 2 // geometricPrecision
  });
  const pngData = resvg.render();
  fs.writeFileSync(pngPath, pngData.asPng());
  console.log(`Rendered: ${pngFilename} (${(pngData.asPng().length / 1024).toFixed(1)} KB)`);
}

// =============================================================================
// OPTION A: THE ORIGAMI PEREGRINE FALCON (1:1 Narwhal Minimalist Facet Style)
// =============================================================================
const svgOrigamiFalconMaster = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <clipPath id="squircle-badge-clip-a">
      <rect x="24" y="24" width="976" height="976" rx="220" />
    </clipPath>

    <!-- Planar Lighting Gradients -->
    <linearGradient id="charcoal-top" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#2d3340"/>
      <stop offset="100%" stop-color="#181a22"/>
    </linearGradient>
    <linearGradient id="charcoal-mid" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#222731"/>
      <stop offset="100%" stop-color="#12141a"/>
    </linearGradient>
    <linearGradient id="charcoal-dark" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#161820"/>
      <stop offset="100%" stop-color="#0a0b0f"/>
    </linearGradient>

    <linearGradient id="cyan-electric" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#00f5ff"/>
      <stop offset="100%" stop-color="#00b4d8"/>
    </linearGradient>
    <linearGradient id="cyan-glow" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#38bdf8"/>
      <stop offset="100%" stop-color="#0284c7"/>
    </linearGradient>
    <linearGradient id="cyan-deep" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0096c7"/>
      <stop offset="100%" stop-color="#03045e"/>
    </linearGradient>

    <linearGradient id="gold-culmen" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#fde047"/>
      <stop offset="100%" stop-color="#f59e0b"/>
    </linearGradient>
    <linearGradient id="gold-hook" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#f59e0b"/>
      <stop offset="100%" stop-color="#b45309"/>
    </linearGradient>
  </defs>

  <!-- Luxury White Squircle Container -->
  <rect x="24" y="24" width="976" height="976" rx="220" fill="#ffffff" stroke="#e2e8f0" stroke-width="6" />

  <g clip-path="url(#squircle-badge-clip-a)">
    <!-- Falcon Head Group, positioned and scaled to match Narwhal placement -->
    <g transform="translate(520, 520) scale(1.06) translate(-500, -500)">

      <!-- Bold Outer Contour/Backdrop -->
      <polygon points="
        240,320 440,240 580,270 700,380 790,480 770,550 710,540 650,510 590,620 500,740 400,760 320,680 230,550 180,420
      " fill="#090b10" stroke="#090b10" stroke-width="20" stroke-linejoin="round" />

      <!-- FACET 1: Crown Forehead (Charcoal Top) -->
      <polygon points="440,240 580,270 540,370 410,350" 
        fill="url(#charcoal-top)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 2: Rear Crest Blade (Charcoal Dark) -->
      <polygon points="240,320 440,240 410,350 280,410" 
        fill="url(#charcoal-dark)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 3: Upper Beak Culmen (Gold Bright) -->
      <polygon points="580,270 700,380 640,420 540,370" 
        fill="url(#gold-culmen)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 4: Raptor Hook Tip (Curving Downward Razor Hook) -->
      <polygon points="700,380 790,480 750,530 690,470 640,420" 
        fill="url(#gold-hook)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 5: Lower Beak & Tomial Notch -->
      <polygon points="750,530 770,550 710,540 690,470" 
        fill="#92400e" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 6: Cere / Bridge Accent (Electric Cyan Glow) -->
      <polygon points="540,370 640,420 605,465 520,420" 
        fill="url(#cyan-glow)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 7: Ocular Brow & Predatory Eye Socket (Matte Black) -->
      <polygon points="410,350 540,370 520,420 460,455 380,425" 
        fill="url(#charcoal-dark)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- THE FALCON EYE: Fierce Glowing Cyan Diamond -->
      <polygon points="455,420 515,425 485,455 440,440" 
        fill="#ffffff" stroke="#090b10" stroke-width="8" stroke-linejoin="round" />
      <polygon points="450,423 510,427 480,452" 
        fill="#00ffff" />
      <polygon points="470,428 498,432 482,444" 
        fill="#090b10" />

      <!-- FACET 8: Malar Stripe (Peregrine Mustache - Jet Black) -->
      <polygon points="520,420 605,465 570,550 490,540 460,455" 
        fill="url(#charcoal-dark)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 9: Cheek Auricular Plate (Ice White/Cyan Contrast) -->
      <polygon points="380,425 460,455 490,540 420,530 350,490" 
        fill="#f0f9ff" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 10: Throat & Under-Beak Jaw (Electric Cyan Vivid) -->
      <polygon points="605,465 690,470 710,540 650,510 570,550" 
        fill="url(#cyan-electric)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 11: Jugulum / Fore-Throat (Electric Cyan Vivid) -->
      <polygon points="570,550 650,510 590,620 510,610 490,540" 
        fill="url(#cyan-electric)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 12: Mid Nape Feather Blade (Electric Cyan Vivid) -->
      <polygon points="280,410 410,350 380,425 350,490 230,480" 
        fill="url(#cyan-electric)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 13: Lower Nape Blade (Charcoal Mid) -->
      <polygon points="240,320 280,410 230,480 180,420" 
        fill="url(#charcoal-mid)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 14: Flank Fold Plate (Cyan Deep) -->
      <polygon points="230,480 350,490 320,590 230,550" 
        fill="url(#cyan-deep)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 15: Lower Breast Shield (Charcoal Mid) -->
      <polygon points="350,490 420,530 390,640 320,590" 
        fill="url(#charcoal-mid)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 16: Chest Keel Center (Charcoal Top) -->
      <polygon points="420,530 490,540 510,610 470,690 390,640" 
        fill="url(#charcoal-top)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 17: Lower Throat Armor (Cyan Deep) -->
      <polygon points="510,610 590,620 500,740 470,690" 
        fill="url(#cyan-deep)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

      <!-- FACET 18: Breast Base Wing Cap (Charcoal Dark) -->
      <polygon points="320,590 390,640 470,690 500,740 400,760 320,680 230,550" 
        fill="url(#charcoal-dark)" stroke="#090b10" stroke-width="11" stroke-linejoin="round" />

    </g>
  </g>
</svg>`;

// =============================================================================
// OPTION B: THE CIRCULAR DAG ORBIT FALCON (1:1 Whale Pulse Emblem Style)
// =============================================================================
const svgDagOrbitFalconMaster = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" width="1024" height="1024">
  <defs>
    <clipPath id="squircle-badge-clip-b">
      <rect x="24" y="24" width="976" height="976" rx="220" />
    </clipPath>
    <linearGradient id="beak-gold-grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#fde047"/>
      <stop offset="100%" stop-color="#f59e0b"/>
    </linearGradient>
  </defs>

  <!-- Luxury White Squircle Container -->
  <rect x="24" y="24" width="976" height="976" rx="220" fill="#ffffff" stroke="#e2e8f0" stroke-width="6" />

  <g clip-path="url(#squircle-badge-clip-b)">

    <!-- Center = (512, 512), Radius = 260 -->
    <!-- The Outer Black Circle (Broken cleanly at bottom left for pulse) -->
    <path d="
      M 270 660
      A 260 260 0 1 1 750 620
    " fill="none" stroke="#18181b" stroke-width="26" stroke-linecap="round" />

    <!-- The DAG Workflow Pulse Line in Electric Cyan -->
    <!-- Bursting from the circle break, spiking in DAG rhythm, and swooping up the inner right boundary -->
    <path d="
      M 252 660 
      L 315 660 
      L 338 615 
      L 366 710 
      L 404 535 
      L 438 775 
      L 472 660 
      L 520 660
      C 615 660, 722 610, 744 465
    " fill="none" stroke="#00c8f8" stroke-width="20" stroke-linecap="round" stroke-linejoin="round" />

    <!-- DAG Waypoint Status Nodes on the Peaks -->
    <circle cx="404" cy="535" r="7" fill="#ffffff" stroke="#00c8f8" stroke-width="5" />
    <circle cx="438" cy="775" r="7" fill="#ffffff" stroke="#00c8f8" stroke-width="5" />

    <!-- THE PEREGRINE FALCON (High-Precision Aerodynamic Silhouette) -->
    <g id="falcon-silhouette" transform="translate(10, -5)">

      <!-- Main Body, Arched Scythe Wings & Fanned Tail (Solid Jet Black) -->
      <path d="
        M 255 490 
        C 285 440, 350 335, 475 295
        C 575 265, 680 285, 722 335
        C 742 360, 748 390, 728 415
        C 712 430, 685 435, 655 435
        C 615 435, 565 455, 525 495
        C 495 525, 470 575, 455 615
        C 445 605, 448 565, 460 525
        C 425 540, 380 545, 335 535
        C 290 525, 245 510, 215 530
        C 210 505, 230 490, 255 490
        Z
      " fill="#18181b" />

      <!-- Primary Upper Wing Blade (Crisp White Negative Space Curve) -->
      <path d="
        M 305 435
        C 365 345, 465 315, 555 315
        C 635 315, 685 345, 710 380
        C 675 355, 615 340, 545 340
        C 455 340, 375 375, 305 435
        Z
      " fill="#ffffff" />

      <!-- Secondary Wing Blade Vent (White Aerodynamic Stripe) -->
      <path d="
        M 355 465
        C 415 405, 495 390, 565 395
        C 515 410, 445 425, 385 470
        Z
      " fill="#ffffff" />

      <!-- Raptor Hooked Beak (Golden Amber Accent - Sharp predatory curve) -->
      <path d="
        M 722 335
        C 742 360, 748 390, 728 415
        C 718 425, 708 418, 698 405
        C 712 385, 716 365, 706 350
        Z
      " fill="url(#beak-gold-grad)" />

      <!-- Predatory Eye (White negative space + black pupil) -->
      <circle cx="680" cy="372" r="7" fill="#ffffff" />
      <circle cx="681" cy="372" r="4" fill="#18181b" />

      <!-- Malar Mustache Cutout (Falcon Field Mark) -->
      <path d="
        M 672 384
        L 656 425
        L 642 400
        Z
      " fill="#ffffff" />

      <!-- Keel / Belly Vent Line (White Curve Giving 3D Depth) -->
      <path d="
        M 630 435
        C 585 460, 540 495, 500 545
        C 520 515, 560 480, 610 450
        Z
      " fill="#ffffff" />

      <!-- Fanned Tail Feather Negative Space Accent -->
      <path d="
        M 215 530
        L 255 510
        L 280 525
        L 240 545
        Z
      " fill="#ffffff" opacity="0.35" />

      <!-- Wingtip Leading-Edge Speed Highlight (Electric Cyan) -->
      <path d="
        M 475 295
        C 525 280, 585 275, 635 285
        C 595 285, 545 292, 500 310
        Z
      " fill="#00c8f8" />

    </g>

  </g>
</svg>`;

renderSvg(svgOrigamiFalconMaster, 'falcon_master_a_origami.png', 'falcon_master_a_origami.svg');
renderSvg(svgDagOrbitFalconMaster, 'falcon_master_b_dag_orbit.png', 'falcon_master_b_dag_orbit.svg');
console.log('Master options re-rendered successfully!');
