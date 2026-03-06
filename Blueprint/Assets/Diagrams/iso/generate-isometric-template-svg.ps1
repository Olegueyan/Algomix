$ErrorActionPreference = "Stop"

$outputDir = Join-Path $PSScriptRoot
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$outputPath = Join-Path $outputDir "cube-isometric-3x3.svg"

$svg = @"
<svg xmlns='http://www.w3.org/2000/svg' viewBox='64 60 292 316'>
  <style>
    :root {
      --bg:#f8fafc;
      --line:#0f172a;
      --arrow:#0284c7;
      --focus:#0ea5e9;

      --u1:#ffffff; --u2:#ffffff; --u3:#ffffff;
      --u4:#ffffff; --u5:#ffffff; --u6:#ffffff;
      --u7:#ffffff; --u8:#ffffff; --u9:#ffffff;

      --f1:#22c55e; --f2:#22c55e; --f3:#22c55e;
      --f4:#22c55e; --f5:#22c55e; --f6:#22c55e;
      --f7:#22c55e; --f8:#22c55e; --f9:#22c55e;

      --r1:#ef4444; --r2:#ef4444; --r3:#ef4444;
      --r4:#ef4444; --r5:#ef4444; --r6:#ef4444;
      --r7:#ef4444; --r8:#ef4444; --r9:#ef4444;
    }
    .sticker { stroke:var(--line); stroke-width:1.4; stroke-linejoin:round; }
    .face-outline { fill:none; stroke:var(--line); stroke-width:2.1; stroke-linejoin:round; }
    .focus-outline { fill:none; stroke:var(--focus); stroke-width:5.5; stroke-linejoin:round; }
    .arrow { fill:none; stroke:var(--arrow); stroke-width:5; marker-end:url(#head); stroke-linecap:round; }
    .move-pill { fill:#fff; stroke:var(--arrow); stroke-width:3; rx:12; }
    .move-text { font: 900 28px Verdana, sans-serif; fill:var(--arrow); }
  </style>
  <defs>
    <marker id='head' markerWidth='11' markerHeight='11' refX='9.5' refY='5.5' orient='auto'>
      <path d='M0,0 L11,5.5 L0,11 z' fill='var(--arrow)'/>
    </marker>
  </defs>

  <rect x='0' y='0' width='420' height='420' fill='var(--bg)'/>

  <g transform='translate(-140,-16)'>
  <g id='cube'>
    <polygon class='sticker' fill='var(--u1)' points='350,90 394,114 350,138 306,114'/>
    <polygon class='sticker' fill='var(--u2)' points='394,114 438,138 394,162 350,138'/>
    <polygon class='sticker' fill='var(--u3)' points='438,138 482,162 438,186 394,162'/>
    <polygon class='sticker' fill='var(--u4)' points='306,114 350,138 306,162 262,138'/>
    <polygon class='sticker' fill='var(--u5)' points='350,138 394,162 350,186 306,162'/>
    <polygon class='sticker' fill='var(--u6)' points='394,162 438,186 394,210 350,186'/>
    <polygon class='sticker' fill='var(--u7)' points='262,138 306,162 262,186 218,162'/>
    <polygon class='sticker' fill='var(--u8)' points='306,162 350,186 306,210 262,186'/>
    <polygon class='sticker' fill='var(--u9)' points='350,186 394,210 350,234 306,210'/>

    <polygon class='sticker' fill='var(--f1)' points='218,162 262,186 262,234 218,210'/>
    <polygon class='sticker' fill='var(--f2)' points='262,186 306,210 306,258 262,234'/>
    <polygon class='sticker' fill='var(--f3)' points='306,210 350,234 350,282 306,258'/>
    <polygon class='sticker' fill='var(--f4)' points='218,210 262,234 262,282 218,258'/>
    <polygon class='sticker' fill='var(--f5)' points='262,234 306,258 306,306 262,282'/>
    <polygon class='sticker' fill='var(--f6)' points='306,258 350,282 350,330 306,306'/>
    <polygon class='sticker' fill='var(--f7)' points='218,258 262,282 262,330 218,306'/>
    <polygon class='sticker' fill='var(--f8)' points='262,282 306,306 306,354 262,330'/>
    <polygon class='sticker' fill='var(--f9)' points='306,306 350,330 350,378 306,354'/>

    <polygon class='sticker' fill='var(--r1)' points='482,162 438,186 438,234 482,210'/>
    <polygon class='sticker' fill='var(--r2)' points='438,186 394,210 394,258 438,234'/>
    <polygon class='sticker' fill='var(--r3)' points='394,210 350,234 350,282 394,258'/>
    <polygon class='sticker' fill='var(--r4)' points='482,210 438,234 438,282 482,258'/>
    <polygon class='sticker' fill='var(--r5)' points='438,234 394,258 394,306 438,282'/>
    <polygon class='sticker' fill='var(--r6)' points='394,258 350,282 350,330 394,306'/>
    <polygon class='sticker' fill='var(--r7)' points='482,258 438,282 438,330 482,306'/>
    <polygon class='sticker' fill='var(--r8)' points='438,282 394,306 394,354 438,330'/>
    <polygon class='sticker' fill='var(--r9)' points='394,306 350,330 350,378 394,354'/>

    <polygon class='face-outline' points='350,90 482,162 350,234 218,162'/>
    <polygon class='face-outline' points='218,162 350,234 350,378 218,306'/>
    <polygon class='face-outline' points='482,162 350,234 350,378 482,306'/>
  </g>
  </g>
</svg>
"@

Set-Content -Path $outputPath -Value $svg -Encoding UTF8
Write-Output "Template SVG generated: $outputPath"
