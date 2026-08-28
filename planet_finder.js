#!/usr/bin/env node
// planet_finder.js
const { program } = require('commander');
const chalk = require('chalk');
const fs = require('fs');

// Данные планет (средние элементы)
const PLANET_DATA = {
    Mercury: { L0: 252.2509, n: 4.09233445 },
    Venus:   { L0: 181.9798, n: 1.60213043 },
    Earth:   { L0: 100.4664, n: 0.98560911 },
    Mars:    { L0: 355.4530, n: 0.52407178 },
    Jupiter: { L0: 34.3515,  n: 0.08309125 },
    Saturn:  { L0: 50.0774,  n: 0.03344482 },
    Uranus:  { L0: 314.0550, n: 0.01172312 },
    Neptune: { L0: 304.3487, n: 0.00597733 },
};
const OBLIQUITY = 23.4393; // градусы

function eclipticToEquatorial(lon, lat = 0, obl = OBLIQUITY) {
    const lonRad = lon * Math.PI / 180;
    const latRad = lat * Math.PI / 180;
    const oblRad = obl * Math.PI / 180;
    const ra = Math.atan2(Math.sin(lonRad) * Math.cos(oblRad) - Math.tan(latRad) * Math.sin(oblRad),
                          Math.cos(lonRad));
    const dec = Math.asin(Math.sin(latRad) * Math.cos(oblRad) + Math.cos(latRad) * Math.sin(oblRad) * Math.sin(lonRad));
    return { ra: (ra * 180 / Math.PI) % 360, dec: dec * 180 / Math.PI };
}

function raToHms(raDeg) {
    const hours = raDeg / 15;
    const h = Math.floor(hours);
    const m = Math.floor((hours - h) * 60);
    const s = (hours - h - m/60) * 3600;
    return `${String(h).padStart(2,'0')}h ${String(m).padStart(2,'0')}m ${s.toFixed(2)}s`;
}

function decToDms(decDeg) {
    const sign = decDeg >= 0 ? '+' : '-';
    decDeg = Math.abs(decDeg);
    const d = Math.floor(decDeg);
    const m = Math.floor((decDeg - d) * 60);
    const s = (decDeg - d - m/60) * 3600;
    return `${sign}${String(d).padStart(2,'0')}° ${String(m).padStart(2,'0')}' ${s.toFixed(2)}"`;
}

function computePlanetPosition(date, planet) {
    const j2000 = new Date(Date.UTC(2000, 0, 1, 12, 0));
    const delta = (date - j2000) / (1000 * 60 * 60 * 24);
    const data = PLANET_DATA[planet];
    const lon = (data.L0 + data.n * delta) % 360;
    const { ra, dec } = eclipticToEquatorial(lon);
    return { ra, dec };
}

program
    .option('--date <date>', 'Дата (YYYY-MM-DD)')
    .option('--planet <name>', 'Название планеты')
    .option('--list', 'Показать все планеты')
    .option('--export-json <file>', 'Экспорт в JSON')
    .option('--export-csv <file>', 'Экспорт в CSV')
    .parse(process.argv);

const opts = program.opts();

let date;
if (opts.date) {
    date = new Date(opts.date + 'T00:00:00Z');
    if (isNaN(date)) {
        console.error(chalk.red('Неверный формат даты. Используйте YYYY-MM-DD'));
        process.exit(1);
    }
} else {
    date = new Date();
}

let planets;
if (opts.planet) {
    if (!PLANET_DATA[opts.planet]) {
        console.error(chalk.red(`Планета '${opts.planet}' не найдена.`));
        process.exit(1);
    }
    planets = [opts.planet];
} else {
    planets = Object.keys(PLANET_DATA);
}

const results = [];
for (const p of planets) {
    const { ra, dec } = computePlanetPosition(date, p);
    results.push({
        planet: p,
        ra_deg: ra,
        dec_deg: dec,
        ra: raToHms(ra),
        dec: decToDms(dec)
    });
}

if (opts.exportJson) {
    fs.writeFileSync(opts.exportJson, JSON.stringify(results, null, 2));
    console.log(chalk.green(`Экспортировано в ${opts.exportJson} (JSON)`));
} else if (opts.exportCsv) {
    const header = 'planet,ra_deg,dec_deg,ra_hms,dec_dms\n';
    const rows = results.map(r => `${r.planet},${r.ra_deg},${r.dec_deg},${r.ra},${r.dec}`).join('\n');
    fs.writeFileSync(opts.exportCsv, header + rows);
    console.log(chalk.green(`Экспортировано в ${opts.exportCsv} (CSV)`));
} else {
    console.log(chalk.cyan(`Положение планет на ${date.toISOString().split('T')[0]}:\n`));
    for (const r of results) {
        console.log(chalk.yellow(`${r.planet}:`));
        console.log(`  RA  = ${r.ra}  (${r.ra_deg.toFixed(2)}°)`);
        console.log(`  Dec = ${r.dec}  (${r.dec_deg.toFixed(2)}°)`);
        console.log();
    }
}
