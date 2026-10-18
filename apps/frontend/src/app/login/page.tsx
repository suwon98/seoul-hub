import AuthForm from "@/components/AuthForm";
export default async function LoginPage({ searchParams }: { searchParams: Promise<{ registered?: string }> }) {
  const params = await searchParams;
  return <AuthForm registered={params.registered === "1"} />;
}
