package ws.palladian.extraction.location.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.GeonamesImporter.GeonameLocation;

public class GeonamesImporterTest {

    @Test
    public void testParse() {
        String line = "2926304\tFlein\tFlein\tFlein\t49.10306\t9.21083\tP\tPPLA4\tDE\t\t01\t081\t08125\t08125030\t6558\t\t191\tEurope/Berlin\t2011-04-25";
        Location location = GeonamesImporter.parse(line).buildLocation();
        assertEquals("Flein", location.getPrimaryName());
        assertEquals(49.10306, location.getLatitude(), 0);
        assertEquals(9.21083, location.getLongitude(), 0);
        assertEquals((Long)6558l, location.getPopulation());
        assertEquals(LocationType.CITY, location.getType());

        line = "1529666\tBahnhof Grenzau\tBahnhof Grenzau\t\t50.45715\t7.66512\tS\tRSTN\tDE\t\t08\t\t\t\t0\t\t285\tEurope/Berlin\t2012-09-06";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals("Bahnhof Grenzau", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());

        line = "6255147\tAsia\tAsia\tAasia,Asia,Asie,Asien,Asya,Asía,Azie,Azija,Azio,Azië,Azja,Azsia,Chau A,Châu Á,ajia,an Aise,an Áise,asya,esiya mahadvipa,xecheiy,ya zhou,Àsia,Ásia,Ázsia,Āzija,Ασία,Азия,Азія,אסיה,آسيا,एशिया महाद्वीप,เอเชีย,アジア,亚洲\t29.84064\t89.29688\tL\tCONT\t\t\t\t\t\t\t3812366000\t\t5101\tAustralia/Perth\t2012-08-26";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals((Long)3812366000l, location.getPopulation());
        assertEquals(LocationType.CONTINENT, location.getType());

        line = "357994\tArab Republic of Egypt\tArab Republic of Egypt\t'Isipite,AEgypten,Aegyptus,Agypten,Ai Cap,Ai Cập,Aigyptos,Aikupito,Al Iqlim al Janubi,Al Iqlim al Misri,Al Iqlīm al Janūbī,Al Iqlīm al Mişrī,Al Jumhuriyah al Misriyah,Al Jumhuriyah al `Arabiyah al Muttahidah,Al Jumhūrīyah al Miṣrīyah,Al Jumhūrīyah al ‘Arabīyah al Muttaḩidah,An Eigipt,An Eiphit,An Éigipt,Arab Republic of Egypt,Eajipt,Echipto,Eggittu,Egipat,Egipet,Egipt,Egipta,Egiptas,Egipte,Egiptia,Egiptio,Egipto,Egiptos,Egiptus,Egito,Egitto,Egittu,Egjipt,Egjipti,Egyiptom,Egypt,Egypta,Egyptaland,Egypte,Egypte nutome,Egypten,Egypti,Egypto,Egyptowska,Ehipto,Ejip,Ejipt,Ejyp,Ejypta,El Masr,El Qutr el Masri,Exipto,Exipto - msr,Exipto - مصر,Ezipite,Eziputi,Ezípite,Eġittu,Ijipta,Jumhuriyat Misr al `Arabiyah,Jumhūrīyat Mişr al ‘Arabīyah,Kamita,Kâmitâ,Laegueptaen,Lägüptän,Masar,Masar, Misira,Mesir,Misir,Misiri,Misr,Misri,Mushidi,Mysyr,Mısır,Naggitto,Nisrim,Orileede Egipiti,Orílẹ́ède Égípítì,Republic of Egypt,United Arab Republic,Yr Aifft,ai ji,ejiputo,ekiptu,i-Egypt,ijibteu,ijipt,ijipta,misara,misra,mistra,msr,msrn,msryn,mysr,mysyr,mzrym,prathes xiyipt,xiyipt,Ägypten,Ægypten,Èg·ipte,Égypte,Êgypte,Ēģipte,ʻIsipite,Αίγυπτος,Єгипет,Египат,Египет,Егіпет,Егѵпьтъ,Миср,Мысыр,Эгіпет,Եգիպտոս,מצרים,جمهورية مصر العربية,مصر,مىسىر,میسر,ܡܨܪܝܢ,ܡܨܪܢ,इजिप्ट,इजिप्त,मिस्त्र,मिस्र,মিশর,ઇજિપ્ત,ଇଜିପ୍ଟ,எகிப்து,ఈజిప్ట్,ಈಜಿಪ್ಟ್,ഈജിപ്ത്,ഈജിപ്റ്റ്‌,ඊජිප්තුව,ประเทศอียิปต์,อียิปต์,ອີຢິບ,ཨི་ཇིཔཊ,ཨི་ཇིབྚ།,အီဂျစ်,ეგვიპტე,ግብጽ,ግብፅ,អេហ្ស៉ីប,エジプト,エジプト・アラブ共和国,埃及,이집트\t27\t30\tA\tPCLI\tEG\t\t00\t\t\t\t80471869\t\t199\tAfrica/Cairo\t2012-01-19";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.COUNTRY, location.getType());

        line = "5342041\tDeath Valley\tDeath Valley\t\t36.4555\t-116.86755\tS\t\tUS\t\tCA\t027\t\t\t0\t-52\t205\tAmerica/Los_Angeles\t2010-02-14";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.POI, location.getType());

        line = "5342044\tDeath Valley Canyon\tDeath Valley Canyon\t\t36.25523\t-116.94755\tT\tVAL\tUS\t\tCA\t027\t\t\t0\t371\t371\tAmerica/Los_Angeles\t2006-01-15";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.LANDMARK, location.getType());

        line = "5795921\tGrand Canyon\tGrand Canyon\t\t47.94176\t-123.54046\tT\tVAL\tUS\t\tWA\t009\t\t\t0\t265\t289\tAmerica/Los_Angeles\t2010-09-05";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.LANDMARK, location.getType());

        line = "6254926\tMassachusetts\tMassachusetts\tBay State,MA,Ma-sat-tsu-set,Makakukeka,Masachoset,Masachosèt,Masachouseti,Masachusets,Masachuusic,Masachuzets,Masacuseco,Masacusetsa,Masacusetsas,Masatsusets,Masaĉuseco,Masačusetsas,Masačūsetsa,Massachusets,Massachusets Shitati,Massachusetta,Massachusetts,Massachusetts suyu,Massachuséts Shitati,Massacusets,Massaçusets,Meesichooshish Hahoodzo,Mà-sat-tsû-set,Másáchusẹts,Méésíchóoshish Hahoodzo,Shtat Masachusets,ma sa zhu sai,ma sa zhu sai zhou,macacucets,maesachusecheu ju,maisacusitsa,masachusettsu zhou,masachwst,masacyuserrs,masatshwsts,mesecyusetsa,msz'wsts,Μασαχουσέτη,Масачузетс,Масачусетс,Масачуусиц,Массачусетс,Массачусеттс,Штат Масачусетс,Մասաչուսեթս,מאסאטשוסעטס,מסצ'וסטס,ماساتشوستس,ماساچوست,ماساچووسێتس,میساچوسٹس,میساچیوسٹس,ܡܐܣܐܬܫܘܣܬܣ,मॅसेच्युसेट्स,मैसाचूसिट्स,ম্যাসাচুসেট্‌স,மாசசூசெட்ஸ்,മസാച്യുസെറ്റ്സ്,รัฐแมสซาชูเซตส์,မက်ဆာချူးဆက်ပြည်နယ်,მასაჩუსეტსი,ᒫᓵᓲᓰᑦᔅ,マサチューセッツ州,马萨诸塞,麻薩諸塞州,매사추세츠 주\t42.36565\t-71.10832\tA\tADM1\tUS\t\tMA\t\t\t\t6433422\t\t6\tAmerica/New_York\t2011-09-09";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.UNIT, location.getType());

        line = "4953706\tUniversity of Massachusetts\tUniversity of Massachusetts\t\t42.38898\t-72.5287\tS\tSCH\tUS\t\tMA\t015\t\t\t0\t73\t89\tAmerica/New_York\t2006-01-15";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.POI, location.getType());

        line = "2951839\tFreistaat Bayern\tFreistaat Bayern\tBabiera,Baejaraland,Baian,Baiarn,Baieri,Baijeri,Bajororszag,Bajorország,Bauaria,Bavaari,Bavaria,Bavarija,Bavario,Bavarska,Baviera,Baviere,Bavieres,Bavière,Bavorsko,Bavyera,Bavārija,Bawaria,Bayaen,Bayern,Bayän,Beiere,Beieren,Bæjaraland,Estau Llibre de Baviera,Estáu Llibre de Baviera,Free State of Bavaria,Freistaat Bayern,Land Bayern,ba fa li ya,bafarya,baieleun ju,baierun zhou,bavaria,bayrn,paveriya,Βαυαρία,Бавария,Баварија,Баварска,Баварія,Բավարիա,בוואריה,بافاريا,بایرن,பவேரியா,ბავარია,バイエルン州,巴伐利亚,바이에른 주\t49\t11.5\tA\tADM1\tDE\t\t02\t\t\t\t12510331\t\t503\tEurope/Berlin\t2012-12-10";
        location = GeonamesImporter.parse(line).buildLocation();
        assertEquals(LocationType.UNIT, location.getType());
    }

    @Test
    public void testParse2() {
        String line = "6555517\tFlein\tFlein\t\t49.1031\t9.21083\tA\tADM4\tDE\t\t01\t081\t08125\t08125030\t6644\t\t191\tEurope/Berlin\t2010-11-24";
        GeonameLocation geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.01.081.08125.08125030", geonameLocation.getCombinedCode());

        line = "2926304\tFlein\tFlein\tFlein\t49.10306\t9.21083\tP\tPPLA4\tDE\t\t01\t081\t08125\t08125030\t6558\t\t191\tEurope/Berlin\t2011-04-25";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertTrue(geonameLocation.isAdministrativeCity());
        assertEquals("DE.01.081.08125.08125030", geonameLocation.getCombinedCode());
        assertEquals("DE.01.081.08125.08125030", geonameLocation.getParentCode());

        line = "2921044\tFederal Republic of Germany\tFederal Republic of Germany\tA' Ghearmailt,Alamagn,Alemagne,Alemaina,Alemana,Alemana - Deutschland,Alemanha,Alemani,Alemania,Alemanu,Alemanya,Alemaña,Alemaña - Deutschland,Alimaniya,Alimanya,Alimaɲi,Allemagne,Allemangne,Almaan,Almaañ,Almanija,Almaniya,Almanya,Almayn,Alémani,An Ghearmain,An Ghearmáin,Budaaki,Bundesrepublik Deutschland,Daeitschland,Deitschland,Deitschlånd,Deutaen,Deutschland,Deutän,Discuessiun sura la fundazziun,Discüssiun sura la fundazziun,Dueuetschland,Duiska,Duiskka,Duitschland,Duitsland,Dutslan,Duutsjlandj,Duutsland,Däitschland,Dútslân,Düütschland,Federal Republic of Germany,GJuc,German,Germani,Germania,Germania nutome,Germanija,Germanio,Germanja,Germanujo,Germany,Germània,Girimane,Girmania,Gjermani,Gjermania,Gjermanie,Gyaaman,Heremani,IJalimani,Jamani,Jamus,Jarmal,Jarmalka,Jerman,Jermaniya,Jámánì,Jėrman,Miemiecko,Miemieckô,Nemachka,Nemacka,Nemačka,Nemcija,Nemecko,Nemetorszag,Nemska,Nemčija,Niemcy,Nimeccina,Njamechchyna,Njemacka,Njemačka,Njeremani,Németország,Německo,Němska,Olmon,Olmonija,Olmoniya,Orileede Gemani,Orílẹ́ède Gemani,Saksa,Saksamaa,Siaman,Siamane,THeodiscland,THyskaland,Teutotitlan,Teutōtitlan,Tiamana,Toitshi,Tyskland,Tysklandi,Tôitšhi,Týskland,Ubudage,Udachi,Ujerumani,Vacija,Vokietija,Vācija,Yn Ghermaan,Yr Almaen,Zamani,Zermania,Zâmani,alman,almanya,de guo,dog-il,doitsu,doitsu lian bang gong he guo,dotygu'e,germania,grmn,grmnyh,i-Germany,jamina,jarmani,jerman,jermani,jrmny,jrmny/alman,narmani,prathes yexrmni,shphanth satharnrath yexrmni,yexrman,yexrmni,Þýskaland,Þēodiscland,Đức,Ġermanja,Γερμανία,Алмания,Герман,Германи,Германия,Германија,Германія,Немачка,Нямеччына,Німеччина,Олмон,Олмония,Ӂермания,Գերմանիա,גרמניה,דייטשלאנד,آلمان,ألمانيا,ئەڵمانیا,المان,المانيا,جرمني/آلمان,جرمنی,گېرمانىيە,ܓܪܡܢ,जमिन,जर्मनी,জার্মানি,জাৰ্মানি,જર્મની,ଜର୍ମାନୀ,ஜெர்மனி,ஜெர்மன்,ఙర్మని,ಜರ್ಮನಿ,ജര്‍മനി,ജര്‍മ്മനി,ජර්මනිය,ประเทศเยอรมนี,สหพันธ์สาธารณรัฐเยอรมนี,เยอรมนี,เยอรมัน,ເຢຍລະມັນ,ཇཱར་མ་ནི,འཇར་མན་,ဂျာမဏီ,გერმანია,ጀርመን,អាល្លឺម៉ង់,ドイツ,ドイツ連邦共和国,德国,ꄓꇩ,독일\t51.5\t10.5\tA\tPCLI\tDE\t\t00\t\t\t\t81802257\t\t303\tEurope/Berlin\t2012-09-19";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isCountry());
        assertEquals("", geonameLocation.getParentCode());
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals(1, geonameLocation.getLevel());

        line = "2953481\tBaden-Württemberg\tBaden-Wuerttemberg\tBadaen-Vuertaen,Bade-Wirddebaersch,Bade-Wirddebärsch,Bade-Woeoetebersch,Bade-Wurtemberg,Bade-Wöötebersch,Badehn-Vjurtehmberg,Baden-Virtemberg,Baden-Virtembergo,Baden-Vjurtemberg,Baden-Vuertemberq,Baden-Vyrtemberg,Baden-Vürtemberq,Baden-Wuerrtenberg,Baden-Wuertembaerj,Baden-Wuertembergska,Baden-Wuertembierich,Baden-Wuerttemberg,Baden-Wurtemberch,Baden-Wurtemberg,Baden-Wúrtemberch,Baden-Würrtenberg,Baden-Würtembergska,Baden-Würtembierich,Baden-Würtembärj,Baden-Württemberg,Badenas-Viurtembergas,Badene-Virtemberga,Badenia-Virtembergia,Badenia-Wirtembergia,Badenia-Wurtemberg,Badensko-Wuerttembergska,Badensko-Wuerttembersko,Badensko-Württembergska,Badn-Wuerttmberg,Badän-Vürtän,Bádensko-Württembersko,Bådn-Württmberg,Bādene-Virtemberga,Pays de Bade,Vadi-Vyrtemvergi,Vuitemberg,Wurtemberg,ba deng-fu teng bao,baden=vu~yurutenberuku zhou,badena-vyurtembarga,badenbwileutembeleukeu ju,badenvirtemberg,badn fwrtmbyrgh,badn-wwrtmbrg,badnwrtmbrg,Βάδη-Βυρτεμβέργη,Баден-Виртемберг,Баден-Вюртемберг,Бадэн-Вюртэмберг,באדין-בורטינבירג,באדן-וירטמברג,بادن فورتمبيرغ,بادن-وورتمبرگ,بادنورتمبرگ,بادێن-ڤوورتمبێرگ,باډن ورټم بېرګ,बाडेन-व्युर्टेंबर्ग,รัฐบาเดิน-เวือร์ทเทมแบร์ก,ბადენ-ვიურტემბერგი,バーデン＝ヴュルテンベルク州,巴登-符腾堡,바덴뷔르템베르크 주\t48.5\t9\tA\tADM1\tDE\t\t01\t\t\t\t10744921\t\t327\tEurope/Berlin\t2012-08-08";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals(2, geonameLocation.getLevel());

        line = "3214105\tRegierungsbezirk Stuttgart\tRegierungsbezirk Stuttgart\tDistrict de Stuttgart,Regierungsbezirk Stuttgart\t49.08333\t9.66667\tA\tADM2\tDE\t\t01\t081\t\t\t4000848\t\t365\tEurope/Berlin\t2012-01-19";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals(3, geonameLocation.getLevel());

        line = "3220785\tStadtkreis Stuttgart\tStadtkreis Stuttgart\t\t48.7825\t9.17694\tA\tADM3\tDE\t\t01\t081\t08111\t\t601646\t\t252\tEurope/Berlin\t2010-11-24";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals(4, geonameLocation.getLevel());

        line = "2825297\tStuttgart\tStuttgart\tEstugarda,Gorad Shtutgart,STR,Shhutgart,Shtutgart,Shtutgarti,Shtuttgart,Stocarda,Stoccarda,Stoutnkarde,Stucarda,Stuggart,Stutgardia,Stutgartas,Stutgarte,Stutgarto,Stutqart,Stuttgart,ashtwtgart,ch tuthth kar th,icututkart,shtwtghart,shuto~uttogaruto,si tu jia te,stutagarta,stwtgrt,syututeugaleuteu,Ştutqart,Štutgartas,Štutgarte,Στουτγκάρδη,Горад Штутгарт,Штутгарт,Штуттгарт,Щутгарт,שטוטגארט,שטוטגרט,اشتوتگارت,سٹٹگارٹ,شتوتغارت,شٹوٹگارٹ,श्टुटगार्ट,স্টুটগার্ট,સ્ટુટગાર્ટ,இசுடுட்கார்ட்,സ്റ്റുട്ട്ഗാർട്ട്,ชตุทท์การ์ท,შტუტგარტი,シュトゥットガルト,斯图加特,슈투트가르트\t48.78232\t9.17702\tP\tPPLA\tDE\t\t01\t081\t08111\t\t589793\t\t252\tEurope/Berlin\t2011-06-16";
        geonameLocation = GeonamesImporter.parse(line);
        assertEquals(-1, geonameLocation.getLevel());
        assertFalse(geonameLocation.isAdministrativeUnit());

        line = "2917786\tKreisfreie Stadt Greifswald\tKreisfreie Stadt Greifswald\tKreisfreie Stadt Greifswald,Stadtkreis Greifswald\t54.085\t13.41806\tA\tADM3\tDE\t\t12\t00\t13001\t\t54362\t\t4\tEurope/Berlin\t2012-01-18";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals(4, geonameLocation.getLevel());
        assertEquals("DE.12.00.13001", geonameLocation.getCombinedCode());
        assertEquals("DE.12", geonameLocation.getParentCode());

        line = "3076167\tFlöha\tFloha\tFlajsky Patok,Fleyh-Bach,Floeha River,Flohe,Flájský Patok,Flöhe\t50.85911\t13.08626\tH\tSTM\tDE\tDE,CZ\t\t\t\t\t0\t\t273\tEurope/Berlin\t2008-12-26";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertEquals(-1, geonameLocation.getLevel());
        assertEquals("DE", geonameLocation.getParentCode());

        line = "2831574\tSolkau\tSolkau\t\t52.91123\t10.83853\tP\tPPL\tDE\t\t06\t00\t\t\t0\t\t61\tEurope/Berlin\t2010-11-22";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertEquals(-1, geonameLocation.getLevel());
        assertEquals("DE.06", geonameLocation.getParentCode());

        line = "3220743\tLandkreis Heilbronn\tLandkreis Heilbronn\tLandkreis Heilbronn\t49.2\t9.2\tA\tADM3\tDE\t\t01\t081\t08125\t\t329054\t\t166\tEurope/Berlin\t2012-05-06";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.01.081.08125", geonameLocation.getCombinedCode());
        assertEquals("DE.01.081", geonameLocation.getParentCode());

        line = "2803474\tZwota\tZwota\t\t50.38333\t12.43333\tP\tPPLA4\tDE\t\t13\t145\t14523\t14523470\t1541\t\t744\tEurope/Berlin\t2011-07-31";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeCity());
        assertEquals("DE.13.145.14523.14523470", geonameLocation.getCombinedCode());
        assertEquals("DE.13.145.14523.14523470", geonameLocation.getParentCode());

        line = "2889621\tKleindehsa\tKleindehsa\t\t51.10518\t14.59419\tP\tPPL\tDE\t\t13\t\t\t\t0\t\t332\tEurope/Berlin\t2012-06-05";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeCity());
        assertEquals("DE.13", geonameLocation.getCombinedCode());
        assertEquals("DE.13", geonameLocation.getParentCode());

        line = "6547539\tBerlin, Stadt\tBerlin, Stadt\t\t52.5233\t13.41377\tA\tADM4\tDE\t\t16\t00\t11000\t11000000\t3442675\t\t44\tEurope/Berlin\t2010-11-24";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertFalse(geonameLocation.isAdministrativeCity());
        assertEquals("DE.16.00.11000.11000000", geonameLocation.getCombinedCode());
        assertEquals("DE.16.00.11000", geonameLocation.getParentCode());

        line = "2950159\tBerlin\tBerlin\tBER,Beirlin,Beirlín,Berleno,Berlien,Berliin,Berliini,Berlijn,Berlim,Berlin,Berline,Berlini,Berlino,Berlyn,Berlynas,Berlëno,Berlín,Berlîn,Berlīne,Berolino,Berolinum,Birlinu,Bèrlîn,Estat de Berlin,Estat de Berlín,bai lin,barlina,beleullin,berlini,berurin,bexrlin,brlyn,perlin,Βερολίνο,Берлин,Берлін,Бэрлін,Բերլին,בערלין,ברלין,برلين,برلین,بېرلىن,ܒܪܠܝܢ,बर्लिन,বার্লিন,பெர்லின்,เบอร์ลิน,ბერლინი,ベルリン,柏林,베를린\t52.52437\t13.41053\tP\tPPLC\tDE\t\t16\t00\t11000\t11000000\t3426354\t74\t43\tEurope/Berlin\t2012-09-19";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertTrue(geonameLocation.isAdministrativeCity());
        assertEquals("DE.16.00.11000.11000000", geonameLocation.getCombinedCode());
        assertEquals("DE.16.00.11000.11000000", geonameLocation.getParentCode());

        line = "2917484\tGriefstedt\tGriefstedt\t\t51.22957\t11.12932\tP\tPPLA4\tDE\t\t15\t00\t16068\t16068015\t326\t\t140\tEurope/Berlin\t2012-08-28";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeCity());
        assertEquals("DE.15.00.16068.16068015", geonameLocation.getCombinedCode());
        assertEquals("DE.15.00.16068.16068015", geonameLocation.getParentCode());

    }

}
