import React, { useState } from 'react';
import { Box, Typography, Button, Paper, TextField, MenuItem, CircularProgress, Autocomplete, Snackbar, Alert } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useGetCategories, useUploadFile, useSuggestCategory } from '../hooks/useFiles';

const UploadPage = () => {
  const navigate = useNavigate();
  const [file, setFile] = useState(null);
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState('');
  const [snackbar, setSnackbar] = useState({ open: false, message: '' });

  const { data: categoriesData } = useGetCategories();
  const categories = categoriesData?.data || [];
  
  const uploadMutation = useUploadFile();
  const suggestMutation = useSuggestCategory();

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const selectedFile = e.target.files[0];
      setFile(selectedFile);
      setError('');
      
      suggestMutation.mutate(selectedFile.name, {
        onSuccess: (res) => {
          if (res?.data?.id) {
            setCategoryId(res.data.id);
          } else {
            setCategoryId('');
          }
        },
        onError: () => {
          setSnackbar({ open: true, message: 'Unable to suggest category.' });
        }
      });
    } else {
      setFile(null);
      setCategoryId('');
    }
  };

  const handleUpload = () => {
    if (!file) {
      setError('Please select a file.');
      return;
    }
    if (!categoryId) {
      setError('Please select a category.');
      return;
    }
    if (!description) {
      setError('Please enter a description.');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('categoryId', categoryId);
    formData.append('description', description);

    uploadMutation.mutate(formData, {
      onSuccess: () => {
        // Could show a snackbar here
        navigate('/');
      },
      onError: (err) => {
        if (err.status === 409) {
          setError('');
          setSnackbar({ open: true, message: err.message });
        } else {
          setError(err.message || 'Upload failed');
        }
      }
    });
  };

  return (
    <Box sx={{ maxWidth: 600, mx: 'auto', mt: 4 }}>
      <Paper sx={{ p: 4, display: 'flex', flexDirection: 'column', gap: 3 }}>
        <Typography variant="h5" fontWeight={600}>
          Upload Excel File
        </Typography>
        
        {error && (
          <Typography color="error" variant="body2">
            {error}
          </Typography>
        )}

        <Box sx={{ border: '1px dashed', borderColor: 'divider', borderRadius: 1, p: 3, textAlign: 'center' }}>
          <input
            accept=".xlsx, .xls"
            style={{ display: 'none' }}
            id="excel-upload-file"
            type="file"
            onChange={handleFileChange}
          />
          <label htmlFor="excel-upload-file">
            <Button variant="outlined" component="span">
              Choose File
            </Button>
          </label>
          {file && (
            <Typography variant="body2" sx={{ mt: 2 }}>
              Selected: {file.name}
            </Typography>
          )}
        </Box>

        <Autocomplete
          options={categories}
          getOptionLabel={(option) => option.name || ''}
          isOptionEqualToValue={(option, value) => option.id === value?.id}
          value={categories.find(c => c.id === categoryId) || null}
          onChange={(event, newValue) => {
            setCategoryId(newValue ? newValue.id : '');
          }}
          fullWidth
          size="small"
          renderInput={(params) => {
            const { componentsProps, InputProps, ...restParams } = params;
            return (
              <TextField 
                {...restParams} 
                label="Category" 
                InputProps={{
                  ...(InputProps || {}),
                  endAdornment: (
                    <React.Fragment>
                      {suggestMutation?.isPending ? <CircularProgress color="inherit" size={20} /> : null}
                      {InputProps?.endAdornment || null}
                    </React.Fragment>
                  ),
                }}
              />
            );
          }}
        />

        <TextField
          label="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          multiline
          rows={3}
          fullWidth
          size="small"
        />

        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 2 }}>
          <Button 
            variant="text" 
            color="secondary" 
            onClick={() => navigate(-1)}
            disabled={uploadMutation.isPending}
          >
            Back
          </Button>
          <Button 
            variant="contained" 
            color="primary" 
            onClick={handleUpload}
            disabled={!file || !categoryId || uploadMutation.isPending}
            disableElevation
          >
            {uploadMutation.isPending ? <CircularProgress size={24} color="inherit" /> : 'Upload'}
          </Button>
        </Box>
      </Paper>
      
      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={4000} 
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity="error" sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default UploadPage;
