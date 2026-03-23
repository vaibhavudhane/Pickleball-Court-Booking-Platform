import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "../../api/axiosInstance";

function VenueCreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [form, setForm] = useState({
    name: "",
    address: "",
    description: "",
    numCourts: 1,
    openingTime: "06:00",
    closingTime: "23:00",
    weekdayRate: "",
    weekendRate: "",
    contactPhone: "",
    contactEmail: "",
  });

  const handleChange = (e) =>
    setForm((p) => ({ ...p, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      await api.post("/api/owner/venues", {
        ...form,
        numCourts: parseInt(form.numCourts),
        weekdayRate: parseFloat(form.weekdayRate),
        weekendRate: parseFloat(form.weekendRate),
      });
      setSuccess("Venue created successfully!");
      setTimeout(() => navigate("/owner/dashboard"), 1500);
    } catch (err) {
      const data = err.response?.data;
      if (data && typeof data === "object" && !data.message)
        setError(Object.values(data)[0]);
      else setError(data?.message || "Failed to create venue");
    } finally {
      setLoading(false);
    }
  };

  const inputCls =
    "w-full border border-gray-200 dark:border-gray-700 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-green-500 text-sm";

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-950">
      <div className="bg-gradient-to-r from-green-700 to-green-600 text-white">
        <div className="max-w-3xl mx-auto px-4 py-6 flex items-center gap-4">
          <Link
            to="/owner/dashboard"
            className="text-green-200 hover:text-white text-sm transition-colors"
          >
            ← Back
          </Link>
          <div>
            <h1 className="text-2xl font-black">Create New Venue</h1>
            <p className="text-green-100 text-sm">
              Courts are auto-created based on count
            </p>
          </div>
        </div>
      </div>

      <div className="max-w-3xl mx-auto px-4 py-8">
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-100 dark:border-gray-800 shadow-sm p-6">
          {error && (
            <div className="bg-red-50 dark:bg-red-900/40 border border-red-200 dark:border-red-700 text-red-600 dark:text-red-400 p-3 rounded-xl mb-4 text-sm">
              ⚠️ {error}
            </div>
          )}
          {success && (
            <div className="bg-green-50 dark:bg-green-900/40 border border-green-200 dark:border-green-700 text-green-700 dark:text-green-300 p-3 rounded-xl mb-4 text-sm">
              ✅ {success}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Venue Name *
              </label>
              <input
                type="text"
                name="name"
                value={form.name}
                onChange={handleChange}
                required
                placeholder="Smash Arena"
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Address *
              </label>
              <input
                type="text"
                name="address"
                value={form.address}
                onChange={handleChange}
                required
                placeholder="Baner Road, Pune, Maharashtra"
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Description
              </label>
              <textarea
                name="description"
                value={form.description}
                onChange={handleChange}
                rows={3}
                placeholder="Premium courts with parking and cafeteria..."
                className={inputCls + " resize-none"}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                Number of Courts *
              </label>
              <input
                type="number"
                name="numCourts"
                value={form.numCourts}
                onChange={handleChange}
                min={1}
                max={20}
                required
                className={inputCls}
              />
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-1">
                Courts are auto-named (Court 1, Court 2...)
              </p>
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                ["Opening Time *", "openingTime", "time"],
                ["Closing Time *", "closingTime", "time"],
              ].map(([label, name, type]) => (
                <div key={name}>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    {label}
                  </label>
                  <input
                    type={type}
                    name={name}
                    value={form[name]}
                    onChange={handleChange}
                    required
                    className={inputCls}
                  />
                </div>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-4">
              {[
                ["Weekday Rate (₹/hr) *", "weekdayRate", "500"],
                ["Weekend Rate (₹/hr) *", "weekendRate", "700"],
              ].map(([label, name, ph]) => (
                <div key={name}>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                    {label}
                  </label>
                  <input
                    type="number"
                    name={name}
                    value={form[name]}
                    onChange={handleChange}
                    required
                    min={1}
                    placeholder={ph}
                    className={inputCls}
                  />
                </div>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  Contact Phone
                </label>
                <input
                  type="text"
                  name="contactPhone"
                  value={form.contactPhone}
                  onChange={handleChange}
                  maxLength={10}
                  placeholder="9876543210"
                  className={inputCls}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
                  Contact Email
                </label>
                <input
                  type="email"
                  name="contactEmail"
                  value={form.contactEmail}
                  onChange={handleChange}
                  placeholder="venue@example.com"
                  className={inputCls}
                />
              </div>
            </div>
            <div className="flex gap-3 pt-2">
              <button
                type="submit"
                disabled={loading}
                className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white py-3 rounded-xl font-bold text-sm transition-colors"
              >
                {loading ? "Creating..." : "Create Venue ✓"}
              </button>
              <Link
                to="/owner/dashboard"
                className="flex-1 text-center border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 py-3 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 font-medium text-sm transition-colors"
              >
                Cancel
              </Link>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default VenueCreatePage;
