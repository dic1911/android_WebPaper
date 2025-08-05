# WebPaper - WebView Live Wallpaper

A lightweight Android live wallpaper that displays any website as your wallpaper. With minimal dependencies

## Features

- 🌐 Display any website as your live wallpaper
- ⚡ Smol APK (~1MB)
- 🔋 Pauses when in background, with option to delay resume for being even more battery friendly

## Battery Optimization
Enable **"Delayed Resume"** toggle for better battery life:
- When you return to launcher, wallpaper waits 3 seconds before resuming
- If you open another app quickly, resume is cancelled
- Saves battery by avoiding unnecessary WebView activations during brief launcher visits
