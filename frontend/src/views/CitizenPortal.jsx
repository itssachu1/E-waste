import ComplaintMap from "../components/ComplaintMap";
import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { complaintService } from '../services/api';
import {
    Building, LayoutDashboard, Send, FileText, Users, Sparkles,
    Bell, User, HelpCircle, LogOut, MapPin, Mic, MicOff, Image as ImageIcon,
    ArrowUp, CheckCircle2, Hourglass, BarChart3, ChevronRight, X, ArrowLeft,
    ChevronDown
} from 'lucide-react';

const fallbackComplaints = [
    {
        id: 101,
        originalText: 'Electric wire hanging near market creates immediate danger.',
        translatedText: 'Electric wire hanging near market creates immediate danger.',
        category: 'SAFETY',
        urgency: 'CRITICAL',
        status: 'PENDING',
        upvotes: 14,
        priorityScore: 96,
        affectedPeople: 42,
        duplicateCount: 3,
        suggestedDepartment: 'Emergency Response',
        priorityReason: 'Immediate public safety hazard with wide community impact.',
        isEmergency: true,
        language: 'en',
    },
    {
        id: 102,
        originalText: 'Sewer overflow near school is affecting daily life.',
        translatedText: 'Sewer overflow near school is affecting daily life.',
        category: 'SANITATION',
        urgency: 'HIGH',
        status: 'IN_PROGRESS',
        upvotes: 9,
        priorityScore: 84,
        affectedPeople: 28,
        duplicateCount: 2,
        suggestedDepartment: 'Municipal Sanitation',
        priorityReason: 'Repeated sanitation issue affecting school routes and residents.',
        isEmergency: false,
        language: 'en',
    },
    {
        id: 103,
        originalText: 'Water supply stopped for 2 days in the neighborhood.',
        translatedText: 'Water supply stopped for 2 days in the neighborhood.',
        category: 'WATER',
        urgency: 'HIGH',
        status: 'PENDING',
        upvotes: 8,
        priorityScore: 78,
        affectedPeople: 35,
        duplicateCount: 1,
        suggestedDepartment: 'Water Department',
        priorityReason: 'Essential utility outage with repeated community reports.',
        isEmergency: false,
        language: 'en',
    },
    {
        id: 104,
        originalText: 'Broken road near hospital needs urgent repair.',
        translatedText: 'Broken road near hospital needs urgent repair.',
        category: 'ROAD',
        urgency: 'MEDIUM',
        status: 'PENDING',
        upvotes: 5,
        priorityScore: 65,
        affectedPeople: 12,
        duplicateCount: 0,
        suggestedDepartment: 'Public Works',
        priorityReason: 'Road damage near a hospital increases access risk.',
        isEmergency: false,
        language: 'en',
    },
];

/**
 * CitizenPortal redesigned in Google Material 3 / Modern SaaS aesthetic.
 * Connects with existing Spring Boot APIs while implementing:
 * - Sidebar Navigation Drawer (Dashboard, Submit, My Complaints, Community, AI Insights, Notifications, Profile)
 * - Citizen Overview Dashboard (Stats widgets, Quick actions, Map, Trends list)
 * - Multi-step Submit Complaint Wizard (Voice transcription, Image uploads, AI predictions)
 * - Grievance detail drawer overlays & community upvote boards
 */
export default function CitizenPortal() {
    const { user, logout } = useContext(AuthContext);
    const [activeSection, setActiveSection] = useState('DASHBOARD'); // DASHBOARD, SUBMIT, MY_COMPLAINTS, COMMUNITY, INSIGHTS, NOTIFICATIONS, PROFILE
    const [complaints, setComplaints] = useState([]);
    const [selectedComplaint, setSelectedComplaint] = useState(null); // Detail drawer toggle
    const [loading, setLoading] = useState(false);
    const [apiMessage, setApiMessage] = useState({ text: '', type: '' });

    // Submit Form States
    const [formStep, setFormStep] = useState(1);
    const [complaintText, setComplaintText] = useState('');
    const [attachedFile, setAttachedFile] = useState(null);
    const [recording, setRecording] = useState(false);
    const [prediction, setPrediction] = useState({ category: 'Road Damage', urgency: 'High' });

    // Tab states inside Complaint details
    const [detailTab, setDetailTab] = useState('TIMELINE');

    // Load complaints logged in citizen ward area
    const fetchComplaints = async () => {
        try {
            const data = await complaintService.getByWard(user.wardArea || 'Ward 5');
            setComplaints(Array.isArray(data) && data.length > 0 ? data : fallbackComplaints);
        } catch (e) {
            console.error(e);
            setComplaints(fallbackComplaints);
        }
    };

    useEffect(() => {
        fetchComplaints();
    }, [user.wardArea]);

    // Handle Form Submission
    const handleFormSubmit = async (e) => {
        e.preventDefault();
        if (!complaintText.trim() || loading) return;

        setLoading(true);
        setApiMessage({ text: '', type: '' });

        try {
            const result = await complaintService.submit(
                complaintText,
                user.wardArea || 'Ward 5',
                user.userId
            );

            if (result.parentComplaint) {
                setApiMessage({
                    text: `Duplicate matched! Autolinked to active issue #${result.parentComplaint.id}. Upvote registered!`,
                    type: 'duplicate'
                });
            } else {
                setApiMessage({
                    text: 'Grievance submitted successfully as a new master ticket!',
                    type: 'success'
                });
            }

            // Reset Form
            setComplaintText('');
            setAttachedFile(null);
            setFormStep(1);
            fetchComplaints();
            setActiveSection('MY_COMPLAINTS');
        } catch (err) {
            setApiMessage({
                text: 'Failed to record details. Please check database connectivity.',
                type: 'error'
            });
        } finally {
            setLoading(false);
        }
    };

    // Upvoting trigger
    const handleUpvote = async (id, e) => {
        if (e) e.stopPropagation();
        try {
            const updated = await complaintService.upvote(id, user.userId);
            setApiMessage({
                text: `Voter support cast for Issue #${updated.id}!`,
                type: 'success'
            });
            fetchComplaints();
            if (selectedComplaint && selectedComplaint.id === id) {
                setSelectedComplaint(prev => ({ ...prev, upvotes: updated.upvotes }));
            }
        } catch (err) {
            setApiMessage({
                text: 'You have already upvoted this grievance.',
                type: 'error'
            });
        }
    };

    // Simulate dialect speech recorder
    const triggerVoiceRecord = () => {
        if (!recording) {
            setRecording(true);
            setTimeout(() => {
                setComplaintText('हमारे वार्ड में सड़क पर सीवर का पानी बह रहा है और बहुत बदबू आ रही है।');
                setRecording(false);
                setPrediction({ category: 'Sanitation', urgency: 'High' });
                setApiMessage({
                    text: 'Voice transcribed (Hindi): हमारे वार्ड में सड़क पर सीवर का पानी बह रहा है...',
                    type: 'success'
                });
            }, 2500);
        } else {
            setRecording(false);
        }
    };

    // Simulate file upload
    const triggerFileUpload = () => {
        setAttachedFile('damaged_drain_road_5.png');
        setApiMessage({
            text: 'File attached successfully: damaged_drain_road_5.png',
            type: 'success'
        });
    };

    return (
        <div className="min-h-screen bg-[#F8FAFC] text-[#111827] flex font-sans leading-relaxed">

            {/* ==================================================== */}
            {/* 1. LEFT NAVIGATION SIDEBAR DRAWER */}
            {/* ==================================================== */}
            <aside className="w-64 bg-white border-r border-[#E5E7EB] hidden md:flex flex-col justify-between shrink-0 h-screen sticky top-0">
                <div>
                    {/* Brand Header */}
                    <div className="p-6 border-b border-[#E5E7EB] flex items-center gap-3">
                        <div className="text-[#0F9D58]">
                            <Building className="w-8 h-8" />
                        </div>
                        <div>
                            <h1 className="text-base font-extrabold tracking-tight leading-5">JanVoice AI</h1>
                            <span className="text-[10px] text-[#0F9D58] font-black uppercase tracking-wider">
                                Citizen Portal
                            </span>
                        </div>
                    </div>

                    {/* Navigation Links */}
                    <nav className="p-4 space-y-1">
                        {[
                            { id: 'DASHBOARD', label: 'Dashboard', icon: LayoutDashboard },
                            { id: 'SUBMIT', label: 'Submit Complaint', icon: Send },
                            { id: 'MY_COMPLAINTS', label: 'My Complaints', icon: FileText },
                            { id: 'COMMUNITY', label: 'Community', icon: Users },
                            { id: 'INSIGHTS', label: 'AI Insights', icon: Sparkles },
                            { id: 'NOTIFICATIONS', label: 'Notifications', icon: Bell, indicator: 3 },
                            { id: 'PROFILE', label: 'Profile', icon: User }
                        ].map((menu) => {
                            const IconComp = menu.icon;
                            return (
                                <button
                                    key={menu.id}
                                    onClick={() => {
                                        setActiveSection(menu.id);
                                        setApiMessage({ text: '', type: '' });
                                    }}
                                    className={`w-full flex items-center justify-between px-4 py-3 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer ${activeSection === menu.id
                                        ? 'bg-emerald-50 text-[#0F9D58]'
                                        : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50'
                                        }`}
                                >
                                    <div className="flex items-center gap-3">
                                        <IconComp className="w-4 h-4" />
                                        {menu.label}
                                    </div>
                                    {menu.indicator && (
                                        <span className="bg-emerald-600 text-white text-[9px] font-black px-2 py-0.5 rounded-full">
                                            {menu.indicator}
                                        </span>
                                    )}
                                </button>
                            );
                        })}
                    </nav>
                </div>

                {/* Sidebar Graphics and Profile Card */}
                <div className="p-4 border-t border-[#E5E7EB]">

                    {/* Decorative Voice/Community Suggestion banner */}
                    <div className="bg-[#eaf6ee] rounded-2xl p-4 mb-4 text-center border border-[#d8ecd8]">
                        <p className="text-[11px] font-extrabold text-[#0F9D58] leading-tight">
                            Make your voice heard.
                        </p>
                        <p className="text-[10px] text-slate-500 mt-1 font-medium">
                            Report local municipal concerns.
                        </p>

                        {/* Minimal SVG Citizens illustration representation */}
                        <div className="mt-3 flex justify-center gap-1.5 opacity-60">
                            <span className="w-2.5 h-6 bg-[#0F9D58] rounded-full"></span>
                            <span className="w-2.5 h-8 bg-emerald-500 rounded-full"></span>
                            <span className="w-2.5 h-5 bg-[#0F9D58] rounded-full"></span>
                        </div>
                    </div>

                    <div className="flex items-center justify-between">
                        <div className="truncate pr-2">
                            <p className="text-xs font-bold truncate">{user?.userName || 'Atharv Shukla'}</p>
                            <p className="text-[10px] text-slate-400 font-semibold truncate bg-slate-100 px-2 py-0.5 rounded-md inline-block max-w-full">
                                {user?.wardArea || 'Ward 5'}
                            </p>
                        </div>
                        <button
                            onClick={logout}
                            className="p-2 border border-[#E5E7EB] hover:bg-red-50 text-slate-500 hover:text-red-650 rounded-xl transition-all cursor-pointer"
                        >
                            <LogOut className="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </aside>

            {/* ==================================================== */}
            {/* 2. MAIN LAYOUT AND COMPONENT PANELS */}
            {/* ==================================================== */}
            <div className="flex-1 flex flex-col min-w-0">

                {/* Top Header Navbar */}
                <header className="bg-white border-b border-[#E5E7EB] px-8 py-4 flex flex-row items-center justify-between sticky top-0 z-30">
                    <div>
                        <h2 className="text-base font-black text-slate-900">
                            {activeSection === 'DASHBOARD' && 'Good Morning, Atharv 👋'}
                            {activeSection === 'SUBMIT' && 'Submit a New Complaint'}
                            {activeSection === 'MY_COMPLAINTS' && 'My Grievance Logs'}
                            {activeSection === 'COMMUNITY' && 'Community Voting Board'}
                            {activeSection === 'INSIGHTS' && 'AI Activity Insights'}
                            {activeSection === 'NOTIFICATIONS' && 'Constituency Notifications'}
                            {activeSection === 'PROFILE' && 'Citizen Profile Configuration'}
                        </h2>
                        <p className="text-xs text-slate-400 font-semibold mt-0.5">
                            {activeSection === 'DASHBOARD' && "Here's what is happening in your area today."}
                            {activeSection === 'SUBMIT' && 'Help us understand the issue you are facing.'}
                            {activeSection === 'MY_COMPLAINTS' && 'Track the progress of your submitted tickets.'}
                            {activeSection === 'COMMUNITY' && 'Review, support and upvote local ward concerns.'}
                            {activeSection === 'INSIGHTS' && 'Constituency summaries generated by Gemini.'}
                            {activeSection === 'NOTIFICATIONS' && 'Stay updated on municipal actions.'}
                            {activeSection === 'PROFILE' && 'Update account details and location parameters.'}
                        </p>
                    </div>

                    <div className="flex items-center gap-3">
                        <span className="text-xs font-bold text-slate-700 bg-slate-100 border border-[#E5E7EB] px-3.5 py-2 rounded-xl flex items-center gap-1.5 shadow-2xs">
                            <MapPin className="w-3.5 h-3.5 text-[#0F9D58]" />
                            {user?.wardArea || 'Ward 5, Sector Alpha'}
                        </span>

                        {/* Mobile/Tablet Logout */}
                        <button
                            onClick={logout}
                            className="text-xs font-semibold text-slate-600 hover:text-red-650 flex md:hidden items-center gap-1.5 bg-slate-100 hover:bg-slate-200/50 p-2 rounded-xl transition-all border border-[#E5E7EB]"
                        >
                            <LogOut className="w-3.5 h-3.5" />
                        </button>
                    </div>
                </header>

                {/* Dynamic Panels */}
                <main className="flex-1 p-6 md:p-8 space-y-8 overflow-y-auto max-w-6xl w-full mx-auto">

                    {/* Local alert toaster */}
                    {apiMessage.text && (
                        <div className={`p-4 rounded-xl border text-xs leading-relaxed flex items-center justify-between gap-3 ${apiMessage.type === 'duplicate'
                            ? 'bg-yellow-50 border-yellow-200 text-yellow-900'
                            : apiMessage.type === 'error'
                                ? 'bg-red-50 border-red-200 text-red-800'
                                : 'bg-emerald-50 border-emerald-200 text-emerald-800'
                            }`}>
                            <div className="flex items-center gap-3">
                                {apiMessage.type === 'success' && <CheckCircle2 className="w-4 h-4 text-emerald-650 shrink-0" />}
                                {apiMessage.type === 'duplicate' && <Sparkles className="w-4 h-4 text-yellow-600 shrink-0 animate-pulse" />}
                                {apiMessage.type === 'error' && <HelpCircle className="w-4 h-4 text-red-600 shrink-0" />}
                                <span className="font-semibold">{apiMessage.text}</span>
                            </div>
                            <button onClick={() => setApiMessage({ text: '', type: '' })} className="hover:opacity-75">
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION A: CITIZEN OVERVIEW DASHBOARD */}
                    {/* ==================================================== */}
                    {activeSection === 'DASHBOARD' && (
                        <div className="space-y-6">
                            <div className="rounded-3xl border border-rose-200 bg-gradient-to-r from-rose-50 to-orange-50 p-5 shadow-sm">
                                <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                                    <div>
                                        <p className="text-[10px] font-black uppercase tracking-[0.25em] text-rose-700">People Priority Engine</p>
                                        <h3 className="text-lg font-black text-slate-900">Critical issues are surfaced first so the community can act fast.</h3>
                                    </div>
                                    <div className="rounded-2xl bg-white/80 px-4 py-3 text-sm font-semibold text-slate-700">
                                        4 priority issues ready for action
                                    </div>
                                </div>
                            </div>

                            {/* Row 1: 4 Statistics Cards */}
                            <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
                                {[
                                    { label: 'Priority Score', value: '96', desc: 'Highest urgent issue', icon: Sparkles, border: 'border-l-rose-600', text: 'text-rose-700', bg: 'bg-rose-50' },
                                    { label: 'In Progress', value: '2', desc: 'Complaints', icon: Hourglass, border: 'border-l-orange-500', text: 'text-orange-700', bg: 'bg-orange-50' },
                                    { label: 'Resolved Tickets', value: '1', desc: 'Complaints status', icon: CheckCircle2, border: 'border-l-blue-500', text: 'text-blue-700', bg: 'bg-blue-50' },
                                    { label: 'Community Support', value: '36', desc: 'Total Upvotes Count', icon: ArrowUp, border: 'border-l-[#0F9D58]', text: 'text-emerald-700', bg: 'bg-emerald-50' }
                                ].map((stat, idx) => {
                                    const SIcon = stat.icon;
                                    return (
                                        <div key={idx} className={`bg-white border border-[#E5E7EB] border-l-4 ${stat.border} rounded-2xl p-5 shadow-xs flex items-center justify-between`}>
                                            <div>
                                                <h4 className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider">{stat.label}</h4>
                                                <p className="text-2xl font-black text-slate-900 mt-1 leading-none">{stat.value}</p>
                                                <span className="text-[9.5px] text-slate-400 font-semibold block mt-1">{stat.desc}</span>
                                            </div>
                                            <div className={`p-3 ${stat.bg} ${stat.text} rounded-xl`}>
                                                <SIcon className="w-5 h-5" />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>

                            {/* Row 2: Grid column widgets */}
                            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

                                {/* Visual Widget 1: Quick actions */}
                                <div className="lg:col-span-4 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs flex flex-col justify-between">
                                    <div>
                                        <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Quick Actions</h3>
                                        <div className="space-y-3">
                                            <button
                                                onClick={() => setActiveSection('SUBMIT')}
                                                className="w-full flex items-center justify-between px-4 py-3 bg-[#0F9D58] hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer"
                                            >
                                                Submit Complaint
                                                <span className="text-sm font-black">+</span>
                                            </button>
                                            <button
                                                onClick={() => {
                                                    setActiveSection('SUBMIT');
                                                    triggerVoiceRecord();
                                                }}
                                                className="w-full flex items-center justify-between px-4 py-3 bg-white hover:bg-slate-50 border border-[#E5E7EB] text-slate-700 rounded-xl text-xs font-bold transition-all cursor-pointer"
                                            >
                                                Voice Complaint
                                                <Mic className="w-4 h-4 text-[#0F9D58]" />
                                            </button>
                                            <button
                                                onClick={() => {
                                                    setActiveSection('SUBMIT');
                                                    triggerFileUpload();
                                                }}
                                                className="w-full flex items-center justify-between px-4 py-3 bg-white hover:bg-slate-50 border border-[#E5E7EB] text-slate-700 rounded-xl text-xs font-bold transition-all cursor-pointer"
                                            >
                                                Photo Complaint
                                                <ImageIcon className="w-4 h-4 text-slate-450" />
                                            </button>
                                        </div>
                                    </div>

                                    <div className="text-[10px] text-slate-450 font-semibold mt-4 text-center">
                                        AI assists in translating local dialects.
                                    </div>
                                </div>

                                {/* Visual Widget 2: Map Preview panel */}
                                <div className="lg:col-span-4 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs flex flex-col justify-between">
                                    <div>
                                        <h3 className="text-sm font-black text-slate-905 border-b border-slate-100 pb-3 mb-3">Nearby Issues Map</h3>
                                        {/* Simulated vector map graphic */}
                                        <div className="h-72 rounded-xl overflow-hidden">
                                      <ComplaintMap />
                                       </div>
                                       </div>

                                    <button className="w-full mt-3 py-2 bg-slate-50 hover:bg-slate-100 border border-[#E5E7EB] rounded-xl text-xs text-slate-600 font-bold transition-all text-center">
                                        View on Map
                                    </button>
                                </div>

                                {/* Visual Widget 3: Trending Issues list */}
                                <div className="lg:col-span-4 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs flex flex-col justify-between">
                                    <div>
                                        <div className="flex justify-between items-center border-b border-slate-100 pb-3 mb-3">
                                            <h3 className="text-sm font-black text-slate-900">Trending Issues</h3>
                                            <span className="text-[9px] font-black bg-red-50 text-red-750 px-2 py-0.5 rounded-md border border-red-150">
                                                Most Voted
                                            </span>
                                        </div>

                                        <div className="space-y-2.5">
                                            {[
                                                { title: 'Broken Street Light', area: 'Ward 5', votes: 32 },
                                                { title: 'Water Leakage', area: 'Ward 3', votes: 29 },
                                                { title: 'Garbage Overflow', area: 'Ward 2', votes: 24 }
                                            ].map((item, idx) => (
                                                <div key={idx} className="flex justify-between items-center text-xs">
                                                    <div>
                                                        <p className="font-bold text-slate-800">{item.title}</p>
                                                        <span className="text-[9.5px] text-slate-400 font-semibold">{item.area}</span>
                                                    </div>
                                                    <span className="text-[10px] text-emerald-805 font-bold flex items-center gap-0.5">
                                                        <ArrowUp className="w-3 h-3 text-[#0F9D58]" />
                                                        {item.votes}
                                                    </span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>

                                    <button
                                        onClick={() => setActiveSection('COMMUNITY')}
                                        className="w-full mt-3 text-center text-[10.5px] text-[#0F9D58] hover:text-emerald-700 font-bold hover:underline"
                                    >
                                        View All Issues
                                    </button>
                                </div>

                            </div>

                            {/* Row 3: Insights & Updates */}
                            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">

                                {/* Update log */}
                                <div className="bg-white border border-[#E5E7EB] p-5 rounded-2xl shadow-xs">
                                    <h4 className="text-xs text-slate-400 font-black uppercase tracking-wider mb-3">Recent Updates</h4>
                                    <div className="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-1.5">
                                        <p className="text-xs text-slate-800 font-bold leading-relaxed">
                                            Your complaint about <span className="text-[#0F9D58] font-black">"Water Leakage"</span> has been assigned to Engineering Department.
                                        </p>
                                        <span className="text-[10px] text-slate-400 font-semibold block">2 hours ago</span>
                                    </div>
                                </div>

                                {/* AI suggestion bubble */}
                                <div className="bg-emerald-50/60 border border-emerald-100 p-5 rounded-2xl shadow-xs flex items-center justify-between">
                                    <div className="space-y-1.5">
                                        <div className="flex items-center gap-1.5">
                                            <Sparkles className="w-4 h-4 text-[#0F9D58]" />
                                            <h4 className="text-xs font-black text-emerald-850">AI Weekly Diagnostic Insight</h4>
                                        </div>
                                        <p className="text-[11px] text-emerald-800 font-medium leading-relaxed max-w-sm">
                                            Garbage overflow complaints are high in your area this week. We suggest immediate attention.
                                        </p>
                                        <button
                                            onClick={() => setActiveSection('INSIGHTS')}
                                            className="text-[10.5px] text-[#0F9D58] hover:text-emerald-800 font-extrabold flex items-center gap-1 hover:underline"
                                        >
                                            View Insights ➔
                                        </button>
                                    </div>

                                    <div className="text-emerald-800/25 p-2 hidden sm:block shrink-0">
                                        <Building className="w-14 h-14" />
                                    </div>
                                </div>

                            </div>

                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION B: SUBMIT COMPLAINT FORM PANEL */}
                    {/* ==================================================== */}
                    {activeSection === 'SUBMIT' && (
                        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

                            {/* Left Form: Multi-step layout */}
                            <div className="lg:col-span-8 bg-white border border-[#E5E7EB] p-6 md:p-8 rounded-3xl shadow-sm">

                                {/* Form header steps */}
                                <div className="flex items-center gap-4 border-b border-slate-100 pb-5 mb-6">
                                    <div className="flex gap-1.5 items-center">
                                        {[1, 2, 3, 4].map((step) => (
                                            <div
                                                key={step}
                                                onClick={() => setFormStep(step)}
                                                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold cursor-pointer transition-all ${formStep === step
                                                    ? 'bg-[#0F9D58] text-white'
                                                    : formStep > step
                                                        ? 'bg-[#eaf6ee] text-[#0F9D58] border border-[#0F9D58]/30'
                                                        : 'bg-slate-50 border border-slate-200 text-slate-400'
                                                    }`}
                                            >
                                                {step}
                                            </div>
                                        ))}
                                    </div>

                                    <span className="text-xs text-slate-500 font-bold">
                                        {formStep === 1 && 'Step 1: Describe the issue'}
                                        {formStep === 2 && 'Step 2: Add Details'}
                                        {formStep === 3 && 'Step 3: Location Pin Selection'}
                                        {formStep === 4 && 'Step 4: Review & Submit ticket'}
                                    </span>
                                </div>

                                <form onSubmit={handleFormSubmit} className="space-y-6">

                                    {/* Step 1 Content */}
                                    {formStep === 1 && (
                                        <div className="space-y-4">

                                            <div>
                                                <label className="block text-xs font-black text-slate-805 mb-2">Describe the Issue</label>
                                                <textarea
                                                    rows={4}
                                                    value={complaintText}
                                                    onChange={(e) => setComplaintText(e.target.value)}
                                                    placeholder="Please describe your complaint in detail..."
                                                    className="w-full bg-white border border-slate-200 focus:border-[#0F9D58] focus:ring-1 focus:ring-[#0F9D58] rounded-xl p-4 text-slate-800 text-sm outline-none transition-all shadow-2xs resize-none"
                                                />
                                            </div>

                                            {/* Photo Ingest Box */}
                                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

                                                <div
                                                    onClick={triggerFileUpload}
                                                    className="border-2 border-dashed border-slate-200 hover:border-[#0F9D58] p-5 rounded-2xl flex flex-col items-center justify-center text-center cursor-pointer bg-slate-50/50 hover:bg-slate-50 transition-all min-h-[140px]"
                                                >
                                                    <ImageIcon className="w-7 h-7 text-slate-440 mb-2" />
                                                    <span className="text-xs font-bold text-slate-700 block">Upload Photo (Optional)</span>
                                                    <span className="text-[10px] text-slate-400 mt-1 block">Click to upload or drag and drop</span>
                                                </div>

                                                {/* Dialect Voice recorder mock */}
                                                <div className="border border-slate-200 p-5 rounded-2xl flex flex-col justify-between bg-slate-50/50 min-h-[140px]">
                                                    <div>
                                                        <span className="text-xs font-bold text-slate-700 block">Record Voice (Optional)</span>
                                                        <span className="text-[10px] text-slate-400 mt-1 block">Simulates regional speech recordings</span>
                                                    </div>

                                                    {recording ? (
                                                        <div className="flex items-center gap-2 py-1.5 px-3 bg-red-50 border border-red-100 rounded-xl text-red-700 text-xs font-bold animate-pulse">
                                                            <span className="w-1.5 h-1.5 bg-red-600 rounded-full animate-ping"></span>
                                                            Listening to regional speech...
                                                        </div>
                                                    ) : (
                                                        <button
                                                            type="button"
                                                            onClick={triggerVoiceRecord}
                                                            className="w-full py-2 bg-emerald-50 hover:bg-emerald-100 border border-emerald-250 text-emerald-800 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-1.5 cursor-pointer outline-none"
                                                        >
                                                            <Mic className="w-3.5 h-3.5" />
                                                            Record Hindi Speech
                                                        </button>
                                                    )}
                                                </div>

                                            </div>

                                            {/* Display attached items */}
                                            {attachedFile && (
                                                <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl flex justify-between items-center text-xs">
                                                    <span className="text-slate-650 flex items-center gap-2 truncate font-bold">
                                                        <ImageIcon className="w-4 h-4 text-[#0F9D58]" />
                                                        {attachedFile}
                                                    </span>
                                                    <button onClick={() => setAttachedFile(null)} className="text-red-650 font-bold hover:underline">
                                                        Remove
                                                    </button>
                                                </div>
                                            )}

                                            {/* AI predictions output */}
                                            <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-100">
                                                <div className="p-3.5 bg-slate-50 border border-slate-150 rounded-xl">
                                                    <span className="text-[10px] text-slate-400 font-extrabold block">AI Predicted Category</span>
                                                    <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5 mt-1">
                                                        <Building className="w-3.5 h-3.5 text-[#0F9D58]" />
                                                        {prediction.category}
                                                    </span>
                                                </div>
                                                <div className="p-3.5 bg-slate-50 border border-slate-150 rounded-xl">
                                                    <span className="text-[10px] text-slate-400 font-extrabold block">AI Predicted Urgency</span>
                                                    <span className="text-xs font-bold text-slate-805 flex items-center gap-1.5 mt-1 text-orange-700">
                                                        <span className="w-2 h-2 bg-orange-650 rounded-full"></span>
                                                        {prediction.urgency}
                                                    </span>
                                                </div>
                                            </div>

                                        </div>
                                    )}

                                    {/* Step 2 Content */}
                                    {formStep === 2 && (
                                        <div className="space-y-4">
                                            <div>
                                                <label className="block text-xs font-black text-slate-800 mb-1.5">Supporting Information</label>
                                                <select className="w-full bg-white border border-slate-200 rounded-xl py-2.5 px-4 text-xs font-bold text-slate-700 outline-none">
                                                    <option>Resident Society Grievances</option>
                                                    <option>Public Street Utility Damage</option>
                                                </select>
                                            </div>

                                            <div>
                                                <label className="block text-xs font-black text-slate-800 mb-1.5">Additional comments</label>
                                                <textarea rows={3} placeholder="Provide any extra landmarks..." className="w-full bg-white border border-slate-200 rounded-xl p-3 text-xs outline-none focus:border-[#0F9D58] resize-none" />
                                            </div>
                                        </div>
                                    )}

                                    {/* Step 3 Content */}
                                    {formStep === 3 && (
                                        <div className="space-y-4 text-center">
                                            <p className="text-xs font-bold text-slate-500">Pick the exact location coordinates of the incident:</p>
                                            <div className="h-44 bg-[#eef3f6] border border-slate-200 rounded-xl flex items-center justify-center relative overflow-hidden">
                                                <MapPin className="w-8 h-8 text-red-500 fill-red-500 animate-bounce absolute" />
                                                <span className="absolute bottom-3 bg-white/95 px-3 py-1 rounded-full text-[10px] font-bold border shadow-xs">
                                                    GPS: 12.9716° N, 77.5946° E
                                                </span>
                                            </div>
                                        </div>
                                    )}

                                    {/* Step 4 Content */}
                                    {formStep === 4 && (
                                        <div className="space-y-4 bg-slate-50/50 p-4 border border-slate-150 rounded-2xl">
                                            <h4 className="text-xs font-black text-slate-805 uppercase tracking-wider mb-2">Review Your Grievance Details</h4>
                                            <p className="text-xs text-slate-800 font-bold leading-relaxed whitespace-pre-line">"{complaintText || 'No text entry matches'}"</p>

                                            <div className="flex gap-4 text-[10px] text-slate-450 font-bold pt-3 border-t border-slate-200">
                                                <span>Ward Constituency: {user.wardArea || 'Ward 5'}</span>
                                                <span>Attached Image: {attachedFile || 'None'}</span>
                                            </div>
                                        </div>
                                    )}

                                    {/* Navigation trigger button row */}
                                    <div className="flex justify-between md:justify-end gap-3 pt-6 border-t border-slate-100 select-none">
                                        {formStep > 1 && (
                                            <button
                                                type="button"
                                                onClick={() => setFormStep(formStep - 1)}
                                                className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-xl text-xs font-bold transition-all cursor-pointer outline-none"
                                            >
                                                Prev step
                                            </button>
                                        )}

                                        {formStep < 4 ? (
                                            <button
                                                type="button"
                                                onClick={() => setFormStep(formStep + 1)}
                                                className="px-5 py-2.5 bg-[#0F9D58] hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer outline-none ml-auto"
                                            >
                                                Next: Add Details ➔
                                            </button>
                                        ) : (
                                            <button
                                                type="submit"
                                                disabled={loading || !complaintText.trim()}
                                                className="px-6 py-2.5 bg-[#0F9D58] hover:bg-emerald-700 disabled:bg-slate-200 disabled:text-slate-400 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-emerald-720/10 cursor-pointer flex items-center justify-center gap-1.5 outline-none ml-auto"
                                            >
                                                {loading ? 'AI analyzing...' : 'Submit Grievance'}
                                            </button>
                                        )}
                                    </div>

                                </form>
                            </div>

                            {/* Right Sidebar: Prevent Duplicates list */}
                            <div className="lg:col-span-4 space-y-4">
                                <div className="bg-emerald-50/70 border border-emerald-100 rounded-2xl p-5 shadow-inner">
                                    <h3 className="text-xs font-black text-emerald-850 flex items-center gap-1.5 mb-2.5">
                                        <Sparkles className="w-4 h-4" />
                                        AI Ingestion Duplicates Assist
                                    </h3>
                                    <p className="text-[10.5px] text-emerald-800 leading-relaxed font-semibold">
                                        To prevent constituent ticketing spam, our system matches incoming complaints against open items in your Ward.
                                    </p>
                                </div>

                                <div className="bg-white border border-[#E5E7EB] p-5 rounded-2xl shadow-xs">
                                    <h3 className="text-xs font-black text-slate-805 border-b border-slate-100 pb-2.5 mb-3.5">
                                        People Priority Feed
                                    </h3>

                                    <div className="space-y-4">
                                        {complaints.slice(0, 3).map((item) => (
                                            <div key={item.id} className="text-xs space-y-1.5 p-3 rounded-xl border border-slate-100 bg-slate-50/30">
                                                <div className="flex justify-between items-center text-[10px] text-slate-400 font-bold">
                                                    <span>Ref ID: #{item.id}</span>
                                                    <span className="text-[#0F9D58]">Score {item.priorityScore || 0}</span>
                                                </div>
                                                <p className="font-bold text-slate-800 leading-normal truncate">{item.originalText}</p>
                                                <div className="flex flex-wrap gap-2 text-[10px] text-slate-500">
                                                    <span className="rounded-full bg-slate-100 px-2 py-0.5">{item.category}</span>
                                                    <span className="rounded-full bg-slate-100 px-2 py-0.5">{item.urgency}</span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>

                                    <button
                                        onClick={() => setActiveSection('COMMUNITY')}
                                        className="w-full text-center mt-4 text-[10.5px] text-[#0F9D58] font-bold hover:underline block"
                                    >
                                        View All Ward Grievances
                                    </button>
                                </div>
                            </div>

                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION C: MY COMPLAINTS LATEST LOGS PAGE */}
                    {/* ==================================================== */}
                    {activeSection === 'MY_COMPLAINTS' && (
                        <div className="bg-white border border-[#E5E7EB] rounded-3xl p-6 shadow-sm">
                            <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3.5 mb-4">My Submitted Grievances</h3>

                            {complaints.length === 0 ? (
                                <div className="text-center py-12">
                                    <FileText className="w-8 h-8 text-slate-350 mx-auto mb-2" />
                                    <p className="text-xs text-slate-500 font-semibold">You have not logged any grievances in this ward yet.</p>
                                </div>
                            ) : (
                                <div className="overflow-x-auto">
                                    <table className="w-full text-xs text-left border-collapse">
                                        <thead>
                                            <tr className="border-b border-slate-100 text-slate-400 font-extrabold uppercase text-[10px]">
                                                <th className="py-3 px-4">Grievance Ref</th>
                                                <th className="py-3 px-4">Details Summary</th>
                                                <th className="py-3 px-4">Category</th>
                                                <th className="py-3 px-4">Urgency</th>
                                                <th className="py-3 px-4">Status</th>
                                                <th className="py-3 px-4 text-center">Action</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-50 font-bold">
                                            {complaints.map((item) => (
                                                <tr
                                                    key={item.id}
                                                    onClick={() => setSelectedComplaint(item)}
                                                    className="hover:bg-slate-50/70 cursor-pointer transition-all"
                                                >
                                                    <td className="py-4 px-4 text-[#0F9D58]">#JV-{item.id}</td>
                                                    <td className="py-4 px-4 max-w-xs truncate text-slate-800">{item.originalText}</td>
                                                    <td className="py-4 px-4 uppercase text-[10.5px] text-slate-500">{item.category}</td>
                                                    <td className="py-4 px-4 text-[10.5px]">
                                                        <span className={`px-2 py-0.5 rounded border ${item.urgency === 'CRITICAL' ? 'bg-red-50 border-red-150 text-red-700' : 'bg-slate-100 border-slate-200 text-slate-600'
                                                            }`}>
                                                            {item.urgency}
                                                        </span>
                                                    </td>
                                                    <td className="py-4 px-4 text-[10.5px]">
                                                        <span className={`px-2.5 py-0.5 rounded-full ${item.status === 'RESOLVED' ? 'bg-emerald-50 text-emerald-800 border border-emerald-150' : 'bg-slate-100 text-slate-500 border border-slate-200'
                                                            }`}>
                                                            {item.status}
                                                        </span>
                                                    </td>
                                                    <td className="py-4 px-4 text-center" onClick={(e) => e.stopPropagation()}>
                                                        <button
                                                            onClick={() => handleUpvote(item.id)}
                                                            className="px-3 py-1 bg-emerald-50 border border-emerald-200 hover:bg-[#0F9D58] hover:text-white rounded-lg text-[10px] text-emerald-800 font-extrabold transition-all outline-none"
                                                        >
                                                            Support ({item.upvotes})
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION D: COMMUNITY VOTING & MAP LISTS */}
                    {/* ==================================================== */}
                    {activeSection === 'COMMUNITY' && (
                        <div className="space-y-4">
                            <div className="flex justify-between items-center mb-2">
                                <h3 className="text-base font-black text-slate-900">Ward Grievances Feed</h3>
                                <span className="text-xs text-slate-400 font-semibold">{complaints.length} Total active incidents</span>
                            </div>

                            {complaints.length === 0 ? (
                                <div className="bg-white border border-[#E5E7EB] rounded-3xl p-12 text-center shadow-xs">
                                    <Users className="w-8 h-8 text-slate-350 mx-auto mb-2" />
                                    <p className="text-xs text-slate-500 font-bold">No active grievances logged in your area.</p>
                                </div>
                            ) : (
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    {complaints.map((item) => (
                                        <div
                                            key={item.id}
                                            onClick={() => setSelectedComplaint(item)}
                                            className="bg-white border border-[#E5E7EB] hover:border-emerald-250 cursor-pointer p-5 rounded-2xl shadow-xs hover:shadow-md transition-all flex flex-col justify-between"
                                        >
                                            <div className="space-y-2.5">
                                                <div className="flex justify-between items-center text-[10px] text-slate-400 font-extrabold">
                                                    <span className="uppercase bg-emerald-50 text-emerald-800 px-2 py-0.5 rounded border border-emerald-150 tracking-wider">
                                                        {item.category}
                                                    </span>
                                                    <span className={`px-2 py-0.5 rounded border ${item.urgency === 'CRITICAL' ? 'bg-red-50 border-red-150 text-red-700' : 'bg-slate-100 border-slate-200 text-slate-500'
                                                        }`}>
                                                        {item.urgency}
                                                    </span>
                                                </div>

                                                <p className="font-bold text-slate-900 text-sm leading-relaxed line-clamp-3">"{item.originalText}"</p>
                                                <div className="flex flex-wrap gap-2 pt-2">
                                                    <span className="rounded-full bg-amber-50 px-2 py-0.5 text-[10px] font-black text-amber-700">Priority {item.priorityScore || 0}</span>
                                                    <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-black text-slate-600">{item.suggestedDepartment || 'Municipal Coordination'}</span>
                                                </div>
                                            </div>

                                            <div className="flex items-center justify-between border-t border-slate-50 pt-3.5 mt-4 text-[10px] text-slate-400 font-semibold" onClick={(e) => e.stopPropagation()}>
                                                <span>Grievance: #JV-{item.id}</span>
                                                <button
                                                    onClick={() => handleUpvote(item.id)}
                                                    className="px-3 py-1.5 bg-emerald-50 border border-emerald-200 hover:bg-[#0F9D58] hover:text-white rounded-lg text-emerald-800 transition-all font-black"
                                                >
                                                    Upvote ({item.upvotes})
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION E: AI activity INSIGHTS */}
                    {/* ==================================================== */}
                    {activeSection === 'INSIGHTS' && (
                        <div className="space-y-6">

                            <div className="bg-white border border-[#E5E7EB] p-6 rounded-3xl shadow-sm space-y-4">
                                <div className="flex items-center gap-2 pb-3.5 border-b border-slate-100">
                                    <Sparkles className="w-5 h-5 text-[#0F9D58]" />
                                    <h3 className="text-base font-extrabold text-slate-905">AI Diagnosis & Analytical Breakdown</h3>
                                </div>

                                <div className="p-4 bg-emerald-50/40 border border-emerald-100 rounded-2xl text-xs leading-relaxed text-emerald-990 space-y-3 font-semibold">
                                    <p><strong>1. Categorization Trends</strong>: Roads and Sanitation constitute 68% of local complaints submitted this week.</p>
                                    <p><strong>2. Redundancy Rates</strong>: 44% of logged reports were auto-consolidated as duplicates under master tickets, prevent constituency queue congestion.</p>
                                    <p><strong>3. Resolution Efficiency</strong>: Average resolution times on resolved master cases stands at 4.2 days.</p>
                                </div>
                            </div>

                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION F: NOTIFICATIONS LIST */}
                    {/* ==================================================== */}
                    {activeSection === 'NOTIFICATIONS' && (
                        <div className="bg-white border border-[#E5E7EB] rounded-3xl p-6 shadow-sm space-y-4">
                            <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Constituent Alerts</h3>

                            <div className="space-y-3">
                                {[
                                    { text: 'Your ticket #JV-10 "Broken Light" was marked IN_PROGRESS by the MP.', time: '2 mins ago', icon: Hourglass, bg: 'bg-orange-50 text-orange-700' },
                                    { text: 'Complaint #JV-5 regarding "Sewer Overflows" has been RESOLVED.', time: '2 hours ago', icon: CheckCircle2, bg: 'bg-emerald-50 text-emerald-700' },
                                    { text: 'A new general alert was issued for Sector Alpha water grid repairs.', time: '1 day ago', icon: Bell, bg: 'bg-blue-50 text-blue-700' }
                                ].map((item, idx) => {
                                    const NIcon = item.icon;
                                    return (
                                        <div key={idx} className="flex gap-4 items-start p-4 bg-slate-50/50 border border-slate-100 rounded-2xl text-xs">
                                            <div className={`p-2 rounded-xl shrink-0 ${item.bg}`}>
                                                <NIcon className="w-4 h-4" />
                                            </div>
                                            <div className="space-y-1">
                                                <p className="font-bold text-slate-800 leading-normal">{item.text}</p>
                                                <span className="text-[10px] text-slate-400 font-semibold block">{item.time}</span>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION G: PROFILE SETTINGS OPTIONS */}
                    {/* ==================================================== */}
                    {activeSection === 'PROFILE' && (
                        <div className="bg-white border border-[#E5E7EB] rounded-3xl p-6 shadow-sm max-w-md">
                            <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3.5 mb-5">Profile Configuration</h3>

                            <div className="space-y-4 text-xs font-bold">
                                <div>
                                    <label className="block text-slate-500 mb-1.5">Registered Username</label>
                                    <input type="text" disabled defaultValue={user?.userName || 'Atharv Shukla'} className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-slate-500 outline-none cursor-not-allowed" />
                                </div>

                                <div>
                                    <label className="block text-slate-550 mb-1.5">Constituency Ward Area</label>
                                    <input type="text" disabled defaultValue={user?.wardArea || 'Ward 5 - Sector Alpha'} className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2.5 px-4 text-slate-500 outline-none cursor-not-allowed" />
                                </div>

                                <div className="p-4 bg-slate-50 border border-slate-100 rounded-2xl">
                                    <span className="text-[11px] text-slate-440 font-bold block mb-1">Assigned Role Mode</span>
                                    <span className="text-emerald-700 font-black">Authorized Citizen Contributor</span>
                                </div>
                            </div>
                        </div>
                    )}

                </main>
            </div>

            {/* ==================================================== */}
            {/* 3. GRIEVANCE DETAIL DRAWER OVERLAY */}
            {/* ==================================================== */}
            {selectedComplaint && (
                <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-end">
                    <div className="w-full max-w-lg bg-white h-screen overflow-y-auto flex flex-col justify-between shadow-2xl relative animate-slide-in">

                        {/* Drawer Header */}
                        <div>
                            <div className="p-6 border-b border-[#E5E7EB] flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <button
                                        onClick={() => setSelectedComplaint(null)}
                                        className="p-1.5 hover:bg-slate-105 rounded-lg border text-slate-500 hover:text-slate-800 transition-all outline-none"
                                    >
                                        <ArrowLeft className="w-4 h-4" />
                                    </button>
                                    <span className="text-xs font-black text-slate-555">
                                        Grievance ID: #JV-{selectedComplaint.id}
                                    </span>
                                </div>

                                <span className={`text-[10px] font-black uppercase px-2.5 py-1 rounded-full ${selectedComplaint.status === 'RESOLVED'
                                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-150'
                                    : 'bg-slate-100 text-slate-500 border border-slate-250'
                                    }`}>
                                    {selectedComplaint.status}
                                </span>
                            </div>

                            {/* Drawer Body Panel */}
                            <div className="p-6 space-y-6">
                                <div>
                                    <span className="text-[10px] bg-red-50 text-red-750 px-2 py-0.5 border border-red-150 rounded font-black uppercase tracking-wider mb-2 inline-block">
                                        {selectedComplaint.urgency} Urgency
                                    </span>
                                    <h3 className="text-base font-black text-slate-900 leading-snug">
                                        {selectedComplaint.originalText}
                                    </h3>

                                    {selectedComplaint.translatedText && selectedComplaint.language !== 'en' && (
                                        <div className="mt-3 p-3 bg-slate-50 border border-slate-100 rounded-xl">
                                            <span className="text-[9px] text-[#0F9D58] font-black uppercase tracking-wider block mb-1">
                                                AI English Translation
                                            </span>
                                            <p className="text-xs text-slate-650 italic">"{selectedComplaint.translatedText}"</p>
                                        </div>
                                    )}
                                </div>

                                {/* Simulated images evidence */}
                                <div className="space-y-2">
                                    <span className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider block">Attached Evidence</span>
                                    <div className="grid grid-cols-3 gap-2">
                                        <div className="aspect-square bg-slate-100 border border-slate-200 rounded-lg flex items-center justify-center text-slate-400">
                                            <ImageIcon className="w-5 h-5 text-slate-350" />
                                        </div>
                                        <div className="aspect-square bg-slate-100 border border-slate-200 rounded-lg flex items-center justify-center text-slate-400">
                                            <ImageIcon className="w-5 h-5 text-slate-350" />
                                        </div>
                                        <div className="aspect-square bg-slate-100 border border-slate-205 rounded-lg flex items-center justify-center text-slate-405">
                                            +1 Evidence
                                        </div>
                                    </div>
                                </div>

                                {/* Tab layout details */}
                                <div className="border-b border-slate-100 flex gap-4 text-xs font-bold pb-1 select-none">
                                    {['TIMELINE', 'COMMENTS (4)', 'SUPPORTING EVIDENCE'].map((tab) => (
                                        <button
                                            key={tab}
                                            onClick={() => setDetailTab(tab)}
                                            className={`pb-2 outline-none ${detailTab === tab ? 'border-b-2 border-[#0F9D58] text-[#0F9D58]' : 'text-slate-405 hover:text-slate-700'
                                                }`}
                                        >
                                            {tab}
                                        </button>
                                    ))}
                                </div>

                                {detailTab === 'TIMELINE' && (
                                    <div className="space-y-4">
                                    {[
                                        { label: 'Received', detail: 'Complaint logged and AI analyzed', active: true },
                                        { label: 'Assigned', detail: selectedComplaint.suggestedDepartment || 'Municipal Coordination', active: true },
                                        { label: 'In Progress', detail: selectedComplaint.status === 'RESOLVED' ? 'Resolved by officials' : 'Active municipal response', active: selectedComplaint.status !== 'PENDING' },
                                        { label: 'Resolved', detail: selectedComplaint.status === 'RESOLVED' ? 'Closed successfully' : 'Pending follow-up', active: selectedComplaint.status === 'RESOLVED' },
                                    ].map((step, index) => (
                                        <div key={step.label} className="flex gap-3 text-xs">
                                            <span className={`w-2.5 h-2.5 rounded-full mt-1.5 ring-4 ${step.active ? 'bg-emerald-650 ring-emerald-50' : 'bg-slate-300 ring-slate-100'}`}></span>
                                            <div>
                                                <p className="font-bold text-slate-800">{index + 1}. {step.label}</p>
                                                <span className="text-[10px] text-slate-400">{step.detail}</span>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                                )}

                            </div>
                        </div>

                        {/* Footer drawer vote count trigger */}
                        <div className="p-6 border-t border-[#E5E7EB] bg-slate-50 flex items-center justify-between">
                            <div className="text-xs">
                                <span className="text-slate-405 block">People Priority Score</span>
                                <span className="font-black text-slate-800 text-sm">{selectedComplaint.priorityScore || 0} • {selectedComplaint.affectedPeople || selectedComplaint.upvotes || 0} affected</span>
                            </div>

                            <button
                                onClick={() => handleUpvote(selectedComplaint.id)}
                                className="px-6 py-2.5 bg-[#0F9D58] hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer outline-none"
                            >
                                Upvote Priority
                            </button>
                        </div>

                    </div>
                </div>
            )}

        </div>
    );
}
