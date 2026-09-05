import { forwardRef, type InputHTMLAttributes, type TextareaHTMLAttributes } from "react";
import { cn } from "@/lib/utils/cn";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(({ className, invalid, ...props }, ref) => (
  <input
    ref={ref}
    className={cn(
      "h-10 w-full rounded-md border bg-white px-3 text-sm text-ink-800 placeholder:text-ink-400",
      "focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-1",
      invalid ? "border-danger-400" : "border-surface-border",
      "disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-ink-400",
      className,
    )}
    aria-invalid={invalid || undefined}
    {...props}
  />
));
Input.displayName = "Input";

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(({ className, invalid, ...props }, ref) => (
  <textarea
    ref={ref}
    className={cn(
      "min-h-[96px] w-full rounded-md border bg-white px-3 py-2 text-sm text-ink-800 placeholder:text-ink-400",
      "focus:outline-none focus:ring-2 focus:ring-brand-500 focus:ring-offset-1",
      invalid ? "border-danger-400" : "border-surface-border",
      "disabled:cursor-not-allowed disabled:bg-surface-muted disabled:text-ink-400",
      className,
    )}
    aria-invalid={invalid || undefined}
    {...props}
  />
));
Textarea.displayName = "Textarea";
