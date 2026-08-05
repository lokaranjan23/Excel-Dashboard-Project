import axios from 'axios';

const axiosClient = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.response.use(
  (response) => {
    // Backend wraps response in ApiResponse: { success, message, data, timestamp }
    return response.data;
  },
  (error) => {
    // Handle specific backend error responses if present
    const message = error.response?.data?.message || error.message || 'An unexpected error occurred';
    const err = new Error(message);
    err.status = error.response?.status;
    err.data = error.response?.data;
    return Promise.reject(err);
  }
);

export default axiosClient;
