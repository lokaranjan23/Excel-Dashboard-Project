import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fileApi } from '../api/fileApi';

export const useGetFiles = (params) => {
  return useQuery({
    queryKey: ['files', params],
    queryFn: () => fileApi.getAllFiles(params),
    keepPreviousData: true,
  });
};

export const useGetCategories = () => {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => fileApi.getAllCategories(),
    staleTime: 5 * 60 * 1000, // cache for 5 minutes
  });
};

export const useGetSheetNames = (fileId) => {
  return useQuery({
    queryKey: ['sheets', fileId],
    queryFn: () => fileApi.getSheetNames(fileId),
    enabled: !!fileId,
  });
};

export const useUploadFile = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (formData) => fileApi.uploadFile(formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['files'] });
    },
  });
};

export const useSuggestCategory = () => {
  return useMutation({
    mutationFn: (fileName) => fileApi.suggestCategory(fileName),
  });
};

export const useGetFileSuggestions = (search) => {
  return useQuery({
    queryKey: ['fileSuggestions', search],
    queryFn: () => fileApi.getFileSuggestions(search),
    enabled: !!search && search.trim().length > 0,
    keepPreviousData: true,
  });
};
