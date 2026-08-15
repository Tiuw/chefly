package com.skripsi.chefly.util

fun String.toDatabaseKey(): String {
    return when (val cleanedLabel = this.trim().lowercase()) {
        // 1. CABAI & BAWANG
        "cabai merah", "cabe merah" -> "cabe_merah"
        "cabai hijau", "cabe hijau" -> "cabe_hijau"
        "cabai rawit", "cabe rawit" -> "cabe_rawit"
        "bawang merah" -> "bawang_merah"
        "bawang putih" -> "bawang_putih"
        "bawang bombay", "bawang bombai" -> "bawang_bombay"
        "daun bawang", "loncang" -> "daun_bawang"

        // 2. DAUN, REMPAH & BUMBU DAPUR
        "daun jeruk" -> "daun_jeruk"
        "daun salam" -> "daun_salam"
        "daun kemangi", "kemangi" -> "daun_kemangi"
        "daun pandan", "pandan" -> "daun_pandan"
        "daun seledri", "seledri" -> "daun_seledri"
        "daun kunyit" -> "daun_kunyit"
        "daun pisang" -> "daun_pisang"
        "daun singkong" -> "daun_singkong"
        "daun pepaya" -> "daun_pepaya"
        "pala", "biji pala" -> "pala"
        "asam jawa", "asem jawa" -> "asam_jawa"
        "bunga lawang", "pekak" -> "bunga_lawang"
        "kapulaga" -> "kapulaga"
        "kayu manis" -> "kayu_manis"
        "ketumbar", "biji ketumbar" -> "ketumbar"
        "merica", "lada", "lada bubuk" -> "merica"
        "jahe" -> "jahe"
        "kunyit", "kunir" -> "kunyit"
        "laos" -> "lengkuas"
        "lengkuas" -> "lengkuas"
        "serai", "sereh" -> "serai"
        "kemiri", "biji kemiri" -> "kemiri"
        "terasi", "belacan" -> "terasi"
        "jeruk nipis" -> "jeruk_nipis"
        "kencur" -> "kencur"

        // 3. PENYEDAP & KALDU BUBUK
        "penyedap", "micin", "msg", "kaldu bubuk", "penyedap rasa" -> "penyedap"

        // 4. GULA, GARAM, SAUS & MINYAK
        "gula merah", "gula jawa", "gula aren" -> "gula_merah"
        "gula pasir", "gula" -> "gula_pasir"
        "garam" -> "garam"
        "kecap manis" -> "kecap_manis"
        "kecap asin" -> "kecap_asin"
        "saus tiram" -> "saus_tiram"
        "saus sambal", "saos sambal" -> "saus_sambal"
        "saus tomat", "saos tomat" -> "saus_tomat"
        "santan", "santan kelapa" -> "santan"
        "kelapa parut" -> "kelapa_parut"
        "mentega" -> "mentega"
        "margarin" -> "margarin"

        // 5. DAGING, UNGGAS, SEAFOOD & TELUR
        "ayam", "daging ayam" -> "daging_ayam"
        "sapi", "daging sapi" -> "daging_sapi"
        "kambing", "daging kambing" -> "daging_kambing"
        "ikan" -> "ikan"
        "udang" -> "udang"
        "telur", "telur ayam" -> "telur"

        // 6. SAYUR, JAMUR & KACANG-KACANGAN
        "tahu" -> "tahu"
        "tempe" -> "tempe"
        "jamur" -> "jamur"
        "kentang" -> "kentang"
        "wortel" -> "wortel"
        "kol", "kubis" -> "kubis"
        "bayam" -> "bayam"
        "kangkung" -> "kangkung"
        "tomat" -> "tomat"
        "jagung", "jagung manis" -> "jagung"
        "sawi", "sawi hijau", "caisim" -> "sawi"
        "terong", "terung" -> "terong"
        "kacang tanah" -> "kacang_tanah"
        "kacang panjang" -> "kacang_panjang"
        "kacang hijau", "kacang ijo" -> "kacang_hijau"
        "kacang merah" -> "kacang_merah"
        "brokoli" -> "brokoli"

        // 7. TEPUNG, KARBOHIDRAT, OLAHAN & DAIRY
        "nasi" -> "nasi"
        "keju" -> "keju"
        "susu", "susu cair" -> "susu"
        "tepung terigu", "terigu" -> "tepung_terigu"
        "tepung beras" -> "tepung_beras"
        "tepung tapioka", "tapioka", "kanji" -> "tepung_tapioka"
        "tepung maizena", "maizena" -> "tepung_maizena"
        "tepung panir", "tepung roti" -> "tepung_panir"
        "tepung bumbu", "tepung serbaguna" -> "tepung_bumbu"
        "makaroni" -> "makaroni"
        "bihun" -> "bihun"
        "mie", "mi" -> "mie"
        "roti", "roti tawar" -> "roti"
        "sosis" -> "sosis"

        // DEFAULT: Ganti spasi/karakter pemisah menjadi underscore
        else -> cleanedLabel.replace(" ", "_")
    }
}