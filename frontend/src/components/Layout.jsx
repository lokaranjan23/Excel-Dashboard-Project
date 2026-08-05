import React from 'react';
import { Box, AppBar, Toolbar, Typography, Button, Container } from '@mui/material';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';

const Layout = () => {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar 
        position="static" 
        color="transparent" 
        elevation={0} 
        sx={{ borderBottom: '1px solid', borderColor: 'divider' }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Typography variant="h6" color="text.primary" sx={{ fontWeight: 600 }}>
            <Box component="span" sx={{ color: 'primary.main', mr: 1 }}>■</Box>
            Excel Dashboard
          </Typography>
          <Box>
            <Button 
              color={location.pathname === '/' ? 'primary' : 'inherit'} 
              onClick={() => navigate('/')}
            >
              Dashboard
            </Button>
          </Box>
        </Toolbar>
      </AppBar>
      
      <Box component="main" sx={{ flexGrow: 1, py: 4 }}>
        <Container maxWidth="xl">
          <Outlet />
        </Container>
      </Box>
    </Box>
  );
};

export default Layout;
