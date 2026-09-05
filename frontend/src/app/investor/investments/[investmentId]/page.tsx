"use client";

import { useRouter } from "next/navigation";
import { PageHeader } from "@/components/ui/PageHeader";
import { InvestmentStatusPanel } from "@/components/domain/InvestmentStatusPanel";

export default function InvestmentStatusPage({ params }: { params: { investmentId: string } }) {
  const { investmentId } = params;
  const router = useRouter();

  return (
    <div className="max-w-2xl">
      <PageHeader title="Investment Status" description="Track and manage this investment." />
      <InvestmentStatusPanel investmentId={investmentId} onViewPortfolio={() => router.push("/investor/portfolio")} />
    </div>
  );
}
