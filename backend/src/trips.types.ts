export type TripLocationDto = {
  ts: number;
  lat: number;
  lon: number;
  alt?: number | null;
  speed?: number | null;
  bearing?: number | null;
  acc?: number | null;
};

export type TripEventDto = {
  ts: number;
  type: string;
  label: string;
  severity?: number;
  lat?: number | null;
  lon?: number | null;
};

export type TripUpsertBody = {
  clientTripId: string;
  startAtMs: number;
  endAtMs?: number | null;
  distanceM?: number;
  durationMs?: number;
  avgSpeedKmh?: number;
  maxSpeedKmh?: number;
  qualityScore?: number;
  explorationXp?: number;
  newCells?: number;
  elevationGainM?: number;
  stopCount?: number;
  pointCount?: number;
  locations?: TripLocationDto[];
  events?: TripEventDto[];
};
