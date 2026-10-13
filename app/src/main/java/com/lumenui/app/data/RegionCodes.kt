package com.lumenui.app.data

/**
 * Normalises the assorted region codes the probes surface — two-letter ISO-3166 alpha-2 as-is,
 * three-letter ISO alpha-3 and GitHub's AWS-style edge names mapped down to alpha-2.
 */
internal object RegionCodes {

    fun normalize(code: String): String? {
        val upper = code.uppercase()
        if (upper.length == 2) return upper
        return ALIASES[upper]
    }

    private val ALIASES = mapOf(
        "TWN" to "TW", "NTW" to "TW", "TWD" to "TW",
        "JPN" to "JP", "JPY" to "JP",
        "KOR" to "KR", "KRW" to "KR",
        "SGP" to "SG", "SGD" to "SG",
        "GBR" to "GB", "GBP" to "GB",
        "USA" to "US", "USD" to "US",
        "HKG" to "HK", "HKD" to "HK",
        "CHN" to "CN", "RUS" to "RU", "IRN" to "IR", "PRK" to "KP",
        "SYR" to "SY", "CUB" to "CU", "BLR" to "BY", "VEN" to "VE",
        "MMR" to "MM", "SDN" to "SD", "AFG" to "AF", "SSD" to "SS",
        "YEM" to "YE", "ZWE" to "ZW", "MAC" to "MO",
        "DEU" to "DE", "FRA" to "FR", "NLD" to "NL", "CAN" to "CA",
        "AUS" to "AU", "IND" to "IN", "BRA" to "BR", "MEX" to "MX",
        "ITA" to "IT", "ESP" to "ES", "SWE" to "SE", "CHE" to "CH",
        "AUT" to "AT", "POL" to "PL", "TUR" to "TR", "VNM" to "VN",
        "THA" to "TH", "MYS" to "MY", "IDN" to "ID", "PHL" to "PH",
        "ARG" to "AR", "CHL" to "CL", "ZAF" to "ZA", "ARE" to "AE",
        "ISR" to "IL", "SAU" to "SA", "EGY" to "EG", "NGA" to "NG",
        "KEN" to "KE", "NZL" to "NZ", "IRL" to "IE", "PRT" to "PT",
        "GRC" to "GR", "CZE" to "CZ", "ROU" to "RO", "UKR" to "UA",
        "HUN" to "HU", "FIN" to "FI", "NOR" to "NO", "DNK" to "DK",
        "BEL" to "BE", "COL" to "CO", "PER" to "PE",
        // GitHub reports AWS-style edge regions.
        "SOUTHEASTASIA" to "SG", "EASTASIA" to "HK", "EASTUS" to "US",
        "EASTUS2" to "US", "WESTUS" to "US", "WESTUS2" to "US",
        "CENTRALUS" to "US", "SOUTHCENTRALUS" to "US", "NORTHCENTRALUS" to "US",
        "WESTEUROPE" to "NL", "NORTHEUROPE" to "IE", "JAPANEAST" to "JP",
        "JAPANWEST" to "JP", "AUSTRALIAEAST" to "AU", "CENTRALINDIA" to "IN",
        "SOUTHINDIA" to "IN", "BRAZILSOUTH" to "BR", "SOUTHAMERICA" to "BR",
        "UKSOUTH" to "GB", "FRANCECENTRAL" to "FR", "GERMANYWESTCENTRAL" to "DE",
        "CANADACENTRAL" to "CA", "KOREACENTRAL" to "KR", "SWITZERLANDNORTH" to "CH"
    )
}
