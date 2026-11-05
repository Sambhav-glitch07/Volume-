import React, { useState, useEffect, useRef } from 'react';
import JSZip from 'jszip';
import { 
  Tv, 
  Volume2, 
  VolumeX, 
  Volume1, 
  Download, 
  Copy, 
  Check, 
  Code2, 
  Layers, 
  Sliders, 
  FileCode, 
  ShieldCheck, 
  Sparkles, 
  Eye, 
  ArrowRight, 
  ExternalLink, 
  RotateCcw,
  Smartphone,
  Play,
  Settings,
  HelpCircle,
  FileCheck,
  Radio,
  Share2
} from 'lucide-react';
import { PROJECT_FILES, ProjectFile } from './data/projectFiles';

export default function App() {
  // Volume HUD state
  const [volume, setVolume] = useState<number>(52);
  const [isMuted, setIsMuted] = useState<boolean>(false);
  const [isHudVisible, setIsHudVisible] = useState<boolean>(true);
  const [autoDismissEnabled, setAutoDismissEnabled] = useState<boolean>(true);
  const [secondsRemaining, setSecondsRemaining] = useState<number>(2.0);

  // View state
  const [activeTab, setActiveTab] = useState<'simulator' | 'code' | 'tv-setup' | 'guide'>('simulator');
  const [selectedFile, setSelectedFile] = useState<ProjectFile>(
    PROJECT_FILES.find(f => f.path.includes('GlassVolumeView.kt')) || PROJECT_FILES[0]
  );
  const [fileFilterCategory, setFileFilterCategory] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [copiedPath, setCopiedPath] = useState<string | null>(null);
  const [isZipping, setIsZipping] = useState<boolean>(false);
  const [viewMode, setViewMode] = useState<'comparison' | 'canvas' | 'reference'>('comparison');
  const [tvServiceActive, setTvServiceActive] = useState<boolean>(true);

  // Auto-dismiss timer ref
  const timerRef = useRef<NodeJS.Timeout | null>(null);
  const countdownIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Trigger HUD appearance and reset dismiss timer
  const triggerHud = (newVol?: number, muteToggle?: boolean) => {
    setIsHudVisible(true);
    setSecondsRemaining(2.0);

    if (newVol !== undefined) {
      setVolume(Math.min(100, Math.max(0, newVol)));
    }
    if (muteToggle !== undefined) {
      setIsMuted(muteToggle);
    }

    if (timerRef.current) clearTimeout(timerRef.current);
    if (countdownIntervalRef.current) clearInterval(countdownIntervalRef.current);

    if (autoDismissEnabled) {
      const startTime = Date.now();
      countdownIntervalRef.current = setInterval(() => {
        const elapsed = (Date.now() - startTime) / 1000;
        const left = Math.max(0, 2.0 - elapsed);
        setSecondsRemaining(parseFloat(left.toFixed(1)));
        if (left <= 0 && countdownIntervalRef.current) {
          clearInterval(countdownIntervalRef.current);
        }
      }, 100);

      timerRef.current = setTimeout(() => {
        setIsHudVisible(false);
      }, 2000);
    }
  };

  const handleVolumeUp = () => {
    triggerHud(volume + 4, false);
  };

  const handleVolumeDown = () => {
    triggerHud(volume - 4, false);
  };

  const handleToggleMute = () => {
    triggerHud(volume, !isMuted);
  };

  // Copy code handler
  const handleCopyCode = async (file: ProjectFile) => {
    try {
      await navigator.clipboard.writeText(file.content);
      setCopiedPath(file.path);
      setTimeout(() => setCopiedPath(null), 2500);
    } catch (err) {
      console.error('Failed to copy', err);
    }
  };

  // Download entire Android repository as a clean ZIP
  const handleDownloadZip = async () => {
    setIsZipping(true);
    try {
      const zip = new JSZip();

      // Add all project files into the zip
      for (const file of PROJECT_FILES) {
        zip.file(file.path, file.content);
      }

      // Add gradle wrapper directory files
      zip.file('gradlew', '#!/bin/sh\nexec gradle "$@"\n');
      zip.file('gradlew.bat', '@rem Gradle startup script for Windows\ngradle %*\n');

      const blob = await zip.generateAsync({ type: 'blob' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'sambhav-glass-volume-androidtv.zip';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Error generating zip:', error);
    } finally {
      setIsZipping(false);
    }
  };

  const categories = ['All', 'Kotlin Source', 'Layouts', 'Manifest & XML', 'Drawables', 'Gradle & Config', 'CI/CD & Docs'];

  const filteredFiles = PROJECT_FILES.filter(file => {
    const matchesCategory = fileFilterCategory === 'All' || file.category === fileFilterCategory;
    const matchesSearch = file.path.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          file.description.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-[#0A0C10] text-slate-100 font-sans selection:bg-sky-500 selection:text-white flex flex-col">
      {/* Top Navigation Bar */}
      <header className="border-b border-slate-800/80 bg-[#0E1117]/90 backdrop-blur sticky top-0 z-50 px-4 lg:px-8 py-3.5">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-sky-400/20 via-sky-500/10 to-transparent border border-sky-400/30 flex items-center justify-center shadow-lg shadow-sky-500/10">
              <Tv className="w-5 h-5 text-sky-400" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-base font-bold tracking-tight text-white">SAMBHAV GLASS VOLUME</h1>
                <span className="text-[10px] font-semibold uppercase tracking-wider bg-sky-500/10 text-sky-400 border border-sky-500/25 px-2 py-0.5 rounded-full">
                  Android TV (API 28)
                </span>
                <span className="hidden md:inline-block text-[10px] font-semibold uppercase tracking-wider bg-emerald-500/10 text-emerald-400 border border-emerald-500/25 px-2 py-0.5 rounded-full">
                  TCL Verified
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Standalone Native Android APK • Package: <code className="text-sky-300 font-mono">in.sambhav.glassvolume</code>
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 self-stretch sm:self-auto justify-end">
            <button
              onClick={handleDownloadZip}
              disabled={isZipping}
              className="flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-sky-500 hover:bg-sky-400 active:scale-95 text-slate-950 font-semibold text-xs transition shadow-md shadow-sky-500/20 cursor-pointer"
            >
              <Download className="w-3.5 h-3.5" />
              {isZipping ? 'Bundling ZIP...' : 'Download Android Project (ZIP)'}
            </button>
          </div>
        </div>

        {/* Tab switcher */}
        <div className="max-w-7xl mx-auto mt-3 flex items-center gap-1 border-t border-slate-800/60 pt-2.5 overflow-x-auto">
          <button
            onClick={() => setActiveTab('simulator')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer shrink-0 ${
              activeTab === 'simulator'
                ? 'bg-slate-800 text-sky-400 border border-slate-700 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            Live TV Simulator & Reference Match
          </button>
          <button
            onClick={() => setActiveTab('code')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer shrink-0 ${
              activeTab === 'code'
                ? 'bg-slate-800 text-sky-400 border border-slate-700 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <Code2 className="w-3.5 h-3.5" />
            Project Source Files ({PROJECT_FILES.length})
          </button>
          <button
            onClick={() => setActiveTab('tv-setup')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer shrink-0 ${
              activeTab === 'tv-setup'
                ? 'bg-slate-800 text-sky-400 border border-slate-700 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <Tv className="w-3.5 h-3.5" />
            TCL TV App Screen Preview
          </button>
          <button
            onClick={() => setActiveTab('guide')}
            className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer shrink-0 ${
              activeTab === 'guide'
                ? 'bg-slate-800 text-sky-400 border border-slate-700 shadow-sm'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/40'
            }`}
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            GitHub Actions & Sideload Guide
          </button>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 lg:p-8">
        {/* TAB 1: LIVE SIMULATOR & EXACT VISUAL COMPARISON */}
        {activeTab === 'simulator' && (
          <div className="space-y-6">
            {/* View Mode Controller */}
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 bg-[#131720] border border-slate-800 p-3 rounded-xl">
              <div className="flex items-center gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">View Mode:</span>
                <div className="inline-flex rounded-lg bg-slate-900 p-0.5 border border-slate-800 text-xs">
                  <button
                    onClick={() => setViewMode('comparison')}
                    className={`px-3 py-1 rounded-md transition cursor-pointer ${
                      viewMode === 'comparison' ? 'bg-sky-500 text-slate-950 font-semibold' : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    Side-by-Side Reference
                  </button>
                  <button
                    onClick={() => setViewMode('canvas')}
                    className={`px-3 py-1 rounded-md transition cursor-pointer ${
                      viewMode === 'canvas' ? 'bg-sky-500 text-slate-950 font-semibold' : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    Interactive Glass HUD Only
                  </button>
                  <button
                    onClick={() => setViewMode('reference')}
                    className={`px-3 py-1 rounded-md transition cursor-pointer ${
                      viewMode === 'reference' ? 'bg-sky-500 text-slate-950 font-semibold' : 'text-slate-400 hover:text-white'
                    }`}
                  >
                    Reference Photo
                  </button>
                </div>
              </div>

              {/* Status & timer indicator */}
              <div className="flex items-center gap-4 text-xs">
                <div className="flex items-center gap-2">
                  <span className="text-slate-400">Overlay State:</span>
                  <span className={`inline-flex items-center gap-1.5 font-mono px-2 py-0.5 rounded text-[11px] font-medium ${
                    isHudVisible ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30' : 'bg-slate-800 text-slate-400'
                  }`}>
                    <span className={`w-1.5 h-1.5 rounded-full ${isHudVisible ? 'bg-emerald-400 animate-pulse' : 'bg-slate-500'}`}></span>
                    {isHudVisible ? `VISIBLE (${secondsRemaining}s)` : 'FADED OUT'}
                  </span>
                </div>
                <button
                  onClick={() => triggerHud(volume)}
                  className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-[11px] cursor-pointer"
                >
                  Wake Overlay
                </button>
              </div>
            </div>

            {/* Visual Display Arena */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
              {/* Left/Center: Canvas / Visual Preview */}
              <div className={`${viewMode === 'comparison' ? 'lg:col-span-8' : 'lg:col-span-8'} space-y-6`}>
                {/* TV Screen Container */}
                <div className="relative rounded-2xl overflow-hidden border border-slate-800 bg-[#0c0f14] shadow-2xl">
                  {/* TV Bezel / Header */}
                  <div className="bg-[#12161f] px-4 py-2.5 border-b border-slate-800 flex items-center justify-between text-xs text-slate-400">
                    <div className="flex items-center gap-2">
                      <span className="w-2.5 h-2.5 rounded-full bg-red-500/80"></span>
                      <span className="w-2.5 h-2.5 rounded-full bg-amber-500/80"></span>
                      <span className="w-2.5 h-2.5 rounded-full bg-emerald-500/80"></span>
                      <span className="font-mono text-slate-300 ml-2">TCL Android TV 4K / Android 9 (API 28) Screen</span>
                    </div>
                    <div className="flex items-center gap-3 font-mono text-[11px]">
                      <span>1920 x 1080 (1080p)</span>
                      <span className="text-emerald-400">TYPE_ACCESSIBILITY_OVERLAY</span>
                    </div>
                  </div>

                  {/* TV Screen Contents */}
                  <div 
                    className="relative min-h-[380px] sm:min-h-[460px] flex flex-col items-center justify-center p-6 bg-cover bg-center"
                    style={{
                      backgroundImage: `radial-gradient(ellipse at 50% 30%, rgba(30, 41, 59, 0.45) 0%, rgba(10, 14, 20, 0.95) 100%), url('https://images.unsplash.com/photo-1579546929518-9e396f3cc809?auto=format&fit=crop&w=1920&q=80')`,
                      backgroundBlendMode: 'multiply'
                    }}
                  >
                    {/* Background subtle movie player UI to show real overlay transparency */}
                    <div className="absolute inset-0 pointer-events-none p-6 flex flex-col justify-between opacity-35">
                      <div className="flex justify-between items-center text-xs font-mono text-slate-300">
                        <span>HDMI 1 • TCL Home Cinema 4K</span>
                        <span>Dolby Vision • Atmos</span>
                      </div>
                      <div className="text-center font-mono text-xs text-slate-400">
                        [Playing Background Media - Overlay renders directly on top without stopping video]
                      </div>
                    </div>

                    {/* THE EXACT GLASS VOLUME HUD */}
                    <div
                      className={`relative z-20 flex items-center gap-4 transition-all duration-300 ease-out ${
                        isHudVisible 
                          ? 'opacity-100 scale-100 translate-y-0' 
                          : 'opacity-0 scale-95 -translate-y-2 pointer-events-none'
                      }`}
                    >
                      {/* Main Pill Slider */}
                      <div 
                        className="relative w-[340px] sm:w-[480px] h-[92px] sm:h-[124px] rounded-full overflow-hidden flex items-center shadow-2xl transition-all"
                        style={{
                          backgroundColor: 'rgba(26, 30, 36, 0.72)',
                          backdropFilter: 'blur(28px) saturate(160%)',
                          WebkitBackdropFilter: 'blur(28px) saturate(160%)',
                          border: '1.25px solid rgba(255, 255, 255, 0.28)',
                          boxShadow: '0 24px 60px -12px rgba(0, 0, 0, 0.75), inset 0 1px 1px 0 rgba(255, 255, 255, 0.35), inset 0 -1px 1px 0 rgba(0, 0, 0, 0.4)'
                        }}
                      >
                        {/* Upper subtle glass lens-flare reflection arc */}
                        <div 
                          className="absolute inset-x-8 top-0 h-[2px] pointer-events-none"
                          style={{
                            background: 'linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.4) 30%, rgba(220,240,255,0.7) 50%, rgba(255,255,255,0.4) 70%, transparent 100%)'
                          }}
                        />

                        {/* Active Volume Fill with curved meniscus edge */}
                        <div 
                          className="absolute left-0 top-0 bottom-0 transition-all duration-200 ease-out"
                          style={{
                            width: `${isMuted ? 0 : volume}%`,
                            background: 'linear-gradient(180deg, rgba(125, 140, 160, 0.55) 0%, rgba(85, 98, 115, 0.52) 48%, rgba(60, 72, 88, 0.58) 100%)'
                          }}
                        >
                          {/* Curved liquid meniscus crest at the leading edge (exact reference reproduction!) */}
                          <div 
                            className="absolute right-0 top-0 bottom-0 w-8 sm:w-12 pointer-events-none translate-x-1/2"
                            style={{
                              background: 'radial-gradient(ellipse 100% 70% at 0% 50%, rgba(125, 140, 160, 0.55) 60%, transparent 100%)'
                            }}
                          />

                          {/* Glowing Specular Blue-White Crystal Highlight along the leading edge */}
                          <div 
                            className="absolute -right-[1.5px] top-0 bottom-0 w-[3px] pointer-events-none rounded-full"
                            style={{
                              background: 'linear-gradient(180deg, rgba(255,255,255,0.95) 0%, rgba(187,225,250,0.95) 30%, rgba(140,195,250,0.85) 65%, rgba(255,255,255,0.2) 100%)',
                              boxShadow: '0 0 16px 2px rgba(165, 216, 255, 0.65), 0 0 4px 1px rgba(255, 255, 255, 0.8)'
                            }}
                          />
                        </div>

                        {/* Left section: Speaker Icon & Realtime Volume Number */}
                        <div className="relative z-10 flex items-center gap-3.5 pl-6 sm:pl-9 select-none">
                          <div className="text-white drop-shadow">
                            {isMuted || volume === 0 ? (
                              <VolumeX className="w-7 h-7 sm:w-8 sm:h-8 stroke-[2.2]" />
                            ) : volume < 40 ? (
                              <Volume1 className="w-7 h-7 sm:w-8 sm:h-8 stroke-[2.2]" />
                            ) : (
                              <Volume2 className="w-7 h-7 sm:w-8 sm:h-8 stroke-[2.2]" />
                            )}
                          </div>
                          <span className="text-white font-medium text-2xl sm:text-3xl tracking-tight drop-shadow font-sans">
                            {isMuted ? 'MUTE' : volume}
                          </span>
                        </div>
                      </div>

                      {/* Right Separate Circular Glass Button */}
                      <div 
                        className="relative w-[92px] sm:w-[124px] h-[92px] sm:h-[124px] rounded-full flex items-center justify-center shadow-2xl transition-all"
                        style={{
                          backgroundColor: 'rgba(26, 30, 36, 0.72)',
                          backdropFilter: 'blur(28px) saturate(160%)',
                          WebkitBackdropFilter: 'blur(28px) saturate(160%)',
                          border: '1.25px solid rgba(255, 255, 255, 0.28)',
                          boxShadow: '0 24px 60px -12px rgba(0, 0, 0, 0.75), inset 0 1px 1px 0 rgba(255, 255, 255, 0.35)'
                        }}
                      >
                        {/* Subtle inner concentric ring disc */}
                        <div 
                          className="absolute inset-3 sm:inset-4 rounded-full border border-white/10 pointer-events-none"
                        />

                        {/* AirPlay / Audio Output Symbol */}
                        <div className="relative z-10 text-white flex items-center justify-center">
                          <svg className="w-7 h-7 sm:w-9 sm:h-9" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            {/* Upward beacon triangle */}
                            <path d="M12 14.5L7.5 20H16.5L12 14.5Z" fill="white" />
                            {/* Inner concentric arc */}
                            <path d="M8.5 12.5C7.2 11.2 6.5 9.4 6.5 7.5C6.5 4.5 9 2 12 2C15 2 17.5 4.5 17.5 7.5C17.5 9.4 16.8 11.2 15.5 12.5" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
                            {/* Outer concentric arc */}
                            <path d="M5.2 14.8C3.2 12.8 2 10.2 2 7.5C2 2 6.5 -0.5 12 -0.5C17.5 -0.5 22 2 22 7.5C22 10.2 20.8 12.8 18.8 14.8" stroke="white" strokeWidth="1.8" strokeLinecap="round" />
                          </svg>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Quick Remote Slider Bar below Screen */}
                  <div className="bg-[#12161f] p-4 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
                    <div className="flex items-center gap-3 w-full sm:w-auto">
                      <button
                        onClick={handleVolumeDown}
                        className="px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-white text-xs font-semibold flex items-center gap-1.5 transition active:scale-95 cursor-pointer"
                      >
                        <Volume1 className="w-4 h-4" />
                        VOL -
                      </button>
                      <button
                        onClick={handleVolumeUp}
                        className="px-3 py-1.5 rounded-lg bg-sky-500 hover:bg-sky-400 text-slate-950 text-xs font-semibold flex items-center gap-1.5 transition active:scale-95 cursor-pointer"
                      >
                        <Volume2 className="w-4 h-4" />
                        VOL +
                      </button>
                      <button
                        onClick={handleToggleMute}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition active:scale-95 cursor-pointer ${
                          isMuted ? 'bg-red-500 text-white' : 'bg-slate-800 hover:bg-slate-700 text-white'
                        }`}
                      >
                        <VolumeX className="w-4 h-4" />
                        {isMuted ? 'UNMUTE' : 'MUTE'}
                      </button>
                    </div>

                    {/* Scrubber */}
                    <div className="flex items-center gap-3 w-full sm:w-64">
                      <span className="text-xs text-slate-400 font-mono">0</span>
                      <input
                        type="range"
                        min="0"
                        max="100"
                        value={volume}
                        onChange={(e) => triggerHud(parseInt(e.target.value), false)}
                        className="flex-1 accent-sky-400 cursor-pointer h-1.5 bg-slate-800 rounded-lg"
                      />
                      <span className="text-xs text-sky-400 font-mono font-bold w-7 text-right">{volume}</span>
                    </div>

                    {/* Auto Dismiss Toggle */}
                    <label className="flex items-center gap-2 text-xs text-slate-400 cursor-pointer select-none">
                      <input
                        type="checkbox"
                        checked={autoDismissEnabled}
                        onChange={(e) => setAutoDismissEnabled(e.target.checked)}
                        className="rounded border-slate-700 text-sky-500 focus:ring-sky-500"
                      />
                      Auto-fade after 2s
                    </label>
                  </div>
                </div>

                {/* Side-by-side reference image card if in comparison mode */}
                {viewMode === 'comparison' && (
                  <div className="bg-[#12161f] border border-slate-800 rounded-2xl p-5 space-y-3">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Sparkles className="w-4 h-4 text-amber-400" />
                        <h3 className="text-sm font-bold text-white">Visual Reference Comparison</h3>
                      </div>
                      <span className="text-xs text-slate-400 font-mono">Uploaded Reference Image</span>
                    </div>

                    <div className="relative rounded-xl overflow-hidden border border-slate-700/60 bg-black/50">
                      <img 
                        src="/assets/aistudio/1793893375822_4688efe7.jpg" 
                        alt="Target Reference"
                        className="w-full h-auto object-cover max-h-[280px]"
                      />
                      <div className="absolute bottom-2 left-2 px-2 py-1 bg-black/75 backdrop-blur text-[11px] text-white rounded font-mono">
                        Target Source of Truth: 480x128dp dark glass pill + 128dp circular button
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Right Column: Virtual TV Remote & Technical Diagnostics */}
              <div className="lg:col-span-4 space-y-6">
                {/* Virtual TCL TV Remote Controller */}
                <div className="bg-[#12161f] border border-slate-800 rounded-2xl p-5 space-y-4 shadow-xl">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                    <div className="flex items-center gap-2">
                      <Smartphone className="w-4 h-4 text-sky-400" />
                      <h3 className="text-sm font-bold text-white">Virtual TCL Remote</h3>
                    </div>
                    <span className="text-[10px] font-mono bg-slate-800 px-2 py-0.5 rounded text-slate-300">
                      KEY FILTERING
                    </span>
                  </div>

                  <p className="text-xs text-slate-400 leading-relaxed">
                    Test how Sambhav Glass Volume intercepts remote keys, adjusts real audio without <code className="text-sky-300">FLAG_SHOW_UI</code>, and prevents the TCL stock UI from popping up:
                  </p>

                  {/* Remote Body */}
                  <div className="bg-[#0b0e14] border border-slate-800/80 rounded-2xl p-4 flex flex-col items-center gap-3">
                    {/* D-Pad */}
                    <div className="relative w-36 h-36 rounded-full bg-slate-900 border border-slate-700 flex items-center justify-center shadow-inner">
                      <button 
                        onClick={() => triggerHud(volume + 5)}
                        className="absolute top-2 w-10 h-7 rounded-t-full bg-slate-800 hover:bg-slate-700 text-[10px] text-slate-300 font-bold flex items-center justify-center cursor-pointer active:scale-95"
                      >
                        ▲
                      </button>
                      <button 
                        onClick={() => triggerHud(volume - 5)}
                        className="absolute bottom-2 w-10 h-7 rounded-b-full bg-slate-800 hover:bg-slate-700 text-[10px] text-slate-300 font-bold flex items-center justify-center cursor-pointer active:scale-95"
                      >
                        ▼
                      </button>
                      <button 
                        onClick={() => triggerHud(volume - 5)}
                        className="absolute left-2 h-10 w-7 rounded-l-full bg-slate-800 hover:bg-slate-700 text-[10px] text-slate-300 font-bold flex items-center justify-center cursor-pointer active:scale-95"
                      >
                        ◄
                      </button>
                      <button 
                        onClick={() => triggerHud(volume + 5)}
                        className="absolute right-2 h-10 w-7 rounded-r-full bg-slate-800 hover:bg-slate-700 text-[10px] text-slate-300 font-bold flex items-center justify-center cursor-pointer active:scale-95"
                      >
                        ►
                      </button>
                      <button 
                        onClick={() => triggerHud(52)}
                        className="w-12 h-12 rounded-full bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs flex items-center justify-center shadow-md cursor-pointer active:scale-90"
                      >
                        OK
                      </button>
                    </div>

                    {/* Hardware Volume Rockers */}
                    <div className="w-full grid grid-cols-2 gap-2 mt-2">
                      <button
                        onClick={handleVolumeUp}
                        className="py-3 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 active:scale-95 text-white font-bold text-xs flex flex-col items-center justify-center gap-1 border border-slate-700/60 shadow-sm cursor-pointer"
                      >
                        <Volume2 className="w-4 h-4 text-sky-400" />
                        VOL +
                        <span className="text-[9px] text-slate-400 font-mono font-normal">KEYCODE 24</span>
                      </button>
                      <button
                        onClick={handleVolumeDown}
                        className="py-3 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 active:scale-95 text-white font-bold text-xs flex flex-col items-center justify-center gap-1 border border-slate-700/60 shadow-sm cursor-pointer"
                      >
                        <Volume1 className="w-4 h-4 text-sky-400" />
                        VOL -
                        <span className="text-[9px] text-slate-400 font-mono font-normal">KEYCODE 25</span>
                      </button>
                    </div>

                    <button
                      onClick={handleToggleMute}
                      className="w-full py-2.5 px-4 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-300 font-medium text-xs flex items-center justify-center gap-2 border border-slate-700/40 cursor-pointer"
                    >
                      <VolumeX className="w-4 h-4 text-red-400" />
                      MUTE TOGGLE (KEYCODE 164)
                    </button>
                  </div>
                </div>

                {/* Architecture Highlights Card */}
                <div className="bg-[#12161f] border border-slate-800 rounded-2xl p-5 space-y-3">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-slate-300">
                    Engineered for TCL TV Performance
                  </h3>
                  <div className="space-y-2.5 text-xs text-slate-400">
                    <div className="flex items-start gap-2.5">
                      <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span><strong className="text-white">Zero Compose Runtime:</strong> Pure native Android View and custom Canvas rendering for zero lag on TV chipsets.</span>
                    </div>
                    <div className="flex items-start gap-2.5">
                      <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span><strong className="text-white">Hardware Key Filtering:</strong> Intercepts key events and consumes them with <code className="text-sky-300">return true</code> to suppress stock UI.</span>
                    </div>
                    <div className="flex items-start gap-2.5">
                      <Check className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                      <span><strong className="text-white">External Volume Sync:</strong> Background ContentObserver detects CEC/Bluetooth adjustments automatically.</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: PROJECT SOURCE CODE EXPLORER */}
        {activeTab === 'code' && (
          <div className="space-y-4">
            <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-3 bg-[#131720] border border-slate-800 p-3 rounded-xl">
              <div>
                <h2 className="text-sm font-bold text-white flex items-center gap-2">
                  <FileCode className="w-4 h-4 text-sky-400" />
                  Full Repository Source Code
                </h2>
                <p className="text-xs text-slate-400">
                  Ready to copy-paste or export directly into Android Studio or GitHub.
                </p>
              </div>

              <div className="flex items-center gap-2 w-full md:w-auto">
                <input
                  type="text"
                  placeholder="Filter files (e.g. GlassVolume, service, manifest)..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-700 text-xs text-white placeholder-slate-500 w-full md:w-64 focus:outline-none focus:border-sky-500"
                />
                <button
                  onClick={handleDownloadZip}
                  className="px-3 py-1.5 rounded-lg bg-sky-500 hover:bg-sky-400 text-slate-950 font-bold text-xs shrink-0 flex items-center gap-1.5 transition cursor-pointer"
                >
                  <Download className="w-3.5 h-3.5" />
                  Download ZIP
                </button>
              </div>
            </div>

            {/* Category Pills */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
              {categories.map(cat => (
                <button
                  key={cat}
                  onClick={() => setFileFilterCategory(cat)}
                  className={`px-3 py-1 rounded-lg text-xs font-medium shrink-0 transition cursor-pointer ${
                    fileFilterCategory === cat 
                      ? 'bg-sky-500 text-slate-950 font-semibold shadow-sm'
                      : 'bg-slate-900 hover:bg-slate-800 text-slate-400 border border-slate-800'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>

            {/* Code Workspace Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-4">
              {/* File List Drawer */}
              <div className="lg:col-span-4 bg-[#12161f] border border-slate-800 rounded-xl overflow-hidden flex flex-col max-h-[640px]">
                <div className="p-3 bg-[#0d1017] border-b border-slate-800 text-xs font-semibold text-slate-400 uppercase tracking-wider flex items-center justify-between">
                  <span>Repository Files</span>
                  <span className="font-mono text-sky-400">{filteredFiles.length} files</span>
                </div>
                <div className="divide-y divide-slate-800/60 overflow-y-auto flex-1">
                  {filteredFiles.map((file) => {
                    const isSelected = selectedFile.path === file.path;
                    return (
                      <button
                        key={file.path}
                        onClick={() => setSelectedFile(file)}
                        className={`w-full text-left p-3 text-xs transition flex items-start gap-2.5 cursor-pointer ${
                          isSelected
                            ? 'bg-sky-500/10 text-sky-300 border-l-2 border-sky-400'
                            : 'hover:bg-slate-800/40 text-slate-300'
                        }`}
                      >
                        <FileCode className={`w-4 h-4 shrink-0 mt-0.5 ${isSelected ? 'text-sky-400' : 'text-slate-500'}`} />
                        <div className="min-w-0 flex-1">
                          <div className="font-mono font-medium truncate">{file.filename}</div>
                          <div className="text-[10px] text-slate-500 truncate">{file.path}</div>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Code Viewer Panel */}
              <div className="lg:col-span-8 bg-[#12161f] border border-slate-800 rounded-xl overflow-hidden flex flex-col max-h-[640px]">
                {/* Code Header Bar */}
                <div className="p-3 bg-[#0d1017] border-b border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 font-mono text-xs text-white">
                      <span className="text-sky-400 font-bold">{selectedFile.filename}</span>
                      <span className="text-slate-500">•</span>
                      <span className="text-[11px] text-slate-400 truncate">{selectedFile.path}</span>
                    </div>
                    <p className="text-[11px] text-slate-400 mt-0.5">{selectedFile.description}</p>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => handleCopyCode(selectedFile)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition cursor-pointer ${
                        copiedPath === selectedFile.path
                          ? 'bg-emerald-500 text-slate-950 font-bold'
                          : 'bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700'
                      }`}
                    >
                      {copiedPath === selectedFile.path ? (
                        <>
                          <Check className="w-3.5 h-3.5" />
                          Copied!
                        </>
                      ) : (
                        <>
                          <Copy className="w-3.5 h-3.5" />
                          Copy Raw Code
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Code Body */}
                <div className="flex-1 overflow-auto bg-[#0a0d14] p-4 text-xs font-mono text-slate-200 leading-relaxed">
                  <pre className="whitespace-pre overflow-x-auto">
                    {selectedFile.content}
                  </pre>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: TCL TV APP SCREEN PREVIEW */}
        {activeTab === 'tv-setup' && (
          <div className="space-y-6">
            <div className="bg-[#131720] border border-slate-800 p-4 rounded-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-bold text-white flex items-center gap-2">
                  <Tv className="w-4 h-4 text-sky-400" />
                  MainActivity 10-Foot TV Setup Interface
                </h2>
                <p className="text-xs text-slate-400">
                  This is the exact UI that appears when launching Sambhav Glass Volume on the TCL Android TV.
                </p>
              </div>

              {/* Service status toggle to test both states */}
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-400">Simulate Service State:</span>
                <button
                  onClick={() => setTvServiceActive(!tvServiceActive)}
                  className={`px-3 py-1 rounded-lg text-xs font-semibold transition cursor-pointer ${
                    tvServiceActive 
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40' 
                      : 'bg-amber-500/20 text-amber-300 border border-amber-500/40'
                  }`}
                >
                  {tvServiceActive ? 'ACTIVE (Green)' : 'DISABLED (Amber)'}
                </button>
              </div>
            </div>

            {/* 10-Foot TV Screen Replica */}
            <div className="bg-[#0F1115] border-2 border-slate-800 rounded-3xl p-6 sm:p-10 shadow-2xl space-y-8">
              {/* Header */}
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-6">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-sky-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
                    <Tv className="w-8 h-8 text-white" />
                  </div>
                  <div>
                    <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white">Sambhav Glass Volume</h1>
                    <p className="text-sm text-slate-400 mt-1">Standalone Frosted Glass Volume Overlay for TCL Android TV</p>
                  </div>
                </div>

                {/* Status Badge */}
                <div className={`px-4 py-2 rounded-xl text-xs font-extrabold tracking-wider flex items-center gap-2 ${
                  tvServiceActive 
                    ? 'bg-[#064E3B] text-emerald-400 border border-emerald-500/30' 
                    : 'bg-[#2E2314] text-amber-400 border border-amber-500/30'
                }`}>
                  <span className={`w-2 h-2 rounded-full ${tvServiceActive ? 'bg-emerald-400 animate-pulse' : 'bg-amber-400'}`}></span>
                  {tvServiceActive ? 'ACTIVE & INTERCEPTING' : 'NOT ENABLED'}
                </div>
              </div>

              {/* Status Description */}
              <p className="text-sm text-slate-300">
                {tvServiceActive 
                  ? 'Service is running in background. Remote volume key presses will show the glass HUD and suppress stock TV popups.'
                  : 'Accessibility Service is currently OFF. Click below to enable Sambhav Glass Volume so it can intercept remote volume keys.'}
              </p>

              {/* Live Preview Card */}
              <div className="bg-[#1A1D24] border border-[#2C3240] rounded-2xl p-6 flex flex-col items-center">
                <span className="text-xs font-bold uppercase tracking-widest text-slate-400 mb-6">
                  EXACT REFERENCE RENDERING (Press D-Pad Left/Right to Test Response)
                </span>

                {/* Rendered View Component */}
                <div className="flex items-center gap-4">
                  <div 
                    className="w-[320px] sm:w-[460px] h-[88px] sm:h-[116px] rounded-full flex items-center shadow-2xl relative overflow-hidden"
                    style={{
                      backgroundColor: 'rgba(32, 36, 44, 0.85)',
                      border: '1.25px solid rgba(255, 255, 255, 0.3)'
                    }}
                  >
                    <div 
                      className="absolute left-0 top-0 bottom-0 bg-slate-600/50"
                      style={{ width: `${volume}%` }}
                    >
                      <div className="absolute right-0 top-0 bottom-0 w-8 bg-slate-600/40 rounded-r-full translate-x-1/2" />
                      <div className="absolute right-0 top-0 bottom-0 w-[2.5px] bg-gradient-to-b from-white via-sky-200 to-sky-400 shadow-[0_0_12px_rgba(165,216,255,0.8)]" />
                    </div>
                    <div className="relative z-10 flex items-center gap-3 pl-8 text-white font-medium text-2xl">
                      <Volume2 className="w-7 h-7" />
                      <span>{volume}</span>
                    </div>
                  </div>

                  <div 
                    className="w-[88px] sm:w-[116px] h-[88px] sm:h-[116px] rounded-full flex items-center justify-center shadow-2xl relative"
                    style={{
                      backgroundColor: 'rgba(32, 36, 44, 0.85)',
                      border: '1.25px solid rgba(255, 255, 255, 0.3)'
                    }}
                  >
                    <div className="absolute inset-3 rounded-full border border-white/10" />
                    <Share2 className="w-8 h-8 text-white rotate-90" />
                  </div>
                </div>
              </div>

              {/* Action Buttons with D-Pad Focus Selector styling */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                <button className="py-4 px-6 rounded-2xl bg-[#0284C7] text-white font-bold text-sm border-2 border-[#38BDF8] shadow-lg shadow-sky-500/20 text-center cursor-pointer">
                  {tvServiceActive ? 'SETTINGS CONFIGURED (CLICK TO MANAGE)' : 'ENABLE GLASS VOLUME'}
                </button>
                <button 
                  onClick={() => triggerHud(volume)}
                  className="py-4 px-6 rounded-2xl bg-[#1E293B] hover:bg-[#253347] text-white font-semibold text-sm border border-[#334155] text-center cursor-pointer"
                >
                  TEST VOLUME OVERLAY
                </button>
                <button className="py-4 px-6 rounded-2xl bg-[#1E293B] hover:bg-[#253347] text-white font-semibold text-sm border border-[#334155] text-center cursor-pointer">
                  TCL COMPATIBILITY GUIDE
                </button>
              </div>

              {/* Activation Instructions */}
              <div className="bg-[#1A1D24] border border-[#2C3240] rounded-2xl p-6 space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-sky-400">
                  First-Time Activation Guide (TCL Android TV)
                </h3>
                <ol className="list-decimal list-inside space-y-1.5 text-xs text-slate-300">
                  <li>Click "ENABLE GLASS VOLUME" to open Android TV Accessibility Settings.</li>
                  <li>Navigate to Downloaded Services &gt; "Sambhav Glass Volume".</li>
                  <li>Toggle the switch ON and confirm "OK".</li>
                  <li>Press BACK to return here. Status will update to green ACTIVE.</li>
                </ol>
                <p className="text-[11px] text-slate-500 pt-2 border-t border-slate-800">
                  To disable or remove this custom volume UI, simply turn off Sambhav Glass Volume in Settings &gt; Device Preferences &gt; Accessibility.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: GITHUB ACTIONS & SIDELOADING GUIDE */}
        {activeTab === 'guide' && (
          <div className="max-w-4xl mx-auto space-y-6">
            <div className="bg-[#131720] border border-slate-800 p-5 rounded-2xl space-y-3">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-sky-400" />
                Automated GitHub Actions Build & Installation Guide
              </h2>
              <p className="text-xs text-slate-400 leading-relaxed">
                You can build the debug APK directly on your phone using GitHub without needing Android Studio installed! Follow the steps below.
              </p>
            </div>

            {/* Workflow File Details Card */}
            <div className="bg-[#12161f] border border-slate-800 rounded-2xl p-6 space-y-4">
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <div className="flex items-center gap-2">
                  <Play className="w-4 h-4 text-emerald-400" />
                  <h3 className="text-sm font-bold text-white">GitHub Actions Automated Build (.github/workflows/build.yml)</h3>
                </div>
                <span className="text-[11px] font-mono text-slate-400">Java 17 + Gradle 8.6</span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1.5">
                  <span className="text-[10px] font-bold text-sky-400 uppercase tracking-wider">Step 1</span>
                  <h4 className="text-xs font-bold text-white">Create GitHub Repo</h4>
                  <p className="text-[11px] text-slate-400">Create a repository on GitHub and upload this project code.</p>
                </div>
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1.5">
                  <span className="text-[10px] font-bold text-emerald-400 uppercase tracking-wider">Step 2</span>
                  <h4 className="text-xs font-bold text-white">Automatic Build</h4>
                  <p className="text-[11px] text-slate-400">GitHub Actions runs automatically and compiles <code className="text-sky-300">app-debug.apk</code>.</p>
                </div>
                <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-1.5">
                  <span className="text-[10px] font-bold text-purple-400 uppercase tracking-wider">Step 3</span>
                  <h4 className="text-xs font-bold text-white">Download APK</h4>
                  <p className="text-[11px] text-slate-400">Go to Actions tab &gt; Artifacts &gt; download <code className="text-emerald-300">Sambhav-Glass-Volume-AndroidTV-Debug</code>.</p>
                </div>
              </div>
            </div>

            {/* TV Sideloading Methods */}
            <div className="bg-[#12161f] border border-slate-800 rounded-2xl p-6 space-y-4">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Tv className="w-4 h-4 text-sky-400" />
                How to Sideload Onto TCL Android TV
              </h3>

              <div className="space-y-3">
                <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-2">
                  <h4 className="text-xs font-bold text-sky-300">Method 1: USB Flash Drive (Simplest)</h4>
                  <p className="text-xs text-slate-300">
                    1. Copy <code className="text-white bg-slate-800 px-1.5 py-0.5 rounded font-mono">app-debug.apk</code> onto a FAT32 USB stick.<br/>
                    2. Insert the USB into your TCL TV.<br/>
                    3. Open the TV's built-in File Manager, click the APK, and choose "Install".<br/>
                    4. When prompted, enable "Install from Unknown Sources" for File Manager.
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-2">
                  <h4 className="text-xs font-bold text-sky-300">Method 2: "Send Files to TV" App (Direct from Android Phone)</h4>
                  <p className="text-xs text-slate-300">
                    1. Install <strong>Send Files to TV</strong> from Play Store on both your phone and TCL TV.<br/>
                    2. Download the APK artifact to your phone, open Send Files to TV, and send to TV.<br/>
                    3. On the TV, use a TV File Explorer to click the received file and install.
                  </p>
                </div>

                <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800/80 space-y-2">
                  <h4 className="text-xs font-bold text-sky-300">Method 3: Wireless ADB Command</h4>
                  <pre className="p-3 bg-black/60 rounded-lg text-xs font-mono text-emerald-400 overflow-x-auto">
{`adb connect <YOUR_TCL_TV_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk`}
                  </pre>
                </div>
              </div>
            </div>

            {/* TCL TV Remote Firmware Compatibility Notes */}
            <div className="bg-[#12161f] border border-slate-800 rounded-2xl p-6 space-y-3">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <HelpCircle className="w-4 h-4 text-amber-400" />
                TCL Firmware & Remote Key Filtering Details
              </h3>
              <p className="text-xs text-slate-300 leading-relaxed">
                Standard Android 9 (API 28) passes hardware keys to Accessibility Services with <code className="text-sky-300">FLAG_REQUEST_FILTER_KEY_EVENTS</code>. 
                Our service catches <code className="text-sky-300">KEYCODE_VOLUME_UP</code>, <code className="text-sky-300">KEYCODE_VOLUME_DOWN</code>, and <code className="text-sky-300">KEYCODE_VOLUME_MUTE</code>, 
                consumes them via <code className="text-sky-300">return true</code>, and calls <code className="text-sky-300">AudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0)</code>. 
                Passing <code className="text-sky-300">0</code> as the flag suppresses the system UI popup!
              </p>
              <p className="text-xs text-slate-400 leading-relaxed">
                If your TCL TV remote operates via direct HDMI-CEC / ARC soundbar audio bypass, our companion ContentObserver on <code className="text-sky-300">Settings.System.CONTENT_URI</code> automatically catches external volume updates and triggers the glass HUD smoothly.
              </p>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 bg-[#0E1117] py-4 px-6 text-center text-xs text-slate-500">
        <p>Sambhav Glass Volume • Target: TCL Android TV (Android 9 API 28) • Package: <code className="text-slate-400 font-mono">in.sambhav.glassvolume</code></p>
      </footer>
    </div>
  );
}
