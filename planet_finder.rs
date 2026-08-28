// planet_finder.rs
use chrono::{DateTime, NaiveDate, TimeZone, Utc};
use clap::{App, Arg};
use serde::{Deserialize, Serialize};
use serde_json;
use std::collections::HashMap;
use std::fs;
use std::io::Write;
use colored::*;

type PlanetMap = HashMap<String, (f64, f64)>; // (L0, n)

fn planet_data() -> PlanetMap {
    let mut m = HashMap::new();
    m.insert("Mercury".to_string(), (252.2509, 4.09233445));
    m.insert("Venus".to_string(), (181.9798, 1.60213043));
    m.insert("Earth".to_string(), (100.4664, 0.98560911));
    m.insert("Mars".to_string(), (355.4530, 0.52407178));
    m.insert("Jupiter".to_string(), (34.3515, 0.08309125));
    m.insert("Saturn".to_string(), (50.0774, 0.03344482));
    m.insert("Uranus".to_string(), (314.0550, 0.01172312));
    m.insert("Neptune".to_string(), (304.3487, 0.00597733));
    m
}

const OBLIQUITY: f64 = 23.4393;

#[derive(Serialize, Deserialize)]
struct Result {
    planet: String,
    ra_deg: f64,
    dec_deg: f64,
    ra: String,
    dec: String,
}

fn ecliptic_to_equatorial(lon: f64, lat: f64, obl: f64) -> (f64, f64) {
    let lon_rad = lon.to_radians();
    let lat_rad = lat.to_radians();
    let obl_rad = obl.to_radians();
    let ra = (lon_rad.sin() * obl_rad.cos() - lat_rad.tan() * obl_rad.sin()).atan2(lon_rad.cos());
    let dec = (lat_rad.sin() * obl_rad.cos() + lat_rad.cos() * obl_rad.sin() * lon_rad.sin()).asin();
    let ra_deg = (ra.to_degrees() % 360.0 + 360.0) % 360.0;
    let dec_deg = dec.to_degrees();
    (ra_deg, dec_deg)
}

fn ra_to_hms(ra_deg: f64) -> String {
    let hours = ra_deg / 15.0;
    let h = hours.floor() as i32;
    let m = ((hours - h as f64) * 60.0).floor() as i32;
    let s = (hours - h as f64 - m as f64 / 60.0) * 3600.0;
    format!("{:02}h {:02}m {:05.2}s", h, m, s)
}

fn dec_to_dms(dec_deg: f64) -> String {
    let sign = if dec_deg >= 0.0 { '+' } else { '-' };
    let dec_abs = dec_deg.abs();
    let d = dec_abs.floor() as i32;
    let m = ((dec_abs - d as f64) * 60.0).floor() as i32;
    let s = (dec_abs - d as f64 - m as f64 / 60.0) * 3600.0;
    format!("{}{:02}° {:02}' {:05.2}\"", sign, d, m, s)
}

fn compute_position(date: DateTime<Utc>, planet: &str, data: &PlanetMap) -> (f64, f64) {
    let j2000 = Utc.with_ymd_and_hms(2000, 1, 1, 12, 0, 0).unwrap();
    let delta = (date - j2000).num_seconds() as f64 / 86400.0;
    let (l0, n) = data[planet];
    let lon = (l0 + n * delta) % 360.0;
    ecliptic_to_equatorial(lon, 0.0, OBLIQUITY)
}

fn main() {
    let matches = App::new("Planet Finder")
        .arg(Arg::with_name("date").long("date").takes_value(true).help("Дата (YYYY-MM-DD)"))
        .arg(Arg::with_name("planet").long("planet").takes_value(true).help("Название планеты"))
        .arg(Arg::with_name("list").long("list").help("Показать все планеты"))
        .arg(Arg::with_name("export-json").long("export-json").takes_value(true).help("Экспорт в JSON"))
        .arg(Arg::with_name("export-csv").long("export-csv").takes_value(true).help("Экспорт в CSV"))
        .get_matches();

    let date_str = matches.value_of("date").unwrap_or("");
    let date = if date_str.is_empty() {
        Utc::now()
    } else {
        let naive = NaiveDate::parse_from_str(date_str, "%Y-%m-%d").expect("Неверный формат даты");
        Utc.from_utc_datetime(&naive.and_hms_opt(0,0,0).unwrap())
    };

    let planet_name = matches.value_of("planet");
    let planets = if let Some(p) = planet_name {
        vec![p.to_string()]
    } else {
        planet_data().keys().cloned().collect::<Vec<_>>()
    };

    let data = planet_data();
    let mut results = Vec::new();
    for p in planets {
        let (ra, dec) = compute_position(date, &p, &data);
        results.push(Result {
            planet: p.clone(),
            ra_deg: ra,
            dec_deg: dec,
            ra: ra_to_hms(ra),
            dec: dec_to_dms(dec),
        });
    }

    if let Some(file) = matches.value_of("export-json") {
        let json = serde_json::to_string_pretty(&results).unwrap();
        fs::write(file, json).unwrap();
        println!("{}", format!("Экспортировано в {} (JSON)", file).green());
    } else if let Some(file) = matches.value_of("export-csv") {
        let mut wtr = csv::Writer::from_path(file).unwrap();
        wtr.write_record(&["planet", "ra_deg", "dec_deg", "ra", "dec"]).unwrap();
        for r in &results {
            wtr.write_record(&[&r.planet, &r.ra_deg.to_string(), &r.dec_deg.to_string(), &r.ra, &r.dec]).unwrap();
        }
        wtr.flush().unwrap();
        println!("{}", format!("Экспортировано в {} (CSV)", file).green());
    } else {
        println!("{}", format!("Положение планет на {}:\n", date.format("%Y-%m-%d")).cyan());
        for r in results {
            println!("{}:", r.planet.yellow());
            println!("  RA  = {}  ({:.2}°)", r.ra, r.ra_deg);
            println!("  Dec = {}  ({:.2}°)", r.dec, r.dec_deg);
            println!();
        }
    }
}
