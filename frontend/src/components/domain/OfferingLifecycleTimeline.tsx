import { ProgressSteps, type Step, type StepState } from "@/components/ui/ProgressSteps";
import { OFFERING_STATUS_ORDER } from "@/lib/utils/constants";
import type { OfferingStatus } from "@/lib/api/types";

const LABELS: Record<(typeof OFFERING_STATUS_ORDER)[number], string> = {
  DRAFT: "Draft",
  UNDER_REVIEW: "Under Review",
  APPROVED: "Approved",
  TOKENIZING: "Tokenizing",
  OPEN: "Open",
  FUNDED: "Funded",
  CLOSED: "Closed",
};

export function OfferingLifecycleTimeline({ status }: { status: OfferingStatus }) {
  if (status === "REJECTED") {
    const steps: Step[] = [
      { key: "DRAFT", label: "Draft", state: "complete" },
      { key: "UNDER_REVIEW", label: "Under Review", state: "complete" },
      { key: "REJECTED", label: "Rejected", state: "failed" },
    ];
    return <ProgressSteps steps={steps} />;
  }

  const currentIndex = OFFERING_STATUS_ORDER.indexOf(status);
  const steps: Step[] = OFFERING_STATUS_ORDER.map((s, index) => {
    let state: StepState = "upcoming";
    if (index < currentIndex) state = "complete";
    else if (index === currentIndex) state = "current";
    return { key: s, label: LABELS[s], state };
  });

  return <ProgressSteps steps={steps} />;
}
