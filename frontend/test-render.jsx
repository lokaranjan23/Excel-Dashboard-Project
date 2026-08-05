import React from 'react';
import { renderToString } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom/server';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import UploadPage from './src/pages/UploadPage';

const queryClient = new QueryClient();

try {
  renderToString(
    <QueryClientProvider client={queryClient}>
      <StaticRouter>
        <UploadPage />
      </StaticRouter>
    </QueryClientProvider>
  );
  console.log('RENDER SUCCESS');
} catch(e) {
  console.error('RENDER ERROR:', e.message, e.stack);
}
