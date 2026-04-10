import { useState } from "react";
import { Rocket, Sparkles, AlertCircle } from "lucide-react";
import { useUser } from "../../context/UserContext";

export function Login() {
  const { register, confirmSignUp, login } = useUser();

  // Form fields
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [confirmCode, setConfirmCode] = useState("");

  // UI state
  const [isSignup, setIsSignup] = useState(false);
  const [confirmStep, setConfirmStep] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  // Held across the confirmation step so we can auto-login after confirm
  const [pendingEmail, setPendingEmail] = useState("");
  const [pendingPassword, setPendingPassword] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    try {
      if (confirmStep) {
        // Step 2: verify email code → auto-login → create backend user
        await confirmSignUp(pendingEmail, confirmCode, pendingPassword);
        // UserContext sets user → AppWrapper renders MainApp automatically

      } else if (isSignup) {
        // Step 1: Cognito sign-up → triggers verification email
        await register(email, password, displayName || email.split('@')[0]);
        setPendingEmail(email);
        setPendingPassword(password);
        setConfirmStep(true);
        setSuccess("Check your email for a verification code.");

      } else {
        // Regular login
        await login(email, password);
      }
    } catch (err: any) {
      console.error("Auth error:", err);
      const code = err?.name;

      if (confirmStep) {
        if (code === 'CodeMismatchException') {
          setError("Invalid verification code. Please try again.");
        } else if (code === 'ExpiredCodeException') {
          setError("Code has expired. Please sign up again.");
        } else {
          setError("Confirmation failed. Please try again.");
        }
      } else if (isSignup) {
        if (code === 'UsernameExistsException') {
          setError("An account with this email already exists.");
        } else if (code === 'InvalidPasswordException') {
          setError("Password must be at least 8 characters and include numbers and symbols.");
        } else {
          setError("Failed to create account. Please try again.");
        }
      } else {
        if (code === 'NotAuthorizedException') {
          setError("Incorrect email or password.");
        } else if (code === 'UserNotFoundException') {
          setError("No account found with this email.");
        } else if (code === 'UserNotConfirmedException') {
          setError("Please verify your email before signing in.");
        } else {
          setError("Login failed. Please try again.");
        }
      }
    }
  };

  const toggleMode = () => {
    setIsSignup(!isSignup);
    setConfirmStep(false);
    setError("");
    setSuccess("");
  };

  const headerSubtitle = confirmStep
      ? "Enter the code we sent to your email"
      : isSignup
          ? "Begin your journey through the stars"
          : "Welcome back, Space Explorer";

  const submitLabel = confirmStep ? "Verify & Launch" : isSignup ? "Launch Into Space" : "Enter Galaxy";

  return (
      <div className="relative w-full h-screen flex items-center justify-center overflow-hidden bg-slate-950">

        {/* Animated space background */}
        <div className="absolute inset-0 bg-gradient-to-b from-slate-950 via-indigo-950 to-slate-950">
          {[...Array(150)].map((_, i) => (
              <div
                  key={i}
                  className="absolute bg-white rounded-full animate-pulse"
                  style={{
                    width: `${Math.random() * 2 + 0.5}px`,
                    height: `${Math.random() * 2 + 0.5}px`,
                    top: `${Math.random() * 100}%`,
                    left: `${Math.random() * 100}%`,
                    animationDelay: `${Math.random() * 3}s`,
                    opacity: Math.random() * 0.8 + 0.2,
                  }}
              />
          ))}
        </div>

        {/* Nebula effects */}
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '8s' }} />
        <div className="absolute bottom-1/4 left-1/3 w-[500px] h-[500px] bg-blue-600/10 rounded-full blur-3xl animate-pulse" style={{ animationDuration: '10s' }} />

        <div className="relative z-10 w-full max-w-md px-6 my-auto">
          <div className="bg-slate-900/80 backdrop-blur-xl border border-slate-700/50 rounded-3xl shadow-2xl p-8 max-h-[90vh] overflow-y-auto scrollbar-hide">

            {/* Header */}
            <div className="text-center mb-8">
              <div className="flex items-center justify-center mb-4">
                <div className="relative">
                  <Rocket className="w-16 h-16 text-blue-400" />
                  <Sparkles className="w-6 h-6 text-yellow-400 absolute -top-2 -right-2 animate-pulse" />
                </div>
              </div>
              <h1 className="text-3xl text-white mb-2">StarList</h1>
              <p className="text-slate-400 text-sm">{headerSubtitle}</p>
            </div>

            {/* Error Message */}
            {error && (
                <div className="mb-6 p-3 bg-red-500/10 border border-red-500/20 rounded-xl flex items-center gap-2 text-red-400 text-sm">
                  <AlertCircle className="w-4 h-4 shrink-0" />
                  <p>{error}</p>
                </div>
            )}

            {/* Success Message */}
            {success && (
                <div className="mb-6 p-3 bg-green-500/10 border border-green-500/20 rounded-xl flex items-center gap-2 text-green-400 text-sm">
                  <Sparkles className="w-4 h-4 shrink-0" />
                  <p>{success}</p>
                </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">

              {/* Confirmation code step */}
              {confirmStep ? (
                  <div>
                    <label htmlFor="confirmCode" className="block text-sm text-slate-300 mb-2">
                      Verification Code
                    </label>
                    <input
                        type="text"
                        id="confirmCode"
                        value={confirmCode}
                        onChange={(e) => setConfirmCode(e.target.value)}
                        className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-transparent transition-all tracking-widest text-center text-lg"
                        placeholder="______"
                        maxLength={6}
                        required
                    />
                  </div>
              ) : (
                  <>
                    {/* Display Name (signup only) */}
                    {isSignup && (
                        <div>
                          <label htmlFor="displayName" className="block text-sm text-slate-300 mb-2">
                            User Name
                          </label>
                          <input
                              type="text"
                              id="displayName"
                              value={displayName}
                              onChange={(e) => setDisplayName(e.target.value)}
                              className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-transparent transition-all"
                              placeholder="e.g. Commander Shepard"
                              required={isSignup}
                          />
                        </div>
                    )}

                    <div>
                      <label htmlFor="email" className="block text-sm text-slate-300 mb-2">
                        Email Address
                      </label>
                      <input
                          type="email"
                          id="email"
                          value={email}
                          onChange={(e) => setEmail(e.target.value)}
                          className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-transparent transition-all"
                          placeholder="Enter your email"
                          required
                      />
                    </div>

                    <div>
                      <label htmlFor="password" className="block text-sm text-slate-300 mb-2">
                        Password
                      </label>
                      <input
                          type="password"
                          id="password"
                          value={password}
                          onChange={(e) => setPassword(e.target.value)}
                          className="w-full px-4 py-3 bg-slate-800/50 border border-slate-600/50 rounded-xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-transparent transition-all"
                          placeholder="Enter your password"
                          required
                      />
                    </div>
                  </>
              )}

              <button
                  type="submit"
                  className="w-full py-3 bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white rounded-xl transition-all shadow-lg hover:shadow-blue-500/50 flex items-center justify-center gap-2 font-bold"
              >
                <Rocket className="w-5 h-5" />
                <span>{submitLabel}</span>
              </button>
            </form>

            {/* Toggle sign-in / sign-up (hidden during confirmation step) */}
            {!confirmStep && (
                <div className="mt-6 text-center">
                  <button
                      type="button"
                      onClick={toggleMode}
                      className="text-sm text-slate-400 hover:text-blue-400 transition-colors"
                  >
                    {isSignup ? "Already have an account? Sign in" : "New explorer? Create account"}
                  </button>
                </div>
            )}
          </div>
        </div>

        <div className="absolute bottom-10 left-10 w-20 h-20 rounded-full bg-gradient-to-br from-blue-400 to-blue-600 opacity-20 blur-xl animate-pulse" />
        <div className="absolute top-20 right-20 w-16 h-16 rounded-full bg-gradient-to-br from-purple-400 to-pink-600 opacity-20 blur-xl animate-pulse" />
      </div>
  );
}
