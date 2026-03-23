import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import { ThemeProvider } from "./context/ThemeContext";
import ProtectedRoute from "./components/ProtectedRoute";
import Navbar from "./components/Navbar";

import LoginPage from "./pages/auth/LoginPage";
import RegisterPage from "./pages/auth/RegisterPage";
import MarketplacePage from "./pages/booker/MarketplacePage";
import VenueDetailPage from "./pages/booker/VenueDetailPage";
import CartPage from "./pages/booker/CartPage";
import MyBookingsPage from "./pages/booker/MyBookingsPage";
import OwnerDashboard from "./pages/owner/OwnerDashboard";
import VenueCreatePage from "./pages/owner/VenueCreatePage";
import VenueEditPage from "./pages/owner/VenueEditPage";

function Home() {
  const { isAuthenticated, user } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  return user?.role === "OWNER" ? (
    <Navigate to="/owner/dashboard" replace />
  ) : (
    <Navigate to="/marketplace" replace />
  );
}

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <div className="min-h-screen bg-white dark:bg-gray-950 transition-colors duration-200">
            <Navbar />
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route
                path="/marketplace"
                element={
                  <ProtectedRoute role="BOOKER">
                    <MarketplacePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/venues/:venueId"
                element={
                  <ProtectedRoute role="BOOKER">
                    <VenueDetailPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/cart"
                element={
                  <ProtectedRoute role="BOOKER">
                    <CartPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/my-bookings"
                element={
                  <ProtectedRoute role="BOOKER">
                    <MyBookingsPage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/owner/dashboard"
                element={
                  <ProtectedRoute role="OWNER">
                    <OwnerDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/owner/venues/create"
                element={
                  <ProtectedRoute role="OWNER">
                    <VenueCreatePage />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/owner/venues/:id/edit"
                element={
                  <ProtectedRoute role="OWNER">
                    <VenueEditPage />
                  </ProtectedRoute>
                }
              />
            </Routes>
          </div>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}

export default App;
