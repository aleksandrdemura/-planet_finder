// PlanetFinder.kt
import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import com.google.gson.GsonBuilder
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PlanetFinder {
    @Parameter(names = ["--date"])
    private var dateStr: String? = null

    @Parameter(names = ["--planet"])
    private var planetName: String? = null

    @Parameter(names = ["--list"])
    private var list: Boolean = false

    @Parameter(names = ["--export-json"])
    private var exportJson: String? = null

    @Parameter(names = ["--export-csv"])
    private var exportCsv: String? = null

    data class Result(val planet: String, val ra_deg: Double, val dec_deg: Double, val ra: String, val dec: String)

    private val planetData = mapOf(
        "Mercury" to doubleArrayOf(252.2509, 4.09233445),
        "Venus" to doubleArrayOf(181.9798, 1.60213043),
        "Earth" to doubleArrayOf(100.4664, 0.98560911),
        "Mars" to doubleArrayOf(355.4530, 0.52407178),
        "Jupiter" to doubleArrayOf(34.3515, 0.08309125),
        "Saturn" to doubleArrayOf(50.0774, 0.03344482),
        "Uranus" to doubleArrayOf(314.0550, 0.01172312),
        "Neptune" to doubleArrayOf(304.3487, 0.00597733)
    )
    private val obliquity = 23.4393

    private fun eclipticToEquatorial(lon: Double, lat: Double, obl: Double): Pair<Double, Double> {
        val lonRad = Math.toRadians(lon)
        val latRad = Math.toRadians(lat)
        val oblRad = Math.toRadians(obl)
        val ra = Math.atan2(Math.sin(lonRad) * Math.cos(oblRad) - Math.tan(latRad) * Math.sin(oblRad),
                           Math.cos(lonRad))
        val dec = Math.asin(Math.sin(latRad) * Math.cos(oblRad) + Math.cos(latRad) * Math.sin(oblRad) * Math.sin(lonRad))
        var raDeg = Math.toDegrees(ra) % 360
        if (raDeg < 0) raDeg += 360.0
        return Pair(raDeg, Math.toDegrees(dec))
    }

    private fun raToHms(raDeg: Double): String {
        val hours = raDeg / 15.0
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        val s = (hours - h - m / 60.0) * 3600
        return String.format("%02dh %02dm %05.2fs", h, m, s)
    }

    private fun decToDms(decDeg: Double): String {
        val sign = if (decDeg >= 0) '+' else '-'
        val d = Math.abs(decDeg)
        val degrees = d.toInt()
        val minutes = ((d - degrees) * 60).toInt()
        val seconds = (d - degrees - minutes / 60.0) * 3600
        return String.format("%c%02d° %02d' %05.2f\"", sign, degrees, minutes, seconds)
    }

    private fun computePosition(date: LocalDate, planet: String): Pair<Double, Double> {
        val j2000 = LocalDate.of(2000, 1, 1)
        val days = date.toEpochDay() - j2000.toEpochDay()
        val data = planetData[planet]!!
        val lon = (data[0] + data[1] * days) % 360.0
        return eclipticToEquatorial(lon, 0.0, obliquity)
    }

    fun run() {
        val date = if (dateStr != null) LocalDate.parse(dateStr) else LocalDate.now(ZoneOffset.UTC)

        val planets = if (planetName != null) {
            if (!planetData.containsKey(planetName)) {
                System.err.println("\u001B[31mПланета '$planetName' не найдена.\u001B[0m")
                System.exit(1)
            }
            listOf(planetName!!)
        } else {
            planetData.keys.toList()
        }

        val results = planets.map { p ->
            val (ra, dec) = computePosition(date, p)
            Result(p, ra, dec, raToHms(ra), decToDms(dec))
        }

        when {
            exportJson != null -> {
                val gson = GsonBuilder().setPrettyPrinting().create()
                File(exportJson).writeText(gson.toJson(results))
                println("\u001B[32mЭкспортировано в $exportJson (JSON)\u001B[0m")
            }
            exportCsv != null -> {
                File(exportCsv).printWriter().use { pw ->
                    pw.println("planet,ra_deg,dec_deg,ra_hms,dec_dms")
                    results.forEach { pw.println("${it.planet},${it.ra_deg},${it.dec_deg},${it.ra},${it.dec}") }
                }
                println("\u001B[32mЭкспортировано в $exportCsv (CSV)\u001B[0m")
            }
            else -> {
                println("\u001B[36mПоложение планет на $date:\u001B[0m\n")
                results.forEach {
                    println("\u001B[33m${it.planet}:\u001B[0m")
                    println("  RA  = ${it.ra}  (${String.format("%.2f", it.ra_deg)}°)")
                    println("  Dec = ${it.dec}  (${String.format("%.2f", it.dec_deg)}°)")
                    println()
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val pf = PlanetFinder()
    JCommander.newBuilder().addObject(pf).build().parse(*args)
    pf.run()
}
