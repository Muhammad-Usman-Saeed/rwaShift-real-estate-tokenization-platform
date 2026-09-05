import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { AppProviders } from "@/providers/AppProviders";
import { auth } from "@/lib/auth/auth";
import "./globals.css";

const inter = Inter({ subsets: ["latin"], variable: "--font-inter", display: "swap" });

export const metadata: Metadata = {
  title: "rwaShift Real Estate",
  description: "Structure, issue and manage tokenized real estate investments.",
};

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const session = await auth();

  return (
    <html lang="en" className={inter.variable}>
      <body className="font-sans">
        <AppProviders session={session}>{children}</AppProviders>
      </body>
    </html>
  );
}
