/**
 * Design System Wrapper
 * Integrates the design system with the application
 * Provides design system utilities and ensures consistency
 */

import React from 'react';
import { ThemeProvider, useTheme } from '../auth/ThemeContext.jsx';

/**
 * Design System Component
 * Wraps the application with theme provider and design system utilities
 */
export function DesignSystem({ children }) {
  return (
    <ThemeProvider>
      {children}
    </ThemeProvider>
  );
}

/**
 * Hook to access design system utilities
 */
export function useDesignSystem() {
  const { theme, tokens, toggleTheme } = useTheme();
  
  return {
    theme,
    tokens,
    toggleTheme,
    // Design system utilities
    spacing: tokens.spacing,
    colors: tokens.colors,
    typography: tokens.typography,
    borderRadius: tokens.borderRadius,
    shadows: tokens.shadows,
  };
}

export default DesignSystem;
