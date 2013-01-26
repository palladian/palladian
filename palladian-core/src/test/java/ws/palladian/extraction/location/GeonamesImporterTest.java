package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GeonamesImporterTest {

    @Test
    public void testParse() {
        String line = "2926304\tFlein\tFlein\tFlein\t49.10306\t9.21083\tP\tPPLA4\tDE\t\t01\t081\t08125\t08125030\t6558\t\t191\tEurope/Berlin\t2011-04-25";
        Location location = GeonamesImporter.parse(line);
        assertEquals("Flein", location.getPrimaryName());
        assertEquals(49.10306, location.getLatitude(), 0);
        assertEquals(9.21083, location.getLongitude(), 0);
        assertEquals((Long)6558l, location.getPopulation());
        assertEquals(LocationType.CITY, location.getType());

        line = "1529666\tBahnhof Grenzau\tBahnhof Grenzau\t\t50.45715\t7.66512\tS\tRSTN\tDE\t\t08\t\t\t\t0\t\t285\tEurope/Berlin\t2012-09-06";
        location = GeonamesImporter.parse(line);
        assertEquals("Bahnhof Grenzau", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        line = "6255147\tAsia\tAsia\tAasia,Asia,Asie,Asien,Asya,Asía,Azie,Azija,Azio,Azië,Azja,Azsia,Chau A,Châu Á,ajia,an Aise,an Áise,asya,esiya mahadvipa,xecheiy,ya zhou,Àsia,Ásia,Ázsia,Āzija,Ασία,Азия,Азія,אסיה,آسيا,एशिया महाद्वीप,เอเชีย,アジア,亚洲\t29.84064\t89.29688\tL\tCONT\t\t\t\t\t\t\t3812366000\t\t5101\tAustralia/Perth\t2012-08-26";
        location = GeonamesImporter.parse(line);
        assertEquals((Long)3812366000l, location.getPopulation());
        assertEquals(LocationType.CONTINENT, location.getType());

        line = "357994\tArab Republic of Egypt\tArab Republic of Egypt\t'Isipite,AEgypten,Aegyptus,Agypten,Ai Cap,Ai Cập,Aigyptos,Aikupito,Al Iqlim al Janubi,Al Iqlim al Misri,Al Iqlīm al Janūbī,Al Iqlīm al Mişrī,Al Jumhuriyah al Misriyah,Al Jumhuriyah al `Arabiyah al Muttahidah,Al Jumhūrīyah al Miṣrīyah,Al Jumhūrīyah al ‘Arabīyah al Muttaḩidah,An Eigipt,An Eiphit,An Éigipt,Arab Republic of Egypt,Eajipt,Echipto,Eggittu,Egipat,Egipet,Egipt,Egipta,Egiptas,Egipte,Egiptia,Egiptio,Egipto,Egiptos,Egiptus,Egito,Egitto,Egittu,Egjipt,Egjipti,Egyiptom,Egypt,Egypta,Egyptaland,Egypte,Egypte nutome,Egypten,Egypti,Egypto,Egyptowska,Ehipto,Ejip,Ejipt,Ejyp,Ejypta,El Masr,El Qutr el Masri,Exipto,Exipto - msr,Exipto - مصر,Ezipite,Eziputi,Ezípite,Eġittu,Ijipta,Jumhuriyat Misr al `Arabiyah,Jumhūrīyat Mişr al ‘Arabīyah,Kamita,Kâmitâ,Laegueptaen,Lägüptän,Masar,Masar, Misira,Mesir,Misir,Misiri,Misr,Misri,Mushidi,Mysyr,Mısır,Naggitto,Nisrim,Orileede Egipiti,Orílẹ́ède Égípítì,Republic of Egypt,United Arab Republic,Yr Aifft,ai ji,ejiputo,ekiptu,i-Egypt,ijibteu,ijipt,ijipta,misara,misra,mistra,msr,msrn,msryn,mysr,mysyr,mzrym,prathes xiyipt,xiyipt,Ägypten,Ægypten,Èg·ipte,Égypte,Êgypte,Ēģipte,ʻIsipite,Αίγυπτος,Єгипет,Египат,Египет,Егіпет,Егѵпьтъ,Миср,Мысыр,Эгіпет,Եգիպտոս,מצרים,جمهورية مصر العربية,مصر,مىسىر,میسر,ܡܨܪܝܢ,ܡܨܪܢ,इजिप्ट,इजिप्त,मिस्त्र,मिस्र,মিশর,ઇજિપ્ત,ଇଜିପ୍ଟ,எகிப்து,ఈజిప్ట్,ಈಜಿಪ್ಟ್,ഈജിപ്ത്,ഈജിപ്റ്റ്‌,ඊජිප්තුව,ประเทศอียิปต์,อียิปต์,ອີຢິບ,ཨི་ཇིཔཊ,ཨི་ཇིབྚ།,အီဂျစ်,ეგვიპტე,ግብጽ,ግብፅ,អេហ្ស៉ីប,エジプト,エジプト・アラブ共和国,埃及,이집트\t27\t30\tA\tPCLI\tEG\t\t00\t\t\t\t80471869\t\t199\tAfrica/Cairo\t2012-01-19";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.COUNTRY, location.getType());

        line = "5342041\tDeath Valley\tDeath Valley\t\t36.4555\t-116.86755\tS\t\tUS\t\tCA\t027\t\t\t0\t-52\t205\tAmerica/Los_Angeles\t2010-02-14";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.POI, location.getType());

        line = "5342044\tDeath Valley Canyon\tDeath Valley Canyon\t\t36.25523\t-116.94755\tT\tVAL\tUS\t\tCA\t027\t\t\t0\t371\t371\tAmerica/Los_Angeles\t2006-01-15";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.LANDMARK, location.getType());

        line = "5795921\tGrand Canyon\tGrand Canyon\t\t47.94176\t-123.54046\tT\tVAL\tUS\t\tWA\t009\t\t\t0\t265\t289\tAmerica/Los_Angeles\t2010-09-05";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.LANDMARK, location.getType());

        line = "6254926\tMassachusetts\tMassachusetts\tBay State,MA,Ma-sat-tsu-set,Makakukeka,Masachoset,Masachosèt,Masachouseti,Masachusets,Masachuusic,Masachuzets,Masacuseco,Masacusetsa,Masacusetsas,Masatsusets,Masaĉuseco,Masačusetsas,Masačūsetsa,Massachusets,Massachusets Shitati,Massachusetta,Massachusetts,Massachusetts suyu,Massachuséts Shitati,Massacusets,Massaçusets,Meesichooshish Hahoodzo,Mà-sat-tsû-set,Másáchusẹts,Méésíchóoshish Hahoodzo,Shtat Masachusets,ma sa zhu sai,ma sa zhu sai zhou,macacucets,maesachusecheu ju,maisacusitsa,masachusettsu zhou,masachwst,masacyuserrs,masatshwsts,mesecyusetsa,msz'wsts,Μασαχουσέτη,Масачузетс,Масачусетс,Масачуусиц,Массачусетс,Массачусеттс,Штат Масачусетс,Մասաչուսեթս,מאסאטשוסעטס,מסצ'וסטס,ماساتشوستس,ماساچوست,ماساچووسێتس,میساچوسٹس,میساچیوسٹس,ܡܐܣܐܬܫܘܣܬܣ,मॅसेच्युसेट्स,मैसाचूसिट्स,ম্যাসাচুসেট্‌স,மாசசூசெட்ஸ்,മസാച്യുസെറ്റ്സ്,รัฐแมสซาชูเซตส์,မက်ဆာချူးဆက်ပြည်နယ်,მასაჩუსეტსი,ᒫᓵᓲᓰᑦᔅ,マサチューセッツ州,马萨诸塞,麻薩諸塞州,매사추세츠 주\t42.36565\t-71.10832\tA\tADM1\tUS\t\tMA\t\t\t\t6433422\t\t6\tAmerica/New_York\t2011-09-09";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.UNIT, location.getType());

        line = "4953706\tUniversity of Massachusetts\tUniversity of Massachusetts\t\t42.38898\t-72.5287\tS\tSCH\tUS\t\tMA\t015\t\t\t0\t73\t89\tAmerica/New_York\t2006-01-15";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.POI, location.getType());

        line = "2951839\tFreistaat Bayern\tFreistaat Bayern\tBabiera,Baejaraland,Baian,Baiarn,Baieri,Baijeri,Bajororszag,Bajorország,Bauaria,Bavaari,Bavaria,Bavarija,Bavario,Bavarska,Baviera,Baviere,Bavieres,Bavière,Bavorsko,Bavyera,Bavārija,Bawaria,Bayaen,Bayern,Bayän,Beiere,Beieren,Bæjaraland,Estau Llibre de Baviera,Estáu Llibre de Baviera,Free State of Bavaria,Freistaat Bayern,Land Bayern,ba fa li ya,bafarya,baieleun ju,baierun zhou,bavaria,bayrn,paveriya,Βαυαρία,Бавария,Баварија,Баварска,Баварія,Բավարիա,בוואריה,بافاريا,بایرن,பவேரியா,ბავარია,バイエルン州,巴伐利亚,바이에른 주\t49\t11.5\tA\tADM1\tDE\t\t02\t\t\t\t12510331\t\t503\tEurope/Berlin\t2012-12-10";
        location = GeonamesImporter.parse(line);
        assertEquals(LocationType.UNIT, location.getType());

    }

}
