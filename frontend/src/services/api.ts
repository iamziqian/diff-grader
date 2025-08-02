import axios, { AxiosProgressEvent } from 'axios';
import { 
  APIResponse, 
  GradingSession, 
  ComparisonResult, 
  Feedback, 
  UploadProgress 
} from '../types';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('authToken');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const apiService = {
  // File upload
  uploadFile: async (
    file: File, 
    type: 'student' | 'reference',
    onProgress?: (progress: UploadProgress) => void
  ): Promise<APIResponse<{ fileId: string }>> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    const response = await api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent: AxiosProgressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress: UploadProgress = {
            loaded: progressEvent.loaded,
            total: progressEvent.total,
            percentage: Math.round((progressEvent.loaded * 100) / progressEvent.total),
          };
          onProgress(progress);
        }
      },
    });

    return response.data;
  },

  // Create grading session
  createGradingSession: async (
    studentFileId: string, 
    referenceFileId: string
  ): Promise<APIResponse<GradingSession>> => {
    const response = await api.post('/grading-sessions', {
      studentFileId,
      referenceFileId,
    });
    return response.data;
  },

  // Get grading session
  getGradingSession: async (sessionId: string): Promise<APIResponse<GradingSession>> => {
    const response = await api.get(`/grading-sessions/${sessionId}`);
    return response.data;
  },

  // Get comparison result
  getComparison: async (sessionId: string): Promise<APIResponse<ComparisonResult>> => {
    const response = await api.get(`/grading-sessions/${sessionId}/comparison`);
    return response.data;
  },

  // Submit feedback
  submitFeedback: async (
    sessionId: string, 
    feedback: Omit<Feedback, 'id' | 'createdAt' | 'updatedAt'>
  ): Promise<APIResponse<Feedback>> => {
    const response = await api.post(`/grading-sessions/${sessionId}/feedback`, feedback);
    return response.data;
  },

  // Update feedback
  updateFeedback: async (
    sessionId: string, 
    feedbackId: string, 
    feedback: Partial<Feedback>
  ): Promise<APIResponse<Feedback>> => {
    const response = await api.put(`/grading-sessions/${sessionId}/feedback/${feedbackId}`, feedback);
    return response.data;
  },

  // Get all feedback for a session
  getFeedbacks: async (sessionId: string): Promise<APIResponse<Feedback[]>> => {
    const response = await api.get(`/grading-sessions/${sessionId}/feedback`);
    return response.data;
  },

  // Complete grading session
  completeGradingSession: async (
    sessionId: string, 
    overallScore: number, 
    finalComments: string
  ): Promise<APIResponse<GradingSession>> => {
    const response = await api.put(`/grading-sessions/${sessionId}/complete`, {
      overallScore,
      finalComments,
    });
    return response.data;
  },

  // Get all grading sessions
  getGradingSessions: async (): Promise<APIResponse<GradingSession[]>> => {
    const response = await api.get('/grading-sessions');
    return response.data;
  },

  // Delete grading session
  deleteGradingSession: async (sessionId: string): Promise<APIResponse<void>> => {
    const response = await api.delete(`/grading-sessions/${sessionId}`);
    return response.data;
  },
};

export default apiService; 