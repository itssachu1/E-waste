import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.jsx'
import './index.css'
import { AuthProvider } from './context/AuthContext.jsx'

// PWA Install Prompt Handling
let deferredPrompt = null;
const installPromptElement = document.createElement('div');
installPromptElement.id = 'pwa-install-prompt';
installPromptElement.style.cssText = `
  position: fixed;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  background: #168448;
  color: white;
  padding: 14px 20px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(22, 132, 72, 0.4);
  display: none;
  align-items: center;
  gap: 12px;
  z-index: 9999;
  font-family: Inter, system-ui, sans-serif;
  font-size: 14px;
  max-width: 90vw;
  backdrop-filter: blur(8px);
`;
installPromptElement.innerHTML = `
  <span style="flex:1;font-weight:500">Install E-Waste Saathi for a better experience</span>
  <button id="pwa-install-btn" style="background:white;color:#168448;border:none;padding:8px 16px;border-radius:8px;font-weight:600;cursor:pointer;font-size:13px;white-space:nowrap">Install</button>
  <button id="pwa-dismiss-btn" style="background:transparent;border:none;color:rgba(255,255,255,0.8);cursor:pointer;padding:8px;font-size:18px">×</button>
`;
document.body.appendChild(installPromptElement);

window.addEventListener('beforeinstallprompt', (event) => {
  event.preventDefault();
  deferredPrompt = event;
  installPromptElement.style.display = 'flex';
});

document.getElementById('pwa-install-btn')?.addEventListener('click', async () => {
  if (!deferredPrompt) return;
  deferredPrompt.prompt();
  const { outcome } = await deferredPrompt.userChoice;
  if (outcome === 'accepted') {
    installPromptElement.style.display = 'none';
  }
  deferredPrompt = null;
});

document.getElementById('pwa-dismiss-btn')?.addEventListener('click', () => {
  installPromptElement.style.display = 'none';
});

// Listen for app installed event
window.addEventListener('appinstalled', () => {
  installPromptElement.style.display = 'none';
  deferredPrompt = null;
});

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>,
)
