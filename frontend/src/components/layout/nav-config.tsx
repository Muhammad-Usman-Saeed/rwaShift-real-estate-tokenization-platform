import type { ReactNode } from "react";
import {
  Activity,
  Building,
  CircleDollar,
  Coins,
  FileText,
  Gauge,
  Layers,
  Receipt,
  Settings,
  ShieldCheck,
  Users,
  Wallet,
} from "@/components/ui/icons";

export interface NavItem {
  label: string;
  href: string;
  icon: ReactNode;
}

const iconClass = "h-4 w-4";

export const issuerNav: NavItem[] = [
  { label: "Dashboard", href: "/issuer/dashboard", icon: <Gauge className={iconClass} /> },
  { label: "Activity", href: "/issuer/activity", icon: <Activity className={iconClass} /> },
  { label: "Assets", href: "/issuer/assets", icon: <Building className={iconClass} /> },
  { label: "Offerings", href: "/issuer/offerings", icon: <Layers className={iconClass} /> },
  { label: "Investors", href: "/issuer/investors", icon: <Users className={iconClass} /> },
  { label: "Payments", href: "/issuer/payments", icon: <Wallet className={iconClass} /> },
  { label: "Distributions", href: "/issuer/distributions", icon: <Coins className={iconClass} /> },
  { label: "Documents", href: "/issuer/documents", icon: <FileText className={iconClass} /> },
  { label: "Reports", href: "/issuer/reports", icon: <Receipt className={iconClass} /> },
  { label: "Settings", href: "/issuer/settings", icon: <Settings className={iconClass} /> },
];

export const investorNav: NavItem[] = [
  { label: "Opportunities", href: "/investor/opportunities", icon: <Building className={iconClass} /> },
  { label: "Activity", href: "/investor/activity", icon: <Activity className={iconClass} /> },
  { label: "My Portfolio", href: "/investor/portfolio", icon: <Gauge className={iconClass} /> },
  { label: "Investments", href: "/investor/investments", icon: <CircleDollar className={iconClass} /> },
  { label: "Distributions", href: "/investor/distributions", icon: <Coins className={iconClass} /> },
  { label: "Transactions", href: "/investor/transactions", icon: <Receipt className={iconClass} /> },
  { label: "Documents", href: "/investor/documents", icon: <FileText className={iconClass} /> },
  { label: "Profile & Verification", href: "/investor/profile", icon: <ShieldCheck className={iconClass} /> },
];

export const adminNav: NavItem[] = [
  { label: "Dashboard", href: "/admin/dashboard", icon: <Gauge className={iconClass} /> },
  { label: "Activity", href: "/admin/activity", icon: <Activity className={iconClass} /> },
  { label: "Organizations", href: "/admin/organizations", icon: <Building className={iconClass} /> },
  { label: "Assets", href: "/admin/assets", icon: <Layers className={iconClass} /> },
  { label: "Offerings", href: "/admin/offerings", icon: <Layers className={iconClass} /> },
  { label: "Offering Approvals", href: "/admin/offering-approvals", icon: <ShieldCheck className={iconClass} /> },
  { label: "Investors", href: "/admin/investors", icon: <Users className={iconClass} /> },
  { label: "KYC Reviews", href: "/admin/kyc-reviews", icon: <ShieldCheck className={iconClass} /> },
  { label: "Compliance", href: "/admin/compliance", icon: <ShieldCheck className={iconClass} /> },
  { label: "Tokenization", href: "/admin/tokenization", icon: <Coins className={iconClass} /> },
  { label: "Blockchain Transactions", href: "/admin/blockchain-transactions", icon: <Receipt className={iconClass} /> },
  { label: "Audit Logs", href: "/admin/audit-logs", icon: <FileText className={iconClass} /> },
  { label: "System", href: "/admin/system", icon: <Settings className={iconClass} /> },
];
