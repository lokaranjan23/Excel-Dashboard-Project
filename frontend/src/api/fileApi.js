import axiosClient from './axiosClient';

export const fileApi = {
  uploadFile: async (formData) => {
    // formData should contain 'file', 'categoryId', 'description'
    return axiosClient.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  getAllFiles: async (params) => {
    // params: page, size, sortBy, direction, categoryId, search
    return axiosClient.get('/files', { params });
  },

  getAllCategories: async () => {
    return axiosClient.get('/files/category');
  },

  getSheetNames: async (fileId) => {
    return axiosClient.get(`/excel/${fileId}/sheets`);
  },

  suggestCategory: async (fileName) => {
    return axiosClient.get(`/files/category/suggest`, { params: { fileName } });
  },

  getFileSuggestions: async (search) => {
    return axiosClient.get(`/files/suggestions`, { params: { search } });
  }
};
