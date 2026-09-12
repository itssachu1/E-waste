import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";

export default function ComplaintMap() {
  return (
    <MapContainer
      center={[22.7196, 75.8577]} // Indore
      zoom={13}
      style={{ height: "100%", width: "100%" }}
    >
      <TileLayer
        attribution='&copy; OpenStreetMap contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      <Marker position={[22.7196, 75.8577]}>
        <Popup>
          Water Leakage Complaint
        </Popup>
      </Marker>
    </MapContainer>
  );
}