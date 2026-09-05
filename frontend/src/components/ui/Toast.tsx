"use client";

import * as ToastPrimitive from "@radix-ui/react-toast";
import { createContext, useCallback, useContext, useState, type ReactNode } from "react";
import { cn } from "@/lib/utils/cn";
import { CheckCircle, X, XCircle, AlertTriangle } from "@/components/ui/icons";

export interface ToastMessage {
  id: string;
  title: string;
  description?: string;
  variant?: "success" | "error" | "info";
}

interface ToastContextValue {
  push: (toast: Omit<ToastMessage, "id">) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const VARIANT_ICON = {
  success: CheckCircle,
  error: XCircle,
  info: AlertTriangle,
} as const;

const VARIANT_CLASS = {
  success: "border-success-500/30 text-success-700",
  error: "border-danger-500/30 text-danger-700",
  info: "border-info-500/30 text-info-600",
} as const;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const push = useCallback((toast: Omit<ToastMessage, "id">) => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { ...toast, id }]);
  }, []);

  const remove = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ push }}>
      <ToastPrimitive.Provider swipeDirection="right" duration={6000}>
        {children}
        {toasts.map((toast) => {
          const Icon = VARIANT_ICON[toast.variant ?? "info"];
          return (
            <ToastPrimitive.Root
              key={toast.id}
              onOpenChange={(open) => !open && remove(toast.id)}
              className={cn(
                "flex items-start gap-3 rounded-md border bg-white p-4 shadow-raised",
                "data-[state=open]:animate-in data-[state=open]:slide-in-from-bottom-2",
                VARIANT_CLASS[toast.variant ?? "info"],
              )}
            >
              <Icon className="mt-0.5 h-4 w-4 shrink-0" />
              <div className="flex-1">
                <ToastPrimitive.Title className="text-sm font-semibold text-ink-900">{toast.title}</ToastPrimitive.Title>
                {toast.description && (
                  <ToastPrimitive.Description className="mt-0.5 text-sm text-ink-600">
                    {toast.description}
                  </ToastPrimitive.Description>
                )}
              </div>
              <ToastPrimitive.Close aria-label="Dismiss" className="text-ink-400 hover:text-ink-700">
                <X className="h-4 w-4" />
              </ToastPrimitive.Close>
            </ToastPrimitive.Root>
          );
        })}
        <ToastPrimitive.Viewport className="fixed bottom-4 right-4 z-[100] flex w-96 max-w-[calc(100vw-2rem)] flex-col gap-2 outline-none" />
      </ToastPrimitive.Provider>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used within ToastProvider");
  return ctx;
}
