import React, { useEffect, useRef, useState } from "react";
import {
  Recycle, Camera, IndianRupee, MapPin, Package, WalletCards,
  ArrowRight, CheckCircle2, ShieldCheck, TrendingUp, QrCode,
  Search, X, Leaf, Smartphone, Laptop, Battery, Cable, CircuitBoard,
  ChevronRight, Clock3, Truck, CircleAlert, ScanLine, Banknote,
  Languages, Menu, XCircle
} from "lucide-react";
import { materialService, priceService, recyclerService, lotService, transactionService, earningsService, dashboardService } from "./services/api";
import "./App.css";

const MATERIAL_ICONS = {
  "Mobile Phone": Smartphone,
  Smartphone,
  Laptop,
  "Computer Monitor": Laptop,
  Battery,
  "Printed Circuit Board": CircuitBoard,
  "Copper Cable": Cable,
};

function materialIcon(material) {
  return MATERIAL_ICONS[material.deviceType] || MATERIAL_ICONS[material.commonName] || Recycle;
}

const copy = {
  en: {
    pageTitles: { scan: "Scan & identify electronic devices", prices: "Today's fair-price reference", recyclers: "Verified recyclers near you", lots: "Your digital material lots", transactions: "Transaction history", earnings: "Your earnings ledger" },
    nav: ["Dashboard", "Scan E-Waste", "Today's Prices", "Find Recycler", "My Lots", "Transactions", "My Earnings"],
    greeting: "Good afternoon, Sachin", dashboardTitle: "Turn e-waste materials into traceable value.",
    dashboardText: "Identify electronic devices, see a fair price, find an authorized recycler and keep a digital earnings record.",
    scan: "Scan e-waste", prices: "Check prices", viewAll: "View all", quickActions: "Quick actions", startWorkflow: "Start a collection workflow",
  },
  hi: {
    pageTitles: { scan: "ई-वेस्ट सामग्री पहचानें", prices: "आज के उचित भाव", recyclers: "पास के अधिकृत रीसाइकलर", lots: "आपके डिजिटल सामग्री लॉट", transactions: "लेन-देन इतिहास", earnings: "आपकी कमाई का रिकॉर्ड" },
    nav: ["डैशबोर्ड", "ई-वेस्ट स्कैन", "आज के भाव", "रीसाइकलर खोजें", "मेरे लॉट", "लेन-देन", "मेरी कमाई"],
    greeting: "नमस्ते, सचिन", dashboardTitle: "ई-वेस्ट सामग्री को रिकॉर्ड योग्य मूल्य में बदलें।",
    dashboardText: "इलेक्ट्रॉनिक उपकरण पहचानें, उचित भाव देखें, अधिकृत रीसाइकलर खोजें और कमाई का डिजिटल रिकॉर्ड रखें।",
    scan: "ई-वेस्ट स्कैन करें", prices: "भाव देखें", viewAll: "सभी देखें", quickActions: "त्वरित कार्य", startWorkflow: "संग्रह प्रक्रिया शुरू करें",
  },
};

function speak(text, language = "en-IN") {
  if (typeof window === "undefined" || !window.speechSynthesis) return false;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  utterance.lang = language;
  utterance.rate = 0.9;
  window.speechSynthesis.speak(utterance);
  return true;
}

function SpokenButton({ text, hindi = false }) {
  return <button className="outline tiny" type="button" onClick={() => speak(text, hindi ? "hi-IN" : "en-IN")} title={hindi ? "भाव सुनें" : "Speak rate"}><span aria-hidden="true">🔊</span> {hindi ? "सुनें" : "Listen"}</button>;
}

function priceSummary(records) {
  if (!records?.length) return null;
  const rates = records.map(record => Number(record.rate)).filter(Number.isFinite);
  if (!rates.length) return null;
  const latest = records.reduce((current, record) => record.quoted_at > current.quoted_at ? record : current, records[0]);
  return {
    min: Math.min(...rates),
    max: Math.max(...rates),
    unit: latest.unit,
    location: latest.location,
    source: latest.source_type,
    quotedAt: latest.quoted_at,
    verified: records.every(record => record.verification_status === "VERIFIED"),
    count: records.length,
  };
}

export default function App() {
  const [active, setActive] = useState("home");
  const [lots, setLots] = useState([]);
  const [lotStatus, setLotStatus] = useState("loading");
  const [transactions, setTransactions] = useState([]);
  const [transactionStatus, setTransactionStatus] = useState("loading");
  const [earnings, setEarnings] = useState(null);
  const [earningsStatus, setEarningsStatus] = useState("loading");
  const [dashboardSummary, setDashboardSummary] = useState(null);
  const [recentLots, setRecentLots] = useState([]);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [dashboardStatus, setDashboardStatus] = useState("loading");
  const [materialStatus, setMaterialStatus] = useState("loading");
  const [materials, setMaterials] = useState([]);
  const [material, setMaterial] = useState("");
  const [priceRecords, setPriceRecords] = useState({});
  const [priceStatus, setPriceStatus] = useState("loading");
  const [recyclerMatches, setRecyclerMatches] = useState([]);
  const [recyclerStatus, setRecyclerStatus] = useState("loading");
  const [weight, setWeight] = useState("");
  const [photo, setPhoto] = useState(null);
  const [notice, setNotice] = useState("");
  const [mobileMenu, setMobileMenu] = useState(false);
  const [hindi, setHindi] = useState(false);
  const fileRef = useRef();
  const language = hindi ? copy.hi : copy.en;

  useEffect(() => {
    let mounted = true;
    materialService.getAll()
      .then(data => {
        if (!mounted) return;
        setMaterials(Array.isArray(data) ? data : []);
        setMaterialStatus("ready");
        if (data?.length) setMaterial(data[0].commonName);
      })
      .catch(() => mounted && setMaterialStatus("error"));
    return () => { mounted = false; };
  }, []);

  useEffect(() => {
    let mounted = true;
    Promise.all([transactionService.getAll(), earningsService.get()])
      .then(([transactionData, earningsData]) => { if (mounted) { setTransactions(Array.isArray(transactionData) ? transactionData : []); setEarnings(earningsData); setTransactionStatus("ready"); setEarningsStatus("ready"); } })
      .catch(() => { if (mounted) { setTransactionStatus("error"); setEarningsStatus("error"); } });
    return () => { mounted = false; };
  }, []);

  const refreshLots = async () => {
    setLotStatus("loading");
    try {
      const data = await lotService.getAll();
      setLots(Array.isArray(data) ? data : []);
      setLotStatus("ready");
    } catch {
      setLotStatus("error");
    }
  };

  useEffect(() => {
    refreshLots();
  }, []);

  // Dashboard is always re-fetched from live endpoints whenever the home view
  // becomes active, so numbers and recent lists can never go stale.
  useEffect(() => {
    if (active !== "home") return;
    let mounted = true;
    setDashboardStatus("loading");
    Promise.all([dashboardService.summary(), dashboardService.recentLots(5), dashboardService.recentTransactions(5)])
      .then(([summary, lotsData, txData]) => {
        if (!mounted) return;
        setDashboardSummary(summary);
        setRecentLots(Array.isArray(lotsData) ? lotsData : []);
        setRecentTransactions(Array.isArray(txData) ? txData : []);
        setDashboardStatus("ready");
      })
      .catch(() => mounted && setDashboardStatus("error"));
    return () => { mounted = false; };
  }, [active]);

  useEffect(() => {
    const selectedMaterial = materials.find(item => item.commonName === material);
    if (!selectedMaterial) return;
    let mounted = true;
    setRecyclerStatus("loading");
    recyclerService.match(selectedMaterial.id, "Indore")
      .then(data => { if (mounted) { setRecyclerMatches(Array.isArray(data) ? data : []); setRecyclerStatus("ready"); } })
      .catch(() => mounted && setRecyclerStatus("error"));
    return () => { mounted = false; };
  }, [materials, material]);

  useEffect(() => {
    let mounted = true;
    setPriceStatus("loading");
    priceService.getCurrent({ location: "Indore" })
      .then(data => {
        if (!mounted) return;
        setPriceRecords((Array.isArray(data) ? data : []).reduce((grouped, record) => {
          (grouped[record.material_id] ||= []).push(record);
          return grouped;
        }, {}));
        setPriceStatus("ready");
      })
      .catch(() => mounted && setPriceStatus("error"));
    return () => { mounted = false; };
  }, []);

  const notify = (msg) => { setNotice(msg); window.setTimeout(() => setNotice(""), 4500); };

  const scanPhoto = () => {
    notify(hindi ? "AI पहचान अभी उपलब्ध नहीं है। कृपया सामग्री चुनें।" : "AI classification is unavailable. Please select the material.");
  };

  const createLot = async () => {
    const selectedMaterial = materials.find(item => item.commonName === material);
    if (!selectedMaterial || !Number(weight) || Number(weight) <= 0) return notify(hindi ? "सामग्री और सही वजन भरें।" : "Select a material and enter a valid weight.");
    const selectedPrice = priceRecords[selectedMaterial.id]?.find(record => record.verification_status !== "EXPIRED");
    try {
      const lot = await lotService.create({ materialId: selectedMaterial.id, approximateWeight: Number(weight), photoUrl: null, selectedPriceRecordId: selectedPrice?.id || null });
      setLots(current => [lot, ...current]);
      setLotStatus("ready");
      setActive("lots");
      notify(`${hindi ? "डिजिटल लॉट तैयार" : "Digital lot created"}: ${lot.lot_reference}`);
    } catch (error) {
      notify(error.response?.data?.detail || (hindi ? "लॉट नहीं बन सका। कृपया फिर कोशिश करें।" : "Unable to create lot. Please try again."));
    }
  };

  const navigate = (id) => { setActive(id); setMobileMenu(false); };
  const refreshLedger = async () => {
    const [transactionData, earningsData] = await Promise.all([transactionService.getAll(), earningsService.get()]);
    setTransactions(Array.isArray(transactionData) ? transactionData : []);
    setEarnings(earningsData);
    setTransactionStatus("ready");
    setEarningsStatus("ready");
  };

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
            {[ ["home", TrendingUp], ["scan", Camera], ["prices", IndianRupee], ["recyclers", MapPin], ["lots", Package], ["transactions", Banknote], ["earnings", WalletCards] ].map(([id, Icon], index) => <Nav key={id} active={active} id={id} label={language.nav[index]} icon={<Icon size={18}/>} setActive={navigate}/>)}
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
            <h1>{active === "home" ? `${language.greeting} 👋` : language.pageTitles[active]}</h1>
          </div>
          <div className="top-actions">
            <button className="lang-btn" onClick={() => setHindi(v => !v)}><Languages size={16}/> {hindi ? "EN" : "हिंदी"}</button>
            <div className="profile">SN</div>
          </div>
        </header>

        {notice && <div className="notice"><CheckCircle2 size={18}/><span>{notice}</span><button onClick={() => setNotice("")}><X size={16}/></button></div>}

        {active === "home" && <Dashboard summary={dashboardSummary} recentLots={recentLots} recentTransactions={recentTransactions} status={dashboardStatus} setActive={navigate} hindi={hindi} language={language}/>}
        {active === "scan" && <ScanPage materials={materials} materialStatus={materialStatus} material={material} setMaterial={setMaterial} weight={weight} setWeight={setWeight} photo={photo} setPhoto={setPhoto} fileRef={fileRef} scanPhoto={scanPhoto} createLot={createLot} hindi={hindi} priceRecords={priceRecords} priceStatus={priceStatus}/>}
        {active === "prices" && <Prices hindi={hindi} materials={materials} priceRecords={priceRecords} priceStatus={priceStatus}/>}
        {active === "recyclers" && <Recyclers material={material} matches={recyclerMatches} status={recyclerStatus} hindi={hindi}/>}
        {active === "lots" && <Lots lots={lots} status={lotStatus} setActive={navigate} hindi={hindi} refreshLots={refreshLots}/>}
        {active === "transactions" && <Transactions lots={lots} transactions={transactions} status={transactionStatus} refreshLedger={refreshLedger} hindi={hindi}/>}
        {active === "earnings" && <Earnings earnings={earnings} status={earningsStatus} transactions={transactions} hindi={hindi}/>}
      </main>

      <button className="mobile-scan" onClick={() => navigate("scan")}><Camera size={19}/> {hindi ? "स्कैन" : "Scan"}</button>
    </div>
  );
}

function Nav({active,id,label,icon,setActive}) {
  return <button className={active===id ? "nav active" : "nav"} onClick={() => setActive(id)}>{icon}<span>{label}</span>{active===id && <ChevronRight size={15}/>}</button>;
}

function Dashboard({summary, recentLots, recentTransactions, status, setActive, hindi, language}) {
  if (status === "loading") return <section className="content"><div className="empty panel"><span>{hindi ? "डैशबोर्ड लोड हो रहा है..." : "Loading dashboard..."}</span></div></section>;
  if (status === "error") return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "डैशबोर्ड लोड नहीं हो सका। कृपया फिर कोशिश करें।" : "Unable to load dashboard. Please try again."}</span></div></section>;
  const total = Number(summary?.total_earnings || 0);
  const pendingAmount = Number(summary?.pending_amount || 0);
  const kg = Number(summary?.total_kg_collected || 0);
  const digitalLots = Number(summary?.total_lots_count || 0);
  const activeLots = Number(summary?.active_lots_count || 0);
  const completedHandovers = Number(summary?.completed_handovers_count || 0);
  return <section className="content">
    <div className="hero-card dashboard-hero">
      <div className="hero-copy"><span className="pill">VERNACULAR • OFFLINE-FRIENDLY</span><h2>{language.dashboardTitle}</h2><p>{language.dashboardText}</p><div className="hero-actions"><button className="primary" onClick={() => setActive("scan")}><Camera size={18}/> {language.scan}</button><button className="hero-secondary" onClick={() => setActive("prices")}><IndianRupee size={17}/> {language.prices}</button></div></div>
      <div className="hero-visual"><div className="hero-orbit orbit-a"></div><div className="hero-orbit orbit-b"></div><div className="hero-symbol big"><Leaf size={66}/></div><span className="floating-chip chip-1">{hindi ? "डेटा" : "DATA"}</span><span className="floating-chip chip-2">{hindi ? "रिक्त" : "EMPTY"}</span></div>
    </div>

    <div className="stats six">
      <Stat label="Total earnings" value={`₹${total.toLocaleString("en-IN")}`} icon={<IndianRupee/>} />
      <Stat label="Pending amount" value={`₹${pendingAmount.toLocaleString("en-IN")}`} icon={<Clock3/>} />
      <Stat label="E-waste collected" value={`${kg.toFixed(1)} kg`} icon={<Recycle/>} />
      <Stat label="Digital lots" value={digitalLots} icon={<Package/>} />
      <Stat label="Active lots" value={activeLots} icon={<TrendingUp/>} />
      <Stat label="Completed handovers" value={completedHandovers} icon={<ShieldCheck/>} />
    </div>

    <div className="two-col">
      <div className="panel">
        <div className="section-head"><div><h3>{hindi ? "हाल के डिजिटल लॉट" : "Recent digital lots"}</h3><p>{hindi ? "वास्तविक लॉट रिकॉर्ड — स्थिति और तारीख के साथ" : "Real lot records — status and date"}</p></div><button className="link" onClick={() => setActive("lots")}>{language.viewAll}</button></div>
        {recentLots.length === 0 ? <div className="empty"><XCircle size={22}/><span>{hindi ? "अभी कोई लॉट नहीं" : "No lots yet"}</span></div> : recentLots.map(l => <DashboardLotRow key={l.id} lot={l} hindi={hindi}/>)}
      </div>
      <div className="panel">
        <div className="section-head"><div><h3>{hindi ? "हाल के लेन-देन" : "Recent transactions"}</h3><p>{hindi ? "दर्ज किए गए भुगतान रिकॉर्ड" : "Recorded payment entries"}</p></div><button className="link" onClick={() => setActive("transactions")}>{language.viewAll}</button></div>
        {recentTransactions.length === 0 ? <div className="empty"><XCircle size={22}/><span>{hindi ? "अभी कोई लेन-देन नहीं" : "No transactions yet"}</span></div> : recentTransactions.map(t => <DashboardTxRow key={t.id} tx={t} hindi={hindi}/>)}
      </div>
    </div>

    <div className="panel">
      <div className="section-head"><div><h3>{language.quickActions}</h3><p>{language.startWorkflow}</p></div></div>
      <Action onClick={() => setActive("scan")} icon={<ScanLine/>} title={hindi ? "ई-वेस्ट सामग्री पहचानें" : "Identify e-waste"} text={hindi ? "फोटो + AI श्रेणी" : "Photo + AI-assisted category"} />
      <Action onClick={() => setActive("prices")} icon={<Banknote/>} title={hindi ? "उचित भाव देखें" : "Check fair price"} text={hindi ? "क्षेत्रीय दरों की तुलना" : "Compare reference ranges"} />
      <Action onClick={() => setActive("recyclers")} icon={<Truck/>} title={hindi ? "रीसाइकलर खोजें" : "Find recycler"} text={hindi ? "इंदौर के सत्यापित साझेदार" : "Verified partners near Indore"} />
    </div>

    <div className="workflow panel">
      <div className="section-head"><div><span className="pill light">CORE WORKFLOW</span><h3>{hindi ? "ई-वेस्ट सामग्री से सत्यापित भुगतान तक" : "From e-waste material to verified payment"}</h3></div></div>
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

function ScanPage({materials,materialStatus,material,setMaterial,weight,setWeight,photo,setPhoto,fileRef,scanPhoto,createLot,hindi,priceRecords,priceStatus}) {
  const selectedMaterial = materials.find(item => item.commonName === material);
  const summary = selectedMaterial ? priceSummary(priceRecords[selectedMaterial.id]) : null;
  const safetyText = hindi ? "सुरक्षित हैंडओवर के लिए सामग्री को न जलाएं और बैटरी को सावधानी से रखें।" : "Do not burn materials. Store batteries safely and use a verified process when available.";
  const materialContent = materialStatus === "loading"
    ? <div className="empty"><span>Loading materials...</span></div>
    : materialStatus === "error"
      ? <div className="empty"><span>Unable to load materials. Please try again.</span></div>
      : materials.length === 0
        ? <div className="empty"><span>No materials available.</span></div>
        : <div className="material-list">{materials.map(item => { const Icon = materialIcon(item); return <button className={material === item.commonName ? "material selected" : "material"} key={item.id} onClick={() => setMaterial(item.commonName)}><span className="material-icon"><Icon size={18}/></span><span>{item.commonName}</span>{material === item.commonName && <CheckCircle2 size={16}/>}</button>; })}</div>;
  return <section className="content">
    <div className="hero-card compact-hero"><div><span className="pill">MATERIAL MASTER</span><h2>{hindi ? "ई-वेस्ट सामग्री चुनें" : "Select your e-waste material"}</h2><p>{hindi ? "फोटो लें, सामग्री की पुष्टि करें और वजन भरें।" : "Capture a photo, confirm the material from the database, and enter weight."}</p></div><div className="hero-symbol"><Camera size={40}/></div></div>
    <div className="scan-grid">
      <div className="panel upload-panel">
        <div className="panel-title"><div><h3>1. {hindi ? "उपकरण की फोटो लें" : "Capture device"}</h3><p>{hindi ? "साधारण Android फोन पर भी काम करता है" : "Photo works on entry-level Android"}</p></div><Camera size={20}/></div>
        <div className={`dropzone ${photo ? "has-photo" : ""}`} onClick={() => fileRef.current?.click()}>
          <input ref={fileRef} type="file" accept="image/*" capture="environment" hidden onChange={e => { if (e.target.files?.[0]) setPhoto(URL.createObjectURL(e.target.files[0])); }}/>
          {photo ? <><img src={photo} className="preview" alt="Uploaded electronic device"/><span className="photo-label">{hindi ? "फोटो तैयार" : "Photo ready"}</span></> : <><div className="upload-icon"><Camera size={28}/></div><b>{hindi ? "फोटो अपलोड / कैमरा खोलें" : "Upload / capture photo"}</b><span>JPG or PNG • {hindi ? "कहीं भी टैप करें" : "tap anywhere"}</span></>}
        </div>
        <button className="outline wide" onClick={scanPhoto}><ScanLine size={18}/> {hindi ? "सामग्री चुनें" : "Choose material"}</button>
        <div className="demo-note"><CircleAlert size={15}/><span>{hindi ? "AI पहचान अभी उपलब्ध नहीं है।" : "AI classification is not available yet. Select the material yourself."}</span></div>
      </div>

      <div className="panel">
        <div className="panel-title"><div><h3>2. {hindi ? "सामग्री चुनें" : "Select material"}</h3><p>{hindi ? "डेटाबेस की सामग्री सूची" : "Materials from the database"}</p></div><CheckCircle2 size={20}/></div>
        {materialContent}
      </div>
    </div>

    <div className="panel result">
      <div className="result-head"><div><span className="pill">READY TO REVIEW</span><h2>{selectedMaterial?.commonName || (hindi ? "सामग्री उपलब्ध नहीं" : "No material selected")}</h2><p>{selectedMaterial?.recoverableMaterials || (hindi ? "डेटाबेस से सामग्री चुनें" : "Select a material from the database")}</p></div></div>
      <div className="form-row">
        <label>{hindi ? "वजन (किलो)" : "Weight (kg)"}<input type="number" min="0" step="0.1" value={weight} onChange={e => setWeight(e.target.value)}/></label>
        <div className="estimate"><span>{hindi ? "स्थानीय अनुमानित मूल्य" : "Estimated local value"}</span><strong>{priceStatus === "loading" ? (hindi ? "भाव लोड हो रहे हैं..." : "Loading prices...") : priceStatus === "error" ? (hindi ? "भाव लोड नहीं हो सके" : "Unable to load prices. Please try again.") : summary ? `₹${Math.round(Number(weight || 0) * summary.min)}–₹${Math.round(Number(weight || 0) * summary.max)} (${summary.unit})` : (hindi ? "स्थानीय भाव आवश्यक" : "Price data unavailable — local quote required")}</strong><small>{summary ? `${summary.verified ? (hindi ? "सत्यापित" : "Verified") : (hindi ? "असत्यापित" : "Unverified")} • ${summary.count} ${hindi ? "स्थानीय भाव" : "local quotes"}` : (hindi ? "दर उपलब्ध होने पर मूल्य दिखेगा" : "No rate is shown without a local price record")}</small></div>
      </div>
      <div className="safety"><ShieldCheck size={19}/><div><b>{hindi ? "सुरक्षा मार्गदर्शन" : "Safety guidance"}</b><span>{safetyText}</span></div><SpokenButton text={safetyText} hindi={hindi}/></div>
      <button className="primary wide" onClick={createLot} disabled={!selectedMaterial || !Number(weight)}><Package size={18}/> {hindi ? "डिजिटल लॉट बनाएं" : "Create digital lot"} <ArrowRight size={18}/></button>
    </div>
  </section>;
}

function Prices({hindi,materials,priceRecords,priceStatus}) {
  const statusText = priceStatus === "loading" ? (hindi ? "भाव लोड हो रहे हैं..." : "Loading prices...") : priceStatus === "error" ? (hindi ? "भाव लोड नहीं हो सके। कृपया फिर कोशिश करें।" : "Unable to load prices. Please try again.") : "";
  return <section className="content"><div className="section-head page-section"><div><span className="pill">PRICE DATA</span><h2>{hindi ? "आज की सामग्री दरें" : "Today's material prices"}</h2><p>{hindi ? "इंदौर के वास्तविक स्थानीय रिकॉर्ड से पारदर्शी दरें।" : "Transparent rates from real local records in Indore."}</p></div></div>{priceStatus !== "ready" ? <div className="empty panel"><CircleAlert size={23}/><span>{statusText}</span></div> : <div className="price-grid">{materials.map(material => <PriceCard key={material.id} material={material} summary={priceSummary(priceRecords[material.id])} hindi={hindi}/>)}</div>}</section>;
}

function PriceCard({material,summary,hindi}) {
  const Icon = materialIcon(material);
  if (!summary) return <div className="price-card"><div className="price-icon"><Icon size={23}/></div><b>{material.commonName}</b><span>{hindi ? "स्थानीय रिकॉर्ड" : "Local record"}</span><strong>{hindi ? "स्थानीय भाव आवश्यक" : "Price data unavailable — local quote required"}</strong></div>;
  const rateText = `${material.commonName}: ${summary.min} to ${summary.max} ${summary.unit} in ${summary.location}`;
  return <div className="price-card"><div className="price-icon"><Icon size={23}/></div><b>{material.commonName}</b><span>{summary.location} • {summary.unit}</span><strong>₹{summary.min}–₹{summary.max} / {summary.unit}</strong><small>{summary.verified ? (hindi ? "सत्यापित" : "Verified") : (hindi ? "असत्यापित" : "Unverified")} • {summary.count} {hindi ? "स्थानीय भाव" : "local quotes"}</small><small>{hindi ? "अपडेट" : "Updated"} {new Date(summary.quotedAt).toLocaleDateString("en-IN")} • {summary.source}</small><SpokenButton text={rateText} hindi={hindi}/></div>;
}

function RecyclersEmpty({hindi}) {
  return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "इस चरण में सत्यापित रीसाइकलर उपलब्ध नहीं हैं।" : "No verified recycler available for this material/location"}</span></div></section>;
}

function Recyclers({material,matches,status,hindi}) {
  if (status === "loading") return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "रीसाइकलर खोजे जा रहे हैं..." : "Finding recyclers..."}</span></div></section>;
  if (status === "error") return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "रीसाइकलर लोड नहीं हो सके। कृपया फिर कोशिश करें।" : "Unable to load recyclers. Please try again."}</span></div></section>;
  return <section className="content"><div className="map-banner"><MapPin size={24}/><div><b>{hindi ? "इंदौर रीसाइकलर मैच" : "Indore recycler matches"}</b><span>{hindi ? `${material} स्वीकार करने वाले वास्तविक रिकॉर्ड` : `Live records accepting ${material}`}</span></div></div>{matches.length === 0 ? <div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "इस सामग्री/स्थान के लिए अधिकृत रीसाइकलर उपलब्ध नहीं है" : "No verified recycler available for this material/location"}</span></div> : <div className="recycler-list">{matches.map(recycler => { const verified = recycler.authorization_status === "VERIFIED"; return <div className="recycler" key={recycler.id}><div className="recycler-logo"><Recycle size={24}/></div><div className="recycler-info"><div><b>{recycler.business_name}</b><span className={verified ? "verified" : "pending-badge"}><ShieldCheck size={12}/> {verified ? (hindi ? "सत्यापित" : "Verified") : (hindi ? "सत्यापन लंबित" : "Pending Verification — not yet confirmed")}</span></div><span>{recycler.city}{recycler.service_area ? ` • ${recycler.service_area}` : ""}</span><small>{recycler.accepted_materials.map(item => item.common_name).join(", ")}</small><small>{recycler.pickup_available ? (hindi ? "पिकअप उपलब्ध" : "Pickup available") : (hindi ? "पिकअप उपलब्ध नहीं" : "Pickup unavailable")}{recycler.pickup_radius ? ` • ${recycler.pickup_radius} km radius` : ""}</small></div></div>; })}</div>}</section>;
}


function TokenModal({lot,hindi,onClose}) {
  const spoken = hindi ? `लॉट ${lot.id} का ट्रेस करने योग्य टोकन ${lot.token} है।` : `The traceable token for lot ${lot.id} is ${lot.token}.`;
  return <div className="token-overlay" role="presentation" onClick={onClose}><div className="panel token-modal" role="dialog" aria-modal="true" aria-labelledby="token-title" onClick={event => event.stopPropagation()}><button className="mobile-close token-close" onClick={onClose} aria-label={hindi ? "बंद करें" : "Close"}><X size={18}/></button><div className="token-icon"><QrCode size={46}/></div><span className="pill">{hindi ? "ट्रेस करने योग्य टोकन" : "TRACEABLE TOKEN"}</span><h2 id="token-title">{hindi ? "डिजिटल लॉट तैयार है" : "Digital lot is ready"}</h2><p>{hindi ? "इस QR/टोकन को अधिकृत रीसाइकलर हैंडओवर पर सत्यापित कर सकता है।" : "An authorized recycler can verify this QR/token at handover."}</p><div className="token-code"><strong>{lot.id}</strong><span>{lot.token}</span></div><div className="token-meta"><span>{lot.material} • {lot.weight} kg</span><span>₹{lot.value.toLocaleString("en-IN")}</span></div><div className="hero-actions"><button className="primary" onClick={() => speak(spoken, hindi ? "hi-IN" : "en-IN")}><span aria-hidden="true">🔊</span> {hindi ? "टोकन सुनें" : "Speak token"}</button><button className="outline" onClick={onClose}>{hindi ? "लॉट देखें" : "View lot"}</button></div></div></div>;
}

function Lots({lots,status,setActive,hindi,refreshLots}) {
  const statusText = status => ({Completed: hindi ? "पूरा" : "Completed", "Recycler Selected": hindi ? "रीसाइकलर चुना गया" : "Recycler Selected", "Awaiting Recycler": hindi ? "रीसाइकलर की प्रतीक्षा" : "Awaiting Recycler", HANDED_OVER: hindi ? "हैंडओवर किया गया" : "Handed Over", RECYCLER_CONFIRMED: hindi ? "रीसाइकलर पुष्टिकृत" : "Recycler Confirmed"}[status] || status);
  const lotValue = lot => lot.estimated_value == null ? (hindi ? "उपलब्ध नहीं" : "Not available") : `₹${Number(lot.estimated_value).toLocaleString("en-IN")}`;
  const role = (() => { try { return JSON.parse(localStorage.getItem("janvoice_user") || "null")?.role || ""; } catch { return ""; } })();
  const isCollector = role === "CITIZEN" || role === "COLLECTOR";
  const isRecycler = role === "RECYCLER" || role === "VERIFIED_RECYCLER";
  const [finalWeight, setFinalWeight] = useState("");
  const [busyId, setBusyId] = useState(null);
  const formatTime = value => value ? new Date(value).toLocaleString("en-IN") : null;
  const markHandedOver = async lot => { setBusyId(lot.id); try { await lotService.markHandedOver(lot.id); await refreshLots(); } finally { setBusyId(null); } };
  const confirmReceipt = async lot => { setBusyId(lot.id); try { await lotService.confirmHandover(lot.id, finalWeight ? Number(finalWeight) : null); setFinalWeight(""); await refreshLots(); } finally { setBusyId(null); } };
  if (status === "loading") return <section className="content"><div className="empty panel"><span>{hindi ? "आपके लॉट लोड हो रहे हैं..." : "Loading your lots..."}</span></div></section>;
  if (status === "error") return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "लॉट लोड नहीं हो सके। कृपया फिर कोशिश करें।" : "Unable to load lots. Please try again."}</span></div></section>;
  return <section className="content"><div className="panel"><div className="section-head"><div><span className="pill">{hindi ? "ट्रेसबिलिटी" : "TRACEABILITY"}</span><h2>{hindi ? "डिजिटल लॉट इतिहास" : "Digital lot history"}</h2><p>{hindi ? "हर लॉट का वास्तविक रिकॉर्ड यहां दिखता है।" : "Your server-backed digital lot records appear here."}</p></div><QrCode className="qr" size={27}/></div>{lots.length === 0 ? <div className="empty"><Package size={23}/><span>{hindi ? "अभी कोई लॉट नहीं बना। अपना पहला डिजिटल लॉट बनाएं।" : "No lots created yet. Create your first digital lot."}</span></div> : <div className="timeline">{lots.map(l => { const spoken = `Lot ${l.lot_reference}, ${l.material_name}, ${l.approximate_weight} kilograms, ${l.status}.`; const handedAt = formatTime(l.handed_over_at); const confirmedAt = formatTime(l.recycler_confirmed_at); return <div className="lot-card" key={l.id}><div className="lot-main"><div className="lot-icon"><Package size={20}/></div><div><b>{l.lot_reference}</b><span>{l.material_name} • {l.approximate_weight} kg • {new Date(l.created_at).toLocaleString("en-IN")}</span><small><MapPin size={12}/> {l.recycler_id ? `Recycler #${l.recycler_id}` : (hindi ? "रीसाइकलर चयनित नहीं" : "Recycler not selected")}</small></div></div><div className="lot-actions"><strong>{lotValue(l)}</strong><span className="status">{statusText(l.status)}</span>{handedAt && <small className="handover-time"><Truck size={12}/> {hindi ? "हैंडओवर" : "Handover"}: {handedAt}</small>}{confirmedAt && <small className="handover-time"><CheckCircle2 size={12}/> {hindi ? "पुष्टि" : "Confirmed"}: {confirmedAt}</small>}{isCollector && l.status === "READY_FOR_HANDOVER" && <button className="outline tiny handover-btn" type="button" disabled={busyId === l.id} onClick={() => markHandedOver(l)}><Truck size={14}/> {hindi ? "हैंडओवर करें" : "Mark handed over"}</button>}{isRecycler && l.status === "HANDED_OVER" && <div className="handover-confirm"><input type="number" min="0.01" step="0.01" value={finalWeight} placeholder={hindi ? "वजन (किलो)" : "final kg"} onChange={event => setFinalWeight(event.target.value)} /><button className="outline tiny handover-btn" type="button" disabled={busyId === l.id} onClick={() => confirmReceipt(l)}><CheckCircle2 size={14}/> {hindi ? "रसीद पुष्टि" : "Confirm receipt"}</button></div>}<SpokenButton text={spoken} hindi={hindi}/></div></div>; })}</div>}<button className="primary" onClick={() => setActive("scan")}><Package size={17}/> {hindi ? "नया लॉट बनाएं" : "Create a new lot"}</button></div></section>;
}

function Transactions({lots,transactions,status,refreshLedger,hindi}) {
  const [amount, setAmount] = useState("");
  const [paymentMode, setPaymentMode] = useState("CASH");
  const [paymentReference, setPaymentReference] = useState("");
  const [busy, setBusy] = useState(false);
  const eligibleLot = lots.find(lot => lot.status === "READY_FOR_HANDOVER" || lot.status === "HANDED_OVER");
  const role = (() => { try { return JSON.parse(localStorage.getItem("janvoice_user") || "null")?.role || ""; } catch { return ""; } })();
  const createTransaction = async event => {
    event.preventDefault();
    if (!eligibleLot || !Number(amount) || Number(amount) <= 0) return;
    setBusy(true);
    try { await transactionService.create({ lotId: eligibleLot.id, amount: Number(amount), paymentMode, paymentReference: paymentReference || null }); await refreshLedger(); setAmount(""); setPaymentReference(""); } catch { /* Error is represented by the unchanged ledger state. */ } finally { setBusy(false); }
  };
  const markPaid = async transaction => { setBusy(true); try { await transactionService.updateStatus(transaction.id, "PAID"); await refreshLedger(); } finally { setBusy(false); } };
  if (status === "loading") return <section className="content"><div className="empty panel"><span>{hindi ? "लेन-देन लोड हो रहे हैं..." : "Loading transactions..."}</span></div></section>;
  if (status === "error") return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "लेन-देन लोड नहीं हो सके। कृपया फिर कोशिश करें।" : "Unable to load transactions. Please try again."}</span></div></section>;
  return <section className="content"><div className="panel"><div className="section-head"><div><span className="pill">PAYMENT LEDGER</span><h2>{hindi ? "लेन-देन रिकॉर्ड करें" : "Record a transaction"}</h2><p>{hindi ? "केवल हैंडओवर के लिए तैयार लॉट पर भुगतान रिकॉर्ड करें।" : "Record payment only for a lot ready for handover."}</p></div><Banknote size={21}/></div>{eligibleLot ? <form className="form-row" onSubmit={createTransaction}><label>Lot<input value={eligibleLot.lot_reference} readOnly /></label><label>{hindi ? "राशि" : "Amount"}<input type="number" min="0.01" step="0.01" value={amount} onChange={event => setAmount(event.target.value)} required /></label><label>{hindi ? "भुगतान माध्यम" : "Payment mode"}<select value={paymentMode} onChange={event => setPaymentMode(event.target.value)}><option>CASH</option><option>UPI</option><option>BANK_TRANSFER</option><option>OTHER</option></select></label>{paymentMode !== "CASH" && <label>{hindi ? "भुगतान संदर्भ" : "Payment reference"}<input value={paymentReference} onChange={event => setPaymentReference(event.target.value)} /></label>}<button className="primary" disabled={busy}>{hindi ? "लंबित भुगतान रिकॉर्ड करें" : "Record pending payment"}</button></form> : <div className="empty"><span>{hindi ? "कोई लॉट हैंडओवर के लिए तैयार नहीं है।" : "No lot is ready for handover."}</span></div>}</div><div className="panel"><div className="section-head"><div><h3>{hindi ? "लेन-देन इतिहास" : "Transaction history"}</h3><p>{hindi ? "सत्यापन तक भुगतान लंबित रहता है।" : "Payments remain pending until explicitly confirmed."}</p></div></div>{transactions.length === 0 ? <div className="empty"><span>{hindi ? "अभी कोई लेन-देन नहीं।" : "No transactions yet."}</span></div> : transactions.map(transaction => <div className="lot-row" key={transaction.id}><div className="lot-icon"><Banknote size={18}/></div><div><b>{transaction.lot_reference}</b><span>{transaction.payment_mode} • {transaction.payment_reference || (hindi ? "संदर्भ उपलब्ध नहीं" : "No reference")}</span></div><div className="lot-right"><strong>₹{Number(transaction.amount).toLocaleString("en-IN")}</strong><small className={transaction.payment_status === "PAID" ? "done-text" : "pending-text"}>{transaction.payment_status}</small>{transaction.payment_status === "PENDING" && (role === "RECYCLER" || role === "VERIFIED_RECYCLER" || role === "ADMIN") && <button className="outline tiny" disabled={busy} onClick={() => markPaid(transaction)}>{transaction.payment_mode === "CASH" ? (hindi ? "नकद प्राप्ति की पुष्टि" : "Confirm cash received") : (hindi ? "भुगतान पुष्टि" : "Confirm payment")}</button>}</div></div>)}</div></section>;
}

function Earnings({earnings,status,transactions,hindi}) {
  if (status === "loading") return <section className="content"><div className="empty panel"><span>{hindi ? "कमाई लोड हो रही है..." : "Loading earnings..."}</span></div></section>;
  if (status === "error") return <section className="content"><div className="empty panel"><CircleAlert size={23}/><span>{hindi ? "कमाई लोड नहीं हो सकी। कृपया फिर कोशिश करें।" : "Unable to load earnings. Please try again."}</span></div></section>;
  const total = Number(earnings?.total_earnings || 0);
  const pending = Number(earnings?.pending_amount || 0);
  const kg = Number(earnings?.total_kg_collected || 0);
  const paid = Number(earnings?.completed_handovers_count || 0);
  const summaryText = hindi ? `कुल कमाई ${total} रुपये और ${kg} किलो है।` : `Total earnings are ${total} rupees and ${kg} kilograms.`;
  return <section className="content"><div className="earn-card"><div><span>{hindi ? "कुल दर्ज कमाई" : "Total recorded earnings"}</span><strong>₹{total.toLocaleString("en-IN")}</strong><small>{paid} {hindi ? "भुगतान किए गए हैंडओवर" : "paid handovers"} • {hindi ? "लंबित" : "Pending"} ₹{pending.toLocaleString("en-IN")}</small></div><div className="earn-icon"><WalletCards size={35}/></div><SpokenButton text={summaryText} hindi={hindi}/></div><div className="earn-grid"><div className="panel mini-stat"><span>{hindi ? "भुगतान किए गए हैंडओवर" : "Paid handovers"}</span><strong>{paid}</strong></div><div className="panel mini-stat"><span>{hindi ? "भुगतान किया गया वजन" : "Paid weight"}</span><strong>{kg.toFixed(1)} kg</strong></div><div className="panel mini-stat"><span>{hindi ? "लंबित राशि" : "Pending amount"}</span><strong>₹{pending.toLocaleString("en-IN")}</strong></div></div>{transactions.length === 0 ? <div className="empty panel"><span>{hindi ? "अभी कमाई नहीं है। पूरे भुगतान किए गए हैंडओवर यहां दिखेंगे।" : "No earnings yet. Completed paid handovers will appear here."}</span></div> : <div className="panel"><div className="section-head"><h3>{hindi ? "भुगतान रिकॉर्ड" : "Payment records"}</h3></div>{transactions.filter(transaction => transaction.payment_status === "PAID").map(transaction => <LotRow key={transaction.id} lot={{lot_reference: transaction.lot_reference, material_name: "", approximate_weight: "", estimated_value: transaction.amount, status: "PAID"}} />)}</div>}</section>;
}


function Stat({label,value,icon}) { return <div className="stat"><div className="stat-icon">{icon}</div><span>{label}</span><strong>{value}</strong></div>; }
function Action({onClick,icon,title,text}) { return <button className="action" onClick={onClick}>{icon}<div><b>{title}</b><span>{text}</span></div><ChevronRight/></button>; }
function Step({n,icon,title,text}) { return <div className="step"><div className="step-number">{n}</div><div className="step-icon">{icon}</div><b>{title}</b><span>{text}</span></div>; }
function LotRow({lot}) { return <div className="lot-row"><div className="lot-icon"><Package size={18}/></div><div><b>{lot.lot_reference || lot.id}</b><span>{lot.material_name || lot.material} • {lot.approximate_weight || lot.weight} kg</span></div><div className="lot-right"><strong>{lot.estimated_value == null ? "Not available" : `₹${Number(lot.estimated_value).toLocaleString("en-IN")}`}</strong><small className={lot.status === "PAID" ? "done-text" : "pending-text"}>{lot.status}</small></div></div>; }
function DashboardLotRow({lot,hindi}) { return <div className="lot-row"><div className="lot-icon"><Package size={18}/></div><div><b>{lot.lot_reference}</b><span>{lot.material_name} • {Number(lot.approximate_weight || 0).toFixed(1)} kg • {lot.created_at ? new Date(lot.created_at).toLocaleDateString("en-IN") : ""}</span></div><div className="lot-right"><strong>{lot.estimated_value == null ? (hindi ? "उपलब्ध नहीं" : "Not available") : `₹${Number(lot.estimated_value).toLocaleString("en-IN")}`}</strong><small className={lot.status === "PAID" ? "done-text" : "pending-text"}>{lot.status}</small></div></div>; }
function DashboardTxRow({tx,hindi}) { return <div className="lot-row"><div className="lot-icon"><Banknote size={18}/></div><div><b>{tx.lot_reference}</b><span>{tx.payment_mode} • ₹{Number(tx.amount || 0).toLocaleString("en-IN")} • {tx.created_at ? new Date(tx.created_at).toLocaleDateString("en-IN") : ""}</span></div><div className="lot-right"><small className={tx.payment_status === "PAID" ? "done-text" : "pending-text"}>{tx.payment_status}</small></div></div>; }
function EmptyState({text}) { return <div className="empty"><XCircle size={23}/><span>{text}</span></div>; }
