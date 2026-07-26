package com.asg.spindleserp.common.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CountryCodes — ISO 3166-1 alpha-3 to country name and demonym (nationality),
 * used to turn an MRZ code such as "BGD" into "Bangladesh" / "Bangladeshi".
 *
 * Also carries the handful of non-ISO codes that legitimately appear in real
 * MRZs: the UN travel-document codes (D for Germany, GBR sub-codes for the
 * British nationality classes, and the stateless / refugee codes from ICAO
 * 9303 Part 3).
 *
 * Anything not in the table falls back to the raw three-letter code, which is
 * always better than a blank field — the operator can correct it in the form.
 */
public final class CountryCodes {

    private CountryCodes() { }

    private record Entry(String country, String demonym) { }

    private static final Map<String, Entry> MAP = new LinkedHashMap<>();

    private static void p(String code, String country, String demonym) {
        MAP.put(code, new Entry(country, demonym));
    }

    static {
        // ── South Asia (the bulk of ASG's traffic) ────────────────────────────
        p("BGD", "Bangladesh", "Bangladeshi");
        p("IND", "India", "Indian");
        p("PAK", "Pakistan", "Pakistani");
        p("NPL", "Nepal", "Nepalese");
        p("LKA", "Sri Lanka", "Sri Lankan");
        p("BTN", "Bhutan", "Bhutanese");
        p("MDV", "Maldives", "Maldivian");
        p("AFG", "Afghanistan", "Afghan");
        p("MMR", "Myanmar", "Burmese");

        // ── Gulf / Middle East ────────────────────────────────────────────────
        p("SAU", "Saudi Arabia", "Saudi");
        p("ARE", "United Arab Emirates", "Emirati");
        p("QAT", "Qatar", "Qatari");
        p("KWT", "Kuwait", "Kuwaiti");
        p("OMN", "Oman", "Omani");
        p("BHR", "Bahrain", "Bahraini");
        p("JOR", "Jordan", "Jordanian");
        p("LBN", "Lebanon", "Lebanese");
        p("IRQ", "Iraq", "Iraqi");
        p("IRN", "Iran", "Iranian");
        p("ISR", "Israel", "Israeli");
        p("PSE", "Palestine", "Palestinian");
        p("SYR", "Syria", "Syrian");
        p("YEM", "Yemen", "Yemeni");
        p("TUR", "Turkiye", "Turkish");

        // ── South-East & East Asia ────────────────────────────────────────────
        p("MYS", "Malaysia", "Malaysian");
        p("SGP", "Singapore", "Singaporean");
        p("THA", "Thailand", "Thai");
        p("IDN", "Indonesia", "Indonesian");
        p("PHL", "Philippines", "Filipino");
        p("VNM", "Viet Nam", "Vietnamese");
        p("KHM", "Cambodia", "Cambodian");
        p("LAO", "Laos", "Lao");
        p("BRN", "Brunei", "Bruneian");
        p("CHN", "China", "Chinese");
        p("HKG", "Hong Kong SAR", "Hong Konger");
        p("MAC", "Macao SAR", "Macanese");
        p("TWN", "Taiwan", "Taiwanese");
        p("JPN", "Japan", "Japanese");
        p("KOR", "South Korea", "South Korean");
        p("PRK", "North Korea", "North Korean");
        p("MNG", "Mongolia", "Mongolian");

        // ── Central Asia & Caucasus ───────────────────────────────────────────
        p("KAZ", "Kazakhstan", "Kazakhstani");
        p("UZB", "Uzbekistan", "Uzbek");
        p("TKM", "Turkmenistan", "Turkmen");
        p("KGZ", "Kyrgyzstan", "Kyrgyzstani");
        p("TJK", "Tajikistan", "Tajikistani");
        p("AZE", "Azerbaijan", "Azerbaijani");
        p("ARM", "Armenia", "Armenian");
        p("GEO", "Georgia", "Georgian");

        // ── Europe ────────────────────────────────────────────────────────────
        p("GBR", "United Kingdom", "British");
        p("IRL", "Ireland", "Irish");
        p("FRA", "France", "French");
        p("DEU", "Germany", "German");
        p("D",   "Germany", "German");            // legacy MRZ code
        p("ITA", "Italy", "Italian");
        p("ESP", "Spain", "Spanish");
        p("PRT", "Portugal", "Portuguese");
        p("NLD", "Netherlands", "Dutch");
        p("BEL", "Belgium", "Belgian");
        p("LUX", "Luxembourg", "Luxembourgish");
        p("CHE", "Switzerland", "Swiss");
        p("AUT", "Austria", "Austrian");
        p("SWE", "Sweden", "Swedish");
        p("NOR", "Norway", "Norwegian");
        p("DNK", "Denmark", "Danish");
        p("FIN", "Finland", "Finnish");
        p("ISL", "Iceland", "Icelandic");
        p("POL", "Poland", "Polish");
        p("CZE", "Czechia", "Czech");
        p("SVK", "Slovakia", "Slovak");
        p("HUN", "Hungary", "Hungarian");
        p("ROU", "Romania", "Romanian");
        p("BGR", "Bulgaria", "Bulgarian");
        p("GRC", "Greece", "Greek");
        p("CYP", "Cyprus", "Cypriot");
        p("MLT", "Malta", "Maltese");
        p("HRV", "Croatia", "Croatian");
        p("SVN", "Slovenia", "Slovenian");
        p("SRB", "Serbia", "Serbian");
        p("BIH", "Bosnia and Herzegovina", "Bosnian");
        p("MNE", "Montenegro", "Montenegrin");
        p("MKD", "North Macedonia", "Macedonian");
        p("ALB", "Albania", "Albanian");
        p("UKR", "Ukraine", "Ukrainian");
        p("BLR", "Belarus", "Belarusian");
        p("MDA", "Moldova", "Moldovan");
        p("RUS", "Russia", "Russian");
        p("EST", "Estonia", "Estonian");
        p("LVA", "Latvia", "Latvian");
        p("LTU", "Lithuania", "Lithuanian");

        // ── Americas ──────────────────────────────────────────────────────────
        p("USA", "United States", "American");
        p("CAN", "Canada", "Canadian");
        p("MEX", "Mexico", "Mexican");
        p("BRA", "Brazil", "Brazilian");
        p("ARG", "Argentina", "Argentine");
        p("CHL", "Chile", "Chilean");
        p("COL", "Colombia", "Colombian");
        p("PER", "Peru", "Peruvian");
        p("VEN", "Venezuela", "Venezuelan");
        p("ECU", "Ecuador", "Ecuadorian");
        p("BOL", "Bolivia", "Bolivian");
        p("URY", "Uruguay", "Uruguayan");
        p("PRY", "Paraguay", "Paraguayan");
        p("CUB", "Cuba", "Cuban");
        p("DOM", "Dominican Republic", "Dominican");
        p("JAM", "Jamaica", "Jamaican");
        p("TTO", "Trinidad and Tobago", "Trinidadian");
        p("GTM", "Guatemala", "Guatemalan");
        p("PAN", "Panama", "Panamanian");
        p("CRI", "Costa Rica", "Costa Rican");
        p("HND", "Honduras", "Honduran");
        p("SLV", "El Salvador", "Salvadoran");
        p("NIC", "Nicaragua", "Nicaraguan");
        p("HTI", "Haiti", "Haitian");

        // ── Africa ────────────────────────────────────────────────────────────
        p("EGY", "Egypt", "Egyptian");
        p("LBY", "Libya", "Libyan");
        p("TUN", "Tunisia", "Tunisian");
        p("DZA", "Algeria", "Algerian");
        p("MAR", "Morocco", "Moroccan");
        p("SDN", "Sudan", "Sudanese");
        p("SSD", "South Sudan", "South Sudanese");
        p("ETH", "Ethiopia", "Ethiopian");
        p("ERI", "Eritrea", "Eritrean");
        p("SOM", "Somalia", "Somali");
        p("DJI", "Djibouti", "Djiboutian");
        p("KEN", "Kenya", "Kenyan");
        p("UGA", "Uganda", "Ugandan");
        p("TZA", "Tanzania", "Tanzanian");
        p("RWA", "Rwanda", "Rwandan");
        p("BDI", "Burundi", "Burundian");
        p("NGA", "Nigeria", "Nigerian");
        p("GHA", "Ghana", "Ghanaian");
        p("SEN", "Senegal", "Senegalese");
        p("CIV", "Cote d'Ivoire", "Ivorian");
        p("MLI", "Mali", "Malian");
        p("CMR", "Cameroon", "Cameroonian");
        p("ZAF", "South Africa", "South African");
        p("ZWE", "Zimbabwe", "Zimbabwean");
        p("ZMB", "Zambia", "Zambian");
        p("MOZ", "Mozambique", "Mozambican");
        p("AGO", "Angola", "Angolan");
        p("BWA", "Botswana", "Motswana");
        p("NAM", "Namibia", "Namibian");
        p("MUS", "Mauritius", "Mauritian");
        p("MDG", "Madagascar", "Malagasy");
        p("SYC", "Seychelles", "Seychellois");
        p("COD", "DR Congo", "Congolese");
        p("COG", "Congo", "Congolese");
        p("GAB", "Gabon", "Gabonese");

        // ── Oceania ───────────────────────────────────────────────────────────
        p("AUS", "Australia", "Australian");
        p("NZL", "New Zealand", "New Zealander");
        p("FJI", "Fiji", "Fijian");
        p("PNG", "Papua New Guinea", "Papua New Guinean");

        // ── ICAO special codes ────────────────────────────────────────────────
        p("UNO", "United Nations", "UN Official");
        p("UNA", "United Nations Agency", "UN Agency Official");
        p("XXA", "Stateless", "Stateless");
        p("XXB", "Refugee", "Refugee");
        p("XXC", "Refugee (non-Convention)", "Refugee");
        p("XXX", "Unspecified Nationality", "Unspecified");
        p("XOM", "Sovereign Military Order of Malta", "Maltese Order");
    }

    /** Full country name for an alpha-3 code, or the code itself if unknown. */
    public static String countryName(String alpha3) {
        if (alpha3 == null || alpha3.isBlank()) return null;
        Entry e = MAP.get(alpha3.trim().toUpperCase());
        return e != null ? e.country() : alpha3.trim().toUpperCase();
    }

    /** Demonym (what goes in the passenger's Nationality field), or the code. */
    public static String nationality(String alpha3) {
        if (alpha3 == null || alpha3.isBlank()) return null;
        Entry e = MAP.get(alpha3.trim().toUpperCase());
        return e != null ? e.demonym() : alpha3.trim().toUpperCase();
    }

    /** True when the code is one we recognise — used to flag suspect OCR. */
    public static boolean isKnown(String alpha3) {
        return alpha3 != null && MAP.containsKey(alpha3.trim().toUpperCase());
    }
}
