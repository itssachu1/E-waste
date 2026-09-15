import React, { useState } from "react";
import { ShieldCheck, MapPin, Package, Truck, AlertCircle, Building2, CheckCircle2 } from "lucide-react";

// Mirrors the backend RecyclerRequest phone validation (@Pattern on the DTO).
const PHONE_PATTERN = /^[+]?[0-9 ()-]{10,20}$/;

/**
 * RecyclerProfile — "My Profile" view.
 * - status "ready" → read-only display of the recycler's own profile (GET /api/recyclers/me).
 * - status "none"  → the account has no profile yet (fresh signup; the backend
 *   answers 404). Shows a first-time setup form that POSTs /api/recyclers — the
 *   profile is created as PENDING_VERIFICATION until an admin verifies it.
 * - status "error" → the profile could not be loaded (network/server problem).
 */
export default function RecyclerProfile({ profile, status, hindi, materials = [], onCreated }) {
  if (status === "loading") {
    return <section className="content"><div className="empty panel"><span>{hindi ? "प्रोफ़ाइल लोड हो रही है..." : "Loading profile..."}</span></div></section>;
  }
  if (status === "error") {
    return <section className="content"><div className="empty panel"><AlertCircle size={23} /><span>{hindi ? "प्रोफ़ाइल लोड नहीं हो सकी। कृपया प्रशासक से संपर्क करें।" : "Unable to load profile. Please contact an admin."}</span></div></section>;
  }
  if (status === "none") {
    return <ProfileSetup materials={materials} hindi={hindi} onCreated={onCreated} />;
  }
  if (!profile) {
    return <section className="content"><div className="empty panel"><AlertCircle size={23} /><span>{hindi ? "प्रोफ़ाइल लोड नहीं हो सकी। कृपया प्रशासक से संपर्क करें।" : "Unable to load profile. Please contact an admin."}</span></div></section>;
  }

  const statusLabel = {
    VERIFIED: hindi ? "सत्यापित" : "Verified",
    PENDING_VERIFICATION: hindi ? "सत्यापन लंबित" : "Pending verification",
    REJECTED: hindi ? "अस्वीकृत" : "Rejected",
    EXPIRED: hindi ? "समाप्त" : "Expired",
  };

  const statusClass = {
    VERIFIED: "badge-verified",
    PENDING_VERIFICATION: "badge-pending",
    REJECTED: "badge-rejected",
    EXPIRED: "badge-rejected",
  };

  return (
    <section className="content">
      <div className="panel">
        <div className="section-head">
          <div>
            <h2>{hindi ? "मेरी प्रोफ़ाइल" : "My Profile"}</h2>
            <p>{hindi ? "आपकी रीसाइकलर प्रोफ़ाइल विवरण" : "Your recycler profile details"}</p>
          </div>
          <span className={statusClass[profile.authorization_status] || "badge-pending"}>
            {statusLabel[profile.authorization_status] || profile.authorization_status}
          </span>
        </div>

        <div className="profile-grid">
          <div className="profile-field">
            <label>{hindi ? "व्यवसाय का नाम" : "Business name"}</label>
            <strong>{profile.business_name}</strong>
          </div>
          <div className="profile-field">
            <label>{hindi ? "संपर्क नाम" : "Contact name"}</label>
            <span>{profile.contact_name || "—"}</span>
          </div>
          <div className="profile-field">
            <label>{hindi ? "फ़ोन" : "Phone"}</label>
            <span>{profile.phone}</span>
          </div>
          <div className="profile-field">
            <label>{hindi ? "शहर" : "City"}</label>
            <span>{profile.city}</span>
          </div>
          <div className="profile-field">
            <label>{hindi ? "पता" : "Address"}</label>
            <span>{profile.address || "—"}</span>
          </div>
          <div className="profile-field">
            <label>{hindi ? "सेवा क्षेत्र" : "Service area"}</label>
            <span>{profile.service_area || "—"}</span>
          </div>
          <div className="profile-field">
            <label>{hindi ? "पिकअप उपलब्ध" : "Pickup available"}</label>
            <span>{profile.pickup_available ? (hindi ? "हाँ" : "Yes") : (hindi ? "नहीं" : "No")}</span>
          </div>
          <div className="profile-field">
            <label>{hindi ? "पिकअप त्रिज्या (km)" : "Pickup radius (km)"}</label>
            <span>{profile.pickup_radius != null ? `${profile.pickup_radius} km` : "—"}</span>
          </div>
          {profile.registration_number && (
            <div className="profile-field">
              <label>{hindi ? "पंजीकरण संख्या" : "Registration number"}</label>
              <span>{profile.registration_number}</span>
            </div>
          )}
          {profile.last_verified_at && (
            <div className="profile-field">
              <label>{hindi ? "अंतिम सत्यापन" : "Last verified"}</label>
              <span>{new Date(profile.last_verified_at).toLocaleDateString("en-IN")}</span>
            </div>
          )}
        </div>

        <div className="panel" style={{marginTop: "18px"}}>
          <div className="section-head"><div><h3>{hindi ? "स्वीकृत सामग्री" : "Accepted materials"}</h3></div></div>
          {profile.accepted_materials?.length > 0 ? (
            <div className="material-tags">
              {profile.accepted_materials.map(m => (
                <span key={m.id} className="material-tag">{m.common_name}</span>
              ))}
            </div>
          ) : (
            <div className="empty"><span>{hindi ? "कोई सामग्री चयनित नहीं है।" : "No materials selected."}</span></div>
          )}
        </div>
      </div>
    </section>
  );
}

/**
 * First-time setup form. The payload matches the backend RecyclerRequest DTO:
 * businessName / phone / city / acceptedMaterialIds are required (server-side
 * @NotBlank/@NotEmpty/@Pattern), everything else is optional. The created
 * profile starts as PENDING_VERIFICATION until an admin verifies it.
 */
function ProfileSetup({ materials, hindi, onCreated }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [contactName, setContactName] = useState("");
  const [phone, setPhone] = useState("");
  const [city, setCity] = useState("Indore");
  const [address, setAddress] = useState("");
  const [serviceArea, setServiceArea] = useState("");
  const [registrationNumber, setRegistrationNumber] = useState("");
  const [pickupAvailable, setPickupAvailable] = useState(true);
  const [pickupRadius, setPickupRadius] = useState("");
  const [accepted, setAccepted] = useState([]);

  const toggleMaterial = (id) => setAccepted(current => (current.includes(id) ? current.filter(value => value !== id) : [...current, id]));

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    if (!businessName.trim()) { setError(hindi ? "व्यवसाय का नाम आवश्यक है।" : "Business name is required."); return; }
    if (!PHONE_PATTERN.test(phone.trim())) { setError(hindi ? "10–20 अंकों का मान्य फ़ोन नंबर भरें।" : "Enter a valid phone number (10-20 digits)."); return; }
    if (!city.trim()) { setError(hindi ? "शहर आवश्यक है।" : "City is required."); return; }
    if (accepted.length === 0) { setError(hindi ? "कम से कम एक स्वीकृत सामग्री चुनें।" : "Select at least one accepted material."); return; }
    setBusy(true);
    try {
      await onCreated({
        businessName: businessName.trim(),
        contactName: contactName.trim() || null,
        phone: phone.trim(),
        address: address.trim() || null,
        city: city.trim(),
        acceptedMaterialIds: accepted,
        pickupAvailable,
        pickupRadius: pickupAvailable && pickupRadius ? Number(pickupRadius) : null,
        serviceArea: serviceArea.trim() || null,
        registrationNumber: registrationNumber.trim() || null,
      });
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="content">
      <div className="panel">
        <div className="section-head">
          <div>
            <h2>{hindi ? "रीसाइकलर प्रोफ़ाइल सेटअप" : "Set up your recycler profile"}</h2>
            <p>{hindi ? "अपनी सुविधा का विवरण भरें — व्यवस्थापक के सत्यापन के बाद प्रोफ़ाइल सक्रिय होगी।" : "Submit your facility details — an admin verifies before your profile goes live."}</p>
          </div>
          <Building2 className="qr" size={27} />
        </div>

        {error && <div className="inline-notice" style={{ color: "#c2410c" }}><AlertCircle size={15} /> <span>{error}</span></div>}

        <form className="form-row" onSubmit={submit}>
          <label>{hindi ? "व्यवसाय का नाम *" : "Business name *"}
            <input value={businessName} onChange={event => setBusinessName(event.target.value)} placeholder={hindi ? "उदा. ग्रीन रीसाइकल प्राइवेट लिमिटेड" : "e.g. Green Recycle Pvt Ltd"} maxLength={120} />
          </label>
          <label>{hindi ? "संपर्क नाम" : "Contact name"}
            <input value={contactName} onChange={event => setContactName(event.target.value)} maxLength={120} />
          </label>
          <label>{hindi ? "फ़ोन *" : "Phone *"}
            <input value={phone} onChange={event => setPhone(event.target.value)} placeholder="9876543210" inputMode="tel" maxLength={20} />
          </label>
          <label>{hindi ? "शहर *" : "City *"}
            <input value={city} onChange={event => setCity(event.target.value)} maxLength={80} />
          </label>
          <label>{hindi ? "पता" : "Address"}
            <input value={address} onChange={event => setAddress(event.target.value)} maxLength={200} />
          </label>
          <label>{hindi ? "सेवा क्षेत्र" : "Service area"}
            <input value={serviceArea} onChange={event => setServiceArea(event.target.value)} placeholder={hindi ? "उदा. इंदौर + 30 km" : "e.g. Indore + 30 km"} maxLength={120} />
          </label>
          <label>{hindi ? "पंजीकरण संख्या (SPCB/CPCB)" : "Registration number (SPCB/CPCB)"}
            <input value={registrationNumber} onChange={event => setRegistrationNumber(event.target.value)} maxLength={60} />
          </label>
          <label>{hindi ? "पिकअप उपलब्ध?" : "Pickup available?"}
            <select value={pickupAvailable ? "yes" : "no"} onChange={event => setPickupAvailable(event.target.value === "yes")}>
              <option value="yes">{hindi ? "हाँ" : "Yes"}</option>
              <option value="no">{hindi ? "नहीं" : "No"}</option>
            </select>
          </label>
          {pickupAvailable && (
            <label>{hindi ? "पिकअप त्रिज्या (km)" : "Pickup radius (km)"}
              <input type="number" min="1" step="1" value={pickupRadius} onChange={event => setPickupRadius(event.target.value)} placeholder="25" />
            </label>
          )}

          <div style={{ gridColumn: "1 / -1" }}>
            <label>{hindi ? "स्वीकृत सामग्री *" : "Accepted materials *"}</label>
            <div className="material-list">
              {materials.map(item => (
                <button type="button" key={item.id} className={accepted.includes(item.id) ? "material selected" : "material"} onClick={() => toggleMaterial(item.id)}>
                  <span>{item.commonName}</span>
                  {accepted.includes(item.id) && <CheckCircle2 size={16} />}
                </button>
              ))}
              {materials.length === 0 && <div className="empty"><span>{hindi ? "सामग्री सूची लोड नहीं हुई।" : "Material list unavailable."}</span></div>}
            </div>
          </div>

          <button className="primary" disabled={busy}>
            <ShieldCheck size={17} /> {busy ? (hindi ? "भेजा जा रहा है..." : "Submitting...") : (hindi ? "सत्यापन के लिए भेजें" : "Submit for verification")}
          </button>
        </form>
      </div>
    </section>
  );
}
