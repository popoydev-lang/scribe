package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

object PsgcData {
    val provinceCitiesMap: Map<String, List<String>> = mapOf(
        "Metro Manila" to listOf("Manila", "Quezon City", "Caloocan", "Las Piñas", "Makati", "Malabon", "Mandaluyong", "Marikina", "Muntinlupa", "Navotas", "Parañaque", "Pasay", "Pasig", "Pateros", "San Juan", "Taguig", "Valenzuela"),
        "Abra" to listOf("Bangued", "Bucay", "Dolores", "La Paz", "Lagangilang", "San Juan", "Tayum", "Manabo", "Pilar", "Peñarrubia"),
        "Agusan del Norte" to listOf("Butuan City", "Cabadbaran City", "Buenavista", "Carmen", "Jabonga", "Kitcharao", "Las Nieves", "Magallanes", "Nasipit", "Santiago", "Tubay"),
        "Agusan del Sur" to listOf("Bayugan City", "Prosperidad", "San Francisco", "Bunawan", "Esperanza", "Loreto", "Santa Josefa", "Talacogon", "Trento", "Veruela", "Sibagat"),
        "Aklan" to listOf("Kalibo", "Malay", "Ibajay", "Numancia", "New Washington", "Banga", "Batan", "Makato", "Nabas", "Balete"),
        "Albay" to listOf("Legazpi City", "Daraga", "Tabaco City", "Ligao City", "Camalig", "Guinobatan", "Polangui", "Tiwi", "Oas", "Malilipot", "Bacacay"),
        "Antique" to listOf("San Jose de Buenavista", "Sibalom", "Hamtic", "Culasi", "Pandan", "Bugasong", "Patnongon", "San Remigio", "Tobias Fornier"),
        "Apayao" to listOf("Kabugao", "Conner", "Flora", "Luna", "Pudtol", "Santa Marcela", "Calanasan"),
        "Aurora" to listOf("Baler", "Maria Aurora", "Casiguran", "Dipaculao", "Dinalungan", "Dilasag", "San Luis", "Dingalan"),
        "Basilan" to listOf("Isabela City", "Lamitan City", "Tuburan", "Tipo-Tipo", "Lantawan", "Sumisip", "Maluso", "Ungkaya Pukan"),
        "Bataan" to listOf("Balanga City", "Mariveles", "Dinalupihan", "Hermosa", "Orani", "Orion", "Limay", "Abucay", "Bagac", "Morong", "Pilar", "Samal"),
        "Batanes" to listOf("Basco", "Itbayat", "Ivana", "Mahatao", "Sabtang", "Uyugan"),
        "Batangas" to listOf("Batangas City", "Lipa City", "Tanauan City", "Santo Tomas City", "Nasugbu", "Calaca City", "Bauan", "Balayan", "Lemery", "Taal", "Mabini", "Lian", "San Juan", "Rosario", "Malvar"),
        "Benguet" to listOf("Baguio City", "La Trinidad", "Itogon", "Tuba", "Tublay", "Sablan", "Mankayan", "Kapangan", "Buguias", "Kabayan"),
        "Biliran" to listOf("Naval", "Almeria", "Biliran", "Cabucgayan", "Caibiran", "Culaba", "Kawayan", "Maripipi"),
        "Bohol" to listOf("Tagbilaran City", "Tubigon", "Talibon", "Ubay", "Jagna", "Loon", "Calape", "Carmen", "Alicia", "Guindulman", "Inabanga", "Loboc", "Panglao", "Dauis", "Baclayon", "Balilihan", "Corella", "Cortes", "Maribojoc"),
        "Bukidnon" to listOf("Malaybalay City", "Valencia City", "Maramag", "Quezon", "Manolo Fortich", "Libona", "Baungon", "Talakag", "Impasugong", "Lantapan", "Cabanglasan", "San Fernando"),
        "Bulacan" to listOf("Malolos City", "Meycauayan City", "San Jose del Monte City", "Baliuag City", "Marilao", "Bocaue", "Santa Maria", "Guiguinto", "Plaridel", "Hagonoy", "San Miguel", "San Ildefonso", "Bustos", "Norzagaray", "Pandi", "Calumpit", "Bulakan", "Pulilan", "Balagtas", "Obando", "Paombong", "Angat", "San Rafael"),
        "Cagayan" to listOf("Tuguegarao City", "Aparri", "Lal-lo", "Baggao", "Ballesteros", "Claveria", "Gonzaga", "Gattaran", "Tuao", "Solana", "Buguey", "Enrile", "Peñablanca"),
        "Camarines Norte" to listOf("Daet", "Jose Panganiban", "Labo", "Mercedes", "Paracale", "Basud", "Capalonga", "San Vicente", "Santa Elena", "Talisay", "Vinzons"),
        "Camarines Sur" to listOf("Naga City", "Iriga City", "Pili", "Calabanga", "Libmanan", "Sipocot", "Nabua", "Goa", "Tinambac", "Bula", "Canaman", "Caramoan", "Buhi", "Baao", "Ocampo"),
        "Camiguin" to listOf("Mambajao", "Catarman", "Guinsiliban", "Mahinog", "Sagay"),
        "Capiz" to listOf("Roxas City", "Panay", "Pontevedra", "Pilar", "Dumarao", "Dumalag", "Dao", "Mambusao", "Sigma", "Jamindan", "Tapaz"),
        "Catanduanes" to listOf("Virac", "Bato", "San Andres", "Caramoran", "Pandan", "Bagamanoc", "Panganiban", "Viga", "Gigmoto", "San Miguel", "Baras"),
        "Cavite" to listOf("Bacoor City", "Imus City", "Dasmariñas City", "Tagaytay City", "General Trias City", "Silang", "Cavite City", "Trece Martires City", "Kawit", "Noveleta", "Naic", "Rosario", "Tanza", "Indang", "Alfonso", "Amadeo", "Maragondon", "General Mariano Alvarez", "Carmona City", "Magallanes", "Mendez", "Ternate"),
        "Cebu" to listOf("Cebu City", "Mandaue City", "Lapu-Lapu City", "Talisay City", "Toledo City", "Danao City", "Carcar City", "Naga City", "Bogo City", "Consolacion", "Liloan", "Minglanilla", "Cordova", "Bantayan", "Balamban", "Argao", "Sibonga", "Carmen", "Tuburan", "Compostela", "San Fernando", "Ginatilan", "Barili", "Oslob", "Moalboal", "Pinamungajan"),
        "Cotabato" to listOf("Kidapawan City", "Midsayap", "Kabacan", "Pigcawayan", "Makilala", "Carmen", "Libungan", "Banisilan", "Tulunan", "Arakan", "President Roxas", "Antipas", "Magpet"),
        "Davao de Oro" to listOf("Nabunturan", "Monkayo", "Compostela", "Pantukan", "Maco", "Maragusan", "Mawab", "Montevista", "New Bataan", "Laak"),
        "Davao del Norte" to listOf("Tagum City", "Panabo City", "Island Garden City of Samal", "Carmen", "Sto. Tomas", "Asuncion", "Kapalong", "New Corella", "San Isidro", "Talaingod", "Braulio E. Dujali"),
        "Davao del Sur" to listOf("Davao City", "Digos City", "Santa Cruz", "Bansalan", "Hagonoy", "Malalag", "Magsaysay", "Padada", "Kiblawan", "Sulop"),
        "Davao Occidental" to listOf("Malita", "Santa Maria", "Don Marcelino", "Jose Abad Santos", "Sarangani"),
        "Davao Oriental" to listOf("Mati City", "Lupon", "Banaybanay", "Cateel", "Boston", "Caraga", "Manay", "San Isidro", "Governor Generoso", "Baganga", "Tarragona"),
        "Dinagat Islands" to listOf("San Jose", "Basilisa", "Cagdianao", "Dinagat", "Libjo", "Loreto", "Tubajon"),
        "Eastern Samar" to listOf("Borongan City", "Guiuan", "Taft", "Dolores", "Oras", "General MacArthur", "Llorente", "Balangiga", "Arteche", "Maydolong", "San Julian"),
        "Guimaras" to listOf("Jordan", "Buenavista", "Nueva Valencia", "San Lorenzo", "Sibunag"),
        "Ifugao" to listOf("Lagawe", "Banaue", "Alfonso Lista", "Lamut", "Aguinaldo", "Kiangan", "Hingyon", "Mayoyao", "Asipulo", "Tinoc"),
        "Ilocos Norte" to listOf("Laoag City", "Batac City", "Bangui", "Pagudpud", "San Nicolas", "Paoay", "Bacarra", "Dingras", "Pasuquin", "Sarrat", "Currimao"),
        "Ilocos Sur" to listOf("Vigan City", "Candon City", "Narvacan", "Tagudin", "Sinait", "Cabugao", "Santa Maria", "Magsingal", "Bantay", "San Juan", "Santo Domingo"),
        "Iloilo" to listOf("Iloilo City", "Passi City", "Oton", "Tigbauan", "Miagao", "Guimbal", "Santa Barbara", "Cabatuan", "Janiuay", "Pototan", "Dumangas", "Barotac Nuevo", "Barotac Viejo", "Lambunao", "Calinog", "Carles", "Estancia", "Sara", "Leganes", "Pavia", "San Joaquin"),
        "Isabela" to listOf("Ilagan City", "Santiago City", "Cauayan City", "Echague", "Alicia", "Cabagan", "Roxas", "Tumauini", "San Mariano", "Ramon", "Angadanan", "Jones", "San Mateo"),
        "Kalinga" to listOf("Tabuk City", "Rizal", "Balbalan", "Lubuagan", "Pasil", "Pinukpuk", "Tinglayan", "Tanudan"),
        "La Union" to listOf("San Fernando City", "Bauang", "Agoo", "Tubao", "Rosario", "Bacnotan", "Balaoan", "Bangar", "Naguilian", "San Juan", "Caba"),
        "Laguna" to listOf("Calamba City", "Santa Rosa City", "Biñan City", "San Pedro City", "Cabuyao City", "San Pablo City", "Santa Cruz", "Los Baños", "Pagsanjan", "Calauan", "Bay", "Victoria", "Siniloan", "Nagcarlan", "Alaminos", "Paete", "Majayjay", "Cavinti", "Liliw", "Luisiana", "Lumban", "Pangil", "Pila"),
        "Lanao del Norte" to listOf("Iligan City", "Tubod", "Kapatagan", "Maigo", "Lala", "Kolambugan", "Balo-i", "Linamon", "Baroy", "Salvador", "Sapad", "Kauswagan"),
        "Lanao del Sur" to listOf("Marawi City", "Balindong", "Wao", "Malabang", "Ganassi", "Tugaya", "Marantao", "Saguiaran", "Ramain", "Bayang", "Bacolod-Kalawi"),
        "Leyte" to listOf("Tacloban City", "Ormoc City", "Baybay City", "Palo", "Tanauan", "Abuyog", "Carigara", "Hilongos", "Burauen", "Dulag", "Jaro", "Bato", "Isabel", "Merida", "Palompon", "Kananga", "Capoocan", "Albuera", "Dagami"),
        "Maguindanao del Norte" to listOf("Cotabato City", "Datu Odin Sinsuat", "Upi", "Parang", "Sultan Kudarat", "Buldon", "Barira", "Matanog"),
        "Maguindanao del Sur" to listOf("Shariff Aguak", "Buluan", "Datu Paglas", "Pagalungan", "Gen. S.K. Pendatun", "Ampatuan", "Mamasapano", "Paglat"),
        "Marinduque" to listOf("Boac", "Mogpog", "Santa Cruz", "Gasan", "Buenavista", "Torrijos"),
        "Masbate" to listOf("Masbate City", "Aroroy", "Milagros", "Cataingan", "Claveria", "San Pascual", "Cawayan", "Baleno", "Mobo", "Uson", "Placer"),
        "Misamis Occidental" to listOf("Ozamiz City", "Oroquieta City", "Tangub City", "Clarin", "Jimenez", "Lopez Jaena", "Plaridel", "Aloran", "Tudela", "Sinacaban", "Calamba"),
        "Misamis Oriental" to listOf("Cagayan de Oro City", "Gingoog City", "El Salvador City", "Opol", "Tagoloan", "Villanueva", "Claveria", "Balingasag", "Alubijid", "Initao", "Gitagum", "Manticao"),
        "Mountain Province" to listOf("Bontoc", "Sagada", "Bauko", "Tadian", "Besao", "Sabangan", "Sadanga", "Paracelis", "Natonin", "Barlig"),
        "Negros Occidental" to listOf("Bacolod City", "Bago City", "Cadiz City", "Escalante City", "Himamaylan City", "Kabankalan City", "La Carlota City", "Sagay City", "San Carlos City", "Silay City", "Sipalay City", "Talisay City", "Victorias City", "Murcia", "Pontevedra", "Pulupandan", "Valladolid", "Hinigaran", "Enrique B. Magalona", "Binalbagan", "Isabela", "San Enrique"),
        "Negros Oriental" to listOf("Dumaguete City", "Bayawan City", "Tanjay City", "Bais City", "Canlaon City", "Guihulngan City", "Sibulan", "Valencia", "Bacong", "Dauin", "Zanguanguita", "Siaton", "Santa Catalina", "Manjuyod", "Bindoy", "Ayungon"),
        "Northern Samar" to listOf("Catarman", "Laoang", "Allen", "Gamay", "Lavezares", "San Jose", "Mondragon", "Bobon", "Pambujan", "Palapag", "Catubig"),
        "Nueva Ecija" to listOf("Cabanatuan City", "San Jose City", "Gapan City", "Science City of Muñoz", "Palayan City", "Talavera", "Guimba", "San Isidro", "Cabiao", "Zaragoza", "Santa Rosa", "Aliaga", "Gen. Tinio"),
        "Nueva Vizcaya" to listOf("Bayombong", "Solano", "Bagabag", "Bambang", "Dupax del Norte", "Dupax del Sur", "Aritao", "Kasibu", "Santa Fe", "Villaverde", "Diadi"),
        "Occidental Mindoro" to listOf("Mamburao", "San Jose", "Sablayan", "Lubang", "Looc", "Abra de Ilog", "Calintaan", "Magsaysay", "Paluan", "Rizal", "Santa Cruz"),
        "Oriental Mindoro" to listOf("Calapan City", "Puerto Galera", "Naujan", "Pinamalayan", "Roxas", "Bongabong", "Mansalay", "San Teodoro", "Baco", "Socorro", "Pola"),
        "Palawan" to listOf("Puerto Princesa City", "El Nido", "Coron", "Taytay", "Narra", "Brooke's Point", "Roxas", "Aborlan", "Bataraza", "Busuanga", "Quezon", "San Vicente"),
        "Pampanga" to listOf("Angeles City", "San Fernando City", "Mabalacat City", "Guagua", "Lubao", "Mexico", "Arayat", "Apalit", "Floridablanca", "Candaba", "Macabebe", "Masantol", "Porac", "San Luis", "San Simon", "Santa Rita", "Santo Tomas", "Sasmuan", "Bacolor", "Minalin"),
        "Pangasinan" to listOf("Dagupan City", "Urdaneta City", "Alaminos City", "San Carlos City", "Lingayen", "Calasiao", "Mangaldan", "Manaoag", "Binmaley", "Bayambang", "Malasiqui", "Mangatarem", "San Fabian", "Bugallon", "Rosales", "Pozorrubio", "Villasis", "Bolinao", "Tayug", "San Manuel", "Umingan", "Sual", "Infanta", "Anda"),
        "Quezon" to listOf("Lucena City", "Tayabas City", "Sariaya", "Candelaria", "Tiaong", "Pagbilao", "Mauban", "Infanta", "Real", "General Nakar", "Gumaca", "Lopez", "Calauag", "Guinayangan", "Alabat"),
        "Quirino" to listOf("Cabarroguis", "Diffun", "Maddela", "Saguday", "Aglipay", "Nagtipunan"),
        "Rizal" to listOf("Antipolo City", "Cainta", "Taytay", "Angono", "Binangonan", "San Mateo", "Rodriguez", "Tanay", "Morong", "Pililla", "Baras", "Cardona", "Teresa", "Jalajala"),
        "Romblon" to listOf("Romblon", "Odiongan", "Looc", "San Fernando", "Cajidiocan", "Magdiwang", "Alcantara", "Santa Fe", "Calatrava", "San Andres"),
        "Samar" to listOf("Catbalogan City", "Calbayog City", "Tarangnan", "Gandara", "Basey", "Santa Rita", "Wright", "Zumarraga", "Pinabacdao", "Jiabong", "San Sebastian"),
        "Sarangani" to listOf("Alabel", "Glan", "Kiamba", "Maitum", "Malapatan", "Malungon", "Maasim"),
        "Siquijor" to listOf("Siquijor", "Larena", "Lazi", "San Juan", "Maria", "Enrique Villanueva"),
        "Sorsogon" to listOf("Sorsogon City", "Bulan", "Gubat", "Irosin", "Casiguran", "Castilla", "Donsol", "Pilar", "Prieto Diaz", "Juban", "Barcelona", "Bulusan", "Matnog", "Magallanes"),
        "South Cotabato" to listOf("General Santos City", "Koronadal City", "Polomolok", "Tupi", "Surallah", "Banga", "Norala", "Tantangan", "Tampakan", "Santo Niño", "Lake Sebu"),
        "Southern Leyte" to listOf("Maasin City", "Sogod", "Liloan", "Saint Bernard", "Hinunangan", "Hinundayan", "Bontoc", "Macrohon", "Padre Burgos", "Silago", "Malitbog", "Limasawa"),
        "Sultan Kudarat" to listOf("Isulan", "Tacurong City", "Lebak", "Kalamansig", "Esperanza", "Bagumbayan", "Lambayong", "Senator Ninoy Aquino", "Columbio", "President Quirino"),
        "Sulu" to listOf("Jolo", "Patikul", "Indanan", "Parang", "Maimbung", "Talipao", "Siasi", "Pangutaran", "Luuk"),
        "Surigao del Norte" to listOf("Surigao City", "General Luna", "Del Carmen", "Dapa", "Claver", "Placer", "Mainit", "Tubod", "Bacuag", "San Francisco"),
        "Surigao del Sur" to listOf("Tandag City", "Bislig City", "Barobo", "Cantilan", "Madrid", "Hinatuan", "Tagbina", "Carrascal", "Lanuza", "San Agustin", "Cortes", "Lianga"),
        "Tarlac" to listOf("Tarlac City", "Concepcion", "Capas", "Paniqui", "Victoria", "Camiling", "Gerona", "Moncada", "Bamban", "Santa Ignacia", "Pura", "Mayantoc", "San Manuel"),
        "Tawi-Tawi" to listOf("Bongao", "Sitangkai", "Panglima Sugala", "Simunul", "Mapun", "South Ubian", "Sapa-Sapa"),
        "Zambales" to listOf("Olongapo City", "Subic", "Castillejos", "San Marcelino", "San Antonio", "San Narciso", "San Felipe", "Cabangan", "Botolan", "Iba", "Masinloc", "Palauig", "Santa Cruz", "Candelaria"),
        "Zamboanga del Norte" to listOf("Dipolog City", "Dapitan City", "Sindangan", "Liloy", "Labason", "Manukan", "Polanco", "Katipunan", "Roxas", "Siocon", "Gutalac"),
        "Zamboanga del Sur" to listOf("Zamboanga City", "Pagadian City", "Aurora", "Molave", "Margosatubig", "Kumalarang", "Labangan", "Dumalinao", "San Pablo", "Guipos", "Ramon Magsaysay"),
        "Zamboanga Sibugay" to listOf("Ipil", "Kabasalan", "Alicia", "Buug", "Diplahan", "Imelda", "Malangas", "Naga", "Roseller Lim", "Siay", "Titay", "Tungawan")
    )

    val provinceNameToCode = mapOf(
        "Ilocos Norte" to "0102800000",
        "Ilocos Sur" to "0102900000",
        "La Union" to "0103300000",
        "Pangasinan" to "0105500000",
        "Batanes" to "0200900000",
        "Cagayan" to "0201500000",
        "Isabela" to "0203100000",
        "Nueva Vizcaya" to "0205000000",
        "Quirino" to "0205700000",
        "Bataan" to "0300800000",
        "Bulacan" to "0301400000",
        "Nueva Ecija" to "0304900000",
        "Pampanga" to "0305400000",
        "Tarlac" to "0306900000",
        "Zambales" to "0307100000",
        "Aurora" to "0307700000",
        "Batangas" to "0401000000",
        "Cavite" to "0402100000",
        "Laguna" to "0403400000",
        "Quezon" to "0405600000",
        "Rizal" to "0405800000",
        "Marinduque" to "1704000000",
        "Occidental Mindoro" to "1705100000",
        "Oriental Mindoro" to "1705200000",
        "Palawan" to "1705300000",
        "Romblon" to "1705900000",
        "Albay" to "0500500000",
        "Camarines Norte" to "0501600000",
        "Camarines Sur" to "0501700000",
        "Catanduanes" to "0502000000",
        "Masbate" to "0504100000",
        "Sorsogon" to "0506200000",
        "Aklan" to "0600400000",
        "Antique" to "0600600000",
        "Capiz" to "0601900000",
        "Iloilo" to "0603000000",
        "Negros Occidental" to "0604500000",
        "Guimaras" to "0607900000",
        "Bohol" to "0701200000",
        "Cebu" to "0702200000",
        "Negros Oriental" to "0704600000",
        "Siquijor" to "0706100000",
        "Eastern Samar" to "0802600000",
        "Leyte" to "0803700000",
        "Northern Samar" to "0804800000",
        "Samar" to "0806000000",
        "Southern Leyte" to "0806400000",
        "Biliran" to "0807800000",
        "Zamboanga del Norte" to "0907200000",
        "Zamboanga del Sur" to "0907300000",
        "Zamboanga Sibugay" to "0908300000",
        "Bukidnon" to "1001300000",
        "Camiguin" to "1001800000",
        "Lanao del Norte" to "1003500000",
        "Misamis Occidental" to "1004200000",
        "Misamis Oriental" to "1004300000",
        "Davao del Norte" to "1102300000",
        "Davao del Sur" to "1102400000",
        "Davao Oriental" to "1102500000",
        "Davao de Oro" to "1108200000",
        "Davao Occidental" to "1108600000",
        "Cotabato" to "1204700000",
        "South Cotabato" to "1206300000",
        "Sultan Kudarat" to "1206500000",
        "Sarangani" to "1208000000",
        "Abra" to "1400100000",
        "Benguet" to "1401100000",
        "Ifugao" to "1402700000",
        "Kalinga" to "1403200000",
        "Mountain Province" to "1404400000",
        "Apayao" to "1408100000",
        "Agusan del Norte" to "1600200000",
        "Agusan del Sur" to "1600300000",
        "Surigao del Norte" to "1606700000",
        "Surigao del Sur" to "1606800000",
        "Dinagat Islands" to "1608500000",
        "Basilan" to "1900700000",
        "Lanao del Sur" to "1903600000",
        "Sulu" to "1906600000",
        "Tawi-Tawi" to "1907000000",
        "Maguindanao del Norte" to "1908700000",
        "Maguindanao del Sur" to "1908800000"
    )

    fun getCitiesAndMunicipalitiesForProvince(province: String): List<String> {
        return provinceCitiesMap[province] ?: listOf(
            "Poblacion", "Centro", "San Jose", "San Pedro", "San Miguel", "Santa Maria", "Santo Tomas"
        )
    }

    suspend fun fetchCitiesForProvinceAsync(province: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val code = provinceNameToCode[province]
            val url = if (province == "Metro Manila") {
                "https://psgc.cloud/api/regions/1300000000/cities-municipalities"
            } else if (code != null) {
                "https://psgc.cloud/api/provinces/$code/cities-municipalities"
            } else {
                null
            }

            if (url == null) {
                return@withContext getCitiesAndMunicipalitiesForProvince(province)
            }

            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("PsgcData", "Unexpected code $response")
                    return@withContext getCitiesAndMunicipalitiesForProvince(province)
                }
                val bodyStr = response.body?.string() ?: return@withContext getCitiesAndMunicipalitiesForProvince(province)
                val jsonArray = JSONArray(bodyStr)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val rawName = item.optString("name", "")
                    if (rawName.isNotEmpty()) {
                        list.add(cleanCityName(rawName))
                    }
                }
                if (list.isNotEmpty()) {
                    return@withContext list.distinct().sorted()
                }
            }
        } catch (e: Exception) {
            Log.e("PsgcData", "Error fetching from PSGC API for $province", e)
        }
        return@withContext getCitiesAndMunicipalitiesForProvince(province)
    }

    private fun cleanCityName(name: String): String {
        var cleaned = name
        // Clean double-UTF8 / Mojibake encoding of 'ñ'
        cleaned = cleaned.replace("Las Pi\u00c3\u00b1as", "Las Piñas")
        cleaned = cleaned.replace("Para\u00c3\u00b1aque", "Parañaque")
        cleaned = cleaned.replace("Pi\u00c3\u00b1a", "Piña")
        cleaned = cleaned.replace("pi\u00c3\u00b1a", "piña")

        // Clean common starting "City of " or "City Of " to look extremely professional and clean
        if (cleaned.startsWith("City of ", ignoreCase = true)) {
            cleaned = cleaned.substring(8)
        }
        return cleaned.trim()
    }
}
