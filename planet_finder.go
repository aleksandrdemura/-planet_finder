// planet_finder.go
package main

import (
	"encoding/csv"
	"encoding/json"
	"flag"
	"fmt"
	"math"
	"os"
	"strconv"
	"time"
)

type PlanetData struct {
	L0 float64
	N  float64
}

var planetData = map[string]PlanetData{
	"Mercury": {252.2509, 4.09233445},
	"Venus":   {181.9798, 1.60213043},
	"Earth":   {100.4664, 0.98560911},
	"Mars":    {355.4530, 0.52407178},
	"Jupiter": {34.3515, 0.08309125},
	"Saturn":  {50.0774, 0.03344482},
	"Uranus":  {314.0550, 0.01172312},
	"Neptune": {304.3487, 0.00597733},
}
const OBLIQUITY = 23.4393 // degrees

type Result struct {
	Planet  string  `json:"planet"`
	RADeg   float64 `json:"ra_deg"`
	DecDeg  float64 `json:"dec_deg"`
	RA      string  `json:"ra"`
	Dec     string  `json:"dec"`
}

func eclipticToEquatorial(lon, lat, obl float64) (float64, float64) {
	lonRad := lon * math.Pi / 180
	latRad := lat * math.Pi / 180
	oblRad := obl * math.Pi / 180
	ra := math.Atan2(math.Sin(lonRad)*math.Cos(oblRad)-math.Tan(latRad)*math.Sin(oblRad),
		math.Cos(lonRad))
	dec := math.Asin(math.Sin(latRad)*math.Cos(oblRad) + math.Cos(latRad)*math.Sin(oblRad)*math.Sin(lonRad))
	raDeg := math.Mod(ra*180/math.Pi+360, 360)
	decDeg := dec * 180 / math.Pi
	return raDeg, decDeg
}

func raToHms(raDeg float64) string {
	hours := raDeg / 15
	h := int(hours)
	m := int((hours - float64(h)) * 60)
	s := (hours - float64(h) - float64(m)/60) * 3600
	return fmt.Sprintf("%02dh %02dm %05.2fs", h, m, s)
}

func decToDms(decDeg float64) string {
	sign := '+'
	if decDeg < 0 {
		sign = '-'
		decDeg = -decDeg
	}
	d := int(decDeg)
	m := int((decDeg - float64(d)) * 60)
	s := (decDeg - float64(d) - float64(m)/60) * 3600
	return fmt.Sprintf("%c%02d° %02d' %05.2f\"", sign, d, m, s)
}

func computePlanetPosition(date time.Time, planet string) (float64, float64) {
	j2000 := time.Date(2000, 1, 1, 12, 0, 0, 0, time.UTC)
	delta := date.Sub(j2000).Hours() / 24
	data := planetData[planet]
	lon := math.Mod(data.L0+data.N*delta+360, 360)
	ra, dec := eclipticToEquatorial(lon, 0, OBLIQUITY)
	return ra, dec
}

func main() {
	var (
		dateStr    string
		planetName string
		list       bool
		exportJson string
		exportCsv  string
	)
	flag.StringVar(&dateStr, "date", "", "Дата (YYYY-MM-DD)")
	flag.StringVar(&planetName, "planet", "", "Название планеты")
	flag.BoolVar(&list, "list", false, "Показать все планеты")
	flag.StringVar(&exportJson, "export-json", "", "Экспорт в JSON")
	flag.StringVar(&exportCsv, "export-csv", "", "Экспорт в CSV")
	flag.Parse()

	var date time.Time
	if dateStr != "" {
		var err error
		date, err = time.Parse("2006-01-02", dateStr)
		if err != nil {
			fmt.Println("\033[31mНеверный формат даты. Используйте YYYY-MM-DD\033[0m")
			os.Exit(1)
		}
	} else {
		date = time.Now()
	}

	var planets []string
	if planetName != "" {
		if _, ok := planetData[planetName]; !ok {
			fmt.Printf("\033[31mПланета '%s' не найдена.\033[0m\n", planetName)
			os.Exit(1)
		}
		planets = []string{planetName}
	} else {
		for p := range planetData {
			planets = append(planets, p)
		}
	}

	results := make([]Result, 0, len(planets))
	for _, p := range planets {
		ra, dec := computePlanetPosition(date, p)
		results = append(results, Result{
			Planet:  p,
			RADeg:   ra,
			DecDeg:  dec,
			RA:      raToHms(ra),
			Dec:     decToDms(dec),
		})
	}

	if exportJson != "" {
		data, _ := json.MarshalIndent(results, "", "  ")
		os.WriteFile(exportJson, data, 0644)
		fmt.Printf("\033[32mЭкспортировано в %s (JSON)\033[0m\n", exportJson)
	} else if exportCsv != "" {
		f, _ := os.Create(exportCsv)
		defer f.Close()
		w := csv.NewWriter(f)
		defer w.Flush()
		w.Write([]string{"planet", "ra_deg", "dec_deg", "ra_hms", "dec_dms"})
		for _, r := range results {
			w.Write([]string{r.Planet, fmt.Sprintf("%f", r.RADeg), fmt.Sprintf("%f", r.DecDeg), r.RA, r.Dec})
		}
		fmt.Printf("\033[32mЭкспортировано в %s (CSV)\033[0m\n", exportCsv)
	} else {
		fmt.Printf("\033[36mПоложение планет на %s:\033[0m\n\n", date.Format("2006-01-02"))
		for _, r := range results {
			fmt.Printf("\033[33m%s:\033[0m\n", r.Planet)
			fmt.Printf("  RA  = %s  (%.2f°)\n", r.RA, r.RADeg)
			fmt.Printf("  Dec = %s  (%.2f°)\n", r.Dec, r.DecDeg)
			fmt.Println()
		}
	}
}
