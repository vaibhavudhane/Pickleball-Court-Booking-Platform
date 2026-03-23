/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState } from "react";

const AuthContext = createContext(null);

function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
}

function loadFromStorage() {
  const token = localStorage.getItem("token");
  const user = JSON.parse(localStorage.getItem("user") || "null");
  if (!token || isTokenExpired(token)) {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    return { token: null, user: null };
  }
  return { token, user };
}

export function AuthProvider({ children }) {
  const initial = loadFromStorage();
  const [token, setToken] = useState(initial.token);
  const [user, setUser] = useState(initial.user);

  const login = (newToken, userData) => {
    localStorage.setItem("token", newToken);
    localStorage.setItem("user", JSON.stringify(userData));
    setToken(newToken);
    setUser(userData);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{ token, user, login, logout, isAuthenticated: !!token }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
