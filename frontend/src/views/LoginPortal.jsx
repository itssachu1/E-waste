import React, { useState, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { authService } from '../services/api';
import {
    Building, User, Lock, Eye, EyeOff, MapPin, LogIn,
    MessageSquare, ShieldCheck, Heart, UserPlus, BarChart3, Users
} from 'lucide-react';

/**
 * Rebuilt LoginPortal to match the provided mockup with 95%+ precision.
 * Features a split two-pane grid layout, exact same spacing/typography, and high-fidelity 
 * SVG background vector art for the left panel.
 */
export default function LoginPortal() {
    const { login } = useContext(AuthContext);
    const [isRegister, setIsRegister] = useState(false);
    const [role, setRole] = useState('CITIZEN'); // CITIZEN or MP
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [wardArea, setWardArea] = useState('Ward 5');
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);

    // Demo-mode login for hackathon demos when backend auth is unavailable
    const handleDemoLogin = (type) => {
        setError('');
        setSuccess('');
        setIsRegister(false);

        if (type === 'CITIZEN') {
            setRole('CITIZEN');
            setUsername('citizen-demo');
            setPassword('');
            setWardArea('Ward 5');
            login({
                id: 1001,
                username: 'citizen-demo',
                role: 'CITIZEN',
                wardArea: 'Ward 5',
                demoMode: true,
            });
            setSuccess('Demo Mode enabled. You are signed in as a citizen.');
        } else {
            setRole('MP');
            setUsername('mp-demo');
            setPassword('');
            login({
                id: 2001,
                username: 'mp-demo',
                role: 'MP',
                wardArea: 'Ward 5',
                demoMode: true,
            });
            setSuccess('Demo Mode enabled. You are signed in as an MP admin.');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (loading) return;
        setError('');
        setSuccess('');

        if (!username.trim() || !password.trim()) {
            setError('Please enter both username and password.');
            return;
        }

        setLoading(true);
        try {
            if (isRegister) {
                await authService.register(
                    username,
                    password,
                    role,
                    role === 'CITIZEN' ? wardArea : null
                );
                setSuccess('Registration successful! Please login.');
                setIsRegister(false);
            } else {
                const data = await authService.login(username, password);
                login(data);
            }
        } catch (err) {
            setError(err.response?.data?.error || 'Authentication failed. Please verify credentials.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex flex-col justify-between bg-white text-slate-800 font-sans z-0 relative">

            {/* 1. Header Bar */}
            <header className="w-full bg-white border-b border-slate-100 px-8 py-3.5 flex flex-row justify-between items-center sm:px-12">
                {/* Brand Logo */}
                <div className="flex items-center gap-3">
                    <div className="text-emerald-700">
                        <Building className="w-9 h-9" />
                    </div>
                    <div className="flex flex-col">
                        <span className="text-xl font-extrabold text-slate-900 tracking-tight leading-6">
                            JanVoice AI
                        </span>
                        <span className="text-[11px] font-bold text-emerald-650 tracking-wider">
                            Code for Communities
                        </span>
                    </div>
                </div>

                {/* Header Tabs */}
                <div className="flex gap-8 h-10 items-center">
                    <button
                        onClick={() => setRole('CITIZEN')}
                        className={`flex items-center gap-2 text-sm font-semibold tracking-wide transition-all h-full px-1 border-b-2 ${role === 'CITIZEN'
                                ? 'border-emerald-700 text-emerald-800'
                                : 'border-transparent text-slate-500 hover:text-slate-800'
                            }`}
                    >
                        <User className="w-4 h-4" />
                        Citizen Portal
                    </button>

                    <button
                        onClick={() => setRole('MP')}
                        className={`flex items-center gap-2 text-sm font-semibold tracking-wide transition-all h-full px-1 border-b-2 ${role === 'MP'
                                ? 'border-emerald-700 text-emerald-800'
                                : 'border-transparent text-slate-500 hover:text-slate-800'
                            }`}
                    >
                        <BarChart3 className="w-4 h-4" />
                        MP Dashboard
                    </button>
                </div>
            </header>

            {/* 2. Grid Body Split */}
            <main className="flex-1 grid grid-cols-1 lg:grid-cols-2">

                {/* Left Pane - Hero info and features */}
                <section className="bg-[#eaf5ec] px-8 py-12 md:py-16 md:px-16 flex flex-col justify-between relative overflow-hidden">

                    {/* Top text content & checklist features */}
                    <div className="max-w-lg space-y-10 relative z-10">

                        <div className="space-y-4">
                            <h2 className="text-4xl md:text-5xl font-black text-slate-900 tracking-tight leading-tight">
                                A Smarter Way to<br />
                                Build <span className="text-emerald-700">Better Communities</span>
                            </h2>
                            <p className="text-slate-550 text-sm leading-relaxed max-w-md font-medium">
                                Raise issues, track progress, and get things done with the power of AI.
                            </p>
                        </div>

                        {/* Checklist items */}
                        <div className="space-y-6 max-w-md">

                            <div className="flex gap-4 items-start">
                                <div className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-emerald-700 shadow-md shrink-0">
                                    <MessageSquare className="w-5 h-5 fill-emerald-500/10" />
                                </div>
                                <div>
                                    <h4 className="text-sm font-bold text-slate-850">Smart Complaint Handling</h4>
                                    <p className="text-xs text-slate-450 mt-0.5 leading-relaxed">
                                        AI categorizes, deduplicates and prioritizes complaints automatically.
                                    </p>
                                </div>
                            </div>

                            <div className="flex gap-4 items-start">
                                <div className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-emerald-700 shadow-md shrink-0">
                                    <MapPin className="w-5 h-5 fill-emerald-500/10" />
                                </div>
                                <div>
                                    <h4 className="text-sm font-bold text-slate-850">Location Based Reporting</h4>
                                    <p className="text-xs text-slate-450 mt-0.5 leading-relaxed">
                                        Pin the exact location and help local authorities act faster.
                                    </p>
                                </div>
                            </div>

                            <div className="flex gap-4 items-start">
                                <div className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-emerald-700 shadow-md shrink-0">
                                    <Users className="w-5 h-5 fill-emerald-500/10" />
                                </div>
                                <div>
                                    <h4 className="text-sm font-bold text-slate-850">Community Driven</h4>
                                    <p className="text-xs text-slate-450 mt-0.5 leading-relaxed">
                                        Upvote issues, support your neighbors and bring real change.
                                    </p>
                                </div>
                            </div>

                            <div className="flex gap-4 items-start">
                                <div className="w-11 h-11 bg-white rounded-full flex items-center justify-center text-emerald-700 shadow-md shrink-0">
                                    <BarChart3 className="w-5 h-5 fill-emerald-500/10" />
                                </div>
                                <div>
                                    <h4 className="text-sm font-bold text-slate-850">MP Analytics Dashboard</h4>
                                    <p className="text-xs text-slate-450 mt-0.5 leading-relaxed">
                                        Real-time insights to track issues, departments and area-wise progress.
                                    </p>
                                </div>
                            </div>

                        </div>
                    </div>

                    {/* Bottom Banner Card */}
                    <div className="mt-12 relative z-10 w-full max-w-sm">
                        <div className="bg-white/80 border border-white/50 backdrop-blur-xs px-6 py-3.5 rounded-2xl shadow-sm text-center">
                            <span className="text-[11px] text-slate-500 font-bold flex items-center justify-center gap-1.5">
                                Built with <Heart className="w-3.5 h-3.5 text-red-500 fill-red-500" /> for a better tomorrow
                            </span>
                            <div className="flex justify-center gap-4 text-[10.5px] font-black text-emerald-800/80 mt-1.5">
                                <span>#JanVoiceAI</span>
                                <span>#SmartGovernance</span>
                                <span>#BetterIndia</span>
                            </div>
                        </div>
                    </div>

                    {/* Detailed SVG Landscape bottom illustration */}
                    <div className="absolute bottom-0 right-0 left-0 h-48 pointer-events-none z-0 select-none opacity-45 flex items-end">
                        <svg viewBox="0 0 800 200" className="w-full h-full text-emerald-800/10 fill-current">
                            {/* Silhouette skyline */}
                            <rect x="50" y="80" width="40" height="120" rx="3" />
                            <rect x="100" y="60" width="55" height="140" rx="3" />
                            <rect x="175" y="90" width="45" height="110" rx="3" />
                            <rect x="230" y="40" width="50" height="160" rx="3" />
                            <rect x="290" y="70" width="60" height="130" rx="3" />
                            <rect x="360" y="100" width="35" height="100" rx="3" />
                            <rect x="620" y="70" width="50" height="130" rx="3" />
                            <rect x="680" y="50" width="60" height="150" rx="3" />
                            <rect x="750" y="90" width="40" height="110" rx="3" />

                            {/* Soft Hills overlay */}
                            <path d="M 0 160 Q 200 120 400 160 T 800 150 L 800 200 L 0 200 Z" opacity="0.6" />
                            <path d="M 0 170 Q 300 140 600 180 T 800 170 L 800 200 L 0 200 Z" opacity="0.9" />

                            {/* Park elements */}
                            {/* Smart Street lamp */}
                            <line x1="530" y1="180" x2="530" y2="90" stroke="currentColor" strokeWidth="2.5" />
                            <path d="M 520 90 L 540 90 L 530 80 Z" />
                            <circle cx="530" cy="98" r="5" fill="#f59e0b" opacity="0.8" />

                            {/* Benches */}
                            <rect x="550" y="165" width="35" height="15" rx="1.5" stroke="currentColor" strokeWidth="1.5" fill="none" />
                            <line x1="550" y1="180" x2="550" y2="170" stroke="currentColor" strokeWidth="2" />
                            <line x1="585" y1="180" x2="585" y2="170" stroke="currentColor" strokeWidth="2" />

                            {/* Simple tree contours */}
                            <circle cx="460" cy="130" r="28" opacity="0.8" />
                            <line x1="460" y1="180" x2="460" y2="158" stroke="currentColor" strokeWidth="2.5" />

                            <circle cx="490" cy="140" r="20" opacity="0.75" />
                            <line x1="490" y1="180" x2="490" y2="160" stroke="currentColor" strokeWidth="2" />

                            <circle cx="610" cy="150" r="16" opacity="0.8" />
                            <line x1="610" y1="180" x2="610" y2="166" stroke="currentColor" strokeWidth="2" />
                        </svg>
                    </div>

                </section>

                {/* Right Pane - Centered glassmorphic login card */}
                <section className="bg-slate-50/50 flex items-center justify-center px-6 py-12 md:px-12">

                    {/* Glassmorphic floating card */}
                    <div className="bg-white border border-slate-100 rounded-[32px] shadow-2xl shadow-slate-200/50 p-8 md:p-10 max-w-sm w-full relative z-10 transition-all">

                        {/* Round shield icon badge */}
                        <div className="flex justify-center mb-5">
                            <div className="w-16 h-16 bg-[#e6f4ea] rounded-full flex items-center justify-center text-emerald-700 shadow-inner">
                                <ShieldCheck className="w-8 h-8 stroke-[1.75]" />
                            </div>
                        </div>

                        {/* Typography Header */}
                        <div className="text-center mb-6">
                            <h3 className="text-2xl font-black text-slate-805 tracking-tight">
                                {isRegister ? 'Welcome!' : 'Welcome Back!'}
                            </h3>
                            <p className="text-xs text-slate-450 font-bold mt-1">
                                {isRegister ? 'Register your local citizen profile' : 'Sign in to your JanVoice AI account'}
                            </p>
                        </div>

                        {/* Alerts */}
                        {error && (
                            <div className="mb-4 p-3 bg-red-50 border border-red-100 rounded-xl text-red-800 text-xs font-semibold text-center">
                                {error}
                            </div>
                        )}
                        {success && (
                            <div className="mb-4 p-3 bg-emerald-50 border border-emerald-100 rounded-xl text-emerald-800 text-xs font-semibold text-center">
                                {success}
                            </div>
                        )}

                        {/* Form */}
                        <form onSubmit={handleSubmit} className="space-y-4">

                            {/* Username Input Field */}
                            <div>
                                <label className="block text-slate-700 text-xs font-extrabold mb-1.5">Username</label>
                                <div className="relative">
                                    <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">
                                        <User className="w-4 h-4" />
                                    </span>
                                    <input
                                        type="text"
                                        value={username}
                                        onChange={(e) => setUsername(e.target.value)}
                                        placeholder="Enter your username"
                                        className="w-full bg-white border border-slate-200 focus:border-emerald-700 focus:ring-1 focus:ring-emerald-700 rounded-xl py-2.5 pl-9 pr-4 text-slate-800 text-sm outline-none transition-all placeholder:text-slate-400"
                                    />
                                </div>
                            </div>

                            {/* Password Input Field */}
                            <div>
                                <label className="block text-slate-700 text-xs font-extrabold mb-1.5">Password</label>
                                <div className="relative">
                                    <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">
                                        <Lock className="w-4 h-4" />
                                    </span>
                                    <input
                                        type={showPassword ? 'text' : 'password'}
                                        value={password}
                                        onChange={(e) => setPassword(e.target.value)}
                                        placeholder="Enter your password"
                                        className="w-full bg-white border border-slate-200 focus:border-emerald-700 focus:ring-1 focus:ring-emerald-700 rounded-xl py-2.5 pl-9 pr-10 text-slate-800 text-sm outline-none transition-all placeholder:text-slate-400"
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword(!showPassword)}
                                        className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-650 outline-none"
                                    >
                                        {showPassword ? <EyeOff className="w-4.5 h-4.5" /> : <Eye className="w-4.5 h-4.5" />}
                                    </button>
                                </div>
                            </div>

                            {/* Dropdown for Citizens only */}
                            {role === 'CITIZEN' && (
                                <div>
                                    <label className="block text-slate-700 text-xs font-extrabold mb-1.5">Constituency / Area</label>
                                    <div className="relative">
                                        <span className="absolute inset-y-0 left-0 pl-3 flex items-center text-slate-400">
                                            <MapPin className="w-4 h-4" />
                                        </span>
                                        <select
                                            value={wardArea}
                                            onChange={(e) => setWardArea(e.target.value)}
                                            className="w-full bg-white border border-slate-200 focus:border-emerald-700 focus:ring-1 focus:ring-emerald-700 rounded-xl py-2.5 pl-9 pr-10 text-slate-700 text-sm outline-none transition-all cursor-pointer appearance-none"
                                        >
                                            <option value="Ward 1">Ward 1 - Sector Alpha</option>
                                            <option value="Ward 2">Ward 2 - Sector Beta</option>
                                            <option value="Ward 3">Ward 3 - Sector Gamma</option>
                                            <option value="Ward 4">Ward 4 - Sector Delta</option>
                                            <option value="Ward 5">Sector Epsilon (Demo Default)</option>
                                            <option value="Ward 6">Ward 6 - Sector Zeta</option>
                                        </select>
                                        <span className="absolute inset-y-0 right-0 pr-3.5 flex items-center pointer-events-none text-slate-450 text-[10px]">
                                            ▼
                                        </span>
                                    </div>
                                </div>
                            )}

                            {/* Sign In Primary Action Button */}
                            <button
                                type="submit"
                                disabled={loading}
                                className="w-full bg-emerald-755 hover:bg-emerald-800 active:scale-[0.98] text-white py-3.5 rounded-xl text-sm font-bold transition-all shadow-md shadow-emerald-805/10 flex justify-center items-center gap-1.5 focus:outline-none cursor-pointer mt-6"
                                style={{ backgroundColor: '#2e7d32' }}
                            >
                                <LogIn className="w-4 h-4" />
                                {isRegister ? 'Sign Up' : 'Sign In'}
                            </button>
                        </form>

                        {/* Separator */}
                        <div className="my-5 flex items-center justify-between text-xs text-slate-400">
                            <span className="w-[42%] border-b border-slate-100"></span>
                            <span>or</span>
                            <span className="w-[42%] border-b border-slate-100"></span>
                        </div>

                        {/* Register Toggle Outline Button */}
                        <button
                            onClick={() => setIsRegister(!isRegister)}
                            className="w-full border border-emerald-700 text-emerald-800 hover:bg-slate-50 py-3.5 rounded-xl text-sm font-bold transition-all flex justify-center items-center gap-1.5 cursor-pointer bg-white"
                        >
                            <UserPlus className="w-4 h-4" />
                            {isRegister ? 'Already registered? Sign In' : "Don't have an account? Register"}
                        </button>

                    </div>
                </section>

            </main>

            {/* 3. Dark Green Quick Autofills Footer */}
            <footer className="w-full bg-[#0c382e] py-4.5 px-6 border-t border-emerald-950 text-center relative z-20">
                <div className="flex items-center justify-center gap-2 mb-2.5">
                    <p className="text-[10px] text-emerald-400 font-black uppercase tracking-wider select-none">
                        Hackathon Quick Demo
                    </p>
                    <span className="rounded-full border border-emerald-400/40 bg-emerald-900/40 px-2.5 py-0.5 text-[10px] font-semibold text-emerald-200">
                        Demo Mode
                    </span>
                </div>
                <div className="flex flex-wrap justify-center gap-3">
                    <button
                        onClick={() => handleDemoLogin('CITIZEN')}
                        className="px-4 py-2 border border-emerald-400/40 hover:bg-emerald-900/50 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all outline-none cursor-pointer"
                    >
                        <User className="w-4 h-4 text-emerald-350" />
                        Continue as Citizen
                    </button>

                    <button
                        onClick={() => handleDemoLogin('MP')}
                        className="px-4 py-2 border border-emerald-400/40 hover:bg-emerald-900/50 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all outline-none cursor-pointer"
                    >
                        <BarChart3 className="w-4 h-4 text-emerald-350" />
                        Continue as MP/Admin
                    </button>
                </div>
            </footer>

        </div>
    );
}
