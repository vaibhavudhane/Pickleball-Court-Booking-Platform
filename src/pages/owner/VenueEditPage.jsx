import { useState, useEffect } from "react";
import { useNavigate, useParams, Link } from "react-router-dom";
import api from "../../api/axiosInstance";

function VenueEditPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [loadingVenue, setLoadingVenue] = useState(true);
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
  const [photos, setPhotos] = useState([]);
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [uploadingPhotos, setUploadingPhotos] = useState(false);
  const [deletingPhotoId, setDeletingPhotoId] = useState(null);
  const [photoError, setPhotoError] = useState("");
  const [photoSuccess, setPhotoSuccess] = useState("");
  // ← Track if upload just completed to delay warning
  const [justUploaded, setJustUploaded] = useState(false);

  useEffect(() => {
    fetchVenue();
  }, [id]);

  const fetchVenue = async () => {
    try {
      const res = await api.get(`/api/venues/${id}`);
      const v = res.data;
      setForm({
        name: v.name || "",
        address: v.address || "",
        description: v.description || "",
        numCourts: v.numCourts || 1,
        openingTime: v.openingTime || "06:00",
        closingTime: v.closingTime || "23:00",
        weekdayRate: v.weekdayRate || "",
        weekendRate: v.weekendRate || "",
        contactPhone: v.contactPhone || "",
        contactEmail: v.contactEmail || "",
      });
      const urls = v.photoUrls || [];
      const ids = v.photoIds || [];
      setPhotos(urls.map((url, i) => ({ id: ids[i], url })));
    } catch {
      setError("Failed to load venue");
    } finally {
      setLoadingVenue(false);
    }
  };

  const handleChange = (e) =>
    setForm((p) => ({ ...p, [e.target.name]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      await api.put(`/api/owner/venues/${id}`, {
        ...form,
        numCourts: parseInt(form.numCourts),
        weekdayRate: parseFloat(form.weekdayRate),
        weekendRate: parseFloat(form.weekendRate),
      });
      setSuccess("Venue updated successfully!");
      setTimeout(() => navigate("/owner/dashboard"), 1500);
    } catch (err) {
      const data = err.response?.data;
      if (data && typeof data === "object" && !data.message)
        setError(Object.values(data)[0]);
      else setError(data?.message || "Failed to update venue");
    } finally {
      setLoading(false);
    }
  };

  const handleFileSelect = (e) => {
    setSelectedFiles(Array.from(e.target.files));
    setPhotoError("");
  };

  const handleUpload = async () => {
    if (selectedFiles.length === 0) {
      setPhotoError("Please select at least one photo");
      return;
    }
    if (photos.length + selectedFiles.length > 5) {
      setPhotoError(
        `Cannot upload — venue has ${photos.length} photos. Max is 5.`,
      );
      return;
    }
    setUploadingPhotos(true);
    setPhotoError("");
    setPhotoSuccess("");
    try {
      const formData = new FormData();
      selectedFiles.forEach((f) => formData.append("files", f));
      await api.post(`/api/owner/venues/${id}/photos`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setPhotoSuccess(
        `${selectedFiles.length} photo(s) uploaded successfully!`,
      );
      setSelectedFiles([]);
      setJustUploaded(true); // ← set flag so warning doesn't show immediately
      document.getElementById("photoInput").value = "";
      await fetchVenue();
      // Clear flag after 4 seconds so warning can appear if still at 5
      setTimeout(() => {
        setJustUploaded(false);
        setPhotoSuccess("");
      }, 4000);
    } catch (err) {
      setPhotoError(err.response?.data?.message || "Upload failed");
    } finally {
      setUploadingPhotos(false);
    }
  };

  const handleDelete = async (photoId) => {
    if (!window.confirm("Delete this photo permanently?")) return;
    setDeletingPhotoId(photoId);
    setPhotoError("");
    try {
      await api.delete(`/api/owner/venues/${id}/photos/${photoId}`);
      setPhotos((prev) => prev.filter((p) => p.id !== photoId));
      setJustUploaded(false);
      setPhotoSuccess("Photo deleted!");
      setTimeout(() => setPhotoSuccess(""), 3000);
    } catch (err) {
      setPhotoError(err.response?.data?.message || "Delete failed");
    } finally {
      setDeletingPhotoId(null);
    }
  };

  const inputCls =
    "w-full border border-gray-300 dark:border-gray-600 rounded-xl px-4 py-2.5 bg-white dark:bg-gray-800 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent text-sm transition-colors";

  const showMaxWarning = photos.length >= 5 && !justUploaded;

  if (loadingVenue)
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-3 animate-bounce">🏓</div>
          <p className="text-gray-500 dark:text-gray-400 font-medium">
            Loading venue...
          </p>
        </div>
      </div>
    );

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-gray-950">
      {/* Header */}
      <div className="bg-gradient-to-r from-green-700 to-green-600 text-white shadow-lg">
        <div className="max-w-3xl mx-auto px-4 py-5 flex items-center gap-4">
          <Link
            to="/owner/dashboard"
            className="text-green-200 hover:text-white transition-colors text-sm font-medium"
          >
            ← Back
          </Link>
          <div>
            <h1 className="text-xl font-black">Edit Venue</h1>
            <p className="text-green-100 text-xs mt-0.5">{form.name}</p>
          </div>
        </div>
      </div>

      <div className="max-w-3xl mx-auto px-4 py-6 space-y-5">
        {/* Photo Section */}
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between">
            <div>
              <h2 className="font-bold text-gray-800 dark:text-white flex items-center gap-2">
                📸 Venue Photos
              </h2>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">
                {photos.length}/5 uploaded · Hover photo to delete
              </p>
            </div>
            <div
              className={`text-xs px-2.5 py-1 rounded-full font-bold ${
                photos.length >= 5
                  ? "bg-red-100 dark:bg-red-900/40 text-red-600 dark:text-red-400"
                  : "bg-green-100 dark:bg-green-900/40 text-green-700 dark:text-green-400"
              }`}
            >
              {5 - photos.length} free
            </div>
          </div>

          <div className="p-5">
            {/* 5-slot photo grid */}
            <div className="grid grid-cols-5 gap-2.5 mb-5">
              {Array.from({ length: 5 }).map((_, i) => {
                const photo = photos[i];
                return photo ? (
                  <div key={photo.id} className="relative group aspect-square">
                    <img
                      src={`${import.meta.env.VITE_API_URL}${photo.url}`}
                      alt={`Photo ${i + 1}`}
                      className="w-full h-full object-cover rounded-xl border-2 border-gray-200 dark:border-gray-700"
                      onError={(e) => {
                        e.target.style.display = "none";
                        e.target.nextSibling.style.display = "flex";
                      }}
                    />
                    {/* Fallback */}
                    <div
                      className="w-full h-full rounded-xl bg-gray-100 dark:bg-gray-800 items-center justify-center text-gray-400 text-xs absolute inset-0"
                      style={{ display: "none" }}
                    >
                      📷 {i + 1}
                    </div>
                    {/* Delete overlay */}
                    <div className="absolute inset-0 bg-black/0 group-hover:bg-black/50 rounded-xl transition-all flex items-center justify-center">
                      <button
                        onClick={() => handleDelete(photo.id)}
                        disabled={deletingPhotoId === photo.id}
                        className="opacity-0 group-hover:opacity-100 bg-red-500 hover:bg-red-600 text-white w-8 h-8 rounded-full flex items-center justify-center transition-all text-sm shadow-lg"
                      >
                        {deletingPhotoId === photo.id ? "⟳" : "🗑️"}
                      </button>
                    </div>
                    <div className="absolute top-1 left-1 w-5 h-5 bg-green-600 text-white text-xs rounded-full flex items-center justify-center font-bold">
                      {i + 1}
                    </div>
                  </div>
                ) : (
                  <div
                    key={`e-${i}`}
                    className="aspect-square rounded-xl border-2 border-dashed border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 flex flex-col items-center justify-center gap-1"
                  >
                    <span className="text-xl text-gray-300 dark:text-gray-600">
                      +
                    </span>
                    <span className="text-xs text-gray-300 dark:text-gray-600">
                      {i + 1}
                    </span>
                  </div>
                );
              })}
            </div>

            {/* Upload */}
            {photos.length < 5 && (
              <div className="bg-gray-50 dark:bg-gray-800 rounded-xl p-4 space-y-3">
                <p className="text-xs font-semibold text-gray-600 dark:text-gray-300 uppercase tracking-wide">
                  Add Photos ({5 - photos.length} slots remaining)
                </p>
                <div className="flex flex-wrap gap-3 items-center">
                  <label className="cursor-pointer flex items-center gap-2 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 hover:border-green-500 dark:hover:border-green-500 rounded-xl px-4 py-2 text-sm text-gray-700 dark:text-gray-200 font-medium transition-colors">
                    📁 Choose Files
                    <input
                      id="photoInput"
                      type="file"
                      accept=".jpg,.jpeg,.png"
                      multiple
                      onChange={handleFileSelect}
                      className="hidden"
                    />
                  </label>
                  {selectedFiles.length > 0 && (
                    <>
                      <span className="text-xs text-gray-500 dark:text-gray-400 flex-1 truncate">
                        {selectedFiles.length} file(s) selected
                      </span>
                      <button
                        onClick={handleUpload}
                        disabled={uploadingPhotos}
                        className="bg-green-600 text-white px-4 py-2 rounded-xl text-sm font-bold hover:bg-green-700 disabled:opacity-50 transition-colors"
                      >
                        {uploadingPhotos ? "Uploading..." : "Upload ↑"}
                      </button>
                      <button
                        onClick={() => {
                          setSelectedFiles([]);
                          document.getElementById("photoInput").value = "";
                        }}
                        className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 text-sm font-medium"
                      >
                        ✕
                      </button>
                    </>
                  )}
                </div>
              </div>
            )}

            {/* Max warning — only show if NOT just uploaded */}
            {showMaxWarning && (
              <div className="mt-3 bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-700 text-amber-700 dark:text-amber-300 p-3 rounded-xl text-xs flex items-start gap-2">
                <span>⚠️</span>
                <span>
                  Maximum 5 photos reached. Hover over a photo and click 🗑️ to
                  delete before uploading more.
                </span>
              </div>
            )}

            {photoError && (
              <div className="mt-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 text-red-600 dark:text-red-400 p-3 rounded-xl text-xs flex items-start gap-2">
                <span>⚠️</span>
                <span>{photoError}</span>
              </div>
            )}
            {photoSuccess && (
              <div className="mt-3 bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 text-green-700 dark:text-green-400 p-3 rounded-xl text-xs flex items-center gap-2">
                <span>✅</span>
                <span>{photoSuccess}</span>
              </div>
            )}
          </div>
        </div>

        {/* Venue Details Form */}
        <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm overflow-hidden">
          <div className="px-5 py-4 border-b border-gray-100 dark:border-gray-800">
            <h2 className="font-bold text-gray-800 dark:text-white">
              🏟️ Venue Details
            </h2>
          </div>
          <div className="p-5">
            {error && (
              <div className="bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 text-red-600 dark:text-red-400 p-3 rounded-xl mb-4 text-sm">
                ⚠️ {error}
              </div>
            )}
            {success && (
              <div className="bg-green-50 dark:bg-green-900/30 border border-green-200 dark:border-green-700 text-green-700 dark:text-green-400 p-3 rounded-xl mb-4 text-sm">
                ✅ {success}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
                  Venue Name *
                </label>
                <input
                  type="text"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  required
                  className={inputCls}
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
                  Address *
                </label>
                <input
                  type="text"
                  name="address"
                  value={form.address}
                  onChange={handleChange}
                  required
                  className={inputCls}
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
                  Description
                </label>
                <textarea
                  name="description"
                  value={form.description}
                  onChange={handleChange}
                  rows={3}
                  className={inputCls + " resize-none"}
                />
              </div>
              <div>
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
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
                  Increasing this will auto-create new courts
                </p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                {[
                  ["Opening Time *", "openingTime"],
                  ["Closing Time *", "closingTime"],
                ].map(([label, name]) => (
                  <div key={name}>
                    <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
                      {label}
                    </label>
                    <input
                      type="time"
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
                  ["Weekday Rate (₹/hr) *", "weekdayRate"],
                  ["Weekend Rate (₹/hr) *", "weekendRate"],
                ].map(([label, name]) => (
                  <div key={name}>
                    <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
                      {label}
                    </label>
                    <input
                      type="number"
                      name={name}
                      value={form[name]}
                      onChange={handleChange}
                      required
                      min={1}
                      className={inputCls}
                    />
                  </div>
                ))}
              </div>
              <div className="grid grid-cols-2 gap-4">
                {[
                  ["Contact Phone", "contactPhone", "text"],
                  ["Contact Email", "contactEmail", "email"],
                ].map(([label, name, type]) => (
                  <div key={name}>
                    <label className="block text-sm font-semibold text-gray-700 dark:text-gray-200 mb-1.5">
                      {label}
                    </label>
                    <input
                      type={type}
                      name={name}
                      value={form[name]}
                      onChange={handleChange}
                      className={inputCls}
                    />
                  </div>
                ))}
              </div>
              <div className="flex gap-3 pt-2">
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 text-white py-2.5 rounded-xl font-bold text-sm transition-colors"
                >
                  {loading ? "Saving..." : "Save Changes ✓"}
                </button>
                <Link
                  to="/owner/dashboard"
                  className="flex-1 text-center border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 py-2.5 rounded-xl hover:bg-gray-50 dark:hover:bg-gray-800 font-medium text-sm transition-colors"
                >
                  Cancel
                </Link>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default VenueEditPage;
