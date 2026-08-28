// planet_finder.cpp
#include <iostream>
#include <string>
#include <vector>
#include <map>
#include <cmath>
#include <ctime>
#include <iomanip>
#include <fstream>
#include <sstream>
#include <json/json.h> // using jsoncpp

using namespace std;

const map<string, vector<double>> PLANET_DATA = {
    {"Mercury", {252.2509, 4.09233445}},
    {"Venus",   {181.9798, 1.60213043}},
    {"Earth",   {100.4664, 0.98560911}},
    {"Mars",    {355.4530, 0.52407178}},
    {"Jupiter", {34.3515, 0.08309125}},
    {"Saturn",  {50.0774, 0.03344482}},
    {"Uranus",  {314.0550, 0.01172312}},
    {"Neptune", {304.3487, 0.00597733}}
};
const double OBLIQUITY = 23.4393;

struct Result {
    string planet;
    double ra_deg;
    double dec_deg;
    string ra;
    string dec;
};

double fmod2(double a, double b) { return fmod(fmod(a,b)+b,b); }

void eclipticToEquatorial(double lon, double lat, double obl, double& ra, double& dec) {
    double lonRad = lon * M_PI / 180.0;
    double latRad = lat * M_PI / 180.0;
    double oblRad = obl * M_PI / 180.0;
    ra = atan2(sin(lonRad)*cos(oblRad) - tan(latRad)*sin(oblRad), cos(lonRad));
    dec = asin(sin(latRad)*cos(oblRad) + cos(latRad)*sin(oblRad)*sin(lonRad));
    ra = fmod2(ra * 180.0 / M_PI, 360.0);
    dec = dec * 180.0 / M_PI;
}

string raToHms(double raDeg) {
    double hours = raDeg / 15.0;
    int h = (int)hours;
    int m = (int)((hours - h) * 60);
    double s = (hours - h - m/60.0) * 3600;
    ostringstream oss;
    oss << setfill('0') << setw(2) << h << "h " << setw(2) << m << "m " << fixed << setprecision(2) << s << "s";
    return oss.str();
}

string decToDms(double decDeg) {
    char sign = decDeg >= 0 ? '+' : '-';
    decDeg = abs(decDeg);
    int d = (int)decDeg;
    int m = (int)((decDeg - d) * 60);
    double s = (decDeg - d - m/60.0) * 3600;
    ostringstream oss;
    oss << sign << setfill('0') << setw(2) << d << "° " << setw(2) << m << "' " << fixed << setprecision(2) << s << "\"";
    return oss.str();
}

void computePosition(time_t date, const string& planet, double& ra, double& dec) {
    tm* tm = gmtime(&date);
    tm tm_j2000 = {0};
    tm_j2000.tm_year = 100; // 2000
    tm_j2000.tm_mon = 0;
    tm_j2000.tm_mday = 1;
    tm_j2000.tm_hour = 12;
    time_t j2000 = timegm(&tm_j2000);
    double days = difftime(date, j2000) / 86400.0;
    auto it = PLANET_DATA.find(planet);
    double L0 = it->second[0];
    double n = it->second[1];
    double lon = fmod2(L0 + n * days, 360.0);
    eclipticToEquatorial(lon, 0, OBLIQUITY, ra, dec);
}

int main(int argc, char* argv[]) {
    string dateStr, planetName, exportJson, exportCsv;
    bool list = false;

    for (int i = 1; i < argc; ++i) {
        string arg = argv[i];
        if (arg == "--date" && i+1 < argc) dateStr = argv[++i];
        else if (arg == "--planet" && i+1 < argc) planetName = argv[++i];
        else if (arg == "--list") list = true;
        else if (arg == "--export-json" && i+1 < argc) exportJson = argv[++i];
        else if (arg == "--export-csv" && i+1 < argc) exportCsv = argv[++i];
    }

    time_t t;
    if (!dateStr.empty()) {
        struct tm tm = {0};
        strptime(dateStr.c_str(), "%Y-%m-%d", &tm);
        t = timegm(&tm);
    } else {
        t = time(nullptr);
    }

    vector<string> planets;
    if (!planetName.empty()) {
        if (PLANET_DATA.find(planetName) == PLANET_DATA.end()) {
            cerr << "\033[31mПланета '" << planetName << "' не найдена.\033[0m" << endl;
            return 1;
        }
        planets.push_back(planetName);
    } else {
        for (const auto& p : PLANET_DATA) planets.push_back(p.first);
    }

    vector<Result> results;
    for (const auto& p : planets) {
        double ra, dec;
        computePosition(t, p, ra, dec);
        Result r;
        r.planet = p;
        r.ra_deg = ra;
        r.dec_deg = dec;
        r.ra = raToHms(ra);
        r.dec = decToDms(dec);
        results.push_back(r);
    }

    if (!exportJson.empty()) {
        Json::Value root(Json::arrayValue);
        for (const auto& r : results) {
            Json::Value item;
            item["planet"] = r.planet;
            item["ra_deg"] = r.ra_deg;
            item["dec_deg"] = r.dec_deg;
            item["ra"] = r.ra;
            item["dec"] = r.dec;
            root.append(item);
        }
        ofstream ofs(exportJson);
        ofs << root.toStyledString();
        cout << "\033[32mЭкспортировано в " << exportJson << " (JSON)\033[0m" << endl;
    } else if (!exportCsv.empty()) {
        ofstream ofs(exportCsv);
        ofs << "planet,ra_deg,dec_deg,ra_hms,dec_dms\n";
        for (const auto& r : results) {
            ofs << r.planet << "," << r.ra_deg << "," << r.dec_deg << "," << r.ra << "," << r.dec << "\n";
        }
        cout << "\033[32mЭкспортировано в " << exportCsv << " (CSV)\033[0m" << endl;
    } else {
        char buf[20];
        strftime(buf, sizeof(buf), "%Y-%m-%d", gmtime(&t));
        cout << "\033[36mПоложение планет на " << buf << ":\033[0m" << endl << endl;
        for (const auto& r : results) {
            cout << "\033[33m" << r.planet << ":\033[0m" << endl;
            cout << "  RA  = " << r.ra << "  (" << fixed << setprecision(2) << r.ra_deg << "°)" << endl;
            cout << "  Dec = " << r.dec << "  (" << fixed << setprecision(2) << r.dec_deg << "°)" << endl;
            cout << endl;
        }
    }
    return 0;
}
