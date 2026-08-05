import React, { useState, useEffect } from 'react';
import { Box, Typography, Button, Paper, TextField, MenuItem, Snackbar, Alert, Chip, CircularProgress, Autocomplete } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { DataGrid } from '@mui/x-data-grid';
import { useNavigate } from 'react-router-dom';
import { useGetFiles, useGetCategories, useGetSheetNames, useGetFileSuggestions } from '../hooks/useFiles';
import { useQuerySheet, useGetColumns, useRefreshSheet } from '../hooks/useQuerySheet';
import { useQueryClient } from '@tanstack/react-query';
import FilterDialog from '../components/FilterDialog';
import dayjs from 'dayjs';

const formatUploadDate = (dateString) => {
  if (!dateString) return 'Unknown Date';
  return dayjs(dateString).format('DD MMM YYYY • hh:mm A');
};

const DashboardPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // State for selections
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [debouncedSearchInput, setDebouncedSearchInput] = useState('');
  const [suggestedFileId, setSuggestedFileId] = useState('');
  const [autocompleteOpen, setAutocompleteOpen] = useState(false);
  const [fileSearch, setFileSearch] = useState('');
  const [selectedFileId, setSelectedFileId] = useState('');
  const [selectedSheet, setSelectedSheet] = useState('');
  
  const [dashboardFile, setDashboardFile] = useState(null);
  const [dashboardSheet, setDashboardSheet] = useState('');
  
  const sheetAutocompleteRef = React.useRef(null);
  const shouldFocusSheetRef = React.useRef(false);
  const [sheetDropdownOpen, setSheetDropdownOpen] = useState(false);
  
  const fileAutocompleteRef = React.useRef(null);
  const shouldProcessCategoryChangeRef = React.useRef(false);
  
  // Dialog state
  const [filterDialogOpen, setFilterDialogOpen] = useState(false);
  
  // Snackbar state
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  // Query state
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [queryRequest, setQueryRequest] = useState({
    page: 0,
    size: 50,
    filters: [],
    rangeFilters: [],
    sortColumn: null,
    sortDirection: null
  });

  // Queries
  const { data: categoriesData } = useGetCategories();
  const categories = categoriesData?.data || [];

  const { data: filesData, isLoading: isLoadingFiles } = useGetFiles({
    page: 0,
    size: 100, // fetch up to 100 files for the dropdown
    categoryId: selectedCategoryId || undefined,
    search: fileSearch || undefined,
  });
  const files = filesData?.data?.content || [];

  const { data: sheetsData, isLoading: isLoadingSheets } = useGetSheetNames(selectedFileId);
  const sheets = sheetsData?.data || [];

  const { data: columnsData } = useGetColumns(dashboardFile?.id || '', dashboardSheet);
  const columnsMeta = columnsData?.data || [];

  const { data: sheetData, isLoading: isLoadingQuery, isFetching: isFetchingQuery, isError: isQueryError, error: queryError } = useQuerySheet(
    dashboardFile?.id || '', 
    dashboardSheet, 
    queryRequest
  );

  const refreshMutation = useRefreshSheet();

  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedSearchInput(searchInput);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchInput]);

  const { data: suggestionsData, isLoading: isLoadingSuggestions } = useGetFileSuggestions(debouncedSearchInput);
  const suggestions = (suggestionsData?.data || []).slice(0, 10).map(suggestion => {
    const fullFile = files.find(f => f.id === suggestion.id);
    return fullFile ? { ...suggestion, ...fullFile } : suggestion;
  });

  const autocompleteOptions = debouncedSearchInput.trim().length > 0 ? suggestions : files;

  const handleResetSelection = () => {
    setSearchInput('');
    setFileSearch('');
    setSelectedCategoryId('');
    setSelectedFileId('');
    setSelectedSheet('');
    setDashboardFile(null);
    setDashboardSheet('');
    setPage(0);
    setQueryRequest(prev => ({
      ...prev,
      page: 0,
      filters: [],
      rangeFilters: [],
      sortColumn: null,
      sortDirection: null
    }));
  };

  useEffect(() => {
    if (shouldProcessCategoryChangeRef.current && !isLoadingFiles && filesData) {
      if (files.length === 0) {
        setAutocompleteOpen(true);
        const input = fileAutocompleteRef.current?.querySelector('input');
        if (input) input.focus();
        shouldProcessCategoryChangeRef.current = false;
      } else if (files.length === 1) {
        const file = files[0];
        setSearchInput(file.originalName);
        setSuggestedFileId(file.id);
        setFileSearch(file.originalName);
        
        shouldFocusSheetRef.current = true;
        setSelectedFileId(file.id);
        setSelectedSheet('');
        
        setDashboardFile(file);
        setDashboardSheet('');
        
        setPage(0);
        setQueryRequest(prev => ({
          ...prev,
          page: 0,
          filters: [],
          rangeFilters: [],
          sortColumn: null,
          sortDirection: null
        }));
        shouldProcessCategoryChangeRef.current = false;
      } else if (files.length > 1) {
        setAutocompleteOpen(true);
        const input = fileAutocompleteRef.current?.querySelector('input');
        if (input) input.focus();
        shouldProcessCategoryChangeRef.current = false;
      }
    }
  }, [filesData, files, isLoadingFiles]);

  useEffect(() => {
    if (filesData && files) {
      if (files.length === 1 && fileSearch !== '') {
        if (selectedFileId !== files[0].id) {
          setSelectedFileId(files[0].id);
          setSelectedSheet('');
          
          setDashboardFile(files[0]);
          setDashboardSheet('');
          
          setPage(0);
          setQueryRequest(prev => ({
            ...prev,
            page: 0,
            filters: [],
            rangeFilters: [],
            sortColumn: null,
            sortDirection: null
          }));
        }
      } else if (files.length === 0 && fileSearch !== '') {
        if (selectedFileId !== '') {
          setSelectedFileId('');
          setSelectedSheet('');
          setPage(0);
        }
      }
    }
  }, [filesData, files, fileSearch]);

  useEffect(() => {
    if (sheetsData && sheets && selectedFileId) {
      if (sheets.length === 1) {
        if (selectedSheet !== sheets[0]) {
          setSelectedSheet(sheets[0]);
          setDashboardSheet(sheets[0]);
          setPage(0);
          setQueryRequest(prev => ({
            ...prev,
            page: 0,
            filters: [],
            rangeFilters: [],
            sortColumn: null,
            sortDirection: null
          }));
        }
      } else if (sheets.length > 1) {
        if (shouldFocusSheetRef.current) {
          setSheetDropdownOpen(true);
          const input = sheetAutocompleteRef.current?.querySelector('input');
          if (input) input.focus();
          shouldFocusSheetRef.current = false;
        }
      }
    }
  }, [sheetsData, sheets, selectedFileId]);

  const handleRefresh = () => {
    if (!dashboardFile || !dashboardSheet) {
      setSnackbar({ open: true, message: 'Please select a file and sheet first.', severity: 'warning' });
      return;
    }
    
    refreshMutation.mutate({ fileId: dashboardFile.id, sheetName: dashboardSheet }, {
      onSuccess: () => {
        setPage(0);
        setQueryRequest(prev => ({ ...prev, page: 0 }));
        
        queryClient.invalidateQueries({ queryKey: ['columns', dashboardFile.id, dashboardSheet] });
        queryClient.invalidateQueries({ queryKey: ['querySheet', dashboardFile.id, dashboardSheet] });
        
        setSnackbar({ open: true, message: 'Sheet refreshed successfully.', severity: 'success' });
      },
      onError: () => {
        setSnackbar({ open: true, message: 'Failed to refresh sheet', severity: 'error' });
      }
    });
  };

  const handleApplyFilters = (newFilters) => {
    setQueryRequest(prev => ({
      ...prev,
      ...newFilters,
      page: 0 // reset to first page on new filter
    }));
    setPage(0);
  };

  // Convert metadata to DataGrid columns
  const columns = columnsMeta.map(col => ({
    field: col.columnName,
    headerName: col.columnName,
    width: 150,
    sortable: false, // Handled server-side via dialog
  }));

  const rows = sheetData?.data?.content?.map((row, index) => ({ id: index, ...row })) || [];
  const rowCount = sheetData?.data?.totalElements || 0;

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
      {/* Top Header Section */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h5" color="text.primary" fontWeight={600}>
          Data Explorer
        </Typography>
        <Button 
          variant="contained" 
          color="primary" 
          onClick={() => navigate('/upload')}
          disableElevation
        >
          Upload Excel File
        </Button>
      </Box>

      {/* Selection Card */}
      <Paper sx={{ p: 3, display: 'flex', flexDirection: 'column', gap: 3 }}>
        {/* ROW 1: Search and Category */}
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <Autocomplete
            ref={fileAutocompleteRef}
            freeSolo
            open={autocompleteOpen}
            onOpen={() => setAutocompleteOpen(true)}
            onClose={() => setAutocompleteOpen(false)}
            options={autocompleteOptions}
            getOptionLabel={(option) => {
              if (typeof option === 'string') return option;
              return option.originalName || '';
            }}
            inputValue={searchInput}
            onInputChange={(event, newInputValue, reason) => {
              setSearchInput(newInputValue);
              if (reason === 'input') {
                setSuggestedFileId('');
              }
              if (newInputValue.trim() === '') {
                setAutocompleteOpen(false);
                setSuggestedFileId('');
              }
            }}
            onChange={(event, newValue) => {
              if (newValue && typeof newValue === 'object') {
                setSearchInput(newValue.originalName);
                setSuggestedFileId(newValue.id);
                setFileSearch(newValue.originalName);
                
                if (selectedFileId === newValue.id) {
                  if (sheets.length > 1) {
                    setSheetDropdownOpen(true);
                    const input = sheetAutocompleteRef.current?.querySelector('input');
                    if (input) input.focus();
                  }
                } else {
                  shouldFocusSheetRef.current = true;
                }
                
                // Immediately select file to trigger sheet loading
                setSelectedFileId(newValue.id);
                setSelectedSheet('');
                
                setDashboardFile(newValue);
                setDashboardSheet('');
                
                setPage(0);
                setQueryRequest(prev => ({
                  ...prev,
                  page: 0,
                  filters: [],
                  rangeFilters: [],
                  sortColumn: null,
                  sortDirection: null
                }));
              } else {
                setSearchInput(newValue || '');
                setSuggestedFileId('');
                setFileSearch(newValue || '');
              }
            }}
            renderOption={(props, option, { index }) => {
              const { key, ...optionProps } = props;
              const matchIndex = option.originalName.toLowerCase().indexOf(debouncedSearchInput.toLowerCase());
              let highlightedName;
              if (matchIndex !== -1 && debouncedSearchInput.length > 0) {
                const before = option.originalName.substring(0, matchIndex);
                const match = option.originalName.substring(matchIndex, matchIndex + debouncedSearchInput.length);
                const after = option.originalName.substring(matchIndex + debouncedSearchInput.length);
                highlightedName = (
                  <span>
                    {before}
                    <span style={{ fontWeight: 'bold', color: '#E65100' }}>{match}</span>
                    {after}
                  </span>
                );
              } else {
                highlightedName = option.originalName;
              }
              return (
                <li key={option.id} {...optionProps} style={{ ...optionProps.style, display: 'block', padding: '12px 16px', borderBottom: index < autocompleteOptions.length - 1 ? '1px solid #eee' : 'none' }}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', mb: 0.5 }}>
                    <Typography variant="subtitle1" fontWeight={600}>
                      {highlightedName}
                    </Typography>
                    <Chip 
                      label={option.categoryName || 'No Category'} 
                      size="small" 
                      sx={{ bgcolor: '#FFF3E0', color: '#E65100', fontWeight: 600, height: 20 }}
                    />
                  </Box>
                  <Typography 
                    variant="body2" 
                    color="text.secondary" 
                    sx={{ 
                      mb: 1, 
                      display: '-webkit-box', 
                      WebkitLineClamp: 2, 
                      WebkitBoxOrient: 'vertical', 
                      overflow: 'hidden',
                      whiteSpace: 'normal'
                    }}
                  >
                    {option.description || "No description available."}
                  </Typography>
                  <Typography variant="caption" color="text.disabled">
                    {formatUploadDate(option.createdAt || option.uploadedAt || option.uploadDate)}
                  </Typography>
                </li>
              );
            }}
            loading={isLoadingSuggestions && debouncedSearchInput.trim().length > 0}
            renderInput={(params) => (
              <TextField
                {...params}
                label="File"
                placeholder="Search files..."
                size="small"
              />
            )}
            sx={{ flexGrow: 2, minWidth: { xs: '100%', sm: 300 } }}
            componentsProps={{
              paper: {
                sx: { 
                  minWidth: { xs: '100%', sm: 500, md: 600 },
                  maxWidth: '90vw'
                }
              }
            }}
            noOptionsText={selectedCategoryId && files.length === 0 ? "No files available for the selected category." : "No matching files found."}
          />
          <Button 
            variant="outlined" 
            color="secondary" 
            onClick={handleResetSelection}
            disableElevation
            sx={{ height: 40 }}
          >
            Reset
          </Button>

          <Box sx={{ flexGrow: { xs: 0, md: 1 } }} />

          <Autocomplete
            options={categories}
            getOptionLabel={(option) => option.name || ''}
            isOptionEqualToValue={(option, value) => option.id === value?.id}
            value={categories.find(c => c.id === selectedCategoryId) || null}
            onChange={(event, newValue) => {
              setSelectedCategoryId(newValue ? newValue.id : '');
              setSelectedFileId('');
              setSelectedSheet('');
              setSearchInput('');
              setSuggestedFileId('');
              setFileSearch('');
              shouldProcessCategoryChangeRef.current = true;
            }}
            size="small"
            sx={{ minWidth: 200, width: { xs: '100%', sm: 250 } }}
            renderInput={(params) => (
              <TextField 
                {...params} 
                label="Category (Optional)" 
                placeholder="All Categories"
              />
            )}
          />
        </Box>
        
        {fileSearch && files.length === 0 && (
          <Typography color="error" variant="body2">
            No matching files found.
          </Typography>
        )}

        {/* ROW 2: Sheet, Refresh, Filters */}
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>

          <Autocomplete
            ref={sheetAutocompleteRef}
            options={sheets}
            open={sheetDropdownOpen}
            onOpen={() => setSheetDropdownOpen(true)}
            onClose={() => setSheetDropdownOpen(false)}
            getOptionLabel={(option) => option || ''}
            isOptionEqualToValue={(option, value) => option === value}
            value={selectedSheet || null}
            onChange={(event, newValue) => {
              setSelectedSheet(newValue || '');
              setDashboardSheet(newValue || '');
              setPage(0);
              setQueryRequest({
                page: 0,
                size: pageSize,
                filters: [],
                rangeFilters: [],
                sortColumn: null,
                sortDirection: null
              });
              setFilterDialogOpen(false);
            }}
            size="small"
            sx={{ minWidth: 200, flexGrow: 1, width: { xs: '100%', sm: '25%' } }}
            disabled={!selectedFileId || isLoadingSheets}
            renderInput={(params) => (
              <TextField 
                {...params} 
                label="Select Sheet" 
                placeholder="Select a sheet"
              />
            )}
          />
          
          <Box sx={{ flexGrow: { xs: 0, md: 1 } }} />
          
          <Button 
            variant="outlined" 
            color="secondary"
            onClick={handleRefresh}
            disabled={!dashboardFile || !dashboardSheet || refreshMutation.isPending}
            startIcon={refreshMutation.isPending ? <CircularProgress size={20} color="inherit" /> : null}
            sx={{ height: 40 }}
          >
            {refreshMutation.isPending ? 'Refreshing...' : 'Refresh'}
          </Button>
          
          <Button 
            variant="outlined" 
            color="primary"
            onClick={() => setFilterDialogOpen(true)}
            disabled={!dashboardFile || !dashboardSheet}
            sx={{ height: 40 }}
          >
            Filters
          </Button>
        </Box>

        {/* File Information Card */}
        {dashboardFile && (
          <Box sx={{ mt: 2, p: 2, bgcolor: '#fafafa', borderRadius: 1, border: '1px solid #eee' }}>
            <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>File Information</Typography>
            <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 2fr 1fr', gap: 2 }}>
              <Box>
                <Typography variant="caption" color="text.secondary" display="block">Category</Typography>
                <Chip 
                  label={dashboardFile.categoryName || 'No Category'} 
                  size="small" 
                  sx={{ bgcolor: '#FFF3E0', color: '#E65100', fontWeight: 600, height: 20, mt: 0.5 }}
                />
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" display="block">Description</Typography>
                <Typography variant="body2">{dashboardFile.description || "No description available."}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary" display="block">Uploaded On</Typography>
                <Typography variant="body2">{formatUploadDate(dashboardFile.createdAt || dashboardFile.uploadedAt || dashboardFile.uploadDate)}</Typography>
              </Box>
            </Box>
          </Box>
        )}
      </Paper>

      {/* Data Grid Section */}
      <Paper sx={{ height: 600, width: '100%', display: 'flex', flexDirection: 'column' }}>
        {dashboardFile && dashboardSheet ? (
          <Box sx={{ p: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
            <Typography variant="body2" color="text.secondary">
              Showing: <strong>{dashboardFile.originalName}</strong> → <strong>{dashboardSheet}</strong>
            </Typography>
          </Box>
        ) : null}
        
        {isQueryError && (
          <Alert severity="error" sx={{ m: 2 }}>{queryError?.message || 'Failed to load data'}</Alert>
        )}

        <DataGrid
          rows={rows}
          columns={columns}
          rowCount={rowCount}
          loading={isLoadingQuery || isFetchingQuery}
          pageSizeOptions={[10, 25, 50, 100]}
          paginationModel={{ page, pageSize }}
          paginationMode="server"
          onPaginationModelChange={(model) => {
            setPage(model.page);
            setPageSize(model.pageSize);
            setQueryRequest(prev => ({
              ...prev,
              page: model.page,
              size: model.pageSize
            }));
          }}
          disableRowSelectionOnClick
          sx={{ border: 0, flex: 1 }}
        />
        
        {!isLoadingQuery && !isFetchingQuery && dashboardFile && dashboardSheet && rows.length === 0 && !isQueryError && (
          <Box sx={{ p: 2, display: 'flex', justifyContent: 'center' }}>
            <Typography color="text.secondary">No data available for this sheet.</Typography>
          </Box>
        )}
      </Paper>

      <FilterDialog 
        open={filterDialogOpen}
        onClose={() => setFilterDialogOpen(false)}
        onApply={handleApplyFilters}
        columnsMeta={columnsMeta}
        initialRequest={queryRequest}
      />
      
      <Snackbar 
        open={snackbar.open} 
        autoHideDuration={4000} 
        onClose={() => setSnackbar({ ...snackbar, open: false })}
      >
        <Alert severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default DashboardPage;
