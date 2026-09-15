"use client";

import { useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";

type Job = {
  id: string;
  status: string;
  workshopName?: string;
  vehicleId: string;
  bookingId: string;
  extras: { id: string; description: string; status: string }[];
  invoice?: { id: string; total: number; status: string };
};

const styles = {
  page: {
    minHeight: "100vh",
    margin: 0,
    background: "linear-gradient(160deg, #0B0F14 0%, #121821 55%, #0B0F14 100%)",
    color: "#F4F7FB",
    fontFamily: '"Segoe UI", system-ui, sans-serif',
    padding: "32px 24px 64px",
  } as const,
  brand: { fontSize: 42, fontWeight: 700, letterSpacing: "-0.03em", margin: 0 } as const,
  sub: { color: "#8B98A8", marginTop: 8, marginBottom: 28 } as const,
  card: {
    background: "#121821",
    border: "1px solid #1E2A38",
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
  } as const,
  input: {
    width: "100%",
    background: "#0B0F14",
    border: "1px solid #2A3A4C",
    color: "#F4F7FB",
    borderRadius: 8,
    padding: "10px 12px",
    marginBottom: 10,
  } as const,
  btn: {
    background: "#2EE6A6",
    color: "#0B0F14",
    border: "none",
    borderRadius: 8,
    padding: "10px 14px",
    fontWeight: 600,
    cursor: "pointer",
    marginRight: 8,
    marginTop: 8,
  } as const,
  ghost: {
    background: "transparent",
    color: "#3DB9FF",
    border: "1px solid #3DB9FF",
    borderRadius: 8,
    padding: "10px 14px",
    cursor: "pointer",
    marginRight: 8,
    marginTop: 8,
  } as const,
  teal: { color: "#2EE6A6" } as const,
  amber: { color: "#F5A524" } as const,
};

export default function StaffHome() {
  const [token, setToken] = useState("");
  const [workshopId, setWorkshopId] = useState("ws-audi-lhr");
  const [jobs, setJobs] = useState<Job[]>([]);
  const [selected, setSelected] = useState<Job | null>(null);
  const [history, setHistory] = useState<string>("");
  const [error, setError] = useState<string | null>(null);
  const [extraDesc, setExtraDesc] = useState("Brake fluid flush");
  const [extraCost, setExtraCost] = useState("1500");

  const ready = useMemo(() => token.trim().length > 8, [token]);

  async function load() {
    if (!ready) return;
    setError(null);
    try {
      const data = await api("/v1/workshop-staff/jobs", { token, workshopId });
      setJobs(data.items ?? []);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Load failed");
    }
  }

  useEffect(() => {
    // no auto-load until token set
  }, []);

  async function act(path: string, method = "POST", body?: unknown) {
    if (!selected) return;
    setError(null);
    try {
      await api(path, { method, token, workshopId, body });
      await load();
      const refreshed = (await api("/v1/workshop-staff/jobs", { token, workshopId }))
        .items as Job[];
      setSelected(refreshed.find((j) => j.id === selected.id) ?? null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Action failed");
    }
  }

  async function viewHistory() {
    if (!selected) return;
    try {
      const data = await api(`/v1/jobs/${selected.id}/shared-history`, {
        token,
        workshopId,
      });
      setHistory(
        `${data.vehicle.year} ${data.vehicle.make} ${data.vehicle.model} · ${data.services?.length ?? 0} services · scope ${data.scope}`,
      );
    } catch (e) {
      setHistory("");
      setError(e instanceof Error ? e.message : "Share denied");
    }
  }

  return (
    <main style={styles.page}>
      <h1 style={styles.brand}>Meru Bay</h1>
      <p style={styles.sub}>Staff portal — calendar, job, invoice (DEC-014).</p>

      <div style={{ maxWidth: 720 }}>
        <input
          style={styles.input}
          placeholder="Bearer access token (paste without Bearer)"
          value={token}
          onChange={(e) => setToken(e.target.value.replace(/^Bearer\s+/i, ""))}
        />
        <input
          style={styles.input}
          placeholder="Workshop id"
          value={workshopId}
          onChange={(e) => setWorkshopId(e.target.value)}
        />
        <button style={styles.btn} onClick={load} disabled={!ready}>
          Load jobs
        </button>
        {error && <p style={styles.amber}>{error}</p>}

        <h2 style={{ marginTop: 32, fontSize: 18 }}>Today’s bay</h2>
        {jobs.map((j) => (
          <div
            key={j.id}
            style={{
              ...styles.card,
              outline: selected?.id === j.id ? "1px solid #2EE6A6" : undefined,
              cursor: "pointer",
            }}
            onClick={() => {
              setSelected(j);
              setHistory("");
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <strong>{j.workshopName || workshopId}</strong>
              <span style={styles.teal}>{j.status}</span>
            </div>
            <div style={{ color: "#8B98A8", fontSize: 13, marginTop: 6 }}>
              Job {j.id.slice(0, 8)} · vehicle {j.vehicleId.slice(0, 8)}
              {j.invoice ? ` · invoice PKR ${j.invoice.total}` : ""}
            </div>
          </div>
        ))}

        {selected && (
          <section style={{ ...styles.card, marginTop: 20 }}>
            <h3 style={{ marginTop: 0 }}>Job desk</h3>
            <p style={{ color: "#8B98A8" }}>Status: {selected.status}</p>
            <button style={styles.btn} onClick={() => act(`/v1/jobs/${selected.id}/check-in`)}>
              Check in
            </button>
            <button style={styles.btn} onClick={() => act(`/v1/jobs/${selected.id}/start`)}>
              Start work
            </button>
            <button style={styles.ghost} onClick={viewHistory}>
              Shared history
            </button>
            {history && <p style={styles.teal}>{history}</p>}

            <div style={{ marginTop: 16 }}>
              <input
                style={styles.input}
                value={extraDesc}
                onChange={(e) => setExtraDesc(e.target.value)}
              />
              <input
                style={styles.input}
                value={extraCost}
                onChange={(e) => setExtraCost(e.target.value)}
              />
              <button
                style={styles.ghost}
                onClick={() =>
                  act(`/v1/jobs/${selected.id}/extras`, "POST", {
                    description: extraDesc,
                    estimatedCost: Number(extraCost) || 0,
                  })
                }
              >
                Propose extra
              </button>
            </div>

            <button
              style={{ ...styles.btn, marginTop: 16 }}
              onClick={() =>
                act("/v1/invoices", "POST", { jobId: selected.id })
              }
            >
              Issue invoice
            </button>
          </section>
        )}
      </div>
    </main>
  );
}
