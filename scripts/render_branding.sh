#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "$0")/.." && pwd)"

magick -size 1280x720 gradient:'#0D130F-#080B09' \
  -fill 'rgba(36,71,53,0.28)' -draw 'circle 1018,214 1268,214' \
  -fill none -stroke 'rgba(184,245,58,0.07)' -strokewidth 2 -draw 'circle 1018,214 1270,214' \
  -stroke 'rgba(255,255,255,0.05)' -draw 'circle 1018,214 1190,214' \
  -stroke none -fill 'rgba(184,245,58,0.035)' -draw 'polygon 910,720 1280,498 1280,720' \
  -fill '#171B18' -stroke 'rgba(255,255,255,0.14)' -strokewidth 3 -draw 'roundrectangle 118,232 310,424 52,52' \
  -stroke none -fill '#B8F53A' -draw 'roundrectangle 147,274 268,382 28,28' \
  -fill '#0B0E0C' -draw 'rectangle 164,301 173,356' \
  -draw 'polygon 192,297 192,359 242,328' \
  -font Arial-Bold -pointsize 96 -kerning -5 -fill '#FFFFFF' -draw "text 358,363 'dwPlayer'" \
  -font Arial-Bold -pointsize 34 -kerning 5 -fill '#8C9890' -draw "text 790,360 'TV'" \
  -fill '#B8F53A' -draw 'roundrectangle 360,401 416,407 3,3' \
  -font Arial-Bold -pointsize 22 -kerning 5 -fill '#A8B2AB' -draw "text 434,414 'YOUR MEDIA. ONE SCREEN.'" \
  -quality 92 "$project_root/tv/src/main/res/drawable-nodpi/tv_banner.webp"

magick -background none "$project_root/branding/app-icon.svg" \
  -resize 1024x1024! "$project_root/branding/app-icon-preview.png"
