const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const html = fs.readFileSync('preview/index.html', 'utf8');
const source = html.slice(html.indexOf('function eventsFromCsv('), html.indexOf('async function loadRetrogradeFeed('));
const { eventsFromCsv } = vm.runInNewContext(source + ';({eventsFromCsv})');
// Horizons vector table 2: JD, calendar date, x, y, z, vx, vy, vz.
const csv = rates => '$$SOE\n' + rates.map((rate, i) => `${2461000.5 + i * 2}, A.D. date, 1, 0, 0, 0, ${rate}, 0,`).join('\n') + '\n$$EOE';
test('accepts a retrograde continuing beyond the forecast window', () => {
  const events = eventsFromCsv('Mercury', csv([0.1, -0.1, -0.2]));
  assert.equal(events.length, 1);
  assert.equal(events[0].end, (2461004.5 - 2440587.5) * 86400000);
  assert.ok(events[0].end > events[0].start);
  assert.equal(events[0].zodiac, 'Aries');
});
test('accepts an entire window inside a retrograde', () => {
  assert.equal(eventsFromCsv('Neptune', csv([-0.1, -0.2, -0.1])).length, 1);
});
test('interpolates a complete retrograde', () => {
  const event = eventsFromCsv('Venus', csv([0.1, -0.1, 0.1]))[0];
  assert.equal(event.end - event.start, 2 * 86400000);
});
test('a direct-only forecast is a valid empty result', () => {
  assert.equal(eventsFromCsv('Mars', csv([0.1, 0.2, 0.3])).length, 0);
});
test('rejects missing or malformed ephemeris samples', () => {
  assert.throws(() => eventsFromCsv('Mercury', 'No matches found.'), /No ephemeris/);
  assert.throws(() => eventsFromCsv('Mercury', '$$SOE\nnot,data\n$$EOE'), /Too few/);
});

const mergeSource = html.slice(html.indexOf('const knownRetrogradeEvents='), html.indexOf('const ephemerisCacheKey='));
const { mergeRetrogradeEvents, knownRetrogradeEvents } = vm.runInNewContext(
  mergeSource + ';({mergeRetrogradeEvents,knownRetrogradeEvents})'
);
const chironLive = {
  planet: 'Chiron', start: Date.parse('2026-08-03T00:00:00Z'),
  end: Date.parse('2027-01-06T12:00:00Z'), zodiac: 'Taurus → Aries'
};
test('live Chiron replaces the overlapping September fallback with one current interval', () => {
  const merged = mergeRetrogradeEvents([chironLive]);
  const chiron = merged.filter(event => event.planet === 'Chiron');
  assert.equal(chiron.length, 1);
  assert.deepEqual({ ...chiron[0] }, chironLive);
  const now = Date.parse('2026-11-01T00:00:00Z');
  assert.equal(merged.filter(event => event.planet === 'Chiron' && event.start <= now && now < event.end).length, 1);
  assert.deepEqual(merged.filter(event => event.planet !== 'Chiron'), knownRetrogradeEvents.filter(event => event.planet !== 'Chiron').sort((a, b) => a.start - b.start));
});
test('missing Chiron live results retain fallback during another planet refresh', () => {
  const mercury = { ...knownRetrogradeEvents.find(event => event.planet === 'Mercury'), zodiac: 'Live Mercury' };
  const merged = mergeRetrogradeEvents([mercury]);
  assert.deepEqual(merged.find(event => event.planet === 'Chiron'), knownRetrogradeEvents.find(event => event.planet === 'Chiron'));
  assert.equal(merged.find(event => event.planet === 'Mercury').zodiac, 'Live Mercury');
});
test('keeps disjoint live events and the existing near-start matching and zodiac fallback', () => {
  const known = knownRetrogradeEvents.find(event => event.planet === 'Mercury');
  const near = { ...known, start: known.start + 86400000, end: known.end + 86400000, zodiac: '' };
  const extra = { planet: 'Chiron', start: Date.parse('2028-07-01'), end: Date.parse('2028-12-01'), zodiac: 'Aries' };
  const merged = mergeRetrogradeEvents([near, extra]);
  assert.equal(merged.filter(event => event.planet === 'Mercury').length, knownRetrogradeEvents.filter(event => event.planet === 'Mercury').length);
  assert.equal(merged.find(event => event.start === near.start).zodiac, known.zodiac);
  assert.ok(merged.some(event => event.start === extra.start));
});
test('merging the saved feed again does not reintroduce the Chiron fallback', () => {
  const merged = mergeRetrogradeEvents([chironLive]);
  assert.deepEqual(mergeRetrogradeEvents(merged), merged);
});

for (const chironFails of [false, true]) test(`live refresh ${chironFails ? 'keeps Chiron fallback on failure' : 'shows one live Chiron interval on success'}`, async () => {
  const loadSource = html.slice(html.indexOf('async function loadRetrogradeFeed('), html.indexOf('document.querySelector("#refreshTracker").addEventListener'));
  const elements = new Map();
  const context = vm.createContext({
    document: { querySelector: selector => { if (!elements.has(selector)) elements.set(selector, {}); return elements.get(selector); } },
    localStorage: { getItem: () => null, setItem: () => {} },
    eventDate: () => 'today', renderTracker: () => {}, localZone: 'UTC',
    horizonsBodies: [{ name: 'Mercury' }, { name: 'Chiron' }],
    requestPlanetEvents: async body => {
      if (body.name === 'Chiron') {
        if (chironFails) throw new Error('NASA/JPL unavailable');
        return [chironLive];
      }
      return [{ ...knownRetrogradeEvents.find(event => event.planet === 'Mercury'), zodiac: 'Live Mercury' }];
    }
  });
  vm.runInContext(mergeSource + `
    const ephemerisCacheKey='test',ephemerisTtl=43200000;
    let retrogradeEvents=knownRetrogradeEvents,trackerIsLoading=false,trackerFeedAvailable=true,trackerRequestId=0;
    ` + loadSource, context);
  await vm.runInContext('loadRetrogradeFeed(true)', context);
  const events = vm.runInContext('retrogradeEvents', context);
  const chiron = events.filter(event => event.planet === 'Chiron');
  assert.equal(chiron.length, 1);
  assert.equal(chiron[0].start, chironFails ? Date.parse('2026-09-17T00:00:00Z') : chironLive.start);
  assert.equal(events.find(event => event.planet === 'Mercury').zodiac, 'Live Mercury');
  assert.match(elements.get('#trackerMessage').textContent, chironFails ? /Chiron could not refresh/ : /NASA\/JPL dates updated/);
});
