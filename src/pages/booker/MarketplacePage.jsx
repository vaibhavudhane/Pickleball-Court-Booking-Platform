import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import api from "../../api/axiosInstance";

const to12hr = (time) => {
  if (!time) return "";
  const [h, m] = time.split(":").map(Number);
  const ampm = h >= 12 ? "PM" : "AM";
  const hour = h % 12 || 12;
  return `${hour}:${String(m).padStart(2, "0")} ${ampm}`;
};

function MarketplacePage() {
  const [venues, setVenues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selectedDate, setSelectedDate] = useState(null);
  const [startTime, setStartTime] = useState("");
  const navigate = useNavigate();

  const dateStr = selectedDate
    ? `${selectedDate.getFullYear()}-${String(selectedDate.getMonth() + 1).padStart(2, "0")}-${String(selectedDate.getDate()).padStart(2, "0")}`
    : "";

  useEffect(() => {
    fetchVenues();
  }, [dateStr, startTime]);

  const fetchVenues = async () => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams();
      if (dateStr) params.append("date", dateStr);
      if (startTime) params.append("startTime", startTime);
      const res = await api.get(
        `/api/venues${params.toString() ? "?" + params : ""}`,
      );
      setVenues(res.data);
    } catch {
      setError("Failed to load venues. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const isDateDisabled = (date) => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return date < today;
  };

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-gray-950">
      {/* Hero */}
      <div className="bg-gradient-to-br from-green-800 via-green-700 to-green-600 text-white relative overflow-hidden">
        <div
          className="absolute inset-0 opacity-5"
          style={{
            backgroundImage:
              "radial-gradient(circle, white 1px, transparent 1px)",
            backgroundSize: "30px 30px",
          }}
        />
        <div className="relative max-w-5xl mx-auto px-4 py-14 text-center">
          <div className="inline-flex items-center gap-2 bg-white/10 border border-white/20 rounded-full px-4 py-1.5 mb-5">
            <span className="w-2 h-2 bg-green-300 rounded-full animate-pulse" />
            <span className="text-green-100 text-sm font-medium">
              Real-time availability
            </span>
          </div>
          <h1 className="text-4xl md:text-5xl font-black mb-3 leading-tight">
            Find Your Perfect Court
          </h1>
          <p className="text-green-100 text-lg mb-10 max-w-lg mx-auto">
            Book pickleball courts instantly. Real-time slots, fair pricing.
          </p>

          {/* Search card */}
          <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl p-5 max-w-2xl mx-auto border border-gray-100 dark:border-gray-800">
            <div className="flex flex-wrap gap-4 items-end">
              {/* Date picker */}
              <div className="flex-1 min-w-44">
                <label className="block text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-1.5 text-left">
                  📅 Date
                </label>
                <DatePicker
                  selected={selectedDate}
                  onChange={(d) => setSelectedDate(d)}
                  filterDate={(d) => !isDateDisabled(d)}
                  dateFormat="dd MMM yyyy"
                  calendarStartDay={1}
                  className="w-full border-2 border-gray-200 dark:border-gray-700 rounded-xl px-4 py-2.5 text-sm font-bold text-gray-800 dark:text-white bg-white dark:bg-gray-800 focus:outline-none focus:border-green-500 cursor-pointer placeholder-gray-400"
                  placeholderText="Any date (today included)"
                  isClearable
                  wrapperClassName="w-full"
                />
              </div>

              {/* Time select */}
              <div className="flex-1 min-w-44">
                <label className="block text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wide mb-1.5 text-left">
                  🕐 Time
                </label>
                <select
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className="w-full border-2 border-gray-200 dark:border-gray-700 rounded-xl px-4 py-2.5 text-sm font-bold bg-white dark:bg-gray-800 text-gray-800 dark:text-white focus:outline-none focus:border-green-500 cursor-pointer"
                >
                  <option value="">Any Time</option>
                  {Array.from({ length: 17 }, (_, i) => {
                    const h = i + 6;
                    const val = `${String(h).padStart(2, "0")}:00`;
                    return (
                      <option key={h} value={val}>
                        {to12hr(val)}
                      </option>
                    );
                  })}
                </select>
              </div>

              {(selectedDate || startTime) && (
                <button
                  onClick={() => {
                    setSelectedDate(null);
                    setStartTime("");
                  }}
                  className="text-sm font-semibold text-red-400 hover:text-red-600 underline transition-colors"
                >
                  Clear
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Results */}
      <div className="max-w-5xl mx-auto px-4 py-10">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-gray-800 dark:text-white">
            {loading
              ? "Searching..."
              : `${venues.length} Venue${venues.length !== 1 ? "s" : ""} Found`}
          </h2>
          {(selectedDate || startTime) && !loading && (
            <span className="text-xs bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-800 px-3 py-1 rounded-full font-semibold">
              Filtered
            </span>
          )}
        </div>

        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 text-red-600 p-3 rounded-xl mb-4 text-sm">
            ⚠️ {error}
          </div>
        )}

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {[1, 2, 3].map((i) => (
              <div
                key={i}
                className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 overflow-hidden animate-pulse"
              >
                <div className="h-48 bg-gray-200 dark:bg-gray-800" />
                <div className="p-4 space-y-3">
                  <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4" />
                  <div className="h-3 bg-gray-100 dark:bg-gray-800 rounded w-1/2" />
                  <div className="h-9 bg-gray-100 dark:bg-gray-800 rounded-xl" />
                </div>
              </div>
            ))}
          </div>
        ) : venues.length === 0 ? (
          <div className="text-center py-24 bg-white dark:bg-gray-900 rounded-2xl border border-dashed border-gray-200 dark:border-gray-700">
            <span className="text-6xl">🔍</span>
            <p className="text-gray-700 dark:text-gray-200 font-bold text-xl mt-4 mb-2">
              No venues found
            </p>
            <p className="text-gray-400 text-sm mb-5">
              {selectedDate || startTime
                ? "Try different filters"
                : "Check back soon!"}
            </p>
            {(selectedDate || startTime) && (
              <button
                onClick={() => {
                  setSelectedDate(null);
                  setStartTime("");
                }}
                className="bg-green-600 text-white px-6 py-2.5 rounded-xl hover:bg-green-700 font-semibold text-sm"
              >
                Clear Filters
              </button>
            )}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {venues.map((venue) => (
              <div
                key={venue.id}
                onClick={() => navigate(`/venues/${venue.id}`)}
                className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer group overflow-hidden"
              >
                {venue.thumbnailUrl ? (
                  <div className="overflow-hidden h-48">
                    <img
                      src={`${import.meta.env.VITE_API_URL}${venue.thumbnailUrl}`}
                      alt={venue.name}
                      className="w-full h-48 object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  </div>
                ) : (
                  <div className="h-48 bg-gradient-to-br from-green-400 to-green-700 flex items-center justify-center">
                    <span className="text-6xl">🏓</span>
                  </div>
                )}
                <div className="p-4">
                  <div className="flex items-start justify-between gap-2 mb-2">
                    <h3 className="font-black text-gray-900 dark:text-white text-base leading-tight group-hover:text-green-600 dark:group-hover:text-green-400 transition-colors">
                      {venue.name}
                    </h3>
                    <div className="text-right flex-shrink-0">
                      <p className="text-green-600 dark:text-green-400 font-black">
                        ₹{venue.startingRate}
                      </p>
                      <p className="text-gray-400 text-xs">/hr</p>
                    </div>
                  </div>
                  <p className="text-gray-500 dark:text-gray-400 text-sm truncate mb-3">
                    📍 {venue.address}
                  </p>
                  <div className="flex items-center justify-between pt-3 border-t border-gray-100 dark:border-gray-800">
                    <span className="text-xs text-gray-400 dark:text-gray-500">
                      🏟️ {venue.totalCourts} courts
                    </span>
                    {venue.availableCourts > 0 ? (
                      <span className="text-xs bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-800 px-2 py-0.5 rounded-full font-bold">
                        ✓ {venue.availableCourts} free
                      </span>
                    ) : (
                      <span className="text-xs bg-amber-50 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-800 px-2 py-0.5 rounded-full font-medium">
                        Check slots →
                      </span>
                    )}
                  </div>
                  <button className="mt-3 w-full bg-gray-900 dark:bg-gray-700 group-hover:bg-green-600 text-white py-2.5 rounded-xl transition-colors text-sm font-bold">
                    View & Book →
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default MarketplacePage;
