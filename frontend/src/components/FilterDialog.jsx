import React, { useState, useEffect } from 'react';
import { 
  Dialog, DialogTitle, DialogContent, DialogActions, 
  Button, TextField, MenuItem, Box, Typography, IconButton, Divider, Autocomplete
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { DateTimePicker } from '@mui/x-date-pickers/DateTimePicker';
import dayjs from 'dayjs';

const ALLOWED_RANGE_TYPES = [
  'INTEGER', 'BIGINT', 'SMALLINT', 'TINYINT', 'DOUBLE', 
  'FLOAT', 'REAL', 'DECIMAL', 'NUMERIC', 'DATE', 'TIMESTAMP'
];

const FilterDialog = ({ open, onClose, onApply, columnsMeta, initialRequest }) => {
  
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState(null);
  
  const [filters, setFilters] = useState([]);
  const [rangeFilters, setRangeFilters] = useState([]);

  useEffect(() => {
    if (open) {
      setSortColumn(initialRequest?.sortColumn || null);
      setSortDirection(initialRequest?.sortDirection || null);
      setFilters(initialRequest?.filters || []);
      setRangeFilters(initialRequest?.rangeFilters || []);
    }
  }, [open, initialRequest]);

  const handleApply = () => {
    onApply({
      sortColumn,
      sortDirection,
      filters: filters.filter(f => f.column && f.value),
      rangeFilters: rangeFilters
        .filter(f => f.column && (f.minValue || f.maxValue))
        .map(f => ({
          ...f,
          minValue: f.minValue || null,
          maxValue: f.maxValue || null
        }))
    });
    onClose();
  };

  const handleReset = () => {
    setSortColumn(null);
    setSortDirection(null);
    setFilters([]);
    setRangeFilters([]);
  };

  const addFilter = () => setFilters([...filters, { column: '', value: '' }]);
  const removeFilter = (index) => setFilters(filters.filter((_, i) => i !== index));
  const updateFilter = (index, field, val) => {
    const newFilters = [...filters];
    newFilters[index] = { ...newFilters[index], [field]: val };
    setFilters(newFilters);
  };

  const addRangeFilter = () => setRangeFilters([...rangeFilters, { column: '', minValue: '', maxValue: '' }]);
  const removeRangeFilter = (index) => setRangeFilters(rangeFilters.filter((_, i) => i !== index));
  const updateRangeFilter = (index, field, val) => {
    const newFilters = [...rangeFilters];
    newFilters[index] = { ...newFilters[index], [field]: val };
    setRangeFilters(newFilters);
  };

  const handleRangeColumnChange = (index, newColumn) => {
    const newFilters = [...rangeFilters];
    newFilters[index] = { ...newFilters[index], column: newColumn, minValue: '', maxValue: '' };
    setRangeFilters(newFilters);
  };

  const handleExactColumnChange = (index, newColumn) => {
    const newFilters = [...filters];
    newFilters[index] = { ...newFilters[index], column: newColumn, value: '' };
    setFilters(newFilters);
  };

  const columnOptions = columnsMeta.map(c => c.columnName);
  const rangeColumnOptions = columnsMeta
    .filter(c => ALLOWED_RANGE_TYPES.includes(c.dataType))
    .map(c => c.columnName);

  const getColumnType = (colName) => {
    return columnsMeta.find(c => c.columnName === colName)?.dataType;
  };

  const isNumeric = (type) => ['INTEGER', 'BIGINT', 'SMALLINT', 'TINYINT', 'DOUBLE', 'FLOAT', 'REAL', 'DECIMAL', 'NUMERIC'].includes(type);
  const isDate = (type) => type === 'DATE';
  const isTimestamp = (type) => type === 'TIMESTAMP';

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ fontWeight: 600 }}>Advanced Filters</DialogTitle>
        <DialogContent dividers sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          
          {/* Sorting Section */}
          <Box>
            <Typography variant="subtitle2" sx={{ mb: 1, fontWeight: 600 }}>Sorting</Typography>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Autocomplete
                options={columnOptions}
                getOptionLabel={(option) => option}
                value={sortColumn || null}
                onChange={(event, newValue) => {
                  setSortColumn(newValue || null);
                  if (!newValue) setSortDirection(null);
                  else if (!sortDirection) setSortDirection('ASC');
                }}
                size="small"
                sx={{ flex: 1 }}
                renderInput={(params) => <TextField {...params} label="Sort Column" placeholder="None" />}
              />
              <Autocomplete
                options={['ASC', 'DESC']}
                getOptionLabel={(option) => option === 'ASC' ? 'Ascending' : 'Descending'}
                value={sortDirection || null}
                onChange={(event, newValue) => setSortDirection(newValue || null)}
                size="small"
                sx={{ flex: 1 }}
                disabled={!sortColumn}
                renderInput={(params) => <TextField {...params} label="Direction" placeholder="None" />}
              />
            </Box>
          </Box>

          <Divider />

          {/* Exact Filters */}
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>Exact Filters</Typography>
              <Button size="small" startIcon={<AddIcon />} onClick={addFilter}>Add Filter</Button>
            </Box>
            {filters.map((filter, index) => {
              const colType = getColumnType(filter.column);
              
              return (
                <Box key={index} sx={{ display: 'flex', gap: 2, mb: 1, alignItems: 'center' }}>
                  <Autocomplete
                    options={columnOptions}
                    getOptionLabel={(option) => option}
                    value={filter.column || null}
                    onChange={(event, newValue) => handleExactColumnChange(index, newValue || '')}
                    size="small"
                    sx={{ flex: 1 }}
                    renderInput={(params) => <TextField {...params} label="Column" placeholder="Select Column" />}
                  />

                  {!filter.column && (
                    <TextField label="Value" size="small" disabled sx={{ flex: 1 }} />
                  )}

                  {filter.column && isNumeric(colType) && (
                    <TextField
                      label="Value"
                      type="number"
                      value={filter.value}
                      onChange={(e) => updateFilter(index, 'value', e.target.value)}
                      size="small"
                      sx={{ flex: 1 }}
                    />
                  )}

                  {filter.column && isDate(colType) && (
                    <DatePicker
                      label="Date"
                      value={filter.value ? dayjs(filter.value) : null}
                      onChange={(val) => updateFilter(index, 'value', val ? val.format('YYYY-MM-DD') : '')}
                      slotProps={{ textField: { size: 'small', sx: { flex: 1 } } }}
                    />
                  )}

                  {filter.column && isTimestamp(colType) && (
                    <DateTimePicker
                      label="Date & Time"
                      value={filter.value ? dayjs(filter.value) : null}
                      onChange={(val) => updateFilter(index, 'value', val ? val.toISOString() : '')}
                      slotProps={{ textField: { size: 'small', sx: { flex: 1 } } }}
                    />
                  )}

                  {filter.column && !isNumeric(colType) && !isDate(colType) && !isTimestamp(colType) && (
                    <TextField
                      label="Value"
                      value={filter.value}
                      onChange={(e) => updateFilter(index, 'value', e.target.value)}
                      size="small"
                      sx={{ flex: 1 }}
                    />
                  )}

                  <IconButton size="small" color="error" onClick={() => removeFilter(index)}>
                    <DeleteIcon />
                  </IconButton>
                </Box>
              );
            })}
            {filters.length === 0 && <Typography variant="body2" color="text.secondary">No exact filters added.</Typography>}
          </Box>

          <Divider />

          {/* Range Filters */}
          <Box>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>Range Filters</Typography>
              <Button size="small" startIcon={<AddIcon />} onClick={addRangeFilter}>Add Range</Button>
            </Box>
            {rangeFilters.map((filter, index) => {
              const colType = getColumnType(filter.column);
              
              return (
                <Box key={index} sx={{ display: 'flex', gap: 2, mb: 1, alignItems: 'center' }}>
                  <Autocomplete
                    options={rangeColumnOptions}
                    getOptionLabel={(option) => option}
                    value={filter.column || null}
                    onChange={(event, newValue) => handleRangeColumnChange(index, newValue || '')}
                    size="small"
                    sx={{ flex: 2 }}
                    renderInput={(params) => <TextField {...params} label="Column" placeholder="Select Column" />}
                  />

                  {!filter.column && (
                    <>
                      <TextField label="Min" size="small" disabled sx={{ flex: 1 }} />
                      <TextField label="Max" size="small" disabled sx={{ flex: 1 }} />
                    </>
                  )}

                  {filter.column && isNumeric(colType) && (
                    <>
                      <TextField
                        label="Min"
                        type="number"
                        value={filter.minValue}
                        onChange={(e) => updateRangeFilter(index, 'minValue', e.target.value)}
                        size="small"
                        sx={{ flex: 1 }}
                      />
                      <TextField
                        label="Max"
                        type="number"
                        value={filter.maxValue}
                        onChange={(e) => updateRangeFilter(index, 'maxValue', e.target.value)}
                        size="small"
                        sx={{ flex: 1 }}
                      />
                    </>
                  )}

                  {filter.column && isDate(colType) && (
                    <>
                      <DatePicker
                        label="From Date"
                        value={filter.minValue ? dayjs(filter.minValue) : null}
                        onChange={(val) => updateRangeFilter(index, 'minValue', val ? val.format('YYYY-MM-DD') : '')}
                        slotProps={{ textField: { size: 'small', sx: { flex: 1 } } }}
                      />
                      <DatePicker
                        label="To Date"
                        value={filter.maxValue ? dayjs(filter.maxValue) : null}
                        onChange={(val) => updateRangeFilter(index, 'maxValue', val ? val.format('YYYY-MM-DD') : '')}
                        slotProps={{ textField: { size: 'small', sx: { flex: 1 } } }}
                      />
                    </>
                  )}

                  {filter.column && isTimestamp(colType) && (
                    <>
                      <DateTimePicker
                        label="From Date & Time"
                        value={filter.minValue ? dayjs(filter.minValue) : null}
                        onChange={(val) => updateRangeFilter(index, 'minValue', val ? val.toISOString() : '')}
                        slotProps={{ textField: { size: 'small', sx: { flex: 1 } } }}
                      />
                      <DateTimePicker
                        label="To Date & Time"
                        value={filter.maxValue ? dayjs(filter.maxValue) : null}
                        onChange={(val) => updateRangeFilter(index, 'maxValue', val ? val.toISOString() : '')}
                        slotProps={{ textField: { size: 'small', sx: { flex: 1 } } }}
                      />
                    </>
                  )}

                  <IconButton size="small" color="error" onClick={() => removeRangeFilter(index)}>
                    <DeleteIcon />
                  </IconButton>
                </Box>
              );
            })}
            {rangeFilters.length === 0 && <Typography variant="body2" color="text.secondary">No range filters added.</Typography>}
          </Box>

        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={handleReset} color="inherit">Reset</Button>
          <Box sx={{ flex: 1 }} />
          <Button onClick={onClose} color="inherit">Cancel</Button>
          <Button onClick={handleApply} variant="contained" color="primary" disableElevation>Apply</Button>
        </DialogActions>
      </Dialog>
    </LocalizationProvider>
  );
};

export default FilterDialog;
