import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../../api/axiosInstance";

function CartPage() {
  const [cart, setCart] = useState(null);
  const [loading, setLoading] = useState(true);
  const [checkingOut, setCheckingOut] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    fetchCart();
  }, []);

  const fetchCart = async () => {
    setLoading(true);
    try {
      const res = await api.get("/api/cart");
      setCart(res.data);
    } catch {
      setError("Failed to load cart");
    } finally {
      setLoading(false);
    }
  };

  const removeItem = async (id) => {
    try {
      await api.delete(`/api/cart/${id}`);
      fetchCart();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to remove");
    }
  };

  const clearCart = async () => {
    if (!window.confirm("Clear all items?")) return;
    try {
      await api.delete("/api/cart/clear");
      fetchCart();
    } catch (err) {
      alert(err.response?.data?.message || "Failed to clear");
    }
  };

  const checkout = async () => {
    setCheckingOut(true);
    setError("");
    try {
      const res = await api.post("/api/bookings/checkout");
      navigate("/my-bookings", { state: { successMessage: res.data.message } });
    } catch (err) {
      setError(err.response?.data?.message || "Checkout failed");
    } finally {
      setCheckingOut(false);
    }
  };

  if (loading)
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-3 animate-bounce">🛒</div>
          <p className="text-gray-500 dark:text-gray-400">Loading cart...</p>
        </div>
      </div>
    );

  const isEmpty = !cart || cart.items.length === 0;

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="max-w-3xl mx-auto px-4 py-8">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-black text-gray-900 dark:text-white">
            🛒 Your Cart
          </h1>
          {!isEmpty && (
            <button
              onClick={clearCart}
              className="text-sm text-red-500 dark:text-red-400 border border-red-200 dark:border-red-800 px-3 py-1.5 rounded-xl hover:bg-red-50 dark:hover:bg-red-900/30 transition-colors"
            >
              Clear All
            </button>
          )}
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/40 border border-red-200 dark:border-red-700 text-red-600 dark:text-red-400 p-3 rounded-xl mb-4 text-sm">
            ⚠️ {error}
          </div>
        )}

        {isEmpty ? (
          <div className="text-center py-20 bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm">
            <p className="text-6xl mb-4">🛒</p>
            <p className="text-gray-700 dark:text-gray-200 font-bold text-xl mb-2">
              Your cart is empty
            </p>
            <p className="text-gray-400 dark:text-gray-500 text-sm mb-6">
              Browse venues and add slots to get started
            </p>
            <Link
              to="/marketplace"
              className="bg-green-600 text-white px-6 py-2.5 rounded-xl hover:bg-green-700 font-semibold text-sm transition-colors"
            >
              Browse Venues →
            </Link>
          </div>
        ) : (
          <>
            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden mb-4">
              {cart.items.map((item, idx) => (
                <div
                  key={item.cartItemId}
                  className={`p-4 flex items-center justify-between gap-4 ${idx !== cart.items.length - 1 ? "border-b border-gray-50 dark:border-gray-800" : ""}`}
                >
                  <div className="flex-1 min-w-0">
                    <p className="font-semibold text-gray-900 dark:text-white">
                      {item.courtName}
                      <span className="text-gray-400 dark:text-gray-500 font-normal text-sm ml-2">
                        @ {item.venueName}
                      </span>
                    </p>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                      📅 {item.bookingDate} &nbsp; 🕐 {item.startTime} –{" "}
                      {item.endTime}
                    </p>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <span className="font-black text-green-600 dark:text-green-400 text-lg">
                      ₹{item.price}
                    </span>
                    <button
                      onClick={() => removeItem(item.cartItemId)}
                      className="text-red-400 dark:text-red-400 hover:text-red-600 border border-red-100 dark:border-red-800 px-2.5 py-1 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/30 text-sm transition-colors"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
            </div>

            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm p-5 mb-4">
              <div className="flex justify-between text-sm text-gray-500 dark:text-gray-400 mb-3">
                <span>
                  {cart.items.length} slot{cart.items.length !== 1 ? "s" : ""}
                </span>
                <span>₹{cart.total}</span>
              </div>
              <div className="flex justify-between font-black text-xl border-t border-gray-100 dark:border-gray-800 pt-3">
                <span className="text-gray-900 dark:text-white">Total</span>
                <span className="text-green-600 dark:text-green-400">
                  ₹{cart.total}
                </span>
              </div>
            </div>

            <button
              onClick={checkout}
              disabled={checkingOut}
              className="w-full bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white py-4 rounded-xl font-black text-lg transition-colors shadow-lg"
            >
              {checkingOut ? "Processing..." : `Confirm & Pay ₹${cart.total} →`}
            </button>
            <p className="text-center text-xs text-gray-400 dark:text-gray-500 mt-2">
              All slots confirmed instantly upon checkout
            </p>
          </>
        )}
      </div>
    </div>
  );
}

export default CartPage;
