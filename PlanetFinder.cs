// PlanetFinder.cs
using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;

namespace PlanetFinder
{
    class Program
    {
        static readonly Dictionary<string, double[]> PlanetData = new()
        {
            ["Mercury"] = new double[] { 252.2509, 4.09233445 },
            ["Venus"]   = new double[] { 181.9798, 1.60213043 },
            ["Earth"]   = new double[] { 100.4664, 0.98560911 },
            ["Mars"]    = new double[] { 355.4530, 0.52407178 },
            ["Jupiter"] = new double[] { 34.3515, 0.08309125 },
            ["Saturn"]  = new double[] { 50.0774, 0.03344482 },
            ["Uranus"]  = new double[] { 314.0550, 0.01172312 },
            ["Neptune"] = new double[] { 304.3487, 0.00597733 }
        };
        const double Obliquity = 23.4393;

        class Result
        {
            public string Planet { get; set; }
            public double RaDeg { get; set; }
            public double DecDeg { get; set; }
            public string Ra { get; set; }
            public string Dec { get; set; }
        }

        static void Main(string[] args)
        {
            var opts = ParseArgs(args);
            DateTime date;
            if (!string.IsNullOrEmpty(opts.Date))
            {
                if (!DateTime.TryParseExact(opts.Date, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.AssumeUniversal, out date))
                {
                    Console.WriteLine("\u001B[31mНеверный формат даты. Используйте YYYY-MM-DD\u001B[0m");
                    return;
                }
            }
            else
                date = DateTime.UtcNow;

            List<string> planets;
            if (!string.IsNullOrEmpty(opts.Planet))
            {
                if (!PlanetData.ContainsKey(opts.Planet))
                {
                    Console.WriteLine($"\u001B[31mПланета '{opts.Planet}' не найдена.\u001B[0m");
                    return;
                }
                planets = new List<string> { opts.Planet };
            }
            else
                planets = PlanetData.Keys.ToList();

            var results = new List<Result>();
            foreach (var p in planets)
            {
                var pos = ComputePosition(date, p);
                results.Add(new Result
                {
                    Planet = p,
                    RaDeg = pos.Item1,
                    DecDeg = pos.Item2,
                    Ra = RaToHms(pos.Item1),
                    Dec = DecToDms(pos.Item2)
                });
            }

            if (!string.IsNullOrEmpty(opts.ExportJson))
            {
                var json = JsonSerializer.Serialize(results, new JsonSerializerOptions { WriteIndented = true });
                File.WriteAllText(opts.ExportJson, json);
                Console.WriteLine($"\u001B[32mЭкспортировано в {opts.ExportJson} (JSON)\u001B[0m");
            }
            else if (!string.IsNullOrEmpty(opts.ExportCsv))
            {
                using var sw = new StreamWriter(opts.ExportCsv);
                sw.WriteLine("planet,ra_deg,dec_deg,ra_hms,dec_dms");
                foreach (var r in results)
                    sw.WriteLine($"{r.Planet},{r.RaDeg},{r.DecDeg},{r.Ra},{r.Dec}");
                Console.WriteLine($"\u001B[32mЭкспортировано в {opts.ExportCsv} (CSV)\u001B[0m");
            }
            else
            {
                Console.WriteLine($"\u001B[36mПоложение планет на {date:yyyy-MM-dd}:\u001B[0m\n");
                foreach (var r in results)
                {
                    Console.WriteLine($"\u001B[33m{r.Planet}:\u001B[0m");
                    Console.WriteLine($"  RA  = {r.Ra}  ({r.RaDeg:F2}°)");
                    Console.WriteLine($"  Dec = {r.Dec}  ({r.DecDeg:F2}°)");
                    Console.WriteLine();
                }
            }
        }

        static Options ParseArgs(string[] args)
        {
            var opts = new Options();
            for (int i = 0; i < args.Length; i++)
            {
                switch (args[i])
                {
                    case "--date": opts.Date = args[++i]; break;
                    case "--planet": opts.Planet = args[++i]; break;
                    case "--list": opts.List = true; break;
                    case "--export-json": opts.ExportJson = args[++i]; break;
                    case "--export-csv": opts.ExportCsv = args[++i]; break;
                }
            }
            return opts;
        }

        class Options
        {
            public string Date { get; set; }
            public string Planet { get; set; }
            public bool List { get; set; }
            public string ExportJson { get; set; }
            public string ExportCsv { get; set; }
        }

        static (double ra, double dec) EclipticToEquatorial(double lon, double lat, double obl)
        {
            double lonRad = lon * Math.PI / 180;
            double latRad = lat * Math.PI / 180;
            double oblRad = obl * Math.PI / 180;
            double ra = Math.Atan2(Math.Sin(lonRad) * Math.Cos(oblRad) - Math.Tan(latRad) * Math.Sin(oblRad),
                                   Math.Cos(lonRad));
            double dec = Math.Asin(Math.Sin(latRad) * Math.Cos(oblRad) + Math.Cos(latRad) * Math.Sin(oblRad) * Math.Sin(lonRad));
            double raDeg = (ra * 180 / Math.PI % 360 + 360) % 360;
            double decDeg = dec * 180 / Math.PI;
            return (raDeg, decDeg);
        }

        static string RaToHms(double raDeg)
        {
            double hours = raDeg / 15;
            int h = (int)hours;
            int m = (int)((hours - h) * 60);
            double s = (hours - h - m / 60.0) * 3600;
            return $"{h:00}h {m:00}m {s:00.00}s";
        }

        static string DecToDms(double decDeg)
        {
            char sign = decDeg >= 0 ? '+' : '-';
            decDeg = Math.Abs(decDeg);
            int d = (int)decDeg;
            int m = (int)((decDeg - d) * 60);
            double s = (decDeg - d - m / 60.0) * 3600;
            return $"{sign}{d:00}° {m:00}' {s:00.00}\"";
        }

        static (double ra, double dec) ComputePosition(DateTime date, string planet)
        {
            var j2000 = new DateTime(2000, 1, 1, 12, 0, 0, DateTimeKind.Utc);
            double days = (date.ToUniversalTime() - j2000).TotalDays;
            var data = PlanetData[planet];
            double lon = (data[0] + data[1] * days) % 360;
            return EclipticToEquatorial(lon, 0, Obliquity);
        }
    }
}
