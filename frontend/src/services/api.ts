import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
    headers: {
        'Content-Type': 'application/json',
    },
});

export const setAuthToken = (userId: number | null) => {
    if (userId) {
        api.defaults.headers.common['X-User-Id'] = userId.toString();
    } else {
        delete api.defaults.headers.common['X-User-Id'];
    }
};

export default api;