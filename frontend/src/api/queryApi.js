import axiosClient from './axiosClient';

export const queryApi = {
  querySheet: async ({ fileId, sheetName, requestPayload }) => {
    const payload = { ...requestPayload };
    if (!payload.sortColumn) {
      delete payload.sortColumn;
      delete payload.sortDirection;
    }
    return axiosClient.post('/query', payload, {
      params: { fileId, sheetName },
    });
  },

  getColumnMetadata: async ({ fileId, sheetName }) => {
    return axiosClient.get('/query/columns', {
      params: { fileId, sheetName },
    });
  },

  refreshSheet: async ({ fileId, sheetName }) => {
    return axiosClient.post('/query/refresh', null, {
      params: { fileId, sheetName },
    });
  }
};
