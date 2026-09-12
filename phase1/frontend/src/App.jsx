import React, { useMemo, useRef, useState } from "react";
import {
  Recycle, Camera, IndianRupee, MapPin, Package, WalletCards,
  ArrowRight, CheckCircle2, ShieldCheck, TrendingUp, QrCode,
  Search, X, Leaf, Smartphone, Laptop, Battery, Cable, CircuitBoard,
  ChevronRight, Clock3, Truck, CircleAlert, ScanLine, Banknote,
  Languages, Menu, XCircle
} from "lucide-react";
import "./App.css";

const MATERIALS = {
  "Mobile Phone": { icon: Smartphone, min: 180, max: 300, tip: "Remove SIM card before handover" },
  Laptop: { icon: Laptop, min: 250, max: 450, tip: "Keep battery intact; do not open" },
  "PCB / Motherboard": { icon: CircuitBoard, min: 250, max: 400, tip: "Do not burn or chemically process" },
  Battery: { icon: Battery, min: 80, max: 150, tip: "Do not puncture, burn or crush" },
  "Charger / Cable": { icon: Cable, min: 100, max: 180, tip: "Separate copper-rich cables if safe" },
};

const RECYCLERS = [
  { name: "GreenCycle Recycling", distance: "2.4 km", accepts: "PCB, Laptop, Mobile", offer: "Fair market offer", pickup: "Today • 6:00 PM", score: "4.8" },
  { name: "EcoLoop E-Waste", distance: "4.1 km", accepts: "Battery, Cable, Mobile", offer: "Same-day pickup", pickup: "Today • 7:30 PM", score: "4.7" },
  { name: "CleanTech Recycler", distance: "6.8 km", accepts: "All common e-waste", offer: "Digital handover", pickup: "Tomorrow • 10:00 AM", score: "4.9" },
];

const seedLots = [
  { id: "EW-IND-00021", material: "PCB / Motherboard", weight: 1.5, value: 525, status: "Completed", date: "Today", recycler: "GreenCycle Recycling" },
  { id: "EW-IND-00018", material: "Mobile Phone", weight: 2, value: 480, status: "Completed", date: "08 Sep", recycler: "EcoLoop E-Waste" },
];

const pageTitles = {
  scan: "Scan & identify e-waste",
  prices: "Today's fair-price reference",
  recyclers: "Verified recyclers near you",
  lots: "Your digital e-waste lots",
  earnings: "Your earnings ledger",
};

export default function App() {
  const [active, setActive] = useState("home");
  const [lots, setLots] = useState(seedLots);
  const [material, setMaterial] = useState("PCB / Motherboard");
  const [weight, setWeight] = useState("1.5");
  const [photo, setPhoto] = useState(null);
  const [notice, setNotice] = useState("");
  const [aiScanned, setAiScanned] = useState(false);
  const [selectedRecycler, setSelectedRecycler] = useState(null);
  const [mobileMenu, setMobileMenu] = useState(false);
  const [hindi, setHindi] = useState(false);
  const fileRef = useRef();

  const pricing = MATERIALS[material];
  const estimate = useMemo(() => {
    const w = Math.max(0, Number(weight) || 0);
    return { min: Math.round(w * pricing.min), max: Math.round(w * pricing.max), mid: Math.round((w * pricing.min + w * pricing.max) / 2) };
  }, [weight, pricing]);

  const completed = lots.filter(l => l.status === "Completed");
  const pending = lots.filter(l => l.status !== "Completed");
  const total = completed.reduce((s, l) => s + l.value, 0);
  const totalWeight = lots.reduce((s, l) => s + Number(l.weight || 0), 0);

  const notify = (msg) => { setNotice(msg); window.setTimeout(() => setNotice(""), 4500); };

  const scanPhoto = () => {
    setMaterial("PCB / Motherboard");
    setWeight("1.5");
    setAiScanned(true);
    notify("AI demo identified PCB / Motherboard with 94% confidence. Please verify before selling.");
  };

  const createLot = () => {
    if (!Number(weight) || Number(weight) <= 0) return notify("Enter a valid weight first.");
    const id = `EW-IND-${String(lots.length + 22).padStart(5, "0")}`;
    const newLot = { id, material, weight: Number(weight), value: estimate.mid, status: "Awaiting Recycler", date: "Just now", recycler: "Not assigned" };
    setLots([newLot, ...lots]);
    setActive("lots");
    notify(`Digital lot ${id} created. Choose a verified recycler for handover.`);
  };

  const assignRecycler = (recycler) => {
    if (!lots[0]) return;
    setLots(prev => prev.map((l, i) => i === 0 && l.status !== "Completed" ? { ...l, recycler: recycler.name, status: "Recycler Selected" } : l));
    setSelectedRecycler(recycler.name);
    notify(`${recycler.name} selected for ${lots[0].id}. Handover is ready to verify.`);
  };

  const completeHandover = (id) => {
    setLots(prev => prev.map(l => l.id === id ? { ...l, status: "Completed", date: "Just now" } : l));
    notify(`Handover verified for ${id}. Payment recorded in your earnings ledger.`);
  };

  const navigate = (id) => { setActive(id); setMobileMenu(false); };

  return (
    <div className="app">
      <aside className={`sidebar ${mobileMenu ? "open" : ""}`}>
        <div>
          <div className="brand">
            <div className="brand-icon"><Recycle size={25}/></div>
            <div><b>E-Waste Saathi</b><span>Collector Portal</span></div>
            <button className="mobile-close" onClick={() => setMobileMenu(false)}><X size={18}/></button>
          </div>
          <nav>
            <Nav active={active} id="home" label={hindi ? "डैशबोर्ड" : "Dashboard"} icon={<TrendingUp size={18}/>} setActive={navigate}/>
            <Nav active={active} id="scan" label={hindi ? "ई-कचरा स्कैन" : "Scan E-Waste"} icon={<Camera size={18}/>} setActive={navigate}/>
            <Nav active={active} id="prices" label={hindi ? "आज के भाव" : "Today's Prices"} icon={<IndianRupee size={18}/>} setActive={navigate}/>
            <Nav active={active} id="recyclers" label={hindi ? "रीसाइकलर खोजें" : "Find Recycler"} icon={<MapPin size={18}/>} setActive={navigate}/>
            <Nav active={active} id="lots" label={hindi ? "मेरे लॉट" : "My Lots"} icon={<Package size={18}/>} setActive={navigate}/>
            <Nav active={active} id="earnings" label={hindi ? "मेरी कमाई" : "My Earnings"} icon={<WalletCards size={18}/>} setActive={navigate}/>
          </nav>
        </div>
        <div className="trust-card">
          <ShieldCheck size={22}/>
          <b>{hindi ? "सुरक्षित और औपचारिक रीसाइक्लिंग" : "Safer formal recycling"}</b>
          <p>{hindi ? "सही भाव जानें और हर handover का रिकॉर्ड रखें।" : "Know the fair value and keep a traceable handover record."}</p>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <button className="menu-btn" onClick={() => setMobileMenu(true)}><Menu size={21}/></button>
          <div>
            <div className="eyebrow">INDORE • COLLECTOR ACCOUNT</div>
            <h1>{active === "home" ? (hindi ? "नमस्ते, आज क्या बेचेंगे? 👋" : "Good afternoon, Sachin 👋") : pageTitles[active]}</h1>
          </div>
          <div className="top-actions">
            <button className="lang-btn" onClick={() => setHindi(v => !v)}><Languages size={16}/> {hindi ? "EN" : "हिंदी"}</button>
            <div className="profile">SN</div>
          </div>
        </header>

        {notice && <div className="notice"><CheckCircle2 size={18}/><span>{notice}</span><button onClick={() => setNotice("")}><X size={16}/></button></div>}

        {active === "home" && <Dashboard total={total} weight={totalWeight} lots={lots} pending={pending} setActive={navigate}/>} 
        {active === "scan" && <ScanPage material={material} setMaterial={setMaterial} weight={weight} setWeight={setWeight} photo={photo} setPhoto={setPhoto} aiScanned={aiScanned} setAiScanned={setAiScanned} fileRef={fileRef} pricing={pricing} estimate={estimate} scanPhoto={scanPhoto} createLot={createLot} />}
        {active === "prices" && <Prices setMaterial={setMaterial} selected={material}/>} 
        {active === "recyclers" && <Recyclers material={material} selectedRecycler={selectedRecycler} assignRecycler={assignRecycler}/>} 
        {active === "lots" && <Lots lots={lots} completeHandover={completeHandover} setActive={navigate}/>} 
        {active === "earnings" && <Earnings total={total} lots={lots}/>} 
      </main>

      <button className="mobile-scan" onClick={() => navigate("scan")}><Camera size={19}/> {hindi ? "स्कैन" : "Scan"}</button>
    </div>
  );
}

function Nav({active,id,label,icon,setActive}) {
  return <button className={active===id ? "nav active" : "nav"} onClick={() => setActive(id)}>{icon}<span>{label}</span>{active===id && <ChevronRight size={15}/>}</button>;
}

function Dashboard({total, weight, lots, pending, setActive}) {
  return <section className="content">
    <div className="hero-card dashboard-hero">
      <div className="hero-copy"><span className="pill">VERNACULAR • OFFLINE-FRIENDLY</span><h2>Turn scrap into traceable value.</h2><p>Identify e-waste, see a fair price, find a verified recycler and keep a digital earnings record.</p><div className="hero-actions"><button className="primary" onClick={() => setActive("scan")}><Camera size={18}/> Scan e-waste</button><button className="hero-secondary" onClick={() => setActive("prices")}><IndianRupee size={17}/> Check prices</button></div></div>
      <div className="hero-visual"><div className="hero-orbit orbit-a"></div><div className="hero-orbit orbit-b"></div><div className="hero-symbol big"><Leaf size={66}/></div><span className="floating-chip chip-1">AI 94%</span><span className="floating-chip chip-2">₹ FAIR</span></div>
    </div>

    <div className="stats">
      <Stat label="Total earnings" value={`₹${total.toLocaleString("en-IN")}`} icon={<IndianRupee/>} />
      <Stat label="E-waste collected" value={`${weight.toFixed(1)} kg`} icon={<Recycle/>} />
      <Stat label="Digital lots" value={lots.length} icon={<Package/>} />
      <Stat label="Pending handovers" value={pending.length} icon={<Clock3/>} />
    </div>

    <div className="two-col">
      <div className="panel">
        <div className="section-head"><div><h3>Recent digital lots</h3><p>Collection → recycler → payment trail</p></div><button className="link" onClick={() => setActive("lots")}>View all</button></div>
        {lots.slice(0,3).map(l => <LotRow key={l.id} lot={l}/>) }
      </div>
      <div className="panel">
        <div className="section-head"><div><h3>Quick actions</h3><p>Start a collection workflow</p></div></div>
        <Action onClick={() => setActive("scan")} icon={<ScanLine/>} title="Identify e-waste" text="Photo + AI-assisted category" />
        <Action onClick={() => setActive("prices")} icon={<Banknote/>} title="Check fair price" text="Compare reference ranges" />
        <Action onClick={() => setActive("recyclers")} icon={<Truck/>} title="Find recycler" text="Verified partners near Indore" />
      </div>
    </div>

    <div className="workflow panel">
      <div className="section-head"><div><span className="pill light">CORE WORKFLOW</span><h3>From scrap to verified payment</h3></div></div>
      <div className="steps">
        <Step n="01" icon={<Camera/>} title="Photo" text="Capture item" />
        <Step n="02" icon={<ScanLine/>} title="Identify" text="AI + confirm" />
        <Step n="03" icon={<IndianRupee/>} title="Fair price" text="Reference range" />
        <Step n="04" icon={<Package/>} title="Digital lot" text="Unique ID" />
        <Step n="05" icon={<ShieldCheck/>} title="Handover" text="Verify + record" />
      </div>
    </div>
  </section>;
}

function ScanPage({material,setMaterial,weight,setWeight,photo,setPhoto,aiScanned,setAiScanned,fileRef,pricing,estimate,scanPhoto,createLot}) {
  return <section className="content">
    <div className="hero-card compact-hero"><div><span className="pill">AI-ASSISTED • DEMO</span><h2>Identify your e-waste</h2><p>Upload a photo, confirm the category, enter weight and create a traceable lot.</p></div><div className="hero-symbol"><Camera size={40}/></div></div>
    <div className="scan-grid">
      <div className="panel upload-panel">
        <div className="panel-title"><div><h3>1. Capture item</h3><p>Photo works on entry-level Android</p></div><Camera size={20}/></div>
        <div className={`dropzone ${photo ? "has-photo" : ""}`} onClick={() => fileRef.current?.click()}>
          <input ref={fileRef} type="file" accept="image/*" hidden onChange={e => { if (e.target.files?.[0]) { setPhoto(URL.createObjectURL(e.target.files[0])); setAiScanned(false); } }}/>
          {photo ? <><img src={photo} className="preview" alt="Uploaded e-waste"/><span className="photo-label">Photo ready for AI analysis</span></> : <><div className="upload-icon"><Camera size={28}/></div><b>Upload / capture photo</b><span>JPG or PNG • tap anywhere</span></>}
        </div>
        <button className="primary wide" onClick={scanPhoto}><ScanLine size={18}/> {photo ? "Analyze with AI" : "Run demo AI scan"}</button>
        <div className="demo-note"><CircleAlert size={15}/><span>This Phase 1 build uses a demo classifier. Connect the backend AI before claiming live image classification.</span></div>
      </div>

      <div className="panel">
        <div className="panel-title"><div><h3>2. Confirm category</h3><p>Collector can correct the AI result</p></div><CheckCircle2 size={20}/></div>
        <div className="material-list">{Object.entries(MATERIALS).map(([name,m]) => {const I=m.icon;return <button className={material===name ? "material selected" : "material"} key={name} onClick={() => setMaterial(name)}><span className="material-icon"><I size={18}/></span><span>{name}</span>{material===name && <CheckCircle2 size={16}/>} </button>})}</div>
      </div>
    </div>

    <div className={`panel result ${aiScanned ? "result-live" : ""}`}>
      <div className="result-head"><div><span className="pill">{aiScanned ? "AI RESULT" : "READY TO REVIEW"}</span><h2>{material}</h2><p>{aiScanned ? "Confidence 94% • Verify the item before selling" : "Select a category or run the demo AI scan"}</p></div><div className="confidence"><strong>{aiScanned ? "94%" : "—"}</strong><span>confidence</span></div></div>
      <div className="form-row">
        <label>Weight (kg)<input type="number" min="0" step="0.1" value={weight} onChange={e => setWeight(e.target.value)}/></label>
        <div className="estimate"><span>Estimated fair value</span><strong>₹{estimate.min.toLocaleString("en-IN")} – ₹{estimate.max.toLocaleString("en-IN")}</strong><small>Reference: ₹{pricing.min}–₹{pricing.max}/kg</small></div>
      </div>
      <div className="safety"><ShieldCheck size={19}/><div><b>Safety guidance</b><span>{pricing.tip}</span></div></div>
      <button className="primary wide" onClick={createLot} disabled={!Number(weight)}><Package size={18}/> Create digital lot <ArrowRight size={18}/></button>
    </div>
  </section>;
}

function Prices({setMaterial,selected}) {
  return <section className="content"><div className="section-head page-section"><div><span className="pill">REFERENCE DATA</span><h2>Today's material prices</h2><p>Use these ranges for negotiation. Final offers vary by quality, quantity and recycler.</p></div></div><div className="price-grid">{Object.entries(MATERIALS).map(([name,m]) => {const I=m.icon;return <button className={`price-card ${selected===name ? "selected" : ""}`} key={name} onClick={() => setMaterial(name)}><div className="price-icon"><I size={23}/></div><b>{name}</b><span>Reference per kg</span><strong>₹{m.min} – ₹{m.max}</strong><small><TrendingUp size={14}/> Tap to use for scan</small></button>})}</div><div className="info"><ShieldCheck size={22}/><div><b>Price transparency</b><p>The app should eventually learn from verified transactions and show historical trends, helping collectors spot unusually low offers.</p></div></div></section>;
}

function Recyclers({material,selectedRecycler,assignRecycler}) {
  return <section className="content"><div className="map-banner"><MapPin size={24}/><div><b>Verified recycler network</b><span>Showing demo partners around Indore • Selected material: {material}</span></div><button className="outline"><Search size={16}/> Search</button></div><div className="recycler-list">{RECYCLERS.map(r => <div className={`recycler ${selectedRecycler===r.name ? "chosen" : ""}`} key={r.name}><div className="recycler-logo"><Recycle size={24}/></div><div className="recycler-info"><div><b>{r.name}</b><span className="verified"><ShieldCheck size={12}/> Verified</span></div><span>{r.distance} away • accepts {r.accepts}</span><small>★ {r.score} • {r.offer} • {r.pickup}</small></div><button className="primary small" onClick={() => assignRecycler(r)}>{selectedRecycler===r.name ? "Selected" : "Select"} <ArrowRight size={14}/></button></div>)}</div><div className="info"><Truck size={22}/><div><b>Next step</b><p>Create a digital lot first, then select a recycler. In the full backend, this screen will use verified partner data and location services.</p></div></div></section>;
}

function Lots({lots,completeHandover,setActive}) {
  return <section className="content"><div className="panel"><div className="section-head"><div><span className="pill">TRACEABILITY</span><h2>Digital lot history</h2><p>Every lot keeps the collection-to-payment trail in one place.</p></div><QrCode className="qr" size={27}/></div><div className="timeline">{lots.map(l => <div className="lot-card" key={l.id}><div className="lot-main"><div className="lot-icon"><Package size={20}/></div><div><b>{l.id}</b><span>{l.material} • {l.weight} kg • {l.date}</span><small><MapPin size={12}/> {l.recycler || "Not assigned"}</small></div></div><div className="lot-actions"><strong>₹{l.value.toLocaleString("en-IN")}</strong><span className={l.status === "Completed" ? "status done" : "status"}>{l.status}</span>{l.status === "Recycler Selected" && <button className="outline tiny" onClick={() => completeHandover(l.id)}><CheckCircle2 size={14}/> Verify handover</button>}</div></div>)}</div>{lots.some(l => l.status !== "Completed") && <div className="info"><ShieldCheck size={20}/><div><b>Traceable handover</b><p>For the demo, clicking “Verify handover” simulates recycler confirmation and records the payment.</p></div></div>}<button className="primary" onClick={() => setActive("recyclers")}><Truck size={17}/> Find a recycler for a lot</button></div></section>;
}

function Earnings({total,lots}) {
  const completed = lots.filter(l => l.status === "Completed");
  const avg = completed.length ? Math.round(total / completed.length) : 0;
  return <section className="content"><div className="earn-card"><div><span>Total recorded earnings</span><strong>₹{total.toLocaleString("en-IN")}</strong><small>{completed.length} completed handovers • Avg. ₹{avg.toLocaleString("en-IN")}</small></div><div className="earn-icon"><WalletCards size={35}/></div></div><div className="earn-grid"><div className="panel mini-stat"><span>Completed sales</span><strong>{completed.length}</strong><small>Verified handovers</small></div><div className="panel mini-stat"><span>Total sold weight</span><strong>{completed.reduce((s,l)=>s+Number(l.weight),0).toFixed(1)} kg</strong><small>Across completed lots</small></div><div className="panel mini-stat"><span>Average lot value</span><strong>₹{avg.toLocaleString("en-IN")}</strong><small>Simple demo average</small></div></div><div className="panel"><div className="section-head"><div><h3>Earnings ledger</h3><p>Payment records linked to digital lots</p></div><Banknote size={21}/></div>{completed.length ? completed.map(l => <LotRow key={l.id} lot={l}/>) : <EmptyState text="No completed handovers yet"/>}</div></section>;
}

function Stat({label,value,icon}) { return <div className="stat"><div className="stat-icon">{icon}</div><span>{label}</span><strong>{value}</strong></div>; }
function Action({onClick,icon,title,text}) { return <button className="action" onClick={onClick}>{icon}<div><b>{title}</b><span>{text}</span></div><ChevronRight/></button>; }
function Step({n,icon,title,text}) { return <div className="step"><div className="step-number">{n}</div><div className="step-icon">{icon}</div><b>{title}</b><span>{text}</span></div>; }
function LotRow({lot}) { return <div className="lot-row"><div className="lot-icon"><Package size={18}/></div><div><b>{lot.id}</b><span>{lot.material} • {lot.weight} kg</span></div><div className="lot-right"><strong>₹{lot.value.toLocaleString("en-IN")}</strong><small className={lot.status === "Completed" ? "done-text" : "pending-text"}>{lot.status}</small></div></div>; }
function EmptyState({text}) { return <div className="empty"><XCircle size={23}/><span>{text}</span></div>; }
