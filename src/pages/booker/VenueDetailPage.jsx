import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import api from "../../api/axiosInstance";

const to12 = (t) => {
  if (!t) return "";
  const [h, m] = t.split(":").map(Number);
  return `${h % 12 || 12}:${String(m).padStart(2, "0")} ${h >= 12 ? "PM" : "AM"}`;
};
const toMins = (t) => {
  if (!t) return 0;
  const [h, m] = t.split(":").map(Number);
  return h * 60 + m;
};
const fromMins = (n) =>
  `${String(Math.floor(n / 60)).padStart(2, "0")}:${String(n % 60).padStart(2, "0")}`;
const fmtDur = (mins) => {
  const h = Math.floor(mins / 60),
    m = mins % 60;
  return `${h ? h + "h " : ""}${m ? m + "m" : ""}`.trim();
};

const PX_PER_MIN = 1.2;
const HALF_MINUTES = [0, 30];
const HOURS = Array.from({ length: 17 }, (_, i) => i + 6);

export default function VenueDetailPage() {
  const { venueId } = useParams();
  const navigate = useNavigate();

  const [venue, setVenue] = useState(null);
  const [avail, setAvail] = useState(null);
  const [selDate, setSelDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1);
    return d;
  });
  const [loadingVenue, setLoadingVenue] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(false);
  const [err, setErr] = useState("");
  const [selected, setSelected] = useState(new Map());
  const [config, setConfig] = useState(new Map());
  const [adding, setAdding] = useState(false);
  const [cartMsg, setCartMsg] = useState("");
  const [cartCount, setCartCount] = useState(0);
  const gridRef = useRef(null);

  const dateStr = selDate
    ? `${selDate.getFullYear()}-${String(selDate.getMonth() + 1).padStart(2, "0")}-${String(selDate.getDate()).padStart(2, "0")}`
    : "";

  const isToday = useCallback(() => {
    if (!selDate) return false;
    return selDate.toDateString() === new Date().toDateString();
  }, [selDate]);

  useEffect(() => {
    fetchVenue();
  }, [venueId]);
  useEffect(() => {
    if (dateStr) {
      setSelected(new Map());
      setConfig(new Map());
      setAvail(null);
      fetchSlots();
    }
  }, [dateStr, venueId]);

  const fetchVenue = async () => {
    try {
      const r = await api.get(`/api/venues/${venueId}`);
      setVenue(r.data);
    } catch {
      setErr("Failed to load venue");
    } finally {
      setLoadingVenue(false);
    }
  };

  const fetchSlots = async () => {
    setLoadingSlots(true);
    setErr("");
    try {
      const r = await api.get(
        `/api/venues/${venueId}/availability?date=${dateStr}`,
      );
      setAvail(r.data);
    } catch (e) {
      setErr(e.response?.data?.message || "Failed to load availability");
    } finally {
      setLoadingSlots(false);
    }
  };

  const maxEnd = useCallback(
    (courtId, startTime) => {
      if (!avail) return startTime;
      const court = avail.courts.find((c) => c.courtId === courtId);
      if (!court) return startTime;
      const idx = court.slots.findIndex((s) => s.startTime === startTime);
      if (idx === -1) return startTime;
      let end = court.slots[idx].endTime;
      for (let i = idx + 1; i < court.slots.length; i++) {
        if (court.slots[i].status !== "AVAILABLE") break;
        end = court.slots[i].endTime;
      }
      return end;
    },
    [avail],
  );

  const toggleSlot = (court, slot) => {
    if (slot.status !== "AVAILABLE") return;
    const key = `${court.courtId}::${slot.startTime}`;
    setSelected((prev) => {
      const next = new Map(prev);
      if (next.has(key)) {
        next.delete(key);
        setConfig((c) => {
          const cc = new Map(c);
          cc.delete(key);
          return cc;
        });
      } else {
        const mEnd = maxEnd(court.courtId, slot.startTime);
        const defEndMins = Math.min(toMins(slot.startTime) + 60, toMins(mEnd));
        next.set(key, {
          courtId: court.courtId,
          courtName: court.courtName,
          startTime: slot.startTime,
          maxEndTime: mEnd,
        });
        setConfig((c) => {
          const cc = new Map(c);
          cc.set(key, {
            endHour: Math.floor(defEndMins / 60),
            endMin: defEndMins % 60,
          });
          return cc;
        });
      }
      return next;
    });
  };

  const calcPrice = useCallback(
    (start, end) => {
      if (!venue || !start || !end) return 0;
      const mins = toMins(end) - toMins(start);
      if (mins <= 0) return 0;
      const weekend = [0, 6].includes(selDate?.getDay());
      const rate = parseFloat(weekend ? venue.weekendRate : venue.weekdayRate);
      return (rate * mins) / 60;
    },
    [venue, selDate],
  );

  const getEndTime = (key) => {
    const c = config.get(key);
    if (!c) return "";
    return fromMins(c.endHour * 60 + c.endMin);
  };

  const handleAddToCart = async () => {
    const items = [];
    let valid = true;
    for (const [key, sel] of selected) {
      const end = getEndTime(key);
      const diff = toMins(end) - toMins(sel.startTime);
      if (diff < 30) {
        alert(
          `${sel.courtName}: End time must be at least 30 min after start.`,
        );
        valid = false;
        break;
      }
      if (toMins(end) > toMins(sel.maxEndTime)) {
        alert(`${sel.courtName}: End time exceeds available window.`);
        valid = false;
        break;
      }
      items.push({
        courtId: sel.courtId,
        venueId: parseInt(venueId),
        date: dateStr,
        startTime: sel.startTime,
        endTime: end,
      });
    }
    if (!valid || items.length === 0) return;
    setAdding(true);
    setCartMsg("");
    try {
      await api.post("/api/cart/add", { items });
      const totalP = items.reduce(
        (s, it) => s + calcPrice(it.startTime, it.endTime),
        0,
      );
      setCartMsg(
        `${items.length} slot${items.length > 1 ? "s" : ""} added! ₹${totalP.toFixed(2)}`,
      );
      setCartCount((c) => c + items.length);
      setSelected(new Map());
      setConfig(new Map());
      setAvail(null);
      await fetchSlots();
    } catch (e) {
      alert(e.response?.data?.message || "Failed to add to cart");
    } finally {
      setAdding(false);
    }
  };

  const closeM = venue ? toMins(venue.closingTime) : 0;
  const openM = venue ? toMins(venue.openingTime) : 0;
  const totalMins = closeM - openM;
  const tlHeight = totalMins * PX_PER_MIN;

  const hourLabels = venue
    ? Array.from({ length: Math.ceil(totalMins / 60) + 1 }, (_, i) => {
        const m = openM + i * 60;
        return m <= closeM ? { m, label: to12(fromMins(m)) } : null;
      }).filter(Boolean)
    : [];

  const totalSel = selected.size;
  const totalPrice = Array.from(selected.keys()).reduce((s, key) => {
    const sel = selected.get(key);
    const end = getEndTime(key);
    return s + calcPrice(sel.startTime, end);
  }, 0);

  const nowTop = (() => {
    if (!isToday() || !venue) return null;
    const n = new Date();
    const nm = n.getHours() * 60 + n.getMinutes();
    if (nm < openM || nm > closeM) return null;
    return (nm - openM) * PX_PER_MIN;
  })();

  if (loadingVenue)
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-gray-950 flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-green-500 to-green-700 flex items-center justify-center mx-auto shadow-xl animate-bounce">
            <span className="text-3xl">🏓</span>
          </div>
          <p className="mt-4 text-gray-500 dark:text-gray-400 font-semibold">
            Loading venue…
          </p>
        </div>
      </div>
    );

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-gray-950 pb-32">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Sora:wght@400;500;600;700;800;900&display=swap');
        .vdp { font-family: 'Sora', sans-serif; }

        /* ── Available slot ── */
        .s-avail {
          cursor: pointer;
          background: #f0fdf4;
          border: 1.5px solid #bbf7d0;
          border-radius: 10px;
          color: #15803d;
          transition: all .16s ease;
        }
        .s-avail:hover {
          background: #dcfce7;
          border-color: #4ade80;
          box-shadow: 0 2px 12px rgba(34,197,94,.18);
          transform: translateY(-1px);
        }

        /* ── Selected slot ── */
        .s-sel {
          cursor: pointer;
          background: linear-gradient(145deg, #2563eb, #1d4ed8);
          border: 2px solid #93c5fd;
          border-radius: 10px;
          color: #fff;
          box-shadow: 0 4px 16px rgba(37,99,235,.30);
          animation: selPop .18s cubic-bezier(.34,1.56,.64,1);
        }
        @keyframes selPop { from { transform: scale(.94) } to { transform: scale(1) } }

        /* ── Booked slot ── */
        .s-booked {
          cursor: not-allowed;
          background: #fef9f9;
          border: 1.5px solid #fecaca;
          border-radius: 10px;
          color: #ef4444;
        }

        /* ── Unavailable / past slot ── */
        .s-unav {
          cursor: not-allowed;
          background: #f8fafc;
          border: 1.5px dashed #e2e8f0;
          border-radius: 10px;
          color: #94a3b8;
        }

        /* ── Dark mode ── */
        .dark .s-avail {
          background: #052e16;
          border-color: #166534;
          color: #86efac;
        }
        .dark .s-avail:hover {
          background: #14532d;
          border-color: #22c55e;
          box-shadow: 0 2px 14px rgba(34,197,94,.20);
        }
        .dark .s-sel {
          background: linear-gradient(145deg, #1e3a8a, #1d4ed8);
          border-color: #60a5fa;
          box-shadow: 0 4px 16px rgba(37,99,235,.35);
        }
        .dark .s-booked {
          background: #1c0a0a;
          border-color: #7f1d1d;
          color: #fca5a5;
        }
        .dark .s-unav {
          background: #0f172a;
          border-color: #1e293b;
          color: #334155;
        }

        /* slot wrapper */
        .tl-slot {
          position: absolute; left: 4px; right: 4px;
          display: flex; flex-direction: column;
          align-items: center; justify-content: center;
          padding: 4px 6px; box-sizing: border-box; overflow: hidden;
          gap: 2px;
        }

        /* grid lines — light */
        .g-hour { position: absolute; left: 0; right: 0; border-top: 1px solid rgba(226,232,240,.8); pointer-events: none; }
        .g-half { position: absolute; left: 0; right: 0; border-top: 1px dashed rgba(226,232,240,.4); pointer-events: none; }
        /* grid lines — dark */
        .dark .g-hour { border-color: rgba(30,41,59,.9); }
        .dark .g-half { border-color: rgba(30,41,59,.5); }

        /* time axis */
        .time-axis-bg { background: #1e293b; }
        .dark .time-axis-bg { background: #0f172a; }

        /* court column */
        .court-col-bg { background: white; }
        .dark .court-col-bg { background: #111827; }
        .dark .court-col-sel { background: rgba(37,99,235,.06) !important; }

        /* now line */
        .now-bar {
          position: absolute; left: 0; right: 0; height: 2px;
          background: linear-gradient(90deg, transparent, #22c55e 10%, #22c55e 90%, transparent);
          z-index: 20; pointer-events: none;
        }
        .now-bar::before {
          content: ''; position: absolute; left: 0; top: -4px;
          width: 10px; height: 10px; background: #22c55e;
          border-radius: 50%; box-shadow: 0 0 8px rgba(34,197,94,.5);
        }

        /* cart bar */
        .cart-bar {
          position: fixed; bottom: 0; left: 0; right: 0; z-index: 50;
          background: rgba(255,255,255,.96); backdrop-filter: blur(16px);
          border-top: 1px solid #e5e7eb;
          box-shadow: 0 -4px 24px rgba(0,0,0,.07);
        }
        .dark .cart-bar {
          background: rgba(15,23,42,.97);
          border-color: #1e293b;
        }

        .add-btn {
          background: linear-gradient(135deg, #16a34a, #15803d);
          box-shadow: 0 3px 14px rgba(22,163,74,.35);
          transition: all .14s;
          color: white; font-weight: 800;
        }
        .add-btn:hover { transform: translateY(-1px); box-shadow: 0 5px 20px rgba(22,163,74,.42); }
        .add-btn:disabled { opacity: .45; cursor: not-allowed; transform: none; box-shadow: none; }

        .react-datepicker-wrapper { width: 100%; }
        .react-datepicker { font-family: 'Sora',sans-serif !important; border-radius: 16px !important; overflow: hidden; box-shadow: 0 16px 48px rgba(0,0,0,.14) !important; }
        .react-datepicker__header { background: #16a34a !important; border: none !important; padding: 12px 0 8px !important; }
        .react-datepicker__current-month, .react-datepicker__day-name { color: white !important; font-weight: 700 !important; }
        .react-datepicker__day--selected { background: #16a34a !important; border-radius: 8px !important; color: white !important; }
        .react-datepicker__day:hover { border-radius: 8px !important; background: #f0fdf4 !important; }
        .react-datepicker__navigation-icon::before { border-color: white !important; }
      `}</style>

      <div className="vdp">
        {/* ── Venue Header ── */}
        <div className="bg-white dark:bg-gray-900 border-b border-gray-100 dark:border-gray-800 shadow-sm">
          <div className="max-w-6xl mx-auto px-4 py-5">
            <Link
              to="/marketplace"
              className="inline-flex items-center gap-1 text-sm text-gray-400 hover:text-green-600 dark:hover:text-green-400 font-semibold transition-colors mb-4 group"
            >
              <span className="group-hover:-translate-x-0.5 transition-transform inline-block">
                ←
              </span>
              Back to Venues
            </Link>
            <div className="flex flex-wrap gap-6 justify-between">
              <div className="flex-1 min-w-0">
                <h1 className="text-2xl font-black tracking-tight text-gray-900 dark:text-white">
                  {venue.name}
                </h1>
                <p className="text-gray-400 text-sm mt-1 flex items-center gap-1">
                  📍 {venue.address}
                </p>
                {venue.description && (
                  <p className="text-gray-500 dark:text-gray-400 text-sm mt-2 max-w-xl leading-relaxed">
                    {venue.description}
                  </p>
                )}
                {(venue.contactPhone || venue.contactEmail) && (
                  <div className="flex gap-4 mt-2 text-sm text-gray-400">
                    {venue.contactPhone && <span>📞 {venue.contactPhone}</span>}
                    {venue.contactEmail && <span>✉️ {venue.contactEmail}</span>}
                  </div>
                )}
              </div>
              <div className="bg-gradient-to-br from-green-600 to-green-700 rounded-2xl p-4 text-white shadow-lg shadow-green-200/40 dark:shadow-green-900/25 min-w-40 flex-shrink-0">
                <p className="text-green-200 text-xs font-semibold uppercase tracking-wider">
                  From
                </p>
                <p className="text-3xl font-black mt-0.5">
                  ₹{venue.weekdayRate}
                </p>
                <p className="text-green-200 text-xs">/hr weekday</p>
                <div className="mt-2 pt-2 border-t border-green-500/40 text-xs space-y-0.5">
                  <div className="flex justify-between">
                    <span className="text-green-200">Weekend</span>
                    <span className="font-bold">₹{venue.weekendRate}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-green-200">Hours</span>
                    <span className="font-bold">
                      {to12(venue.openingTime)}–{to12(venue.closingTime)}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-green-200">Courts</span>
                    <span className="font-bold">{venue.numCourts}</span>
                  </div>
                </div>
              </div>
            </div>
            {venue.photoUrls?.length > 0 && (
              <div className="flex gap-2 mt-4 overflow-x-auto pb-1">
                {venue.photoUrls.map((url, i) => (
                  <img
                    key={i}
                    src={`${import.meta.env.VITE_API_URL}${url}`}
                    alt=""
                    className="h-20 w-32 object-cover rounded-xl flex-shrink-0 border border-gray-100 dark:border-gray-800"
                  />
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="max-w-6xl mx-auto px-4 py-6">
          {/* ── Controls ── */}
          <div className="flex flex-wrap items-end justify-between gap-4 mb-5">
            <div className="flex flex-wrap items-end gap-4">
              <div>
                <label className="block text-xs font-bold text-gray-400 uppercase tracking-wider mb-1.5">
                  📅 Select Date
                </label>
                <DatePicker
                  selected={selDate}
                  onChange={(d) => {
                    setSelDate(d);
                    setSelected(new Map());
                    setConfig(new Map());
                    setAvail(null);
                  }}
                  filterDate={(d) => {
                    const t = new Date();
                    t.setHours(0, 0, 0, 0);
                    return d >= t;
                  }}
                  dateFormat="EEE, dd MMM yyyy"
                  calendarStartDay={1}
                  className="border-2 border-gray-200 dark:border-gray-700 hover:border-green-400 focus:border-green-500 rounded-xl px-4 py-2.5 text-sm font-bold text-gray-800 dark:text-white bg-white dark:bg-gray-800 outline-none cursor-pointer w-52 transition-colors"
                />
              </div>
              <div className="flex items-center gap-3 pb-1 flex-wrap">
                {[
                  { label: "Available", dot: "bg-green-400" },
                  { label: "Selected", dot: "bg-blue-500" },
                  { label: "Booked", dot: "bg-red-300" },
                  {
                    label: "Unavailable",
                    dot: "bg-slate-300 dark:bg-slate-600",
                  },
                ].map(({ label, dot }) => (
                  <div key={label} className="flex items-center gap-1.5">
                    <div
                      className={`w-2.5 h-2.5 rounded-full ${dot} flex-shrink-0`}
                    />
                    <span className="text-xs text-gray-500 dark:text-gray-400 font-medium">
                      {label}
                    </span>
                  </div>
                ))}
              </div>
            </div>

            {!loadingSlots && avail && (
              <div className="flex items-center gap-2 bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-700 rounded-full px-4 py-2 shadow-sm">
                <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                <span className="text-sm font-bold text-gray-700 dark:text-gray-200">
                  {avail.courts.reduce(
                    (s, c) =>
                      s +
                      c.slots.filter((sl) => sl.status === "AVAILABLE").length,
                    0,
                  )}{" "}
                  slots available
                </span>
              </div>
            )}
          </div>

          {/* ── Cart success ── */}
          {cartMsg && (
            <div className="mb-4 flex items-center justify-between bg-green-600 text-white rounded-2xl p-4 shadow-lg">
              <span className="font-bold text-sm">✅ {cartMsg}</span>
              <div className="flex gap-2">
                <button
                  onClick={() => setCartMsg("")}
                  className="w-7 h-7 flex items-center justify-center rounded-lg hover:bg-white/10 text-xl font-bold"
                >
                  ×
                </button>
                <button
                  onClick={() => navigate("/cart")}
                  className="bg-white text-green-700 px-4 py-1.5 rounded-xl text-sm font-black hover:bg-green-50"
                >
                  Cart ({cartCount}) →
                </button>
              </div>
            </div>
          )}

          {err && (
            <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 text-red-600 p-3 rounded-xl text-sm">
              ⚠️ {err}
            </div>
          )}

          {/* ── Selected slots config panel ── */}
          {totalSel > 0 && (
            <div className="mb-5 bg-white dark:bg-gray-900 border border-blue-200 dark:border-blue-800 rounded-2xl overflow-hidden shadow-md">
              <div className="bg-gradient-to-r from-blue-600 to-blue-500 px-5 py-3 flex items-center justify-between">
                <p className="font-black text-white text-sm">
                  ⚡ {totalSel} Slot{totalSel > 1 ? "s" : ""} Selected — Set End
                  Times
                </p>
                <button
                  onClick={() => {
                    setSelected(new Map());
                    setConfig(new Map());
                  }}
                  className="text-blue-100 hover:text-white text-xs font-semibold border border-blue-400/40 px-3 py-1 rounded-lg hover:bg-white/10 transition-colors"
                >
                  Clear all
                </button>
              </div>
              <div className="p-4 space-y-2">
                {Array.from(selected.entries()).map(([key, sel]) => {
                  const cfg = config.get(key) || { endHour: 7, endMin: 0 };
                  const endT = fromMins(cfg.endHour * 60 + cfg.endMin);
                  const diff = toMins(endT) - toMins(sel.startTime);
                  const maxEndM = Math.min(toMins(sel.maxEndTime), closeM);
                  const isValid = diff >= 30 && toMins(endT) <= maxEndM;
                  const slotPrice = isValid
                    ? calcPrice(sel.startTime, endT)
                    : 0;
                  const validHours = HOURS.filter((h) => {
                    const minEnd = toMins(sel.startTime) + 30;
                    return (
                      h >= Math.floor(minEnd / 60) &&
                      h <= Math.floor(maxEndM / 60)
                    );
                  });
                  const validMins = HALF_MINUTES.filter((m) => {
                    const em = cfg.endHour * 60 + m;
                    return em >= toMins(sel.startTime) + 30 && em <= maxEndM;
                  });
                  return (
                    <div
                      key={key}
                      className={`rounded-xl border-2 p-3 transition-colors ${
                        isValid
                          ? "border-green-100 dark:border-green-900 bg-green-50/40 dark:bg-green-900/10"
                          : "border-red-100 dark:border-red-900 bg-red-50/40 dark:bg-red-900/10"
                      }`}
                    >
                      <div className="flex flex-wrap items-center gap-3">
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <div className="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0" />
                          <span className="text-xs font-bold text-gray-700 dark:text-gray-200">
                            {sel.courtName}
                          </span>
                          <span className="text-gray-300 dark:text-gray-600 text-xs">
                            ·
                          </span>
                          <span className="text-xs font-bold text-green-600 dark:text-green-400 bg-green-50 dark:bg-green-900/30 px-2 py-0.5 rounded-lg border border-green-100 dark:border-green-800">
                            {to12(sel.startTime)}
                          </span>
                          <span className="text-gray-400 text-xs">→</span>
                        </div>
                        <div className="flex gap-1.5 items-center">
                          <select
                            value={cfg.endHour}
                            onChange={(e) => {
                              const h = parseInt(e.target.value);
                              const minEnd = toMins(sel.startTime) + 30;
                              const firstMin =
                                HALF_MINUTES.find(
                                  (m) =>
                                    h * 60 + m >= minEnd &&
                                    h * 60 + m <= maxEndM,
                                ) ?? 0;
                              setConfig((c) => {
                                const cc = new Map(c);
                                cc.set(key, { endHour: h, endMin: firstMin });
                                return cc;
                              });
                            }}
                            className="border border-gray-200 dark:border-gray-600 rounded-lg px-2 py-1.5 text-xs font-bold bg-white dark:bg-gray-800 text-gray-800 dark:text-white focus:outline-none focus:border-blue-400 cursor-pointer"
                          >
                            {validHours.map((h) => (
                              <option key={h} value={h}>
                                {h % 12 || 12} {h >= 12 ? "PM" : "AM"}
                              </option>
                            ))}
                          </select>
                          <select
                            value={cfg.endMin}
                            onChange={(e) =>
                              setConfig((c) => {
                                const cc = new Map(c);
                                cc.set(key, {
                                  ...cfg,
                                  endMin: parseInt(e.target.value),
                                });
                                return cc;
                              })
                            }
                            className="border border-gray-200 dark:border-gray-600 rounded-lg px-2 py-1.5 text-xs font-bold bg-white dark:bg-gray-800 text-gray-800 dark:text-white focus:outline-none focus:border-blue-400 cursor-pointer w-16"
                          >
                            {validMins.length > 0 ? (
                              validMins.map((m) => (
                                <option key={m} value={m}>
                                  {String(m).padStart(2, "0")}
                                </option>
                              ))
                            ) : (
                              <option value={cfg.endMin}>
                                {String(cfg.endMin).padStart(2, "0")}
                              </option>
                            )}
                          </select>
                        </div>
                        {isValid ? (
                          <div className="flex items-center gap-2 ml-auto">
                            <span className="text-xs text-gray-400 font-medium bg-gray-50 dark:bg-gray-800 px-2 py-1 rounded-lg">
                              {fmtDur(diff)}
                            </span>
                            <span className="text-sm font-black text-green-700 dark:text-green-400">
                              ₹{slotPrice.toFixed(2)}
                            </span>
                          </div>
                        ) : (
                          <span className="text-xs text-red-500 ml-auto font-semibold">
                            ⚠️ Min 30 min
                          </span>
                        )}
                        <button
                          onClick={() =>
                            toggleSlot(
                              {
                                courtId: sel.courtId,
                                courtName: sel.courtName,
                              },
                              { status: "AVAILABLE", startTime: sel.startTime },
                            )
                          }
                          className="text-gray-300 hover:text-red-400 transition-colors text-xl font-bold leading-none w-7 h-7 flex items-center justify-center rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 flex-shrink-0"
                        >
                          ×
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* ── Availability Grid ── */}
          {loadingSlots ? (
            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 p-10 shadow-sm">
              <div className="flex justify-center items-center gap-3 text-gray-400 mb-8">
                <div className="w-5 h-5 border-2 border-green-500 border-t-transparent rounded-full animate-spin" />
                <span className="font-semibold text-sm">
                  Loading availability…
                </span>
              </div>
              <div className="flex gap-3">
                <div
                  className="w-24 bg-gray-100 dark:bg-gray-800 rounded-xl animate-pulse"
                  style={{ height: 320 }}
                />
                {[1, 2, 3].map((i) => (
                  <div
                    key={i}
                    className="flex-1 bg-gray-100 dark:bg-gray-800 rounded-xl animate-pulse"
                    style={{ height: 320, animationDelay: `${i * 80}ms` }}
                  />
                ))}
              </div>
            </div>
          ) : avail ? (
            <div className="bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm overflow-hidden">
              {/* Court header row */}
              <div className="flex border-b-2 border-gray-100 dark:border-gray-800">
                <div className="w-24 flex-shrink-0 border-r border-gray-100 dark:border-gray-700 time-axis-bg px-3 py-4 flex items-center justify-center">
                  <span className="text-xs font-bold text-slate-400 uppercase tracking-widest">
                    Time
                  </span>
                </div>
                {avail.courts.map((court) => {
                  const n = court.slots.filter(
                    (s) => s.status === "AVAILABLE",
                  ).length;
                  const selCount = Array.from(selected.keys()).filter((k) =>
                    k.startsWith(`${court.courtId}::`),
                  ).length;
                  return (
                    <div
                      key={court.courtId}
                      className={`flex-1 px-5 py-4 border-r border-gray-100 dark:border-gray-800 last:border-r-0 transition-colors ${
                        selCount > 0
                          ? "bg-blue-50 dark:bg-blue-900/20"
                          : "bg-white dark:bg-gray-900"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <p
                          className={`text-sm font-black ${
                            selCount > 0
                              ? "text-blue-600 dark:text-blue-400"
                              : "text-gray-800 dark:text-white"
                          }`}
                        >
                          {court.courtName}
                        </p>
                        {selCount > 0 && (
                          <span className="bg-blue-600 text-white text-xs font-black w-5 h-5 rounded-full flex items-center justify-center">
                            {selCount}
                          </span>
                        )}
                      </div>
                      <p className="text-xs mt-0.5 flex items-center gap-1.5">
                        <span
                          className={`w-1.5 h-1.5 rounded-full flex-shrink-0 ${n > 0 ? "bg-green-500" : "bg-red-400"}`}
                        />
                        <span
                          className={
                            n > 0
                              ? "text-green-600 dark:text-green-400 font-medium"
                              : "text-red-400 font-medium"
                          }
                        >
                          {n > 0 ? `${n} slots free` : "Fully booked"}
                        </span>
                      </p>
                    </div>
                  );
                })}
              </div>

              {/* Timeline body */}
              <div
                className="overflow-y-auto"
                style={{ maxHeight: 560 }}
                ref={gridRef}
              >
                <div className="flex">
                  {/* Time axis */}
                  <div className="w-24 flex-shrink-0 border-r border-gray-100 dark:border-gray-700 time-axis-bg">
                    <div
                      className="relative"
                      style={{ height: `${tlHeight}px` }}
                    >
                      {hourLabels.map(({ m, label }) => (
                        <div
                          key={m}
                          style={{
                            position: "absolute",
                            top: `${(m - openM) * PX_PER_MIN}px`,
                            left: 0,
                            right: 0,
                          }}
                        >
                          <div
                            style={{
                              position: "absolute",
                              left: 0,
                              right: 0,
                              borderTop: "1px solid rgba(71,85,105,.45)",
                            }}
                          />
                          <span
                            style={{
                              position: "absolute",
                              right: 10,
                              transform: "translateY(-50%)",
                              fontSize: 11,
                              fontWeight: 700,
                              color: "#94a3b8",
                              whiteSpace: "nowrap",
                            }}
                          >
                            {label}
                          </span>
                        </div>
                      ))}
                      {hourLabels.map(({ m }) => {
                        const hm = m + 30;
                        if (hm >= closeM) return null;
                        return (
                          <div
                            key={`hh-${m}`}
                            style={{
                              position: "absolute",
                              top: `${(hm - openM) * PX_PER_MIN}px`,
                              right: 10,
                              left: 0,
                              borderTop: "1px dashed rgba(71,85,105,.2)",
                            }}
                          />
                        );
                      })}
                    </div>
                  </div>

                  {/* Court columns */}
                  {avail.courts.map((court) => {
                    const hasSel = Array.from(selected.keys()).some((k) =>
                      k.startsWith(`${court.courtId}::`),
                    );
                    return (
                      <div
                        key={court.courtId}
                        className={`flex-1 border-r border-gray-100 dark:border-gray-800 last:border-r-0 court-col-bg ${hasSel ? "bg-blue-50/20 court-col-sel" : ""}`}
                      >
                        <div
                          className="relative"
                          style={{ height: `${tlHeight}px` }}
                        >
                          {/* Hour lines */}
                          {hourLabels.map(({ m }) => (
                            <div
                              key={m}
                              className="g-hour"
                              style={{ top: `${(m - openM) * PX_PER_MIN}px` }}
                            />
                          ))}
                          {/* Half-hour lines */}
                          {hourLabels.map(({ m }) => {
                            const hm = m + 30;
                            if (hm >= closeM) return null;
                            return (
                              <div
                                key={`hh-${m}`}
                                className="g-half"
                                style={{
                                  top: `${(hm - openM) * PX_PER_MIN}px`,
                                }}
                              />
                            );
                          })}

                          {/* Now indicator */}
                          {nowTop !== null && (
                            <div
                              className="now-bar"
                              style={{ top: `${nowTop}px` }}
                            />
                          )}

                          {/* Slots */}
                          {court.slots.map((slot, i) => {
                            const sm = toMins(slot.startTime),
                              em = toMins(slot.endTime);
                            const top = (sm - openM) * PX_PER_MIN;
                            const height = Math.max(
                              (em - sm) * PX_PER_MIN - 4,
                              20,
                            );
                            const key = `${court.courtId}::${slot.startTime}`;
                            const isSel = selected.has(key);
                            const isAvail = slot.status === "AVAILABLE";
                            const isBooked = slot.status === "BOOKED";
                            const isUnav = slot.status === "UNAVAILABLE";

                            let cls = "tl-slot ";
                            if (isSel) cls += "s-sel";
                            else if (isBooked) cls += "s-booked";
                            else if (isUnav) cls += "s-unav";
                            else cls += "s-avail";

                            const slotDur = em - sm;

                            return (
                              <div
                                key={i}
                                onClick={() => toggleSlot(court, slot)}
                                className={cls}
                                style={{
                                  top: `${top}px`,
                                  height: `${height}px`,
                                  zIndex: isSel ? 5 : 2,
                                }}
                                title={`${court.courtName} · ${to12(slot.startTime)} – ${to12(slot.endTime)}`}
                              >
                                {/* Available */}
                                {isAvail && !isSel && (
                                  <div className="text-center leading-tight w-full">
                                    {height >= 32 && (
                                      <div className="text-xs font-black text-green-700 dark:text-green-400">
                                        ₹{slot.price}
                                      </div>
                                    )}
                                    {height >= 52 && (
                                      <div className="text-xs text-green-600/70 dark:text-green-500/60 font-medium">
                                        {fmtDur(slotDur)}
                                      </div>
                                    )}
                                    {height >= 76 && (
                                      <div className="text-xs text-green-500/50 font-medium mt-0.5">
                                        Tap to select
                                      </div>
                                    )}
                                  </div>
                                )}

                                {/* Selected */}
                                {isSel && (
                                  <div className="text-center leading-tight w-full">
                                    <div className="text-sm font-black">✓</div>
                                    {height >= 44 && (
                                      <div className="text-xs font-semibold opacity-85 mt-0.5">
                                        Selected
                                      </div>
                                    )}
                                  </div>
                                )}

                                {/* Booked */}
                                {isBooked && height >= 24 && (
                                  <div className="text-center leading-tight w-full">
                                    {height >= 32 && (
                                      <div className="text-xs font-bold text-red-400 dark:text-red-300">
                                        Booked
                                      </div>
                                    )}
                                    {height >= 52 && (
                                      <div className="text-xs text-red-300/70 dark:text-red-400/60 font-medium">
                                        {to12(slot.startTime)}
                                      </div>
                                    )}
                                  </div>
                                )}

                                {/* Unavailable */}
                                {isUnav && height >= 28 && (
                                  <div className="text-center leading-tight w-full">
                                    <div className="text-xs font-semibold text-slate-400 dark:text-slate-600">
                                      Unavailable
                                    </div>
                                  </div>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Footer */}
              <div className="px-5 py-3 border-t border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/20 flex items-center justify-between">
                <p className="text-xs text-gray-400 dark:text-gray-500">
                  Tap green slots to select · Set end time in panel above · Min
                  30 min per booking
                </p>
                {isToday() && (
                  <div className="flex items-center gap-1.5 text-xs text-green-600 dark:text-green-500 font-medium">
                    <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" />
                    Live
                  </div>
                )}
              </div>
            </div>
          ) : null}
        </div>
      </div>

      {/* ── Fixed bottom cart bar ── */}
      {totalSel > 0 && (
        <div className="cart-bar">
          <div className="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between gap-4">
            <div className="flex items-center gap-4 flex-wrap flex-1 min-w-0">
              <div className="flex-shrink-0">
                <p className="text-xs text-gray-400 dark:text-gray-500 font-medium">
                  Ready to book
                </p>
                <p className="text-base font-black text-gray-900 dark:text-white">
                  {totalSel} slot{totalSel > 1 ? "s" : ""}
                  <span className="text-green-600 dark:text-green-400 ml-2">
                    ₹{totalPrice.toFixed(2)}
                  </span>
                </p>
              </div>
              <div className="flex gap-2 flex-wrap overflow-hidden">
                {Array.from(selected.entries()).map(([key, sel]) => {
                  const end = getEndTime(key);
                  const diff = toMins(end) - toMins(sel.startTime);
                  const valid = diff >= 30;
                  return (
                    <div
                      key={key}
                      className={`text-xs font-semibold px-3 py-1.5 rounded-full border flex items-center gap-1.5 flex-shrink-0 ${
                        valid
                          ? "bg-green-50 dark:bg-green-900/20 border-green-200 dark:border-green-800 text-green-700 dark:text-green-400"
                          : "bg-red-50 dark:bg-red-900/20 border-red-200 dark:border-red-800 text-red-500"
                      }`}
                    >
                      <span className="font-bold">{sel.courtName}</span>
                      <span className="opacity-40">·</span>
                      <span>{to12(sel.startTime)}</span>
                      <span className="opacity-40">→</span>
                      <span>{to12(end)}</span>
                      {!valid && <span className="text-red-400">⚠️</span>}
                    </div>
                  );
                })}
              </div>
            </div>
            <div className="flex gap-2 flex-shrink-0">
              <button
                onClick={() => {
                  setSelected(new Map());
                  setConfig(new Map());
                }}
                className="border border-gray-200 dark:border-gray-700 text-gray-600 dark:text-gray-300 px-4 py-2.5 rounded-xl text-sm font-semibold hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors"
              >
                Clear
              </button>
              <button
                onClick={handleAddToCart}
                disabled={
                  adding ||
                  Array.from(selected.entries()).some(([key, sel]) => {
                    const end = getEndTime(key);
                    return (
                      toMins(end) - toMins(sel.startTime) < 30 ||
                      toMins(end) > toMins(sel.maxEndTime)
                    );
                  })
                }
                className="add-btn px-6 py-2.5 rounded-xl text-sm"
              >
                {adding ? (
                  <span className="flex items-center gap-2">
                    <span className="animate-spin inline-block">⟳</span>Adding…
                  </span>
                ) : (
                  `🛒 Add ${totalSel > 1 ? `${totalSel} Slots` : "to Cart"} — ₹${totalPrice.toFixed(2)}`
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
