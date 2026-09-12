import React, { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../context/AuthContext';
import { mpService, complaintService } from '../services/api';
import {
    Building, LayoutDashboard, FileText, BarChart3, Map, Sparkles,
    Bell, Settings, LogOut, Search, ListFilter, Calendar, ChevronRight,
    HelpCircle, CheckCircle2, Hourglass, Percent, ArrowUp, ArrowLeft, X,
    ExternalLink, ChevronDown, CheckSquare, AlertCircle
} from 'lucide-react';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, ArcElement } from 'chart.js';
import { Line, Doughnut } from 'react-chartjs-2';

// Register ChartJS modules
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend, ArcElement);

/**
 * MpDashboard completely overhauled according to Google Material 3 reference specifications.
 * Consumes the official backend REST API endpoints.
 */
export default function MpDashboard() {
    const { user, logout } = useContext(AuthContext);
    const [activeSection, setActiveSection] = useState('DASHBOARD'); // DASHBOARD, QUEUE, AI_INSIGHTS, DEPARTMENTS, NOTIFICATIONS, SETTINGS
    const [metrics, setMetrics] = useState({ totalOpened: 0, resolved: 0, pending: 0, inProgress: 0 });
    const [categoryCounts, setCategoryCounts] = useState([]);
    const [complaintQueue, setComplaintQueue] = useState([]);
    const [priorityQueue, setPriorityQueue] = useState([]);
    const [briefing, setBriefing] = useState('');
    const [loadingBriefing, setLoadingBriefing] = useState(false);
    const [selectedComplaint, setSelectedComplaint] = useState(null); // Detail drawer toggle
    const [categoryFilter, setCategoryFilter] = useState('ALL');
    const [searchQuery, setSearchQuery] = useState('');
    const [apiMessage, setApiMessage] = useState({ text: '', type: '' });

    // Load dashboard metrics and active queue items from backend
    const fetchDashDetails = async () => {
        try {
            const ward = user.wardArea || 'Ward 5';

            // stats Endpoint returning pendingCount, inProgressCount, resolvedCount, categories map, aiSummary
            const stats = await mpService.getStats(ward);

            const pendingVal = stats.pendingCount || 0;
            const progressVal = stats.inProgressCount || 0;
            const resolvedVal = stats.resolvedCount || 0;

            setMetrics({
                resolved: resolvedVal,
                pending: pendingVal,
                inProgress: progressVal,
                totalOpened: pendingVal + progressVal + resolvedVal
            });

            // Categories map: convert to arrays e.g. [['Water', 10], ...]
            const catsMap = stats.categories || {};
            const catEntries = Object.entries(catsMap);
            setCategoryCounts(catEntries);

            // AI Summary
            setBriefing(stats.aiSummary || 'AI briefing generation details placeholder...');

            const queueList = await complaintService.getByWard(ward);
            const normalizedQueue = Array.isArray(queueList) && queueList.length > 0 ? queueList : [
                { id: 101, originalText: 'Electric wire hanging near market creates immediate danger.', category: 'SAFETY', urgency: 'CRITICAL', upvotes: 14, priorityScore: 96, affectedPeople: 42, duplicateCount: 3, suggestedDepartment: 'Emergency Response', priorityReason: 'Immediate public safety hazard with wide community impact.', isEmergency: true, status: 'PENDING' },
                { id: 102, originalText: 'Sewer overflow near school is affecting daily life.', category: 'SANITATION', urgency: 'HIGH', upvotes: 9, priorityScore: 84, affectedPeople: 28, duplicateCount: 2, suggestedDepartment: 'Municipal Sanitation', priorityReason: 'Repeated sanitation issue affecting school routes and residents.', isEmergency: false, status: 'IN_PROGRESS' },
                { id: 103, originalText: 'Water supply stopped for 2 days in the neighborhood.', category: 'WATER', urgency: 'HIGH', upvotes: 8, priorityScore: 78, affectedPeople: 35, duplicateCount: 1, suggestedDepartment: 'Water Department', priorityReason: 'Essential utility outage with repeated community reports.', isEmergency: false, status: 'PENDING' },
                { id: 104, originalText: 'Broken road near hospital needs urgent repair.', category: 'ROAD', urgency: 'MEDIUM', upvotes: 5, priorityScore: 65, affectedPeople: 12, duplicateCount: 0, suggestedDepartment: 'Public Works', priorityReason: 'Road damage near a hospital increases access risk.', isEmergency: false, status: 'PENDING' },
            ];
            const sortedQueue = [...normalizedQueue].sort((a, b) => (b.priorityScore || 0) - (a.priorityScore || 0));
            setComplaintQueue(sortedQueue);
            setPriorityQueue(sortedQueue.filter(item => item.status !== 'RESOLVED').slice(0, 3));
        } catch (e) {
            console.error(e);
            setApiMessage({ text: 'Error connecting to Spring Boot backend services.', type: 'error' });
        }
    };

    useEffect(() => {
        fetchDashDetails();
    }, [user.wardArea]);

    // Generate dynamic AI briefing via Gemini
    const triggerBriefingRegen = async () => {
        setLoadingBriefing(true);
        try {
            // Re-fetch since backend getStats checks timestamp automatically and updates briefing if needed
            await fetchDashDetails();
            setApiMessage({ text: 'AI Area Briefing refreshed successfully!', type: 'success' });
        } catch (err) {
            setApiMessage({ text: 'Failed to access Gemini service. Check API key settings.', type: 'error' });
        } finally {
            setLoadingBriefing(false);
        }
    };

    // Update complaint status transition
    const handleUpdateStatus = async (id, status) => {
        try {
            const resp = await mpService.updateStatus(id, status);
            setApiMessage({ text: `Ticket #${id} status updated to ${status}!`, type: 'success' });
            fetchDashDetails();
            if (selectedComplaint && selectedComplaint.id === id) {
                setSelectedComplaint(prev => ({ ...prev, status: resp.status }));
            }
        } catch (e) {
            setApiMessage({ text: 'Error executing status alteration.', type: 'error' });
        }
    };

    // Resolve complaint ticket
    const handleResolveTicket = async (id, e) => {
        if (e) e.stopPropagation();
        try {
            await handleUpdateStatus(id, 'RESOLVED');
        } catch (err) {
            setApiMessage({ text: 'Failed to resolve grievance.', type: 'error' });
        }
    };

    // Filter complaints based on choices
    const filteredQueue = complaintQueue.filter(item => {
        const matchesCategory = categoryFilter === 'ALL' || item.category === categoryFilter;
        const matchesSearch = item.originalText.toLowerCase().includes(searchQuery.toLowerCase()) ||
            (item.id && item.id.toString().includes(searchQuery));
        return matchesCategory && matchesSearch;
    });

    // Calculate resolution rate
    const resRate = metrics.totalOpened > 0 ? Math.round((metrics.resolved / metrics.totalOpened) * 100) : 0;

    // ChartJS configurations
    const lineChartData = {
        labels: ['May 1', 'May 5', 'May 10', 'May 15', 'May 22', 'May 31'],
        datasets: [
            {
                label: 'Submitted',
                data: [120, 240, 180, 310, 420, 480],
                borderColor: '#0F9D58',
                backgroundColor: 'rgba(15, 157, 88, 0.1)',
                tension: 0.4,
                borderWidth: 2,
                pointRadius: 3
            },
            {
                label: 'Resolved',
                data: [90, 180, 210, 290, 340, 410],
                borderColor: '#0284c7',
                backgroundColor: 'rgba(2, 132, 199, 0.1)',
                tension: 0.4,
                borderWidth: 2,
                pointRadius: 3
            }
        ]
    };

    const doughnutData = {
        labels: categoryCounts.length > 0 ? categoryCounts.map(c => c[0]) : ['Water Supply', 'Roads', 'Sanitation', 'Electricity', 'Others'],
        datasets: [
            {
                data: categoryCounts.length > 0 ? categoryCounts.map(c => c[1]) : [35, 25, 20, 12, 8],
                backgroundColor: ['#0F9D58', '#4285F4', '#EA4335', '#FBBC05', '#9ca3af'],
                hoverOffset: 4,
                borderWidth: 1
            }
        ]
    };

    return (
        <div className="min-h-screen bg-[#F8FAFC] text-[#111827] flex font-sans leading-relaxed">

            {/* ==================================================== */}
            {/* 1. LEFT SIDEBAR NAVIGATION PANEL */}
            {/* ==================================================== */}
            <aside className="w-64 bg-white border-r border-[#E5E7EB] hidden md:flex flex-col justify-between shrink-0 h-screen sticky top-0">
                <div>
                    {/* Brand Logo Header */}
                    <div className="p-6 border-b border-[#E5E7EB] flex items-center gap-3">
                        <div className="text-[#0F9D58]">
                            <Building className="w-8 h-8" />
                        </div>
                        <div>
                            <h1 className="text-base font-extrabold tracking-tight leading-5">JanVoice AI</h1>
                            <span className="text-[10px] text-[#0F9D58] font-black uppercase tracking-wider">
                                MP Administration
                            </span>
                        </div>
                    </div>

                    {/* Sidebar Menu Options */}
                    <nav className="p-4 space-y-1">
                        {[
                            { id: 'DASHBOARD', label: 'Overview', icon: LayoutDashboard },
                            { id: 'QUEUE', label: 'Complaints Queue', icon: FileText },
                            { id: 'AI_INSIGHTS', label: 'AI Analytics Insights', icon: Sparkles },
                            { id: 'DEPARTMENTS', label: 'Departments', icon: BarChart3 },
                            { id: 'NOTIFICATIONS', label: 'Notifications', icon: Bell, indicator: 5 },
                            { id: 'SETTINGS', label: 'Settings', icon: Settings }
                        ].map((menu) => {
                            const MIcon = menu.icon;
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
                                        <MIcon className="w-4 h-4" />
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

                {/* Sidebar bottom card */}
                <div className="p-4 border-t border-[#E5E7EB]">

                    <div className="bg-[#eaf6ee] rounded-2xl p-4 mb-4 text-center border border-[#d8ecd8]">
                        <p className="text-[11px] font-extrabold text-[#0F9D58] leading-tight">
                            You're making<br />a difference!
                        </p>

                        {/* Soft SVG landmark parliament icon */}
                        <div className="mt-3 flex justify-center opacity-65">
                            <svg className="w-12 h-10 text-[#0F9D58] fill-current" viewBox="0 0 100 80">
                                <circle cx="50" cy="40" r="18" fill="none" stroke="currentColor" strokeWidth="2.5" />
                                <line x1="50" y1="10" x2="50" y2="70" stroke="currentColor" strokeWidth="2" />
                                <line x1="20" y1="40" x2="80" y2="40" stroke="currentColor" strokeWidth="2" />
                            </svg>
                        </div>
                    </div>

                    <div className="flex items-center justify-between">
                        <div className="truncate pr-2">
                            <p className="text-xs font-bold truncate">Member of Parliament</p>
                            <p className="text-[9.5px] text-slate-400 font-bold uppercase tracking-wider">
                                {user?.wardArea || 'Ward 5'} Area
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
            {/* 2. MAIN LAYOUT AREA CONTENT */}
            {/* ==================================================== */}
            <div className="flex-1 flex flex-col min-w-0">

                {/* Top Navbar header */}
                <header className="bg-white border-b border-[#E5E7EB] px-8 py-4 flex flex-row items-center justify-between sticky top-0 z-30">
                    <div>
                        <h2 className="text-base font-black text-slate-905 flex items-center gap-2">
                            {activeSection === 'DASHBOARD' && 'MP Dashboard'}
                            {activeSection === 'QUEUE' && 'Grievance Priority Queue'}
                            {activeSection === 'AI_INSIGHTS' && 'AI Constituency Insights'}
                            {activeSection === 'DEPARTMENTS' && 'Department Metrics'}
                            {activeSection === 'NOTIFICATIONS' && 'System Notifications Log'}
                            {activeSection === 'SETTINGS' && 'System Configurations'}
                        </h2>
                        <p className="text-xs text-slate-400 font-semibold mt-0.5">
                            {activeSection === 'DASHBOARD' && `Overview of your constituency: ${user.wardArea || 'Ward 5'}`}
                            {activeSection === 'QUEUE' && 'Prioritizing, tagging and reviewing citizen complaints.'}
                            {activeSection === 'AI_INSIGHTS' && 'Gemini AI generated summaries and area actions.'}
                            {activeSection === 'DEPARTMENTS' && 'Monitoring performance parameters of regional bodies.'}
                            {activeSection === 'NOTIFICATIONS' && 'Real-time actions tracker logs.'}
                            {activeSection === 'SETTINGS' && 'Admin values key adjustment parameters.'}
                        </p>
                    </div>

                    <div className="flex items-center gap-3">
                        <span className="text-xs font-bold text-slate-650 bg-slate-100 border border-[#E5E7EB] px-3.5 py-2 rounded-xl flex items-center gap-1.5 shadow-2xs">
                            <Calendar className="w-3.5 h-3.5 text-[#0F9D58]" />
                            May 1 - May 31, 2026
                        </span>
                    </div>
                </header>

                {/* Scrollable primary body workspace */}
                <main className="flex-1 p-6 md:p-8 space-y-8 overflow-y-auto max-w-6xl w-full mx-auto">

                    {/* Notification Alert logs */}
                    {apiMessage.text && (
                        <div className={`p-4 rounded-xl border text-xs font-semibold leading-relaxed flex items-center justify-between gap-3 ${apiMessage.type === 'error'
                                ? 'bg-red-50 border-red-200 text-red-800'
                                : 'bg-emerald-50 border-emerald-200 text-emerald-800'
                            }`}>
                            <div className="flex items-center gap-3">
                                {apiMessage.type === 'success' ? <CheckCircle2 className="w-4 h-4 text-emerald-650" /> : <HelpCircle className="w-4 h-4 text-red-600" />}
                                <span>{apiMessage.text}</span>
                            </div>
                            <button onClick={() => setApiMessage({ text: '', type: '' })} className="hover:opacity-75">
                                <X className="w-4 h-4" />
                            </button>
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION A: OVERVIEW CHARTS & METRICS */}
                    {/* ==================================================== */}
                    {activeSection === 'DASHBOARD' && (
                        <div className="space-y-6">

                            <div className="rounded-3xl border border-rose-200 bg-gradient-to-r from-rose-50 to-orange-50 p-5 shadow-sm">
                                <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
                                    <div>
                                        <p className="text-[10px] font-black uppercase tracking-[0.25em] text-rose-700">People Priority Engine</p>
                                        <h3 className="text-lg font-black text-slate-900">Critical issues are ranked at the top to help officials decide what to solve first.</h3>
                                    </div>
                                    <div className="rounded-2xl bg-white/80 px-4 py-3 text-sm font-semibold text-slate-700">
                                        {priorityQueue.length} critical issue{priorityQueue.length === 1 ? '' : 's'} in focus
                                    </div>
                                </div>
                            </div>

                            {/* Stats overview rows */}
                            <div className="grid grid-cols-2 lg:grid-cols-4 gap-6">
                                {[
                                    { label: 'Total Complaints', value: metrics.totalOpened || '0', change: '+12% this month', icon: FileText, border: 'border-l-[#0F9D58]', bg: 'bg-emerald-50', text: 'text-emerald-700' },
                                    { label: 'In Progress', value: metrics.inProgress || '0', change: '+8% ongoing', icon: Hourglass, border: 'border-l-orange-500', bg: 'bg-orange-50', text: 'text-orange-700' },
                                    { label: 'Resolved Tickets', value: metrics.resolved || '0', change: '+18% closed', icon: CheckCircle2, border: 'border-l-blue-500', bg: 'bg-blue-50', text: 'text-blue-700' },
                                    { label: 'Resolution Rate', value: `${resRate}%` || '0%', change: '+5% efficiency rate', icon: Percent, border: 'border-l-purple-650', bg: 'bg-purple-50', text: 'text-purple-755' }
                                ].map((stat, idx) => {
                                    const SIcon = stat.icon;
                                    return (
                                        <div key={idx} className={`bg-white border border-[#E5E7EB] border-l-4 ${stat.border} rounded-2xl p-5 shadow-xs flex items-center justify-between`}>
                                            <div>
                                                <h4 className="text-[10px] text-slate-400 font-extrabold uppercase tracking-wider">{stat.label}</h4>
                                                <p className="text-2xl font-black text-slate-900 mt-1 leading-none">{stat.value}</p>
                                                <span className="text-[9.5px] text-[#0F9D58] font-bold block mt-1.5">{stat.change}</span>
                                            </div>
                                            <div className={`p-3 rounded-xl ${stat.bg} ${stat.text}`}>
                                                <SIcon className="w-5 h-5" />
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>

                            {/* Chart.js diagrams row */}
                            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

                                {/* Curve Line trend */}
                                <div className="lg:col-span-8 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs">
                                    <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Complaints Trend</h3>
                                    <div className="h-64 flex items-center justify-center">
                                        <Line
                                            data={lineChartData}
                                            options={{
                                                responsive: true,
                                                maintainAspectRatio: false,
                                                plugins: { legend: { position: 'top', labels: { boxWidth: 10, font: { size: 10, weight: 'bold' } } } },
                                                scales: { x: { grid: { display: false } }, y: { ticks: { font: { size: 9 } } } }
                                            }}
                                        />
                                    </div>
                                </div>

                                {/* Donut sectors division */}
                                <div className="lg:col-span-4 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs flex flex-col justify-between">
                                    <div>
                                        <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Top Categories</h3>
                                        <div className="h-44 flex justify-center items-center">
                                            <Doughnut
                                                data={doughnutData}
                                                options={{
                                                    responsive: true,
                                                    maintainAspectRatio: false,
                                                    plugins: { legend: { display: false } }
                                                }}
                                            />
                                        </div>
                                    </div>

                                    {/* Manual legend */}
                                    <div className="grid grid-cols-2 gap-2 text-[10px] text-slate-655 font-bold mt-4 pt-3 border-t border-slate-50">
                                        {categoryCounts.length > 0 ? (
                                            categoryCounts.map((c, i) => (
                                                <span key={i} className="flex items-center gap-1.5 truncate">
                                                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-600 shrink-0"></span>
                                                    {c[0]}: {c[1]}
                                                </span>
                                            ))
                                        ) : (
                                            <>
                                                <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-[#0F9D58] shrink-0"></span>Water Supply: 35%</span>
                                                <span className="flex items-center gap-1.5"><span className="w-2.5 h-2.5 rounded-full bg-[#4285F4] shrink-0"></span>Roads: 25%</span>
                                            </>
                                        )}
                                    </div>
                                </div>

                            </div>

                            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
                                <div className="lg:col-span-12 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs">
                                    <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
                                        <h3 className="text-sm font-black text-slate-900">Critical Issues Spotlight</h3>
                                        <span className="text-[10px] font-black uppercase tracking-[0.2em] text-rose-600">Priority First</span>
                                    </div>
                                    <div className="grid gap-4 md:grid-cols-3">
                                        {priorityQueue.map((item) => (
                                            <div key={item.id} className="rounded-2xl border border-rose-200 bg-rose-50 p-4">
                                                <div className="flex items-center justify-between text-[10px] font-black uppercase tracking-[0.2em] text-rose-700">
                                                    <span>#{item.id}</span>
                                                    <span>{item.priorityScore || 0}</span>
                                                </div>
                                                <p className="mt-2 text-sm font-black text-slate-900">{item.originalText}</p>
                                                <div className="mt-3 flex flex-wrap gap-2 text-[10px] text-slate-600">
                                                    <span className="rounded-full bg-white px-2 py-0.5">{item.category}</span>
                                                    <span className="rounded-full bg-white px-2 py-0.5">{item.urgency}</span>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>
                            </div>

                            {/* Row 3: Heatmap placeholder and department listings */}
                            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">

                                {/* Heatmap area representation */}
                                <div className="lg:col-span-6 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs">
                                    <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Ward Wise Heatmap</h3>

                                    {/* Mock styled constituency sector matrix map */}
                                    <div className="h-56 bg-[#ecf3ee] border border-emerald-100 rounded-xl relative overflow-hidden flex items-center justify-center">
                                        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#0c382e_2.5px,transparent_2.5px)] [background-size:16px_16px]"></div>

                                        {/* Simulated circular priority hazard warnings overlay */}
                                        <div className="w-28 h-28 bg-red-400/25 border border-red-500 rounded-full absolute -top-4 -left-4 animate-pulse flex items-center justify-center">
                                            <span className="text-[10px] text-red-900 bg-white/90 border font-extrabold px-1.5 py-0.5 rounded shadow-2xs">High risk</span>
                                        </div>

                                        <div className="w-32 h-32 bg-orange-400/20 border border-orange-405 rounded-full absolute bottom-4 right-10 flex items-center justify-center">
                                            <span className="text-[10px] text-orange-900 bg-white/95 border font-extrabold px-1.5 py-0.5 rounded shadow-2xs">Medium density</span>
                                        </div>

                                        <div className="w-20 h-20 bg-emerald-400/15 border border-emerald-505 rounded-full absolute top-12 right-32 flex items-center justify-center">
                                            <span className="text-[9.5px] text-emerald-900 bg-white/95 border font-extrabold px-1.5 py-0.5 rounded shadow-2xs">Low risk</span>
                                        </div>

                                        <span className="text-xs bg-slate-900 text-white font-bold tracking-wide py-1.5 px-3.5 rounded-full shadow-md relative z-10">
                                            Constituency GPS Hotspots Grid
                                        </span>
                                    </div>
                                </div>

                                {/* Department analytics grid list */}
                                <div className="lg:col-span-6 bg-white border border-[#E5E7EB] p-6 rounded-2xl shadow-xs">
                                    <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Department Performance</h3>

                                    <div className="space-y-4">
                                        {[
                                            { name: 'Water Board', resolved: 245, pending: 45, rate: '84%', bar: 'bg-[#0F9D58] w-[84%]' },
                                            { name: 'Municipal Corp', resolved: 210, pending: 38, rate: '83%', bar: 'bg-emerald-600 w-[83%]' },
                                            { name: 'PWD Roads', resolved: 180, pending: 60, rate: '75%', bar: 'bg-blue-500 w-[75%]' },
                                            { name: 'Electricity Dept', resolved: 120, pending: 30, rate: '80%', bar: 'bg-orange-500 w-[80%]' }
                                        ].map((dept, idx) => (
                                            <div key={idx} className="text-xs font-bold space-y-1">
                                                <div className="flex justify-between items-center text-slate-700">
                                                    <span>{dept.name}</span>
                                                    <span className="text-[10px] text-slate-400 font-extrabold">Resolved: {dept.resolved} | Pending: {dept.pending}</span>
                                                </div>
                                                {/* Progress slider bar */}
                                                <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden flex items-center">
                                                    <div className={`h-full ${dept.bar}`}></div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                            </div>

                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION B: PRIORITIZED COMPLAINT TICKETS QUEUE */}
                    {/* ==================================================== */}
                    {activeSection === 'QUEUE' && (
                        <div className="space-y-4">

                            {/* Search, filters widgets */}
                            <div className="bg-white border border-[#E5E7EB] p-4.5 rounded-2xl shadow-2xs flex flex-col md:flex-row md:items-center justify-between gap-4">

                                <div className="relative flex-1 max-w-sm">
                                    <span className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-slate-405">
                                        <Search className="w-4 h-4" />
                                    </span>
                                    <input
                                        type="text"
                                        value={searchQuery}
                                        onChange={(e) => setSearchQuery(e.target.value)}
                                        placeholder="Search by keyword, ward or ID..."
                                        className="w-full bg-white border border-slate-205 focus:border-[#0F9D58] focus:ring-1 focus:ring-[#0F9D58] rounded-xl py-2 pl-9 pr-4 text-slate-800 text-xs outline-none transition-all placeholder:text-slate-400 font-bold"
                                    />
                                </div>

                                <div className="flex items-center gap-3">
                                    <ListFilter className="w-4 h-4 text-slate-440 shrink-0" />
                                    <select
                                        value={categoryFilter}
                                        onChange={(e) => setCategoryFilter(e.target.value)}
                                        className="bg-white border border-slate-200 rounded-xl py-2 px-3 text-xs font-bold text-slate-750 outline-none cursor-pointer"
                                    >
                                        <option value="ALL">All Categories</option>
                                        <option value="Road Damage">Road Damage</option>
                                        <option value="Sanitation">Sanitation</option>
                                        <option value="Water Supply">Water Supply</option>
                                        <option value="Electricity">Electricity</option>
                                    </select>
                                </div>

                            </div>

                            {/* Feed items list */}
                            {filteredQueue.length === 0 ? (
                                <div className="bg-white border border-[#E5E7EB] p-12 rounded-3xl text-center shadow-xs">
                                    <FileText className="w-9 h-9 text-slate-350 mx-auto mb-2" />
                                    <p className="text-xs text-slate-550 font-bold">No complaints match search parameters.</p>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    {filteredQueue.map((item) => (
                                        <div
                                            key={item.id}
                                            onClick={() => setSelectedComplaint(item)}
                                            className="bg-white border border-[#E5E7EB] hover:border-emerald-250 cursor-pointer p-5 rounded-2xl shadow-xs hover:shadow-md transition-all flex flex-col md:flex-row md:items-center justify-between gap-4"
                                        >
                                            <div className="space-y-2">
                                                <div className="flex flex-wrap gap-2 items-center text-[10px] text-slate-405 font-extrabold uppercase">
                                                    <span className="text-[#0F9D58] font-black">#JV-{item.id}</span>
                                                    <span>•</span>
                                                    <span className="bg-emerald-50 px-2 py-0.5 rounded border border-emerald-150 text-emerald-800 font-black">
                                                        {item.category}
                                                    </span>
                                                    <span className={`px-2 py-0.5 rounded border ${item.priorityScore >= 80 ? 'bg-rose-50 border-rose-200 text-rose-700' : 'bg-amber-50 border-amber-200 text-amber-700'}`}>
                                                        Score {item.priorityScore || 0}
                                                    </span>
                                                    <span>•</span>
                                                    <span className={`px-2 py-0.5 rounded border ${item.urgency === 'CRITICAL' ? 'bg-red-50 border-red-150 text-red-700' : 'bg-slate-100 border-slate-205 text-slate-550'
                                                        }`}>
                                                        {item.urgency}
                                                    </span>

                                                    {/* Nested duplicate counter indicator */}
                                                    {item.linkedComplaints && item.linkedComplaints.length > 0 && (
                                                        <span className="bg-yellow-50 border border-yellow-200 text-yellow-800 px-2 co py-0.5 rounded font-black flex items-center gap-1">
                                                            <Sparkles className="w-3 h-3 text-yellow-605 animate-pulse" />
                                                            {item.linkedComplaints.length} Consolidations
                                                        </span>
                                                    )}
                                                </div>

                                                <p className="font-bold text-slate-800 text-sm leading-relaxed max-w-2xl line-clamp-2">
                                                    "{item.originalText}"
                                                </p>
                                            </div>

                                            <div className="flex items-center gap-3 shrink-0" onClick={(e) => e.stopPropagation()}>
                                                <span className="text-xs font-black text-slate-700 bg-slate-100 px-2.5 py-1 rounded-lg">
                                                    {item.affectedPeople || item.upvotes || 0} affected
                                                </span>
                                                {item.status !== 'RESOLVED' ? (
                                                    <button
                                                        onClick={(e) => handleResolveTicket(item.id, e)}
                                                        className="px-4 py-2 bg-[#0F9D58] hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer flex items-center gap-1.5 border-none"
                                                    >
                                                        <CheckSquare className="w-3.5 h-3.5" />
                                                        Resolve
                                                    </button>
                                                ) : (
                                                    <span className="px-3.5 py-2 bg-emerald-50 border border-emerald-200 text-emerald-850 text-xs font-black rounded-xl">
                                                        Resolved
                                                    </span>
                                                )}
                                            </div>

                                        </div>
                                    ))}
                                </div>
                            )}

                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION C: AI INSIGHTS ANALYSIS SUMMARY */}
                    {/* ==================================================== */}
                    {activeSection === 'AI_INSIGHTS' && (
                        <div className="space-y-6">

                            <div className="bg-white border border-[#E5E7EB] p-6 rounded-3xl shadow-sm space-y-4">
                                <div className="flex justify-between items-center pb-3.5 border-b border-slate-100">
                                    <div className="flex items-center gap-2">
                                        <Sparkles className="w-5 h-5 text-[#0F9D58]" />
                                        <h3 className="text-base font-extrabold text-slate-900">AI Constituency Executive Briefing</h3>
                                    </div>

                                    <button
                                        onClick={triggerBriefingRegen}
                                        disabled={loadingBriefing}
                                        className="px-3.5 py-2 border border-[#0F9D58] text-[#0F9D58] hover:bg-emerald-50 disabled:bg-slate-50 disabled:text-slate-400 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer outline-none"
                                    >
                                        {loadingBriefing ? 'Regenerating...' : 'Regenerate Briefing'}
                                    </button>
                                </div>

                                <div className="p-5 bg-emerald-50/40 border border-emerald-100 rounded-2xl text-xs font-semibold leading-relaxed text-emerald-990 whitespace-pre-line shadow-inner">
                                    {briefing || 'Loading AI analysis of constituency grievances...'}
                                </div>
                            </div>

                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION D: DEPARTMENTS LISTING */}
                    {/* ==================================================== */}
                    {activeSection === 'DEPARTMENTS' && (
                        <div className="bg-white border border-[#E5E7EB] rounded-3xl p-6 shadow-sm">
                            <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Water, Sanitation & Utility Agencies</h3>

                            <table className="w-full text-xs text-left border-collapse font-bold">
                                <thead>
                                    <tr className="border-b border-slate-100 text-slate-400 font-extrabold uppercase text-[10px]">
                                        <th className="py-3 px-4">Department</th>
                                        <th className="py-3 px-4">Officer in charge</th>
                                        <th className="py-3 px-4">Resolution Eff</th>
                                        <th className="py-3 px-4">Feedback Target</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-slate-50">
                                    {[
                                        { name: 'Water Board', head: 'Ravi Kumar', eff: '84%', limit: '3 Days' },
                                        { name: 'Municipal Corp', head: 'Anil Tyagi', eff: '83%', limit: '2 Days' },
                                        { name: 'PWD Roads', head: 'Suresh Raina', eff: '75%', limit: '5 Days' }
                                    ].map((dept, idx) => (
                                        <tr key={idx}>
                                            <td className="py-4 px-4 text-[#0F9D58]">{dept.name}</td>
                                            <td className="py-4 px-4 text-slate-700">{dept.head}</td>
                                            <td className="py-4 px-4 text-emerald-800">{dept.eff}</td>
                                            <td className="py-4 px-4 text-slate-450">{dept.limit}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION E: NOTIFICATIONS PANEL */}
                    {/* ==================================================== */}
                    {activeSection === 'NOTIFICATIONS' && (
                        <div className="bg-white border border-[#E5E7EB] rounded-3xl p-6 shadow-sm space-y-4">
                            <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3 mb-4">Constituency Alerts Track</h3>

                            <div className="space-y-3">
                                {[
                                    { text: 'New complaint registered matching high risk categories in Sector Alpha.', time: '5 mins ago' },
                                    { text: 'Weekly briefing generated successfully by Gemini agent.', time: '1 hr ago' },
                                    { text: 'Auto consolidated 4 tickets under master grievance Sewage overflow.', time: '1 day ago' }
                                ].map((item, idx) => (
                                    <div key={idx} className="p-3 bg-slate-50 border border-slate-100 rounded-xl text-xs flex gap-3 items-center font-bold">
                                        <Sparkles className="w-4 h-4 text-[#0F9D58] shrink-0" />
                                        <div>
                                            <p className="font-bold text-slate-800">{item.text}</p>
                                            <span className="text-[9px] text-slate-400 font-semibold block">{item.time}</span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* ==================================================== */}
                    {/* SECTION F: ADMIN CONFIGURATION SETTINGS */}
                    {/* ==================================================== */}
                    {activeSection === 'SETTINGS' && (
                        <div className="bg-white border border-[#E5E7EB] p-6 rounded-3xl shadow-sm max-w-sm">
                            <h3 className="text-sm font-black text-slate-900 border-b border-slate-100 pb-3.5 mb-4">Admin Config</h3>
                            <div className="space-y-4 text-xs font-bold font-sans">
                                <div>
                                    <label className="block text-slate-500 mb-1.5">Constituency Jurisdiction Area</label>
                                    <input type="text" disabled defaultValue="Ward 5 - Sector Alpha" className="w-full bg-slate-50 border border-slate-200 rounded-xl py-2 px-3 text-slate-500 cursor-not-allowed outline-none font-bold" />
                                </div>
                                <div>
                                    <label className="block text-slate-500 mb-1.5">Model LLM Engine</label>
                                    <span className="text-[#0F9D58] block mt-1 font-black">Google Gemini 1.5 Flash API</span>
                                </div>
                            </div>
                        </div>
                    )}

                </main>
            </div>

            {/* ==================================================== */}
            {/* 3. MP ACTION DRAWER & DUPLICATE COLLABORATIONS LIST */}
            {/* ==================================================== */}
            {selectedComplaint && (
                <div className="fixed inset-0 z-50 bg-slate-900/40 backdrop-blur-xs flex items-center justify-end">
                    <div className="w-full max-w-xl bg-white h-screen overflow-y-auto flex flex-col justify-between shadow-2xl relative animate-slide-in">

                        {/* Drawer Header */}
                        <div>
                            <div className="p-6 border-b border-[#E5E7EB] flex items-center justify-between">
                                <div className="flex items-center gap-2">
                                    <button
                                        onClick={() => setSelectedComplaint(null)}
                                        className="p-1.5 hover:bg-slate-105 rounded-lg border text-slate-505 transition-all outline-none"
                                    >
                                        <ArrowLeft className="w-4 h-4" />
                                    </button>
                                    <span className="text-xs font-black text-slate-500">
                                        Priority Ticket: #JV-{selectedComplaint.id}
                                    </span>
                                </div>

                                <span className={`text-[10px] font-black uppercase px-2.5 py-1 rounded-full ${selectedComplaint.status === 'RESOLVED'
                                        ? 'bg-emerald-50 text-emerald-800 border border-emerald-150 font-black'
                                        : 'bg-slate-100 text-slate-500 border border-slate-250 font-black'
                                    }`}>
                                    {selectedComplaint.status}
                                </span>
                            </div>

                            {/* Drawer Content */}
                            <div className="p-6 space-y-6">
                                <div>
                                    <span className="text-[10px] bg-red-50 text-red-750 px-2 py-0.5 border border-red-150 rounded font-black uppercase tracking-wider mb-2 inline-block">
                                        {selectedComplaint.urgency} Urgency
                                    </span>
                                    <h3 className="text-sm font-bold text-slate-900 leading-relaxed">
                                        "{selectedComplaint.originalText}"
                                    </h3>
                                    <div className="mt-3 grid grid-cols-2 gap-3 text-[11px] font-semibold text-slate-700">
                                        <div className="rounded-xl bg-slate-50 p-3"><span className="block text-[10px] uppercase text-slate-400">Priority Score</span>{selectedComplaint.priorityScore || 0}</div>
                                        <div className="rounded-xl bg-slate-50 p-3"><span className="block text-[10px] uppercase text-slate-400">Affected People</span>{selectedComplaint.affectedPeople || selectedComplaint.upvotes || 0}</div>
                                        <div className="rounded-xl bg-slate-50 p-3"><span className="block text-[10px] uppercase text-slate-400">Duplicate Count</span>{selectedComplaint.duplicateCount || 0}</div>
                                        <div className="rounded-xl bg-slate-50 p-3"><span className="block text-[10px] uppercase text-slate-400">Suggested Dept</span>{selectedComplaint.suggestedDepartment || 'Municipal Coordination'}</div>
                                    </div>
                                </div>

                                {/* Status action switches */}
                                <div className="space-y-2">
                                    <span className="text-[10.5px] text-slate-400 font-extrabold uppercase tracking-wider block">Modify Status State</span>
                                    <div className="flex gap-2 text-xs font-bold select-none">
                                        {['OPEN', 'IN_PROGRESS', 'RESOLVED'].map((lvl) => (
                                            <button
                                                key={lvl}
                                                onClick={() => handleUpdateStatus(selectedComplaint.id, lvl)}
                                                className={`px-4 py-2 border rounded-xl transition-all cursor-pointer outline-none ${selectedComplaint.status === lvl
                                                        ? 'bg-emerald-50 text-[#0F9D58] border-[#0F9D58] font-black'
                                                        : 'bg-white border-slate-200 text-slate-655 hover:bg-slate-50'
                                                    }`}
                                            >
                                                {lvl}
                                            </button>
                                        ))}
                                    </div>
                                </div>

                                <div className="rounded-2xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
                                    <p className="font-black">AI action recommendation</p>
                                    <p className="mt-1 text-xs font-medium">{selectedComplaint.priorityReason || 'Escalate to the most relevant department and assign a field team.'}</p>
                                </div>

                                {/* CONSOLDIDATED AI REPORTED DUPLICATES GROUP ACCORDION */}
                                {selectedComplaint.linkedComplaints && selectedComplaint.linkedComplaints.length > 0 && (
                                    <div className="space-y-3 pt-5 border-t border-slate-100">
                                        <div className="flex items-center gap-1.5 text-yellow-805">
                                            <Sparkles className="w-4.5 h-4.5" />
                                            <span className="text-xs font-black uppercase tracking-wide">
                                                AI Consolidated Duplicates list
                                            </span>
                                        </div>

                                        <p className="text-[11px] text-slate-500 font-medium leading-relaxed">
                                            Our Gemini duplicate-detection pipeline linked the following similar constituent reports to this master issue:
                                        </p>

                                        <div className="space-y-2 max-h-56 overflow-y-auto">
                                            {selectedComplaint.linkedComplaints.map((dup) => (
                                                <div key={dup.id} className="p-3 bg-yellow-50/30 border border-yellow-105 rounded-xl space-y-1 text-xs">
                                                    <div className="flex justify-between items-center text-[10px] text-slate-400 font-bold">
                                                        <span>Dup Ticket: #{dup.id}</span>
                                                        <span className="text-yellow-750">Upvote weight: +{dup.upvotes}</span>
                                                    </div>
                                                    <p className="text-slate-800 leading-snug font-semibold">{dup.originalText}</p>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}

                            </div>
                        </div>

                        {/* Footer buttons action */}
                        <div className="p-6 border-t border-[#E5E7EB] bg-slate-50 flex items-center justify-between">
                            <div className="text-xs">
                                <span className="text-slate-455 block">Aggregated Priority score</span>
                                <span className="font-black text-slate-800 text-sm">{selectedComplaint.priorityScore || 0} • {selectedComplaint.duplicateCount || 0} duplicate reports</span>
                            </div>

                            <button
                                onClick={() => setSelectedComplaint(null)}
                                className="px-5 py-2.5 bg-slate-805 text-white font-bold rounded-xl text-xs hover:bg-slate-900 transition-all cursor-pointer outline-none"
                            >
                                Close details
                            </button>
                        </div>

                    </div>
                </div>
            )}

        </div>
    );
}
