import { cn } from "@/lib/utils/cn";
import { Check, X } from "@/components/ui/icons";
import { Spinner } from "@/components/ui/Spinner";

export type StepState = "complete" | "current" | "upcoming" | "failed";

export interface Step {
  key: string;
  label: string;
  description?: string;
  state: StepState;
}

const CIRCLE_CLASSES: Record<StepState, string> = {
  complete: "bg-success-500 text-white border-success-500",
  current: "bg-brand-700 text-white border-brand-700",
  upcoming: "bg-white text-ink-400 border-surface-border",
  failed: "bg-danger-500 text-white border-danger-500",
};

/** Horizontal stepper for short flows (Investment, Tokenization). State is conveyed by icon + text, never color alone. */
export function ProgressSteps({ steps, orientation = "horizontal" }: { steps: Step[]; orientation?: "horizontal" | "vertical" }) {
  return (
    <ol
      className={cn(
        orientation === "horizontal" ? "flex w-full items-start" : "flex flex-col gap-0",
      )}
    >
      {steps.map((step, index) => {
        const isLast = index === steps.length - 1;
        return (
          <li
            key={step.key}
            className={cn(
              orientation === "horizontal" ? "flex flex-1 flex-col items-center text-center" : "flex gap-3 pb-6 last:pb-0",
            )}
          >
            {orientation === "horizontal" && (
              <div className="flex w-full items-center">
                <div className={cn("h-px flex-1", index === 0 ? "bg-transparent" : "bg-surface-border")} />
                <StepCircle state={step.state} />
                <div className={cn("h-px flex-1", isLast ? "bg-transparent" : "bg-surface-border")} />
              </div>
            )}
            {orientation === "vertical" && (
              <div className="flex flex-col items-center">
                <StepCircle state={step.state} />
                {!isLast && <div className={cn("w-px flex-1", step.state === "complete" ? "bg-success-400" : "bg-surface-border")} style={{ minHeight: 24 }} />}
              </div>
            )}
            <div className={cn(orientation === "horizontal" ? "mt-2 max-w-[9rem]" : "pb-2 pt-0.5")}>
              <p
                className={cn(
                  "text-sm font-medium",
                  step.state === "upcoming" ? "text-ink-400" : "text-ink-800",
                )}
              >
                {step.label}
              </p>
              {step.description && <p className="mt-0.5 text-xs text-ink-500">{step.description}</p>}
            </div>
          </li>
        );
      })}
    </ol>
  );
}

function StepCircle({ state }: { state: StepState }) {
  return (
    <div
      className={cn(
        "flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2",
        CIRCLE_CLASSES[state],
      )}
      aria-hidden="true"
    >
      {state === "complete" && <Check className="h-3.5 w-3.5" />}
      {state === "failed" && <X className="h-3.5 w-3.5" />}
      {state === "current" && <Spinner className="h-3.5 w-3.5" />}
    </div>
  );
}
