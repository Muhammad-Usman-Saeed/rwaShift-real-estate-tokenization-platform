"use client";

import { useRouter } from "next/navigation";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { Field } from "@/components/ui/Field";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { AuthShell } from "@/components/domain/AuthShell";
import { investorSignUpApi } from "@/lib/api/investor-signup";

const schema = z
  .object({
    email: z.string().email("Enter a valid email"),
    displayName: z.string().min(1, "Name is required").max(200),
    password: z.string().min(8, "Password must be at least 8 characters"),
    confirmPassword: z.string(),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

type FormValues = z.infer<typeof schema>;

/**
 * The only genuinely public account-creation flow in the app — org/issuer users are still
 * admin-provisioned (see `OrganizationUserController`). Wallet sign-in needs no equivalent page:
 * it's self-provisioning the moment a signature verifies (see `WalletAuthenticationProvider`).
 * This form only ever creates the account — it deliberately doesn't try to also sign the investor
 * in immediately after, since password auth happens entirely on the backend's own login page
 * (see `SignInButton`), not via a token this frontend could establish itself.
 */
export default function SignUpPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const signUp = useMutation({
    mutationFn: (values: FormValues) =>
      investorSignUpApi.signUp({ email: values.email, password: values.password, displayName: values.displayName }),
    onSuccess: () => {
      router.push("/login?created=1");
    },
  });

  return (
    <AuthShell
      title="Create your investor account"
      subtitle="Sign up with email and password, or connect a wallet from the sign-in page."
      footer={
        <>
          Already have an account?{" "}
          <Link href="/login" className="text-gold-300 hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      {signUp.isError && <ErrorBanner error={signUp.error} className="mb-4" />}
      <form className="flex flex-col gap-4" onSubmit={handleSubmit((values) => signUp.mutate(values))}>
        <Field label="Full Name" htmlFor="displayName" required error={errors.displayName?.message}>
          <Input id="displayName" {...register("displayName")} invalid={!!errors.displayName} />
        </Field>
        <Field label="Email" htmlFor="email" required error={errors.email?.message}>
          <Input id="email" type="email" {...register("email")} invalid={!!errors.email} />
        </Field>
        <Field label="Password" htmlFor="password" required error={errors.password?.message}>
          <Input id="password" type="password" {...register("password")} invalid={!!errors.password} />
        </Field>
        <Field label="Confirm Password" htmlFor="confirmPassword" required error={errors.confirmPassword?.message}>
          <Input id="confirmPassword" type="password" {...register("confirmPassword")} invalid={!!errors.confirmPassword} />
        </Field>
        <Button type="submit" size="lg" className="w-full" isLoading={signUp.isPending}>
          Create Account
        </Button>
      </form>
    </AuthShell>
  );
}
