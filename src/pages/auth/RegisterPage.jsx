import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import api from "../../api/axiosInstance";

function RegisterPage() {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("BOOKER");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const res = await api.post("/api/auth/register", {
        name,
        email,
        password,
        role,
      });
      const { token, role: userRole, name: userName, userId } = res.data;
      login(token, { role: userRole, name: userName, userId });
      navigate(userRole === "OWNER" ? "/owner/dashboard" : "/marketplace", {
        replace: true,
      });
    } catch (err) {
      const data = err.response?.data;
      if (data && typeof data === "object" && !data.message) {
        setError(Object.values(data)[0]);
      } else {
        setError(data?.message || "Registration failed");
      }
    } finally {
      setLoading(false);
    }
  };

  const inputCls =
    "w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-3 bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent text-sm";

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center px-4 py-8">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-green-600 rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
            <span className="text-3xl">🏓</span>
          </div>
          <h1 className="text-3xl font-black text-gray-900 dark:text-white">
            PickleBall
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-1 text-sm">
            Join thousands of players
          </p>
        </div>

        <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-xl border border-gray-100 dark:border-gray-800 p-8">
          <h2 className="text-xl font-bold text-gray-900 dark:text-white mb-5">
            Create account
          </h2>

          {error && (
            <div className="bg-red-50 dark:bg-red-900/40 border border-red-200 dark:border-red-700 text-red-600 dark:text-red-400 p-3 rounded-xl mb-4 text-sm flex items-center gap-2">
              <span>⚠️</span>
              <span>{error}</span>
            </div>
          )}

          {/* Role selector */}
          <div className="grid grid-cols-2 gap-3 mb-5">
            {[
              {
                value: "BOOKER",
                icon: "🏃",
                title: "Booker",
                sub: "Book courts",
              },
              {
                value: "OWNER",
                icon: "🏟️",
                title: "Court Owner",
                sub: "List venues",
              },
            ].map((r) => (
              <button
                key={r.value}
                type="button"
                onClick={() => setRole(r.value)}
                className={`p-3.5 rounded-xl border-2 text-center transition-all ${
                  role === r.value
                    ? "border-green-500 bg-green-50 dark:bg-green-900/30"
                    : "border-gray-200 dark:border-gray-700 hover:border-gray-300 dark:hover:border-gray-600"
                }`}
              >
                <div className="text-2xl mb-1">{r.icon}</div>
                <div
                  className={`font-bold text-sm ${role === r.value ? "text-green-700 dark:text-green-400" : "text-gray-700 dark:text-gray-300"}`}
                >
                  {r.title}
                </div>
                <div className="text-xs text-gray-400 dark:text-gray-500">
                  {r.sub}
                </div>
              </button>
            ))}
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Full Name
              </label>
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                required
                placeholder="Vaibhav Udhane"
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Email
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="vaibhav@example.com"
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                placeholder="Min 8 chars, uppercase, number, special"
                className={inputCls}
              />
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                Example: Vaibhav@123
              </p>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white font-bold py-3 rounded-xl transition-colors text-sm"
            >
              {loading
                ? "Creating..."
                : `Create ${role === "OWNER" ? "Owner" : "Booker"} Account →`}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            Already have an account?{" "}
            <Link
              to="/login"
              className="text-green-600 dark:text-green-400 font-semibold hover:underline"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

export default RegisterPage;
