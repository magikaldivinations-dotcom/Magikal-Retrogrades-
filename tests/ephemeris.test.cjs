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
