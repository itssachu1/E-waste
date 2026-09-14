import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

const api = axios.create({
    baseURL: API_BASE,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use((config) => {
    try {
        const user = JSON.parse(localStorage.getItem('janvoice_user') || 'null');
        if (user?.sessionToken) config.headers.Authorization = `Bearer ${user.sessionToken}`;
    } catch {
        // Requests remain unauthenticated when local session data is invalid.
    }
    return config;
});

// On any 401 (invalid/expired token), clear the stale session and redirect to login.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error?.response?.status === 401) {
            localStorage.removeItem('janvoice_user');
            // Redirect to root so the app re-evaluates auth state and shows login.
            if (window.location.pathname !== '/') {
                window.location.href = '/';
            } else {
                window.location.reload();
            }
        }
        return Promise.reject(error);
    }
);

/**
 * REST Endpoint clients. Mapping backend controllers.
 */
export const authService = {
    register: async (username, password, role, wardArea) => {
        const response = await api.post('/auth/register', { username, password, role, wardArea });
        return response.data;
    },
    login: async (username, password) => {
        const response = await api.post('/auth/login', { username, password });
        return response.data;
    },
};

export const materialService = {
    getAll: async (params = {}) => {
        const response = await api.get('/materials', { params });
        return response.data;
    },
    getById: async (id) => {
        const response = await api.get(`/materials/${id}`);
        return response.data;
    },
};

export const priceService = {
    getCurrent: async (params = {}) => {
        const response = await api.get('/prices', { params });
        return response.data;
    },
    getHistory: async (params = {}) => {
        const response = await api.get('/prices/history', { params });
        return response.data;
    },
    create: async (record) => {
        const response = await api.post('/prices', record);
        return response.data;
    },
    verify: async (id) => {
        const response = await api.patch(`/prices/${id}/verify`, {});
        return response.data;
    },
};

export const recyclerService = {
    match: async (materialId, location = 'Indore') => {
        const response = await api.get('/recyclers/match', { params: { material_id: materialId, location } });
        return response.data;
    },
    getAll: async (params = {}) => {
        const response = await api.get('/recyclers', { params });
        return response.data;
    },
    getById: async (id) => {
        const response = await api.get(`/recyclers/${id}`);
        return response.data;
    },
};

export const lotService = {
    create: async (lot) => {
        const response = await api.post('/lots', lot);
        return response.data;
    },
    getAll: async (params = {}) => {
        const response = await api.get('/lots', { params });
        return response.data;
    },
    getById: async (id) => {
        const response = await api.get(`/lots/${id}`);
        return response.data;
    },
    updateStatus: async (id, status) => {
        const response = await api.patch(`/lots/${id}/status`, { status });
        return response.data;
    },
    // Phase 8 handover flow: confirmation is recorded directly on the lot.
    markHandedOver: async (id) => {
        const response = await api.patch(`/lots/${id}/status`, { status: 'HANDED_OVER' });
        return response.data;
    },
    confirmHandover: async (id, finalWeight = null) => {
        const response = await api.patch(`/lots/${id}/status`, { status: 'RECYCLER_CONFIRMED', finalWeight });
        return response.data;
    },
    assignRecycler: async (id, recyclerId) => {
        const response = await api.patch(`/lots/${id}/recycler`, { recyclerId });
        return response.data;
    },
};

export const transactionService = {
    create: async (transaction) => {
        const response = await api.post('/transactions', transaction);
        return response.data;
    },
    getAll: async (params = {}) => {
        const response = await api.get('/transactions', { params });
        return response.data;
    },
    updateStatus: async (id, paymentStatus) => {
        const response = await api.patch(`/transactions/${id}/status`, { paymentStatus });
        return response.data;
    },
};

export const earningsService = {
    get: async () => {
        const response = await api.get('/earnings');
        return response.data;
    },
};

export const dashboardService = {
    // Aggregated collector stats, computed live from DB aggregates on the backend.
    summary: async () => {
        const response = await api.get('/dashboard/summary');
        return response.data;
    },
    recentLots: async (limit = 5) => {
        const response = await api.get('/dashboard/recent-lots', { params: { limit } });
        return response.data;
    },
    recentTransactions: async (limit = 5) => {
        const response = await api.get('/dashboard/recent-transactions', { params: { limit } });
        return response.data;
    },
};

export const scanService = {
    // AI suggests, collector confirms. Throws on failure — caller falls
    // back to manual material selection (never blocks lot creation).
    classify: async (file) => {
        const form = new FormData();
        form.append('image', file);
        const response = await api.post('/scan/classify', form, {
            headers: { 'Content-Type': 'multipart/form-data' },
            timeout: 60000,
        });
        return response.data;
    },
};

export default api;
