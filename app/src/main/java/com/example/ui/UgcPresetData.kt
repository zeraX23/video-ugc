package com.example.ui

data class UgcCategory(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val presets: List<UgcPresetPrompt>
)

data class UgcPresetPrompt(
    val title: String,
    val subText: String,
    val promptText: String
)

data class UgcStyle(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val toneInstruction: String
)

object UgcPresetData {

    val categories = listOf(
        UgcCategory(
            id = "fnd",
            name = "Kuliner / F&B",
            iconEmoji = "🍔",
            presets = listOf(
                UgcPresetPrompt(
                    title = "Bakso Mercon Viral",
                    subText = "Review bakso pedas gila berlimpah cabai",
                    promptText = "Mempromosikan bakso mercon jumbo lokal seharga 15 ribu rupiah yang kuahnya merah menyala penuh cabai rawit melimpah. Rekam ekspresi kaget talent saat suapan pertama karena pedasnya gigit banget tapi gurih bikin ketagihan."
                ),
                UgcPresetPrompt(
                    title = "Kopi Susu Gula Aren",
                    subText = "Kopi creamy pelibur lelah kerja / lembur",
                    promptText = "Review kopi susu gula aren botolan premium yang sangat creamy dan wangi kopinya pekat. Cocok untuk mengembalikan fokus pekerja kantoran yang lembur malam atau mahasiswa yang sedang dikejar deadline tugas kuliah."
                ),
                UgcPresetPrompt(
                    title = "Keripik Kaca Kriuk",
                    subText = "Review keripik super tipis nan renyah",
                    promptText = "Review snack keripik kaca pedas daun jeruk yang super tipis, renyah, dan tidak keras saat digigit. Tunjukkan suara kriuk nyaring saat digigit di depan mikrofon untuk efek ASMR yang menggugah selera."
                )
            )
        ),
        UgcCategory(
            id = "beauty",
            name = "Kecantikan / Outfit",
            iconEmoji = "💅",
            presets = listOf(
                UgcPresetPrompt(
                    title = "Serum Glow Up 3 Detik",
                    subText = "Serum lokal mencerahkan noda jerawat",
                    promptText = "Mempromosikan serum wajah lokal berbahan aktif Niacinamide yang meresap cepat dalam 3 detik, terasa dingin menenangkan di kulit, dan terbukti mencerahkan noda hitam bekas jerawat dalam 7 hari pemakaian."
                ),
                UgcPresetPrompt(
                    title = "Outfit Kuliah 100 Ribuan",
                    subText = "Mix & match setelan kuliah keren murah",
                    promptText = "Mekomendasikan padu padan (mix & match) outfit kuliah kemeja oversized linen dan celana highwaist kulot seharga total di bawah 100 ribu rupiah. Tunjukkan bahan kain lembut, tidak menerawang, dan pas dipakai beraktivitas seharian."
                ),
                UgcPresetPrompt(
                    title = "Lip Tint Anti Transfer",
                    subText = "Lip tint tahan luntur seharian",
                    promptText = "Review ketahanan produk lip tint bertekstur gel ringan yang melembabkan bibir, tidak lengket, transferproof (anti luntur) meskipun dipakai makan mie kuah pedas dan minum es kopi susu dari straw sedotan."
                )
            )
        ),
        UgcCategory(
            id = "tech",
            name = "Teknologi / Gadget",
            iconEmoji = "💻",
            presets = listOf(
                UgcPresetPrompt(
                    title = "TWS Bass Konser 100K",
                    subText = "Review earphone bluetooth murah ngebass",
                    promptText = "Review TWS bluetooth nirkabel seharga 100 ribuan dengan fitur bass menggelegar layaknya nonton konser musik langsung. Tunjukkan case bermagnet kuat, indikator baterai LED digital, dan suara surround kedap bising."
                ),
                UgcPresetPrompt(
                    title = "Cooling Fan HP Anti Lag",
                    subText = "Aksesoris gaming bikin HP tetap dingin",
                    promptText = "Membuktikan performa cooling fan eksternal berklip RGB untuk smartphone gaming. Tunjukkan suhu HP yang langsung turun drastis dari 42 derajat ke 25 derajat saat main game online berat, tangan bebas keringat."
                ),
                UgcPresetPrompt(
                    title = "Powerbank Mini Lipstik",
                    subText = "Powerbank portabel praktis muat saku",
                    promptText = "Mempromosikan powerbank mini seukuran lipstik berkapasitas 5000mAh dengan colokan tipe C terintegrasi langsung tanpa kabel tambahan. Pas masuk saku celana atau tas pesta kecil untuk darurat baterai habis."
                )
            )
        ),
        UgcCategory(
            id = "health",
            name = "Kesehatan / Fitnes",
            iconEmoji = "🏋️",
            presets = listOf(
                UgcPresetPrompt(
                    title = "Minuman Fiber Bebas Buncit",
                    subText = "Minuman serat pelangsing rasa buah",
                    promptText = "Review minuman fiber serat alami rasa buah segar yang membeberkan rahasia perut ramping bebas begah dalam semalam. Menggambarkan efek melancarkan pencernaan tanpa rasa mules melilit berlebih keesokan paginya."
                ),
                UgcPresetPrompt(
                    title = "Skipping Digital Counter",
                    subText = "Alat skipping otomatis ngitung kalori",
                    promptText = "Mempromosikan kepraktisan lompat tali (skipping) dengan alat tali skipping tanpa kabel (cordless) yang dilengkapi layar LCD pintar untuk mendeteksi hitungan lompatan, waktu olahraga, dan kalkulasi kalori terbakar."
                ),
                UgcPresetPrompt(
                    title = "Sepatu Lari Empuk 200K",
                    subText = "Review running shoes lokal nyaman",
                    promptText = "Menunjukkan kelenturan dan empuknya sol bantal sepatu lari lokal seharga 200 ribuan. Lakukan pengetesan saat mendarat kaki di aspal, memperlihatkan bahan rajut mesh bersirkulasi udara anti bau kaki."
                )
            )
        ),
        UgcCategory(
            id = "travel",
            name = "Travel / Gaya Hidup",
            iconEmoji = "✈️",
            presets = listOf(
                UgcPresetPrompt(
                    title = "Hidden Gem Kafe Teduh",
                    subText = "Rekomendasi tempat sepi asri healing",
                    promptText = "Mengulas kafe tersembunyi (hidden gem) berkonsep kebon asri penuh tanaman hijau rimbun di tengah kota yang bising. Gambarkan suasana tenang berangin sepoi, gemercik air kolam ikan, cocok untuk me-time atau wfc."
                ),
                UgcPresetPrompt(
                    title = "Pouch Organizer Koper",
                    subText = "Trik packing rapi muat banyak barang",
                    promptText = "Membagikan trik melipat dan menata pakaian di koper menggunakan set travel pouch organizer jaring beresleting. Buktikan koper kabin ukuran kecil jadi bisa memuat pakaian bepergian untuk 5 hari tanpa kusut."
                ),
                UgcPresetPrompt(
                    title = "Staycation Estetik Nordic",
                    subText = "Review hotel bergaya Skandinavia minimalis",
                    promptText = "Staycation singkat di hotel butik bintang 3 bertema desain scandinavian minimalis kayu cerah. Sorot pemandangan kota di malam hari lewat jendela kaca bundar besar, lengkap dengan sarapan pagi terapung (floating breakfast) di kolam."
                )
            )
        )
    )

    val styles = listOf(
        UgcStyle(
            id = "hype",
            name = "Energi Tinggi (Hype)",
            description = "Intonasi cepat, ekspresif, bersemangat, hook bombastis.",
            emoji = "⚡",
            toneInstruction = "Memakai intonasi kalimat yang membara, berbicara agak cepat tetapi artikulasi jelas, menggunakan kata seru seperti 'Sumpah!', 'Kalian wajib tahu!', penuh penekanan antusias."
        ),
        UgcStyle(
            id = "aesthetic",
            name = "Estetik & Minimalis",
            description = "Lembut, puitis, tenang, sinematik, fokus ke detail indah.",
            emoji = "🌸",
            toneInstruction = "Berbicara dengan suara tenang, ketukan lambat, puitis, bersahabat, menggunakan deskripsi sensori yang kaya rasa, sejuk, dan memanjakan panca indera."
        ),
        UgcStyle(
            id = "casual",
            name = "Kasual & Otentik",
            description = "Layaknya curhat ke sahabat, mengalir, santai, jujur.",
            emoji = "🗣️",
            toneInstruction = "Menggunakan bahasa gaul santai sehari-hari seperti 'aku-kamu', menyelipkan celetukan spontan jujur, mengalir natural tanpa kaku seperti sedang menceritakan pengalaman pribadi ke teman dekat."
        ),
        UgcStyle(
            id = "problem",
            name = "Problem-Solver",
            description = "Fokus ke keresahan sehari-hari lalu tawarkan solusi jitu.",
            emoji = "💡",
            toneInstruction = "Memulai dengan nada frustrasi/lelah membicarakan problem menyebalkan, lalu berubah menjadi nada ceria bersemangat menceritakan rasa lega luar biasa setelah menemukan solusinya."
        ),
        UgcStyle(
            id = "story",
            name = "Storytelling Dramatis",
            description = "Bawa penonton hanyut dalam narasi emosional memikat.",
            emoji = "🎭",
            toneInstruction = "Menggunakan nada bercerita teratur berintonasi naik-turun, menyoroti aspek sebelum dan sesudah kejutan, membangun rasa penasaran penonton sejak detik pertama."
        )
    )
}
