"use client";

import React, { Component, type ReactNode } from "react";
import { AlertTriangle, RefreshCw } from "lucide-react";

interface Props {
    children: ReactNode;
    fallback?: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, info: React.ErrorInfo) {
        // In production, log to an error tracking service
        console.error("[ErrorBoundary]", error, info);
    }

    handleReset = () => {
        this.setState({ hasError: false, error: null });
    };

    render() {
        if (this.state.hasError) {
            if (this.props.fallback) return this.props.fallback;

            return (
                <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-8 text-center">
                    <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
                        <AlertTriangle className="h-6 w-6 text-destructive" />
                    </div>
                    <h3 className="text-base font-semibold text-foreground">Something went wrong</h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                        {this.state.error?.message ?? "An unexpected error occurred"}
                    </p>
                    <button
                        onClick={this.handleReset}
                        className="mt-4 flex items-center gap-2 rounded-lg border border-border px-4 py-2 text-sm font-medium
              text-foreground hover:bg-muted transition-colors mx-auto"
                    >
                        <RefreshCw className="h-4 w-4" />
                        Try again
                    </button>
                </div>
            );
        }

        return this.props.children;
    }
}
