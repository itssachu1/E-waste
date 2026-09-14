import React from "react";
import { ShieldCheck, MapPin, Package, Truck, AlertCircle } from "lucide-react";

/**
 * RecyclerProfile — read-only display of the recycler's own profile.
 * Pulled from GET /api/recyclers/me (business name, accepted materials,
 * verification status, service area, etc.)
 */
export default function RecyclerProfile({ profile, status, hindi }) {
  if (status === "loading") {
    return <section className="content"><div className="empty panel"><span>{hindi ? "प्रोफ़ाइल लोड हो रही है..." : "Loading profile..."}</span></div></section>;
  }
  if (status === "error" || !profile) {
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
