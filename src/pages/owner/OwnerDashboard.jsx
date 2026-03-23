import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../api/axiosInstance";

function OwnerDashboard() {
  const [venues, setVenues] = useState([]);
  const [selectedVenueId, setSelectedVenueId] = useState(null);
  const [bookings, setBookings] = useState([]);
  const [date, setDate] = useState(new Date().toISOString().split("T")[0]);
  const [loadingVenues, setLoadingVenues] = useState(true);
  const [loadingBookings, setLoadingBookings] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    fetchMyVenues();
  }, []);
  useEffect(() => {
    if (selectedVenueId) fetchBookings();
  }, [selectedVenueId, date]);

  const fetchMyVenues = async () => {
    try {
      const res = await api.get("/api/owner/venues");
      setVenues(res.data);
      if (res.data.length > 0) setSelectedVenueId(res.data[0].id);
    } catch {
      setError("Failed to load venues");
    } finally {
      setLoadingVenues(false);
    }
  };

  const fetchBookings = async () => {
    setLoadingBookings(true);
    try {
      const res = await api.get(
        `/api/owner/venues/${selectedVenueId}/bookings?date=${date}`,
      );
      setBookings(res.data);
    } catch {
      setError("Failed to load bookings");
    } finally {
      setLoadingBookings(false);
    }
  };

  const StatusBadge = ({ status }) => {
    const s = {
      CONFIRMED:
        "bg-green-50 dark:bg-green-900/40 text-green-700 dark:text-green-400 border border-green-200 dark:border-green-800",
      RESCHEDULED:
        "bg-yellow-50 dark:bg-yellow-900/40 text-yellow-700 dark:text-yellow-400 border border-yellow-200 dark:border-yellow-800",
    };
    return (
      <span
        className={`px-2.5 py-1 rounded-full text-xs font-semibold ${s[status] || "bg-gray-100 text-gray-600"}`}
      >
        {status}
      </span>
    );
  };

  const inputCls =
    "border border-gray-200 dark:border-gray-700 rounded-xl px-3 py-2 text-sm bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-green-500";

  if (loadingVenues)
    return (
      <div className="min-h-screen bg-gray-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-3 animate-bounce">🏓</div>
          <p className="text-gray-500 dark:text-gray-400">Loading venues...</p>
        </div>
      </div>
    );

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      {/* Header */}
      <div className="bg-gradient-to-r from-green-700 to-green-600 text-white">
        <div className="max-w-6xl mx-auto px-4 py-8">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h1 className="text-3xl font-black">Owner Dashboard</h1>
              <p className="text-green-100 text-sm mt-1">
                Manage your venues and bookings
              </p>
            </div>
            <Link
              to="/owner/venues/create"
              className="bg-white text-green-700 px-5 py-2.5 rounded-xl hover:bg-green-50 font-bold text-sm transition-colors shadow"
            >
              ➕ Add New Venue
            </Link>
          </div>

          {venues.length > 0 && (
            <div className="grid grid-cols-3 gap-4">
              {[
                { label: "Total Venues", value: venues.length },
                {
                  label: "Total Courts",
                  value: venues.reduce((s, v) => s + v.numCourts, 0),
                },
                {
                  label: "Bookings Today",
                  value: loadingBookings ? "..." : bookings.length,
                },
              ].map(({ label, value }) => (
                <div key={label} className="bg-white/15 rounded-xl p-4">
                  <p className="text-green-100 text-xs font-semibold uppercase tracking-wide">
                    {label}
                  </p>
                  <p className="text-4xl font-black text-white mt-1">{value}</p>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-4 py-8 space-y-8">
        {error && (
          <div className="bg-red-50 dark:bg-red-900/40 border border-red-200 dark:border-red-700 text-red-600 dark:text-red-400 p-3 rounded-xl text-sm">
            ⚠️ {error}
          </div>
        )}

        {venues.length === 0 ? (
          <div className="text-center py-24 bg-white dark:bg-gray-900 rounded-2xl border-2 border-dashed border-gray-200 dark:border-gray-700">
            <span className="text-7xl">🏟️</span>
            <p className="text-gray-700 dark:text-gray-200 font-bold text-xl mt-4 mb-2">
              No venues yet
            </p>
            <p className="text-gray-400 dark:text-gray-500 text-sm mb-6">
              Create your first venue to start accepting bookings
            </p>
            <Link
              to="/owner/venues/create"
              className="bg-green-600 text-white px-8 py-3 rounded-xl hover:bg-green-700 font-semibold text-sm transition-colors"
            >
              Create Your First Venue
            </Link>
          </div>
        ) : (
          <>
            {/* Bookings section */}
            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm overflow-hidden">
              <div className="p-5 border-b border-gray-100 dark:border-gray-800 flex flex-wrap items-center gap-3">
                <h2 className="font-bold text-gray-800 dark:text-white text-lg flex-1">
                  📅 Bookings
                </h2>
                <select
                  value={selectedVenueId || ""}
                  onChange={(e) => setSelectedVenueId(Number(e.target.value))}
                  className={inputCls}
                >
                  {venues.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.name}
                    </option>
                  ))}
                </select>
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className={inputCls}
                />
                {selectedVenueId && (
                  <Link
                    to={`/owner/venues/${selectedVenueId}/edit`}
                    className="bg-green-600 text-white px-4 py-2 rounded-xl text-sm font-semibold hover:bg-green-700 transition-colors"
                  >
                    ✏️ Edit Venue
                  </Link>
                )}
              </div>

              {loadingBookings ? (
                <div className="text-center py-12 text-gray-400 dark:text-gray-500">
                  Loading...
                </div>
              ) : bookings.length === 0 ? (
                <div className="text-center py-16">
                  <span className="text-5xl">📭</span>
                  <p className="text-gray-500 dark:text-gray-400 font-medium mt-3">
                    No bookings for {date}
                  </p>
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="min-w-full text-sm">
                    <thead>
                      <tr className="bg-gray-50 dark:bg-gray-800 border-b border-gray-100 dark:border-gray-700">
                        {[
                          "Court",
                          "Date",
                          "Time",
                          "Amount",
                          "Status",
                          "Booked At",
                        ].map((h) => (
                          <th
                            key={h}
                            className="px-5 py-3 text-left text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"
                          >
                            {h}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50 dark:divide-gray-800">
                      {bookings.map((b) => (
                        <tr
                          key={b.id}
                          className="hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                        >
                          <td className="px-5 py-4 font-semibold text-gray-800 dark:text-white">
                            {b.courtName}
                          </td>
                          <td className="px-5 py-4 text-gray-600 dark:text-gray-300">
                            {b.bookingDate}
                          </td>
                          <td className="px-5 py-4 text-gray-600 dark:text-gray-300">
                            {b.startTime} – {b.endTime}
                          </td>
                          <td className="px-5 py-4 font-bold text-green-600 dark:text-green-400">
                            ₹{b.amountPaid}
                          </td>
                          <td className="px-5 py-4">
                            <StatusBadge status={b.status} />
                          </td>
                          <td className="px-5 py-4 text-gray-400 dark:text-gray-500 text-xs">
                            {new Date(b.bookedAt).toLocaleString("en-IN")}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            {/* Venues grid */}
            <div>
              <h2 className="text-xl font-black text-gray-900 dark:text-white mb-5">
                My Venues
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
                {venues.map((v) => (
                  <div
                    key={v.id}
                    className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm p-5 hover:shadow-md transition-shadow"
                  >
                    <div className="flex items-start justify-between gap-2 mb-4">
                      <div className="flex-1 min-w-0">
                        <h3 className="font-bold text-gray-900 dark:text-white text-lg leading-tight">
                          {v.name}
                        </h3>
                        <p className="text-gray-400 dark:text-gray-500 text-sm mt-1 truncate">
                          📍 {v.address}
                        </p>
                      </div>
                      <div className="w-10 h-10 bg-green-100 dark:bg-green-900/40 rounded-xl flex items-center justify-center flex-shrink-0">
                        <span className="text-xl">🏓</span>
                      </div>
                    </div>
                    <div className="grid grid-cols-2 gap-2 mb-4">
                      <div className="bg-gray-50 dark:bg-gray-800 rounded-xl p-3 text-center">
                        <p className="text-xs text-gray-400 dark:text-gray-500 mb-1">
                          Courts
                        </p>
                        <p className="font-black text-gray-800 dark:text-white text-2xl">
                          {v.numCourts}
                        </p>
                      </div>
                      <div className="bg-green-50 dark:bg-green-900/30 rounded-xl p-3 text-center">
                        <p className="text-xs text-gray-400 dark:text-gray-500 mb-1">
                          From
                        </p>
                        <p className="font-black text-green-700 dark:text-green-400 text-2xl">
                          ₹{v.weekdayRate}
                        </p>
                      </div>
                    </div>
                    <p className="text-xs text-gray-400 dark:text-gray-500 mb-4">
                      🕐 {v.openingTime} – {v.closingTime}
                    </p>
                    <div className="flex gap-2">
                      <Link
                        to={`/owner/venues/${v.id}/edit`}
                        className="flex-1 text-center bg-green-600 text-white py-2 rounded-xl text-sm font-semibold hover:bg-green-700 transition-colors"
                      >
                        ✏️ Edit
                      </Link>
                      <button
                        onClick={() => {
                          setSelectedVenueId(v.id);
                          window.scrollTo({ top: 0, behavior: "smooth" });
                        }}
                        className="flex-1 text-center border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 py-2 rounded-xl text-sm font-semibold hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
                      >
                        📅 Bookings
                      </button>
                    </div>
                  </div>
                ))}

                <Link
                  to="/owner/venues/create"
                  className="bg-white dark:bg-gray-900 rounded-2xl border-2 border-dashed border-gray-200 dark:border-gray-700 p-5 flex flex-col items-center justify-center gap-3 hover:border-green-400 dark:hover:border-green-600 hover:bg-green-50 dark:hover:bg-green-900/20 transition-all group min-h-48"
                >
                  <div className="w-14 h-14 bg-gray-100 dark:bg-gray-800 rounded-2xl flex items-center justify-center group-hover:bg-green-100 dark:group-hover:bg-green-900/40 transition-colors">
                    <span className="text-3xl">➕</span>
                  </div>
                  <div className="text-center">
                    <p className="font-bold text-gray-500 dark:text-gray-400 group-hover:text-green-700 dark:group-hover:text-green-400 transition-colors">
                      Add New Venue
                    </p>
                    <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                      List another court
                    </p>
                  </div>
                </Link>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default OwnerDashboard;
