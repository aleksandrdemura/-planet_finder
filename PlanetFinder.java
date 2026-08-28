// PlanetFinder.java
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

public class PlanetFinder {
    private static final Map<String, double[]> PLANET_DATA = new HashMap<>();
    static {
        PLANET_DATA.put("Mercury", new double[]{252.2509, 4.09233445});
        PLANET_DATA.put("Venus", new double[]{181.9798, 1.60213043});
        PLANET_DATA.put("Earth", new double[]{100.4664, 0.98560911});
        PLANET_DATA.put("Mars", new double[]{355.4530, 0.52407178});
        PLANET_DATA.put("Jupiter", new double[]{34.3515, 0.08309125});
        PLANET_DATA.put("Saturn", new double[]{50.0774, 0.03344482});
        PLANET_DATA.put("Uranus", new double[]{314.0550, 0.01172312});
        PLANET_DATA.put("Neptune", new double[]{304.3487, 0.00597733});
    }
    private static final double OBLIQUITY = 23.4393;

    @Parameter(names = "--date")
    private String dateStr;
    @Parameter(names = "--planet")
    private String planetName;
    @Parameter(names = "--list")
    private boolean list;
    @Parameter(names = "--export-json")
    private String exportJson;
    @Parameter(names = "--export-csv")
    private String exportCsv;

    static class Result {
        String planet;
        double ra_deg;
        double dec_deg;
        String ra;
        String dec;
    }

    private static double[] eclipticToEquatorial(double lon, double lat, double obl) {
        double lonRad = Math.toRadians(lon);
        double latRad = Math.toRadians(lat);
        double oblRad = Math.toRadians(obl);
        double ra = Math.atan2(Math.sin(lonRad) * Math.cos(oblRad) - Math.tan(latRad) * Math.sin(oblRad),
                               Math.cos(lonRad));
        double dec = Math.asin(Math.sin(latRad) * Math.cos(oblRad) + Math.cos(latRad) * Math.sin(oblRad) * Math.sin(lonRad));
        double raDeg = (Math.toDegrees(ra) % 360 + 360) % 360;
        double decDeg = Math.toDegrees(dec);
        return new double[]{raDeg, decDeg};
    }

    private static String raToHms(double raDeg) {
        double hours = raDeg / 15;
        int h = (int)hours;
        int m = (int)((hours - h) * 60);
        double s = (hours - h - m/60.0) * 3600;
        return String.format("%02dh %02dm %05.2fs", h, m, s);
    }

    private static String decToDms(double decDeg) {
        char sign = decDeg >= 0 ? '+' : '-';
        decDeg = Math.abs(decDeg);
        int d = (int)decDeg;
        int m = (int)((decDeg - d) * 60);
        double s = (decDeg - d - m/60.0) * 3600;
        return String.format("%c%02d° %02d' %05.2f\"", sign, d, m, s);
    }

    private static double[] computePosition(LocalDate date, String planet) {
        LocalDate j2000 = LocalDate.of(2000, 1, 1);
        long days = date.toEpochDay() - j2000.toEpochDay();
        double[] data = PLANET_DATA.get(planet);
        double lon = (data[0] + data[1] * days) % 360;
        return eclipticToEquatorial(lon, 0, OBLIQUITY);
    }

    public void run() throws Exception {
        LocalDate date;
        if (dateStr != null) {
            date = LocalDate.parse(dateStr);
        } else {
            date = LocalDate.now(ZoneOffset.UTC);
        }

        List<String> planets;
        if (planetName != null) {
            if (!PLANET_DATA.containsKey(planetName)) {
                System.err.println("\u001B[31mПланета '" + planetName + "' не найдена.\u001B[0m");
                System.exit(1);
            }
            planets = Collections.singletonList(planetName);
        } else {
            planets = new ArrayList<>(PLANET_DATA.keySet());
        }

        List<Result> results = new ArrayList<>();
        for (String p : planets) {
            double[] pos = computePosition(date, p);
            Result r = new Result();
            r.planet = p;
            r.ra_deg = pos[0];
            r.dec_deg = pos[1];
            r.ra = raToHms(pos[0]);
            r.dec = decToDms(pos[1]);
            results.add(r);
        }

        if (exportJson != null) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.write(Paths.get(exportJson), gson.toJson(results).getBytes());
            System.out.println("\u001B[32mЭкспортировано в " + exportJson + " (JSON)\u001B[0m");
        } else if (exportCsv != null) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(exportCsv))) {
                pw.println("planet,ra_deg,dec_deg,ra_hms,dec_dms");
                for (Result r : results) {
                    pw.printf("%s,%f,%f,%s,%s%n", r.planet, r.ra_deg, r.dec_deg, r.ra, r.dec);
                }
            }
            System.out.println("\u001B[32mЭкспортировано в " + exportCsv + " (CSV)\u001B[0m");
        } else {
            System.out.println("\u001B[36mПоложение планет на " + date + ":\u001B[0m\n");
            for (Result r : results) {
                System.out.println("\u001B[33m" + r.planet + ":\u001B[0m");
                System.out.printf("  RA  = %s  (%.2f°)\n", r.ra, r.ra_deg);
                System.out.printf("  Dec = %s  (%.2f°)\n", r.dec, r.dec_deg);
                System.out.println();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        PlanetFinder pf = new PlanetFinder();
        JCommander.newBuilder().addObject(pf).build().parse(args);
        pf.run();
    }
}
