import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    primary: {
      main: '#F59E0B',
      light: '#FBBF24',
      dark: '#D97706',
      contrastText: '#fff',
    },
    secondary: {
      main: '#6B7280', // light gray
      light: '#9CA3AF',
      dark: '#4B5563',
      contrastText: '#fff',
    },
    background: {
      default: '#ffffff',
      paper: '#ffffff',
    },
    text: {
      primary: '#1F2937', // dark gray
      secondary: '#4B5563',
    },
    divider: '#E5E7EB',
  },
  shape: {
    borderRadius: 8,
  },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    button: {
      textTransform: 'none',
      fontWeight: 500,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          boxShadow: 'none',
          '&:hover': {
            boxShadow: 'none',
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)', // soft shadow
          border: '1px solid #E5E7EB',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        elevation1: {
          boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)',
        },
        elevation2: {
          boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -2px rgba(0, 0, 0, 0.025)',
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-notchedOutline': {
            borderColor: '#E5E7EB',
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: '#D1D5DB',
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: '#F59E0B',
          },
        },
      },
    },
    MuiDataGrid: {
      styleOverrides: {
        root: {
          border: '1px solid #E5E7EB',
          backgroundColor: '#fff',
          '& .MuiDataGrid-row:hover': {
            backgroundColor: '#F9FAFB',
          },
          '& .MuiDataGrid-row.Mui-selected': {
            backgroundColor: 'rgba(245, 158, 11, 0.08)',
            '&:hover': {
              backgroundColor: 'rgba(245, 158, 11, 0.12)',
            },
          },
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: '#F9FAFB',
            borderBottom: '1px solid #E5E7EB',
          },
          '& .MuiDataGrid-cell': {
            borderBottom: '1px solid #E5E7EB',
          },
        },
      },
    },
  },
});

export default theme;
