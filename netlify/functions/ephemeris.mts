// Keep Horizons requests same-origin for browsers and retry temporary upstream failures.
export default async (request: Request) => {
  const input = new URL(request.url).searchParams;
  const body = input.get('body');
  const start = input.get('start');
  const stop = input.get('stop');
  if (!['199', '299', '499', '599', '699', '799', '899', '999', '2060;'].includes(body || '') ||
      !start || !stop || !/^\d{4}-\d{2}-\d{2}$/.test(start) || !/^\d{4}-\d{2}-\d{2}$/.test(stop) ||
      !Number.isFinite(Date.parse(start)) || !Number.isFinite(Date.parse(stop)) ||
      Date.parse(stop) <= Date.parse(start) || Date.parse(stop) - Date.parse(start) > 1100 * 86400000) {
    return Response.json({ error: 'Invalid ephemeris request.' }, { status: 400 });
  }
  const params = new URLSearchParams({ format: 'json' });
  const options = { COMMAND: body!, OBJ_DATA: 'NO', MAKE_EPHEM: 'YES', EPHEM_TYPE: 'VECTORS',
    CENTER: '500@399', START_TIME: start, STOP_TIME: stop, STEP_SIZE: '2 d', TIME_TYPE: 'UT',
    REF_PLANE: 'ECLIPTIC', OUT_UNITS: 'AU-D', VEC_TABLE: '2', VEC_CORR: 'LT', CSV_FORMAT: 'YES', VEC_LABELS: 'NO' };
  for (const [key, value] of Object.entries(options)) params.set(key, `'${value}'`);
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const response = await fetch('https://ssd.jpl.nasa.gov/api/horizons.api?' + params, {
        signal: AbortSignal.timeout(12000), headers: { Accept: 'application/json' }
      });
      if (!response.ok) {
        if (response.status !== 429 && response.status < 500) break;
        throw new Error('Temporary upstream failure');
      }
      const data = await response.json();
      if (data.error || typeof data.result !== 'string' ||
          !data.signature?.source?.toLowerCase().includes('nasa/jpl horizons') ||
          !data.result.includes('$$SOE') || !data.result.includes('$$EOE')) {
        return Response.json({ error: 'NASA/JPL did not return valid ephemeris data.' }, { status: 502 });
      }
      return Response.json(data, { headers: { 'Cache-Control': 'public, max-age=3600' } });
    } catch {
      if (attempt < 2) await new Promise(resolve => setTimeout(resolve, 400 * (attempt + 1)));
    }
  }
  return Response.json({ error: 'NASA/JPL is temporarily unavailable. Try refreshing again.' }, { status: 502 });
};
