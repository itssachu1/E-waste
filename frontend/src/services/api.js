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

export const complaintService = {
    // Post a new citizen complaint
    submit: async (originalText, wardArea, citizenId) => {
        const response = await api.post('/complaints', { originalText, wardArea, citizenId });
        return response.data;
    },
    // Get all active master complaints in area
    getByWard: async (wardArea) => {
        const response = await api.get(`/complaints?wardArea=${encodeURIComponent(wardArea)}`);
        return response.data;
    },
    // Search complaints by text matching
    search: async (wardArea, query) => {
        const response = await api.get(`/complaints/search?wardArea=${encodeURIComponent(wardArea)}&query=${encodeURIComponent(query)}`);
        return response.data;
    },
    // Cast upvote
    upvote: async (complaintId, userId) => {
        const response = await api.post(`/complaints/${complaintId}/upvote?userId=${userId}`);
        return response.data;
    },
};

export const mpService = {
    // Fetch dashboard stats (counters, category maps, dynamic AI briefings)
    getStats: async (wardArea) => {
        const response = await api.get(`/mp/dashboard/stats?wardArea=${encodeURIComponent(wardArea)}`);
        return response.data;
    },
    // Update complaint status (PENDING -> IN_PROGRESS -> RESOLVED)
    updateStatus: async (complaintId, status) => {
        const response = await api.put(`/mp/complaints/${complaintId}/status?status=${status}`);
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

export default api;
