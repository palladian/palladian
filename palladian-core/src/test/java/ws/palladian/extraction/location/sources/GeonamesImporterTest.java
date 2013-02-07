package ws.palladian.extraction.location.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.GeonamesImporter.GeonameLocation;
import ws.palladian.helper.io.ResourceHelper;

public class GeonamesImporterTest {

    @Test
    public void testParse2() {
        String line = "6555517\tFlein\tFlein\t\t49.1031\t9.21083\tA\tADM4\tDE\t\t01\t081\t08125\t08125030\t6644\t\t191\tEurope/Berlin\t2010-11-24";
        GeonameLocation geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.01.081.08125.08125030", geonameLocation.getCombinedCode());

        line = "2926304\tFlein\tFlein\tFlein\t49.10306\t9.21083\tP\tPPLA4\tDE\t\t01\t081\t08125\t08125030\t6558\t\t191\tEurope/Berlin\t2011-04-25";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.01.081.08125.08125030", geonameLocation.getCombinedCode());

        line = "2921044\tFederal Republic of Germany\tFederal Republic of Germany\tA' Ghearmailt,Alamagn,Alemagne,Alemaina,Alemana,Alemana - Deutschland,Alemanha,Alemani,Alemania,Alemanu,Alemanya,Alemaña,Alemaña - Deutschland,Alimaniya,Alimanya,Alimaɲi,Allemagne,Allemangne,Almaan,Almaañ,Almanija,Almaniya,Almanya,Almayn,Alémani,An Ghearmain,An Ghearmáin,Budaaki,Bundesrepublik Deutschland,Daeitschland,Deitschland,Deitschlånd,Deutaen,Deutschland,Deutän,Discuessiun sura la fundazziun,Discüssiun sura la fundazziun,Dueuetschland,Duiska,Duiskka,Duitschland,Duitsland,Dutslan,Duutsjlandj,Duutsland,Däitschland,Dútslân,Düütschland,Federal Republic of Germany,GJuc,German,Germani,Germania,Germania nutome,Germanija,Germanio,Germanja,Germanujo,Germany,Germània,Girimane,Girmania,Gjermani,Gjermania,Gjermanie,Gyaaman,Heremani,IJalimani,Jamani,Jamus,Jarmal,Jarmalka,Jerman,Jermaniya,Jámánì,Jėrman,Miemiecko,Miemieckô,Nemachka,Nemacka,Nemačka,Nemcija,Nemecko,Nemetorszag,Nemska,Nemčija,Niemcy,Nimeccina,Njamechchyna,Njemacka,Njemačka,Njeremani,Németország,Německo,Němska,Olmon,Olmonija,Olmoniya,Orileede Gemani,Orílẹ́ède Gemani,Saksa,Saksamaa,Siaman,Siamane,THeodiscland,THyskaland,Teutotitlan,Teutōtitlan,Tiamana,Toitshi,Tyskland,Tysklandi,Tôitšhi,Týskland,Ubudage,Udachi,Ujerumani,Vacija,Vokietija,Vācija,Yn Ghermaan,Yr Almaen,Zamani,Zermania,Zâmani,alman,almanya,de guo,dog-il,doitsu,doitsu lian bang gong he guo,dotygu'e,germania,grmn,grmnyh,i-Germany,jamina,jarmani,jerman,jermani,jrmny,jrmny/alman,narmani,prathes yexrmni,shphanth satharnrath yexrmni,yexrman,yexrmni,Þýskaland,Þēodiscland,Đức,Ġermanja,Γερμανία,Алмания,Герман,Германи,Германия,Германија,Германія,Немачка,Нямеччына,Німеччина,Олмон,Олмония,Ӂермания,Գերմանիա,גרמניה,דייטשלאנד,آلمان,ألمانيا,ئەڵمانیا,المان,المانيا,جرمني/آلمان,جرمنی,گېرمانىيە,ܓܪܡܢ,जमिन,जर्मनी,জার্মানি,জাৰ্মানি,જર્મની,ଜର୍ମାନୀ,ஜெர்மனி,ஜெர்மன்,ఙర్మని,ಜರ್ಮನಿ,ജര്‍മനി,ജര്‍മ്മനി,ජර්මනිය,ประเทศเยอรมนี,สหพันธ์สาธารณรัฐเยอรมนี,เยอรมนี,เยอรมัน,ເຢຍລະມັນ,ཇཱར་མ་ནི,འཇར་མན་,ဂျာမဏီ,გერმანია,ጀርመን,អាល្លឺម៉ង់,ドイツ,ドイツ連邦共和国,德国,ꄓꇩ,독일\t51.5\t10.5\tA\tPCLI\tDE\t\t00\t\t\t\t81802257\t\t303\tEurope/Berlin\t2012-09-19";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isCountry());
        assertTrue(geonameLocation.isAdministrativeUnit());

        line = "2953481\tBaden-Württemberg\tBaden-Wuerttemberg\tBadaen-Vuertaen,Bade-Wirddebaersch,Bade-Wirddebärsch,Bade-Woeoetebersch,Bade-Wurtemberg,Bade-Wöötebersch,Badehn-Vjurtehmberg,Baden-Virtemberg,Baden-Virtembergo,Baden-Vjurtemberg,Baden-Vuertemberq,Baden-Vyrtemberg,Baden-Vürtemberq,Baden-Wuerrtenberg,Baden-Wuertembaerj,Baden-Wuertembergska,Baden-Wuertembierich,Baden-Wuerttemberg,Baden-Wurtemberch,Baden-Wurtemberg,Baden-Wúrtemberch,Baden-Würrtenberg,Baden-Würtembergska,Baden-Würtembierich,Baden-Würtembärj,Baden-Württemberg,Badenas-Viurtembergas,Badene-Virtemberga,Badenia-Virtembergia,Badenia-Wirtembergia,Badenia-Wurtemberg,Badensko-Wuerttembergska,Badensko-Wuerttembersko,Badensko-Württembergska,Badn-Wuerttmberg,Badän-Vürtän,Bádensko-Württembersko,Bådn-Württmberg,Bādene-Virtemberga,Pays de Bade,Vadi-Vyrtemvergi,Vuitemberg,Wurtemberg,ba deng-fu teng bao,baden=vu~yurutenberuku zhou,badena-vyurtembarga,badenbwileutembeleukeu ju,badenvirtemberg,badn fwrtmbyrgh,badn-wwrtmbrg,badnwrtmbrg,Βάδη-Βυρτεμβέργη,Баден-Виртемберг,Баден-Вюртемберг,Бадэн-Вюртэмберг,באדין-בורטינבירג,באדן-וירטמברג,بادن فورتمبيرغ,بادن-وورتمبرگ,بادنورتمبرگ,بادێن-ڤوورتمبێرگ,باډن ورټم بېرګ,बाडेन-व्युर्टेंबर्ग,รัฐบาเดิน-เวือร์ทเทมแบร์ก,ბადენ-ვიურტემბერგი,バーデン＝ヴュルテンベルク州,巴登-符腾堡,바덴뷔르템베르크 주\t48.5\t9\tA\tADM1\tDE\t\t01\t\t\t\t10744921\t\t327\tEurope/Berlin\t2012-08-08";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());

        line = "3214105\tRegierungsbezirk Stuttgart\tRegierungsbezirk Stuttgart\tDistrict de Stuttgart,Regierungsbezirk Stuttgart\t49.08333\t9.66667\tA\tADM2\tDE\t\t01\t081\t\t\t4000848\t\t365\tEurope/Berlin\t2012-01-19";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());

        line = "3220785\tStadtkreis Stuttgart\tStadtkreis Stuttgart\t\t48.7825\t9.17694\tA\tADM3\tDE\t\t01\t081\t08111\t\t601646\t\t252\tEurope/Berlin\t2010-11-24";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());

        line = "2825297\tStuttgart\tStuttgart\tEstugarda,Gorad Shtutgart,STR,Shhutgart,Shtutgart,Shtutgarti,Shtuttgart,Stocarda,Stoccarda,Stoutnkarde,Stucarda,Stuggart,Stutgardia,Stutgartas,Stutgarte,Stutgarto,Stutqart,Stuttgart,ashtwtgart,ch tuthth kar th,icututkart,shtwtghart,shuto~uttogaruto,si tu jia te,stutagarta,stwtgrt,syututeugaleuteu,Ştutqart,Štutgartas,Štutgarte,Στουτγκάρδη,Горад Штутгарт,Штутгарт,Штуттгарт,Щутгарт,שטוטגארט,שטוטגרט,اشتوتگارت,سٹٹگارٹ,شتوتغارت,شٹوٹگارٹ,श्टुटगार्ट,স্টুটগার্ট,સ્ટુટગાર્ટ,இசுடுட்கார்ட்,സ്റ്റുട്ട്ഗാർട്ട്,ชตุทท์การ์ท,შტუტგარტი,シュトゥットガルト,斯图加特,슈투트가르트\t48.78232\t9.17702\tP\tPPLA\tDE\t\t01\t081\t08111\t\t589793\t\t252\tEurope/Berlin\t2011-06-16";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());

        line = "2917786\tKreisfreie Stadt Greifswald\tKreisfreie Stadt Greifswald\tKreisfreie Stadt Greifswald,Stadtkreis Greifswald\t54.085\t13.41806\tA\tADM3\tDE\t\t12\t00\t13001\t\t54362\t\t4\tEurope/Berlin\t2012-01-18";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.12.00.13001", geonameLocation.getCombinedCode());

        line = "3076167\tFlöha\tFloha\tFlajsky Patok,Fleyh-Bach,Floeha River,Flohe,Flájský Patok,Flöhe\t50.85911\t13.08626\tH\tSTM\tDE\tDE,CZ\t\t\t\t\t0\t\t273\tEurope/Berlin\t2008-12-26";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());

        line = "2831574\tSolkau\tSolkau\t\t52.91123\t10.83853\tP\tPPL\tDE\t\t06\t00\t\t\t0\t\t61\tEurope/Berlin\t2010-11-22";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());

        line = "3220743\tLandkreis Heilbronn\tLandkreis Heilbronn\tLandkreis Heilbronn\t49.2\t9.2\tA\tADM3\tDE\t\t01\t081\t08125\t\t329054\t\t166\tEurope/Berlin\t2012-05-06";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.01.081.08125", geonameLocation.getCombinedCode());

        line = "2803474\tZwota\tZwota\t\t50.38333\t12.43333\tP\tPPLA4\tDE\t\t13\t145\t14523\t14523470\t1541\t\t744\tEurope/Berlin\t2011-07-31";
        geonameLocation = GeonamesImporter.parse(line);
        assertEquals("DE.13.145.14523.14523470", geonameLocation.getCombinedCode());

        line = "2889621\tKleindehsa\tKleindehsa\t\t51.10518\t14.59419\tP\tPPL\tDE\t\t13\t\t\t\t0\t\t332\tEurope/Berlin\t2012-06-05";
        geonameLocation = GeonamesImporter.parse(line);
        assertEquals("DE.13", geonameLocation.getCombinedCode());

        line = "6547539\tBerlin, Stadt\tBerlin, Stadt\t\t52.5233\t13.41377\tA\tADM4\tDE\t\t16\t00\t11000\t11000000\t3442675\t\t44\tEurope/Berlin\t2010-11-24";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.16.00.11000.11000000", geonameLocation.getCombinedCode());

        line = "2950159\tBerlin\tBerlin\tBER,Beirlin,Beirlín,Berleno,Berlien,Berliin,Berliini,Berlijn,Berlim,Berlin,Berline,Berlini,Berlino,Berlyn,Berlynas,Berlëno,Berlín,Berlîn,Berlīne,Berolino,Berolinum,Birlinu,Bèrlîn,Estat de Berlin,Estat de Berlín,bai lin,barlina,beleullin,berlini,berurin,bexrlin,brlyn,perlin,Βερολίνο,Берлин,Берлін,Бэрлін,Բերլին,בערלין,ברלין,برلين,برلین,بېرلىن,ܒܪܠܝܢ,बर्लिन,বার্লিন,பெர்லின்,เบอร์ลิน,ბერლინი,ベルリン,柏林,베를린\t52.52437\t13.41053\tP\tPPLC\tDE\t\t16\t00\t11000\t11000000\t3426354\t74\t43\tEurope/Berlin\t2012-09-19";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertEquals("DE.16.00.11000.11000000", geonameLocation.getCombinedCode());

        line = "2917484\tGriefstedt\tGriefstedt\t\t51.22957\t11.12932\tP\tPPLA4\tDE\t\t15\t00\t16068\t16068015\t326\t\t140\tEurope/Berlin\t2012-08-28";
        geonameLocation = GeonamesImporter.parse(line);
        assertEquals("DE.15.00.16068.16068015", geonameLocation.getCombinedCode());

        line = "2771016\tPolitischer Bezirk Murau\tPolitischer Bezirk Murau\t\t47.13333\t14.2\tA\tADM2\tAT\t\t06\t614\t\t\t0\t\t1510\tEurope/Vienna\t2008-01-06";
        geonameLocation = GeonamesImporter.parse(line);
        assertTrue(geonameLocation.isAdministrativeUnit());
        assertEquals("AT.06.614", geonameLocation.getCombinedCode());

        line = "2766409\tSankt Ruprecht ob Murau\tSankt Ruprecht ob Murau\tSankt Rupercht,Sankt Ruprecht ob Murau\t47.11009\t14.02199\tP\tPPL\tAT\tAT\t06\t614\t61426\t\t0\t\t892\tEurope/Vienna\t2012-02-03";
        geonameLocation = GeonamesImporter.parse(line);
        assertFalse(geonameLocation.isAdministrativeUnit());
        assertEquals("AT.06.614.61426", geonameLocation.getCombinedCode());

    }

    @Test
    public void testImport() throws FileNotFoundException {
        Map<String, GeonameLocation> adminLocations = GeonamesImporter.readAdministrativeItems(0,
                ResourceHelper.getResourceStream("/geonames.org/locationData.txt"));

        // CollectionHelper.print(adminLocations);

        LocationStore locationStore = new CollectionLocationStore();
        // GeonamesImporter.insertAdministrativeItems(locationStore, adminLocations);
        GeonamesImporter.insertRemainingItems(locationStore,
                ResourceHelper.getResourceStream("/geonames.org/locationData.txt"), 0, adminLocations);

        GeonamesImporter.importHierarchy(ResourceHelper.getResourceFile("/geonames.org/hierarchy.txt"), locationStore);

        Location location = locationStore.retrieveLocation(2926304);
        assertEquals("Flein", location.getPrimaryName());
        assertEquals(49.10306, location.getLatitude(), 0);
        assertEquals(9.21083, location.getLongitude(), 0);
        assertEquals((Long)6558l, location.getPopulation());
        assertEquals(LocationType.CITY, location.getType());
        List<Location> hierarchy = locationStore.getHierarchy(location);
        assertEquals(7, hierarchy.size());
        assertEquals(6555517, hierarchy.get(0).getId());
        assertEquals(3220743, hierarchy.get(1).getId());
        assertEquals(3214105, hierarchy.get(2).getId());
        assertEquals(2953481, hierarchy.get(3).getId());
        assertEquals(2921044, hierarchy.get(4).getId());
        assertEquals(6255148, hierarchy.get(5).getId());
        assertEquals(6295630, hierarchy.get(6).getId());

        location = locationStore.retrieveLocation(2825297);
        assertEquals("Stuttgart", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(6, hierarchy.size());
        assertEquals(3220785, hierarchy.get(0).getId());
        assertEquals(3214105, hierarchy.get(1).getId());
        assertEquals(2953481, hierarchy.get(2).getId());
        assertEquals(2921044, hierarchy.get(3).getId());
        assertEquals(6255148, hierarchy.get(4).getId());
        assertEquals(6295630, hierarchy.get(5).getId());

        location = locationStore.retrieveLocation(7268814);
        assertEquals("Pueblo Sud Subbarrio", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(4562997, hierarchy.get(0).getId());
        assertEquals(4566966, hierarchy.get(1).getId());
        assertEquals(6255149, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(2766409);
        assertEquals("Sankt Ruprecht ob Murau", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(2771016, hierarchy.get(0).getId());
        assertEquals(2764581, hierarchy.get(1).getId());
        assertEquals(2782113, hierarchy.get(2).getId());
        assertEquals(6255148, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(2803474);
        assertEquals("Zwota", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(7, hierarchy.size());
        assertEquals(6548548, hierarchy.get(0).getId());
        assertEquals(6547384, hierarchy.get(1).getId());
        assertEquals(3305801, hierarchy.get(2).getId());
        assertEquals(2842566, hierarchy.get(3).getId());
        assertEquals(2921044, hierarchy.get(4).getId());
        assertEquals(6255148, hierarchy.get(5).getId());
        assertEquals(6295630, hierarchy.get(6).getId());

        location = locationStore.retrieveLocation(2831574);
        assertEquals("Solkau", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(2862926, hierarchy.get(0).getId());
        assertEquals(2921044, hierarchy.get(1).getId());
        assertEquals(6255148, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(2917786);
        assertEquals("Kreisfreie Stadt Greifswald", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(2872567, hierarchy.get(0).getId());
        assertEquals(2921044, hierarchy.get(1).getId());
        assertEquals(6255148, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(6547539);
        assertEquals("Berlin, Stadt", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(6547383, hierarchy.get(0).getId());
        assertEquals(2950157, hierarchy.get(1).getId());
        assertEquals(2921044, hierarchy.get(2).getId());
        assertEquals(6255148, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(1529666);
        assertEquals("Bahnhof Grenzau", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(4, hierarchy.size());
        assertEquals(2847618, hierarchy.get(0).getId());
        assertEquals(2921044, hierarchy.get(1).getId());
        assertEquals(6255148, hierarchy.get(2).getId());
        assertEquals(6295630, hierarchy.get(3).getId());

        location = locationStore.retrieveLocation(4953706);
        assertEquals("University of Massachusetts", location.getPrimaryName());
        assertEquals(LocationType.POI, location.getType());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(4938757, hierarchy.get(0).getId());
        assertEquals(6254926, hierarchy.get(1).getId());
        assertEquals(6252001, hierarchy.get(2).getId());
        assertEquals(6255149, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(5795921);
        assertEquals("Grand Canyon", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(5790164, hierarchy.get(0).getId());
        assertEquals(5815135, hierarchy.get(1).getId());
        assertEquals(6252001, hierarchy.get(2).getId());
        assertEquals(6255149, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(5342044);
        assertEquals("Death Valley Canyon", location.getPrimaryName());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(5, hierarchy.size());
        assertEquals(5359604, hierarchy.get(0).getId());
        assertEquals(5332921, hierarchy.get(1).getId());
        assertEquals(6252001, hierarchy.get(2).getId());
        assertEquals(6255149, hierarchy.get(3).getId());
        assertEquals(6295630, hierarchy.get(4).getId());

        location = locationStore.retrieveLocation(6255147);
        assertEquals("Asia", location.getPrimaryName());
        assertEquals((Long)3812366000l, location.getPopulation());
        assertEquals(LocationType.CONTINENT, location.getType());
        hierarchy = locationStore.getHierarchy(location);
        assertEquals(1, hierarchy.size());
        assertEquals(6295630, hierarchy.get(0).getId());

        location = locationStore.retrieveLocation(2953481);
        assertEquals("Baden-Württemberg", location.getPrimaryName());
        assertEquals(LocationType.UNIT, location.getType());

        location = locationStore.retrieveLocation(2921044);
        assertEquals("Federal Republic of Germany", location.getPrimaryName());
        assertEquals(LocationType.COUNTRY, location.getType());

        location = locationStore.retrieveLocation(6255148);
        assertEquals("Europe", location.getPrimaryName());
        assertEquals(LocationType.CONTINENT, location.getType());

        location = locationStore.retrieveLocation(6295630);
        assertEquals("Earth", location.getPrimaryName());
        assertEquals(LocationType.REGION, location.getType());

    }

}
