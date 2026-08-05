import { useQuery, useMutation, keepPreviousData } from '@tanstack/react-query';
import { queryApi } from '../api/queryApi';

export const useQuerySheet = (fileId, sheetName, requestPayload) => {
  return useQuery({
    queryKey: ['querySheet', fileId, sheetName, requestPayload],
    queryFn: () => queryApi.querySheet({ fileId, sheetName, requestPayload }),
    enabled: !!fileId && !!sheetName && !!requestPayload,
    placeholderData: keepPreviousData,
  });
};

export const useGetColumns = (fileId, sheetName) => {
  return useQuery({
    queryKey: ['columns', fileId, sheetName],
    queryFn: () => queryApi.getColumnMetadata({ fileId, sheetName }),
    enabled: !!fileId && !!sheetName,
  });
};

export const useRefreshSheet = () => {
  return useMutation({
    mutationFn: ({ fileId, sheetName }) => queryApi.refreshSheet({ fileId, sheetName }),
  });
};
