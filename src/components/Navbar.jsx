import { Link, useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useTheme } from "../context/ThemeContext";
import { useState, useEffect } from "react";
import api from "../api/axiosInstance";

function Navbar() {
  const { user, logout, isAuthenticated } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const [cartCount, setCartCount] = useState(0);

  // Fetch cart count whenever route changes (for booker only)
  useEffect(() => {
    if (user?.role === "BOOKER") fetchCartCount();
  }, [location.pathname, user]);

  const fetchCartCount = async () => {
    try {
      const res = await api.get("/api/cart");
      setCartCount(res.data?.items?.length || 0);
    } catch {
      setCartCount(0);
    }
  };

  if (!isAuthenticated) return null;

  const isActive = (path) => location.pathname === path;

  const navLink = (to, label, badge = 0) => (
    <Link
      key={to}
      to={to}
      className={`relative px-3 py-2 rounded-lg text-sm font-medium transition-all duration-150 whitespace-nowrap flex items-center gap-1.5 ${
        isActive(to)
          ? "bg-green-600 text-white shadow-sm"
          : "text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 hover:text-gray-900 dark:hover:text-white"
      }`}
    >
      {label}
      {badge > 0 && (
        <span className="bg-red-500 text-white text-xs font-black w-5 h-5 rounded-full flex items-center justify-center leading-none">
          {badge > 9 ? "9+" : badge}
        </span>
      )}
    </Link>
  );

  return (
    <nav className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800 sticky top-0 z-40 shadow-sm">
      <div className="max-w-6xl mx-auto px-4">
        <div className="flex items-center justify-between h-14 gap-4">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 flex-shrink-0">
            <div className="w-8 h-8 bg-green-600 rounded-lg flex items-center justify-center shadow-sm">
              <span className="text-base">🏓</span>
            </div>
            <span className="font-black text-base tracking-tight hidden sm:block">
              <span className="text-gray-900 dark:text-white">Pickle</span>
              <span className="text-green-600">Ball</span>
            </span>
          </Link>

          {/* Nav links */}
          <div className="flex items-center gap-1 flex-1 justify-center">
            {user?.role === "BOOKER" && (
              <>
                {navLink("/marketplace", "🏟️ Venues")}
                {navLink("/cart", "🛒 Cart", cartCount)}
                {navLink("/my-bookings", "📅 Bookings")}
              </>
            )}
            {user?.role === "OWNER" && (
              <>
                {navLink("/owner/dashboard", "📊 Dashboard")}
                {navLink("/owner/venues/create", "➕ Add Venue")}
              </>
            )}
          </div>

          {/* Right */}
          <div className="flex items-center gap-2 flex-shrink-0">
            {/* Theme toggle — pill style */}
            <button
              onClick={toggleTheme}
              className={`relative w-14 h-7 rounded-full transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-green-500 flex-shrink-0 ${
                isDark ? "bg-indigo-600" : "bg-gray-200"
              }`}
              title={isDark ? "Switch to light" : "Switch to dark"}
            >
              <span
                className={`absolute top-0.5 w-6 h-6 rounded-full flex items-center justify-center text-sm shadow-md transition-all duration-300 bg-white ${
                  isDark ? "left-7" : "left-0.5"
                }`}
              >
                {isDark ? "🌙" : "☀️"}
              </span>
            </button>

            {/* User badge */}
            <div className="flex items-center gap-1.5 bg-gray-50 dark:bg-gray-800 border border-gray-200 dark:border-gray-700 rounded-full pl-1.5 pr-2.5 py-1">
              <div className="w-6 h-6 bg-green-600 rounded-full flex items-center justify-center flex-shrink-0">
                <span className="text-white text-xs font-black">
                  {user?.name?.charAt(0).toUpperCase()}
                </span>
              </div>
              <span className="text-xs font-semibold text-gray-700 dark:text-gray-200 max-w-20 truncate">
                {user?.name?.split(" ")[0]}
              </span>
              <span
                className={`text-xs px-1.5 py-0.5 rounded-full font-bold hidden sm:block text-white ${
                  user?.role === "OWNER" ? "bg-purple-600" : "bg-green-600"
                }`}
              >
                {user?.role === "OWNER" ? "OWNER" : "BOOKER"}
              </span>
            </div>

            {/* Logout */}
            <button
              onClick={() => {
                logout();
                navigate("/login");
              }}
              className="text-xs font-bold bg-gray-900 dark:bg-gray-700 text-white px-3 py-2 rounded-lg hover:bg-gray-700 dark:hover:bg-gray-600 transition-colors flex-shrink-0"
            >
              Logout
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}

export default Navbar;
