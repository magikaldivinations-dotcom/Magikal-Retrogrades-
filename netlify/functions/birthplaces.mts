export default async (request: Request) => {
  const query = new URL(request.url).searchParams.get('q')?.trim() || '';
  if (query.length < 2 || query.length > 200) {
    return Response.json({ error: 'Enter a birthplace between 2 and 200 characters.' }, { status: 400 });
  }
  try {
    // The geocoder supports comma-separated city, region, and country searches.
    const params = new URLSearchParams({ name: query, count: '20', language: 'en', format: 'json' });
    const response = await fetch('https://geocoding-api.open-meteo.com/v1/search?' + params, {
      signal: AbortSignal.timeout(10000)
    });
    if (!response.ok) throw new Error('Geocoder unavailable');
    const data = await response.json();
    if (data.error || (data.results !== undefined && !Array.isArray(data.results))) throw new Error('Invalid results');
    const results = (data.results || []).filter(place => {
      if (!place.name || !place.timezone || !Number.isFinite(place.latitude) || !Number.isFinite(place.longitude)) return false;
      try { new Intl.DateTimeFormat('en', { timeZone: place.timezone }); return true; } catch { return false; }
    }).map(({ name, admin1, country, timezone, latitude, longitude }) => ({ name, admin1, country, timezone, latitude, longitude }));
    return Response.json({ results }, { headers: { 'Cache-Control': 'public, max-age=3600' } });
  } catch {
    return Response.json({ error: 'Place search is unavailable right now. Try again in a moment.' }, { status: 502 });
  }
};
