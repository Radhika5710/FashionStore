/**
 * Chart Error Boundary
 * Catches errors in chart rendering (recharts) and displays fallback UI
 * Prevents entire dashboard from crashing if chart library fails
 */

import { Component } from 'react';

class ChartErrorBoundaryClass extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Chart Error Boundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="h-80 flex items-center justify-center bg-[var(--color-bg-secondary)] rounded-xl border border-[var(--color-border)]">
          <div className="text-center">
            <div className="w-12 h-12 rounded-full bg-[var(--color-bg-secondary)] flex items-center justify-center mx-auto mb-3">
              <svg className="w-6 h-6 text-[var(--color-text-muted)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
            <p className="text-sm text-[var(--color-text-muted)]">Chart unavailable</p>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default function ChartErrorBoundary({ children }) {
  return <ChartErrorBoundaryClass>{children}</ChartErrorBoundaryClass>;
}
