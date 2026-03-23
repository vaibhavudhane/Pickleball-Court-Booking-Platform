import { useState, useEffect } from "react";
import { useLocation, Link } from "react-router-dom";
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

const TIME_OPTIONS = Array.from({ length: 35 }, (_, i) => {
  const totalMins = 360 + i * 30;
  const h = Math.floor(totalMins / 60);
  const m = totalMins % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
});

function MyBookingsPage() {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const location = useLocation();

  const [showModal, setShowModal] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState(null);
  const [newDate, setNewDate] = useState(null);
  const [newStartTime, setNewStartTime] = useState("");
  const [newEndTime, setNewEndTime] = useState("");
  const [rescheduling, setRescheduling] = useState(false);
  const [rescheduleError, setRescheduleError] = useState("");
  const [rescheduleSuccess, setRescheduleSuccess] = useState("");

  const successMessage = location.state?.successMessage;

  useEffect(() => {
    fetchBookings();
  }, []);

  const fetchBookings = async () => {
    setLoading(true);
    try {
      const res = await api.get("/api/bookings/my");
      setBookings(res.data);
    } catch {
      setError("Failed to load bookings");
    } finally {
      setLoading(false);
    }
  };

  const canReschedule = (b) => {
    if (b.status === "RESCHEDULED") return false;
    return (
      new Date(`${b.bookingDate}T${b.startTime}`) >
      new Date(Date.now() + 12 * 3600000)
    );
  };

  const openModal = (booking) => {
    setSelectedBooking(booking);
    setNewDate(null);
    setNewStartTime("");
    setNewEndTime("");
    setRescheduleError("");
    setShowModal(true);
  };

  const toDateStr = (d) => {
    if (!d) return "";
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  };

  const handleReschedule = async () => {
    if (!newDate || !newStartTime || !newEndTime) {
      setRescheduleError("Please fill in all fields");
      return;
    }
    const toMins = (t) =>
      parseInt(t.split(":")[0]) * 60 + parseInt(t.split(":")[1]);
    if (toMins(newEndTime) <= toMins(newStartTime)) {
      setRescheduleError("End time must be after start time");
      return;
    }
    if (toMins(newEndTime) - toMins(newStartTime) < 60) {
      setRescheduleError("Minimum 1 hour duration");
      return;
    }
    setRescheduling(true);
    setRescheduleError("");
    try {
      await api.put(`/api/bookings/${selectedBooking.id}/reschedule`, {
        newDate: toDateStr(newDate),
        newStartTime,
        newEndTime,
      });
      setRescheduleSuccess("Booking rescheduled successfully!");
      setShowModal(false);
      fetchBookings();
      setTimeout(() => setRescheduleSuccess(""), 3000);
    } catch (err) {
      const data = err.response?.data;
      setRescheduleError(data?.message || "Reschedule failed");
    } finally {
      setRescheduling(false);
    }
  };

  const StatusBadge = ({ status }) => {
    const map = {
      CONFIRMED:
        "bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-300 border border-green-200 dark:border-green-800",
      RESCHEDULED:
        "bg-amber-100 dark:bg-amber-900/40 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-800",
    };
    return (
      <span
        className={`px-2.5 py-0.5 rounded-full text-xs font-bold ${map[status] || "bg-gray-100 text-gray-600"}`}
      >
        {status}
      </span>
    );
  };

  const selectCls =
    "w-full border-2 border-gray-200 dark:border-gray-600 rounded-xl px-3 py-2.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-white font-semibold text-sm focus:outline-none focus:border-green-500 cursor-pointer";

  if (loading)
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-3 animate-bounce">📅</div>
          <p className="text-gray-500 dark:text-gray-400">
            Loading bookings...
          </p>
        </div>
      </div>
    );

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-gray-950">
      <div className="max-w-4xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-black text-gray-900 dark:text-white mb-6">
          📅 My Bookings
        </h1>

        {successMessage && (
          <div className="bg-green-600 text-white p-4 rounded-xl mb-4 text-sm font-semibold flex items-center gap-2">
            ✅ {successMessage}
          </div>
        )}
        {rescheduleSuccess && (
          <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 text-green-700 dark:text-green-300 p-4 rounded-xl mb-4 text-sm">
            ✅ {rescheduleSuccess}
          </div>
        )}
        {error && (
          <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 text-red-600 p-3 rounded-xl mb-4 text-sm">
            ⚠️ {error}
          </div>
        )}

        {bookings.length === 0 ? (
          <div className="text-center py-20 bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm">
            <p className="text-5xl mb-4">📅</p>
            <p className="text-gray-700 dark:text-gray-200 font-bold text-xl mb-2">
              No bookings yet
            </p>
            <Link
              to="/marketplace"
              className="inline-block bg-green-600 text-white px-6 py-2.5 rounded-xl hover:bg-green-700 font-semibold text-sm mt-2"
            >
              Browse Venues →
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {bookings.map((b) => (
              <div
                key={b.id}
                className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm p-5 hover:shadow-md transition-shadow"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap mb-1.5">
                      <span className="font-black text-gray-900 dark:text-white">
                        {b.courtName}
                      </span>
                      <StatusBadge status={b.status} />
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 text-sm">
                      🏟️ {b.venueName}
                    </p>
                    <p className="text-gray-700 dark:text-gray-300 text-sm mt-1 font-medium">
                      📅 {b.bookingDate} &nbsp;·&nbsp; 🕐 {to12hr(b.startTime)}{" "}
                      – {to12hr(b.endTime)}
                    </p>
                    <p className="font-black text-green-600 dark:text-green-400 mt-1.5 text-lg">
                      ₹{b.amountPaid}
                    </p>
                    <p className="text-gray-400 dark:text-gray-500 text-xs mt-1">
                      Booked {new Date(b.bookedAt).toLocaleString("en-IN")}
                    </p>
                  </div>
                  <div className="flex-shrink-0">
                    {canReschedule(b) ? (
                      <button
                        onClick={() => openModal(b)}
                        className="bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 border border-blue-200 dark:border-blue-800 px-4 py-2 rounded-xl text-sm font-bold hover:bg-blue-100 dark:hover:bg-blue-900/50 transition-colors"
                      >
                        🔄 Reschedule
                      </button>
                    ) : (
                      <span className="text-xs text-gray-400 dark:text-gray-500 block text-right max-w-28">
                        {b.status === "RESCHEDULED"
                          ? "Already rescheduled"
                          : "< 12hrs — locked"}
                      </span>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Reschedule Modal */}
      {showModal && selectedBooking && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 px-4">
          <div className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-800 p-6 w-full max-w-md">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h2 className="text-lg font-black text-gray-900 dark:text-white">
                  🔄 Reschedule
                </h2>
                <p className="text-sm text-gray-500 dark:text-gray-400 mt-0.5">
                  {selectedBooking.courtName} @ {selectedBooking.venueName}
                </p>
              </div>
              <button
                onClick={() => setShowModal(false)}
                className="text-gray-400 hover:text-gray-600 text-2xl font-bold leading-none"
              >
                ×
              </button>
            </div>

            {/* Current booking info */}
            <div className="bg-gray-50 dark:bg-gray-800 rounded-xl p-3 mb-5 text-sm">
              <p className="text-xs font-bold text-gray-400 uppercase tracking-wide mb-1">
                Current Booking
              </p>
              <p className="text-gray-700 dark:text-gray-200 font-semibold">
                📅 {selectedBooking.bookingDate}
              </p>
              <p className="text-gray-700 dark:text-gray-200 font-semibold">
                🕐 {to12hr(selectedBooking.startTime)} –{" "}
                {to12hr(selectedBooking.endTime)}
              </p>
            </div>

            {rescheduleError && (
              <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 text-red-600 p-3 rounded-xl mb-4 text-sm flex items-center gap-2">
                ⚠️ {rescheduleError}
              </div>
            )}

            <div className="space-y-4">
              {/* Date picker */}
              <div>
                <label className="block text-sm font-bold text-gray-700 dark:text-gray-200 mb-1.5">
                  📅 New Date *
                </label>
                <DatePicker
                  selected={newDate}
                  onChange={(d) => setNewDate(d)}
                  minDate={new Date(Date.now() + 86400000)}
                  dateFormat="dd MMM yyyy"
                  calendarStartDay={1}
                  className="w-full border-2 border-gray-200 dark:border-gray-700 rounded-xl px-4 py-2.5 text-sm font-semibold text-gray-800 dark:text-white bg-white dark:bg-gray-800 focus:outline-none focus:border-green-500 cursor-pointer"
                  placeholderText="Select new date"
                />
              </div>

              {/* Time dropdowns */}
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-sm font-bold text-gray-700 dark:text-gray-200 mb-1.5">
                    🕐 Start Time *
                  </label>
                  <select
                    value={newStartTime}
                    onChange={(e) => {
                      setNewStartTime(e.target.value);
                      setNewEndTime("");
                    }}
                    className={selectCls}
                  >
                    <option value="">Select</option>
                    {TIME_OPTIONS.map((t) => (
                      <option key={t} value={t}>
                        {to12hr(t)}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-bold text-gray-700 dark:text-gray-200 mb-1.5">
                    🕐 End Time *
                  </label>
                  <select
                    value={newEndTime}
                    onChange={(e) => setNewEndTime(e.target.value)}
                    className={selectCls}
                    disabled={!newStartTime}
                  >
                    <option value="">Select</option>
                    {TIME_OPTIONS.filter((t) => {
                      if (!newStartTime) return false;
                      const toMins = (x) =>
                        parseInt(x.split(":")[0]) * 60 +
                        parseInt(x.split(":")[1]);
                      return toMins(t) - toMins(newStartTime) >= 60;
                    }).map((t) => (
                      <option key={t} value={t}>
                        {to12hr(t)}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Live price preview */}
              {newStartTime && newEndTime && (
                <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-800 rounded-xl p-3 flex items-center justify-between">
                  <span className="text-sm text-gray-600 dark:text-gray-300 font-medium">
                    {to12hr(newStartTime)} – {to12hr(newEndTime)}
                  </span>
                  <span className="font-black text-green-700 dark:text-green-400">
                    {(() => {
                      const toMins = (x) =>
                        parseInt(x.split(":")[0]) * 60 +
                        parseInt(x.split(":")[1]);
                      const mins = toMins(newEndTime) - toMins(newStartTime);
                      const h = Math.floor(mins / 60);
                      const m = mins % 60;
                      return `${h > 0 ? h + "h " : ""}${m > 0 ? m + "m" : ""}`;
                    })()}
                  </span>
                </div>
              )}

              <p className="text-xs text-gray-400 dark:text-gray-500">
                ⚠️ Must be within venue operating hours. Overlap check applies.
              </p>
            </div>

            <div className="flex gap-3 mt-5">
              <button
                onClick={handleReschedule}
                disabled={rescheduling}
                className="flex-1 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white py-3 rounded-xl font-black text-sm transition-colors"
              >
                {rescheduling ? "Rescheduling..." : "Confirm Reschedule"}
              </button>
              <button
                onClick={() => setShowModal(false)}
                className="flex-1 border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 py-3 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 font-medium text-sm"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default MyBookingsPage;
