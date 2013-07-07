package ws.palladian.extraction.entity.tagger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.NerAnnotation;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.evaluation.EvaluationResult;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotated;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * 
 * <p>
 * The Alchemy service for Named Entity Recognition. This class uses the Alchemy API and therefore requires the
 * application to have access to the Internet.<br>
 * <a href="http://www.alchemyapi.com/api/entity/textc.html">http://www.alchemyapi.com/api/entity/textc.html</a>
 * </p>
 * 
 * <p>
 * Alchemy can recognize the following entities:<br>
 * <ul>
 * <li>Anniversary</li>
 * <li>City</li>
 * <li>Company</li>
 * <li>Continent</li>
 * <li>Country</li>
 * <li>EntertainmentAward</li>
 * <li>Facility</li>
 * <li>FieldTerminology</li>
 * <li>FinancialMarketIndex</li>
 * <li>GeographicFeature</li>
 * <li>HealthCondition</li>
 * <li>Holiday</li>
 * <li>Movie</li>
 * <li>MusicGroup</li>
 * <li>NaturalDisaster</li>
 * <li>Organization</li>
 * <li>Person</li>
 * <li>PrintMedia</li>
 * <li>RadioProgram</li>
 * <li>RadioStation</li>
 * <li>Region</li>
 * <li>Sport</li>
 * <li>StateOrCounty</li>
 * <li>Technology</li>
 * <li>TelevisionShow</li>
 * <li>TelevisionStation</li>
 * <li>AircraftManufacturer</li>
 * <li>Airline</li>
 * <li>AirportOperator</li>
 * <li>ArchitectureFirm</li>
 * <li>AutomobileCompany</li>
 * <li>BicycleManufacturer</li>
 * <li>BottledWater</li>
 * <li>BreweryBrandOfBeer</li>
 * <li>BroadcastDistributor</li>
 * <li>CandyBarManufacturer</li>
 * <li>ComicBookPublisher</li>
 * <li>ComputerManufacturerBrand</li>
 * <li>Distillery</li>
 * <li>EngineeringFirm</li>
 * <li>FashionLabel</li>
 * <li>FilmCompany</li>
 * <li>FilmDistributor</li>
 * <li>GamePublisher</li>
 * <li>ManufacturingPlant</li>
 * <li>MusicalInstrumentCompany</li>
 * <li>OperatingSystemDeveloper</li>
 * <li>ProcessorManufacturer</li>
 * <li>ProductionCompany</li>
 * <li>RadioNetwork</li>
 * <li>RecordLabel</li>
 * <li>Restaurant</li>
 * <li>RocketEngineDesigner</li>
 * <li>RocketManufacturer</li>
 * <li>ShipBuilder</li>
 * <li>SoftwareDeveloper</li>
 * <li>SpacecraftManufacturer</li>
 * <li>SpiritBottler</li>
 * <li>SpiritProductManufacturer</li>
 * <li>TransportOperator</li>
 * <li>TVNetwork</li>
 * <li>VentureFundedCompany</li>
 * <li>VentureInvestor</li>
 * <li>VideoGameDeveloper</li>
 * <li>VideoGameEngineDeveloper</li>
 * <li>VideoGamePublisher</li>
 * <li>WineProducer</li>
 * <li>Airport</li>
 * <li>Bridge</li>
 * <li>HistoricPlace</li>
 * <li>Hospital</li>
 * <li>Lighthouse</li>
 * <li>ShoppingMall</li>
 * <li>SkiArea</li>
 * <li>Skyscraper</li>
 * <li>Stadium</li>
 * <li>Station</li>
 * <li>BodyOfWater</li>
 * <li>Cave</li>
 * <li>GeologicalFormation</li>
 * <li>Glacier</li>
 * <li>Island</li>
 * <li>IslandGroup</li>
 * <li>Lake</li>
 * <li>Mountain</li>
 * <li>MountainPass</li>
 * <li>MountainRange</li>
 * <li>OilField</li>
 * <li>Park</li>
 * <li>ProtectedArea</li>
 * <li>River</li>
 * <li>Waterfall</li>
 * <li>Cave</li>
 * <li>Island</li>
 * <li>Lake</li>
 * <li>Mountain</li>
 * <li>Park</li>
 * <li>ProtectedArea</li>
 * <li>River</li>
 * <li>TropicalCyclone</li>
 * <li>AstronomicalSurveyProjectOrganization</li>
 * <li>AwardPresentingOrganization</li>
 * <li>Club</li>
 * <li>CollegeUniversity</li>
 * <li>CricketAdministrativeBody</li>
 * <li>FinancialSupportProvider</li>
 * <li>FootballOrganization</li>
 * <li>FraternitySorority</li>
 * <li>GovernmentAgency</li>
 * <li>LegislativeCommittee</li>
 * <li>Legislature</li>
 * <li>MartialArtsOrganization</li>
 * <li>MembershipOrganization</li>
 * <li>NaturalOrCulturalPreservationAgency</li>
 * <li>Non-ProfitOrganisation</li>
 * <li>OrganizationCommittee</li>
 * <li>PeriodicalPublisher</li>
 * <li>PoliticalParty</li>
 * <li>ReligiousOrder</li>
 * <li>ReligiousOrganization</li>
 * <li>ReportIssuingInstitution</li>
 * <li>SoccerClub</li>
 * <li>SpaceAgency</li>
 * <li>SportsAssociation</li>
 * <li>StudentOrganization</li>
 * <li>TopLevelDomainRegistry</li>
 * <li>TradeUnion</li>
 * <li>FootballTeam</li>
 * <li>HockeyTeam</li>
 * <li>Legislature</li>
 * <li>MilitaryUnit</li>
 * <li>Non-ProfitOrganisation</li>
 * <li>RecordLabel</li>
 * <li>School</li>
 * <li>SoccerClub</li>
 * <li>TradeUnion</li>
 * <li>Academic</li>
 * <li>AircraftDesigner</li>
 * <li>Appointee</li>
 * <li>Architect</li>
 * <li>ArchitectureFirmPartner</li>
 * <li>Astronaut</li>
 * <li>Astronomer</li>
 * <li>Author</li>
 * <li>AutomotiveDesigner</li>
 * <li>AwardJudge</li>
 * <li>AwardNominee</li>
 * <li>AwardWinner</li>
 * <li>BasketballCoach</li>
 * <li>BasketballPlayer</li>
 * <li>Bassist</li>
 * <li>Blogger</li>
 * <li>BoardMember</li>
 * <li>Boxer</li>
 * <li>BroadcastArtist</li>
 * <li>Celebrity</li>
 * <li>Chef</li>
 * <li>ChessPlayer</li>
 * <li>ChivalricOrderFounder</li>
 * <li>ChivalricOrderMember</li>
 * <li>ChivalricOrderOfficer</li>
 * <li>Collector</li>
 * <li>ComicBookColorist</li>
 * <li>ComicBookCreator</li>
 * <li>ComicBookEditor</li>
 * <li>ComicBookInker</li>
 * <li>ComicBookLetterer</li>
 * <li>ComicBookPenciler</li>
 * <li>ComicBookWriter</li>
 * <li>ComicStripArtist</li>
 * <li>ComicStripCharacter</li>
 * <li>ComicStripCreator</li>
 * <li>CompanyAdvisor</li>
 * <li>CompanyFounder</li>
 * <li>CompanyShareholder</li>
 * <li>Composer</li>
 * <li>ComputerDesigner</li>
 * <li>ComputerScientist</li>
 * <li>ConductedEnsemble</li>
 * <li>Conductor</li>
 * <li>CricketBowler</li>
 * <li>CricketCoach</li>
 * <li>CricketPlayer</li>
 * <li>CricketUmpire</li>
 * <li>Cyclist</li>
 * <li>Dedicatee</li>
 * <li>Dedicator</li>
 * <li>Deity</li>
 * <li>DietFollower</li>
 * <li>DisasterSurvivor</li>
 * <li>DisasterVictim</li>
 * <li>Drummer</li>
 * <li>ElementDiscoverer</li>
 * <li>FashionDesigner</li>
 * <li>FictionalCreature</li>
 * <li>FictionalUniverseCreator</li>
 * <li>FilmActor</li>
 * <li>FilmArtDirector</li>
 * <li>FilmCastingDirector</li>
 * <li>FilmCharacter</li>
 * <li>FilmCinematographer</li>
 * <li>FilmCostumerDesigner</li>
 * <li>FilmCrewmember</li>
 * <li>FilmCritic</li>
 * <li>FilmDirector</li>
 * <li>FilmEditor</li>
 * <li>FilmMusicContributor</li>
 * <li>FilmProducer</li>
 * <li>FilmProductionDesigner</li>
 * <li>FilmSetDesigner</li>
 * <li>FilmTheorist</li>
 * <li>FilmWriter</li>
 * <li>FootballCoach</li>
 * <li>FootballPlayer</li>
 * <li>FootballReferee</li>
 * <li>FootballTeamManager</li>
 * <li>FoundingFigure</li>
 * <li>GameDesigner</li>
 * <li>Golfer</li>
 * <li>Guitarist</li>
 * <li>HallOfFameInductee</li>
 * <li>Hobbyist</li>
 * <li>HockeyCoach</li>
 * <li>HockeyPlayer</li>
 * <li>HonoraryDegreeRecipient</li>
 * <li>Illustrator</li>
 * <li>Interviewer</li>
 * <li>Inventor</li>
 * <li>LandscapeArchitect</li>
 * <li>LanguageCreator</li>
 * <li>Lyricist</li>
 * <li>MartialArtist</li>
 * <li>MilitaryCommander</li>
 * <li>MilitaryPerson</li>
 * <li>Monarch</li>
 * <li>Mountaineer</li>
 * <li>MusicalArtist</li>
 * <li>MusicalGroupMember</li>
 * <li>NoblePerson</li>
 * <li>NobleTitle</li>
 * <li>OlympicAthlete</li>
 * <li>OperaCharacter</li>
 * <li>OperaDirector</li>
 * <li>OperaLibretto</li>
 * <li>OperaSinger</li>
 * <li>PeriodicalEditor</li>
 * <li>Physician</li>
 * <li>PoliticalAppointer</li>
 * <li>Politician</li>
 * <li>ProAthlete</li>
 * <li>ProgrammingLanguageDesigner</li>
 * <li>ProgrammingLanguageDeveloper</li>
 * <li>ProjectParticipant</li>
 * <li>RecordingEngineer</li>
 * <li>RecordProducer</li>
 * <li>ReligiousLeader</li>
 * <li>SchoolFounder</li>
 * <li>ShipDesigner</li>
 * <li>Songwriter</li>
 * <li>SportsLeagueAwardWinner</li>
 * <li>SportsOfficial</li>
 * <li>Surgeon</li>
 * <li>TennisPlayer</li>
 * <li>TennisTournamentChampion</li>
 * <li>TheaterActor</li>
 * <li>TheaterCharacter</li>
 * <li>TheaterChoreographer</li>
 * <li>TheaterDesigner</li>
 * <li>TheaterDirector</li>
 * <li>TheaterProducer</li>
 * <li>TheatricalComposer</li>
 * <li>TheatricalLyricist</li>
 * <li>Translator</li>
 * <li>TVActor</li>
 * <li>TVCharacter</li>
 * <li>TVDirector</li>
 * <li>TVPersonality</li>
 * <li>TVProducer</li>
 * <li>TVProgramCreator</li>
 * <li>TVWriter</li>
 * <li>U.S.Congressperson</li>
 * <li>USPresident</li>
 * <li>USVicePresident</li>
 * <li>VideoGameActor</li>
 * <li>VideoGameDesigner</li>
 * <li>VisualArtist</li>
 * <li>Actor</li>
 * <li>Architect</li>
 * <li>Astronaut</li>
 * <li>Athlete</li>
 * <li>BritishRoyalty</li>
 * <li>Cardinal</li>
 * <li>ChristianBishop</li>
 * <li>CollegeCoach</li>
 * <li>Comedian</li>
 * <li>ComicsCreator</li>
 * <li>Congressman</li>
 * <li>Criminal</li>
 * <li>FootballManager</li>
 * <li>Journalist</li>
 * <li>MilitaryPerson</li>
 * <li>Model</li>
 * <li>Monarch</li>
 * <li>MusicalArtist</li>
 * <li>Philosopher</li>
 * <li>Politician</li>
 * <li>Saint</li>
 * <li>Scientist</li>
 * <li>Writer</li>
 * <li>Magazine</li>
 * <li>Newspaper</li>
 * <li>SchoolNewspaper</li>
 * <li>EnglishRegion</li>
 * <li>FrenchRegion</li>
 * <li>ItalianRegion</li>
 * <li>VideoGameRegion</li>
 * <li>WineRegion</li>
 * <li>MartialArt</li>
 * <li>PoliticalDistrict</li>
 * <li>AdministrativeDivision</li>
 * <li>GovernmentalJurisdiction</li>
 * </ul>
 * </p>
 * 
 * @see <a href="http://www.alchemyapi.com/api/entity/types.html">http://www.alchemyapi.com/api/entity/types.html</a>
 * @author David Urbansky
 */
public class AlchemyNer extends NamedEntityRecognizer {

    /** Identifier for the API key when supplied via {@link Configuration}. */
    public static final String CONFIG_API_KEY = "api.alchemy.key";

    /** The API key for the Alchemy API service. */
    private final String apiKey;

    /** The maximum number of characters allowed to send per request. */
    private final int MAXIMUM_TEXT_LENGTH = 15000;

    /** Turns coreference resolution on/off. */
    private boolean coreferenceResolution = false;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever;

    /**
     * <p>
     * Create a new {@link AlchemyNer} with an API key provided by the supplied {@link Configuration} instance.
     * </p>
     * 
     * @param configuration The configuration providing the API key via {@value #CONFIG_API_KEY}, not <code>null</code>.
     */
    public AlchemyNer(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link AlchemyNer} with the specified API key.
     * </p>
     * 
     * @param apiKey The API key to use for connecting with Alchemy API, not <code>null</code> or empty.
     */
    public AlchemyNer(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    public void setCoreferenceResolution(boolean value) {
        coreferenceResolution = value;
    }

    @Override
    public List<Annotated> getAnnotations(String inputText) {

        Annotations<Annotated> annotations = new Annotations<Annotated>();
        List<String> textChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        Set<String> checkedEntities = new HashSet<String>();
        for (String textChunk : textChunks) {

            try {

                HttpResult httpResult = getHttpResult(textChunk.toString());
                String response = HttpHelper.getStringContent(httpResult);

                if (response.contains("daily-transaction-limit-exceeded")) {
                    LOGGER.warn("--- LIMIT EXCEEDED ---");
                    break;
                }
                JSONObject json = new JSONObject(response);

                JSONArray entities = json.getJSONArray("entities");
                for (int i = 0; i < entities.length(); i++) {

                    JSONObject entity = entities.getJSONObject(i);

                    String entityName = entity.getString("text");
                    String entityType = entity.getString("type");

                    List<String> subTypeList = CollectionHelper.newArrayList();
                    if (entity.has("disambiguated")) {
                        JSONObject disambiguated = entity.getJSONObject("disambiguated");
                        if (disambiguated.has("subType")) {
                            JSONArray subTypes = disambiguated.getJSONArray("subType");
                            for (int j = 0; j < subTypes.length(); j++) {
                                subTypeList.add(subTypes.getString(j));
                            }
                        }
                    }

                    // skip entities that have been processed already
                    if (!checkedEntities.add(entityName)) {
                        continue;
                    }

                    // get locations of named entity
//                    String escapedEntity = StringHelper.escapeForRegularExpression(entityName);
//                    Pattern pattern = Pattern.compile("(?<=\\s)" + escapedEntity + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])"
//                            + escapedEntity + "(?=\\s)", Pattern.DOTALL);
//
//                    Matcher matcher = pattern.matcher(inputText);
//                    while (matcher.find()) {
//                        int offset = matcher.start();
//                        Annotation annotation = new Annotation(offset, entityName, entityType);
//                        annotation.addSubTypes(subTypeList);
//                        annotations.add(annotation);
//                    }
                    
                    List<Integer> entityOffsets = NerHelper.getEntityOffsets(inputText, entityName);
                    for (Integer offset : entityOffsets) {
                        NerAnnotation annotation = new NerAnnotation(offset, entityName, entityType);
                        annotation.addSubTypes(subTypeList);
                        annotations.add(annotation);
                    }

                }
            } catch (JSONException e) {
                LOGGER.error(getName() + " could not parse json, " + e.getMessage());
            } catch (HttpException e) {
                LOGGER.error(getName() + " error performing HTTP POST, " + e.getMessage());
            }
        }

        annotations.sort();
        // CollectionHelper.print(annotations);

        return annotations;
    }

    @Override
    public String getName() {
        return "Alchemy API NER";
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest request = new HttpRequest(HttpMethod.POST,
                "http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");
        request.addParameter("text", inputText);
        request.addParameter("apikey", apiKey);
        request.addParameter("outputMode", "json");
        request.addParameter("disambiguate", "1");
        request.addParameter("maxRetrieve", "500");
        request.addParameter("coreference", coreferenceResolution ? "1" : "0");
        return httpRetriever.execute(request);
    }

    public static void main(String[] args) {

        AlchemyNer tagger = new AlchemyNer("");

        // // HOW TO USE ////
        System.out
                .println(tagger
                        .tag("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. Some of them are also made in Salt Lake City or Cameron."));
        // tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");
        System.exit(0);

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger.evaluate("data/datasets/ner/politician/text/testing.tsv", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

    }

}
