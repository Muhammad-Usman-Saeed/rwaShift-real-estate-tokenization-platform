"use client";

import { signIn } from "next-auth/react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";

export function SignInButton({ callbackUrl }: { callbackUrl?: string }) {
  const [isLoading, setIsLoading] = useState(false);

  return (
    <Button
      size="lg"
      className="w-full"
      isLoading={isLoading}
      onClick={() => {
        setIsLoading(true);
        void signIn("rwashift", { callbackUrl: callbackUrl ?? "/" });
      }}
    >
      Sign in to rwaShift
    </Button>
  );
}
