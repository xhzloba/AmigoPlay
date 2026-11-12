package com.example.movie.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val FlagShape = RoundedCornerShape(2.dp)
private val FlagW = 22.dp
private val FlagH = 15.dp

internal fun parseCountries(raw: String): List<String> =
    raw.split(',', '/', '·', '|', ';')
        .map { it.trim() }
        .filter { it.isNotEmpty() && it != "—" }

internal fun countryIso(name: String): String? {
    val key = name.trim().lowercase()
        .replace('ё', 'е')
        .removePrefix("the ")
    CountryIso[key]?.let { return it }
    // Без коротких алиасов вроде «корея» — иначе ложные совпадения.
    return CountryIso.entries
        .filter { it.key.length >= 4 }
        .firstOrNull { (alias, _) -> key.contains(alias) }
        ?.value
}

@Composable
internal fun CountryFlagsValue(
    countries: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    endAligned: Boolean = true,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 18.sp,
) {
    val items = remember(countries) {
        countries.map { it to countryIso(it) }
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(
            10.dp,
            if (endAligned) Alignment.End else Alignment.Start,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (name, iso) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (iso != null) {
                    RectFlag(iso = iso)
                }
                Text(
                    text = name,
                    color = color,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    textAlign = if (endAligned) TextAlign.End else TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RectFlag(iso: String) {
    Box(
        modifier = Modifier
            .width(FlagW)
            .height(FlagH)
            .clip(FlagShape)
            .background(Color.White.copy(alpha = 0.12f)),
    ) {
        AsyncImage(
            model = "https://flagcdn.com/w40/${iso.lowercase()}.png",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Русские/англ. названия → ISO 3166-1 alpha-2 для flagcdn. */
private val CountryIso = mapOf(
    "россия" to "ru",
    "рф" to "ru",
    "russia" to "ru",
    "russian federation" to "ru",
    "ссср" to "ru",
    "сша" to "us",
    "соединенные штаты" to "us",
    "соединённые штаты" to "us",
    "соединенные штаты америки" to "us",
    "соединённые штаты америки" to "us",
    "америка" to "us",
    "usa" to "us",
    "united states" to "us",
    "united states of america" to "us",
    "великобритания" to "gb",
    "англия" to "gb",
    "britain" to "gb",
    "united kingdom" to "gb",
    "uk" to "gb",
    "great britain" to "gb",
    "франция" to "fr",
    "france" to "fr",
    "германия" to "de",
    "germany" to "de",
    "италия" to "it",
    "italy" to "it",
    "испания" to "es",
    "spain" to "es",
    "канада" to "ca",
    "canada" to "ca",
    "китай" to "cn",
    "кнр" to "cn",
    "china" to "cn",
    "япония" to "jp",
    "japan" to "jp",
    "южная корея" to "kr",
    "республика корея" to "kr",
    "корея" to "kr",
    "south korea" to "kr",
    "korea" to "kr",
    "северная корея" to "kp",
    "кндр" to "kp",
    "north korea" to "kp",
    "индия" to "in",
    "india" to "in",
    "австралия" to "au",
    "australia" to "au",
    "бразилия" to "br",
    "brazil" to "br",
    "мексика" to "mx",
    "mexico" to "mx",
    "турция" to "tr",
    "turkey" to "tr",
    "türkiye" to "tr",
    "украина" to "ua",
    "ukraine" to "ua",
    "польша" to "pl",
    "poland" to "pl",
    "нидерланды" to "nl",
    "голландия" to "nl",
    "netherlands" to "nl",
    "бельгия" to "be",
    "belgium" to "be",
    "швеция" to "se",
    "sweden" to "se",
    "норвегия" to "no",
    "norway" to "no",
    "дания" to "dk",
    "denmark" to "dk",
    "финляндия" to "fi",
    "finland" to "fi",
    "швейцария" to "ch",
    "switzerland" to "ch",
    "австрия" to "at",
    "austria" to "at",
    "чехия" to "cz",
    "czech" to "cz",
    "czech republic" to "cz",
    "венгрия" to "hu",
    "hungary" to "hu",
    "румыния" to "ro",
    "romania" to "ro",
    "греция" to "gr",
    "greece" to "gr",
    "португалия" to "pt",
    "portugal" to "pt",
    "ирландия" to "ie",
    "ireland" to "ie",
    "новая зеландия" to "nz",
    "new zealand" to "nz",
    "аргентина" to "ar",
    "argentina" to "ar",
    "чили" to "cl",
    "chile" to "cl",
    "гонконг" to "hk",
    "hong kong" to "hk",
    "тайвань" to "tw",
    "taiwan" to "tw",
    "таиланд" to "th",
    "тайланд" to "th",
    "thailand" to "th",
    "вьетнам" to "vn",
    "vietnam" to "vn",
    "индонезия" to "id",
    "indonesia" to "id",
    "филиппины" to "ph",
    "philippines" to "ph",
    "малайзия" to "my",
    "malaysia" to "my",
    "сингапур" to "sg",
    "singapore" to "sg",
    "оаэ" to "ae",
    "объединенные арабские эмираты" to "ae",
    "объединённые арабские эмираты" to "ae",
    "uae" to "ae",
    "united arab emirates" to "ae",
    "саудовская аравия" to "sa",
    "saudi arabia" to "sa",
    "израиль" to "il",
    "israel" to "il",
    "иран" to "ir",
    "iran" to "ir",
    "египет" to "eg",
    "egypt" to "eg",
    "юар" to "za",
    "южно-африканская республика" to "za",
    "south africa" to "za",
    "казахстан" to "kz",
    "kazakhstan" to "kz",
    "беларусь" to "by",
    "белоруссия" to "by",
    "belarus" to "by",
    "грузия" to "ge",
    "georgia" to "ge",
    "армения" to "am",
    "armenia" to "am",
    "азербайджан" to "az",
    "azerbaijan" to "az",
    "узбекистан" to "uz",
    "uzbekistan" to "uz",
    "латвия" to "lv",
    "latvia" to "lv",
    "литва" to "lt",
    "lithuania" to "lt",
    "эстония" to "ee",
    "estonia" to "ee",
    "болгария" to "bg",
    "bulgaria" to "bg",
    "сербия" to "rs",
    "serbia" to "rs",
    "хорватия" to "hr",
    "croatia" to "hr",
    "словакия" to "sk",
    "slovakia" to "sk",
    "словения" to "si",
    "slovenia" to "si",
    "исландия" to "is",
    "iceland" to "is",
    "люксембург" to "lu",
    "luxembourg" to "lu",
    "мальта" to "mt",
    "malta" to "mt",
    "кипр" to "cy",
    "cyprus" to "cy",
    "куба" to "cu",
    "cuba" to "cu",
    "колумбия" to "co",
    "colombia" to "co",
    "перу" to "pe",
    "peru" to "pe",
    "венесуэла" to "ve",
    "venezuela" to "ve",
    "пакистан" to "pk",
    "pakistan" to "pk",
    "бангладеш" to "bd",
    "bangladesh" to "bd",
    "нигерия" to "ng",
    "nigeria" to "ng",
    "марокко" to "ma",
    "morocco" to "ma",
    "алжир" to "dz",
    "algeria" to "dz",
    "тунис" to "tn",
    "tunisia" to "tn",
    "евросоюз" to "eu",
    "european union" to "eu",
    "eu" to "eu",
)
