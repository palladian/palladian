package ws.palladian.extraction.entity.ner.tagger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.Entity;
import ws.palladian.extraction.entity.ner.NamedEntityRecognizer;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.evaluation.EvaluationResult;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.retrieval.HTTPPoster;

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
 * 
 * <p>
 * See also <a href="http://www.alchemyapi.com/api/entity/types.html"
 * >http://www.alchemyapi.com/api/entity/types.html</a>
 * </p>
 * 
 * 
 * @author David Urbansky
 * 
 */
public class AlchemyNER extends NamedEntityRecognizer {

    /** The API key for the Alchemy API service. */
    private final String API_KEY;

    /** The maximum number of characters allowed to send per request (actually 150,000). */
    private final int MAXIMUM_TEXT_LENGTH = 140000;

    public AlchemyNER() {
        setName("Alchemy API NER");

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/apikeys.conf");
        } catch (ConfigurationException e) {
            LOGGER.error("could not get api key from config/apikeys.conf, " + e.getMessage());
        }

        if (config != null) {
            API_KEY = config.getString("alchemy.api.key");
        } else {
            API_KEY = "";
        }
    }

    @Override
    public String getModelFileEnding() {
        LOGGER.warn(getName() + " does not support loading models, therefore we don't know the file ending");
        return "";
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        LOGGER.warn(getName() + " does not support loading models, therefore we don't know the file ending");
        return false;
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        LOGGER.warn(getName() + " does not support training");
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        LOGGER.warn(getName() + " does not support loading models");
        return false;
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        return getAnnotations(inputText, "");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {

        Annotations annotations = new Annotations();

        // we need to build chunks of texts because we can not send very long texts at once to open calais
        List<String> sentences = Tokenizer.getSentences(inputText);
        List<StringBuilder> textChunks = new ArrayList<StringBuilder>();
        StringBuilder currentTextChunk = new StringBuilder();
        for (String sentence : sentences) {

            if (currentTextChunk.length() + sentence.length() > MAXIMUM_TEXT_LENGTH) {
                textChunks.add(currentTextChunk);
                currentTextChunk = new StringBuilder();
            }

            currentTextChunk.append(sentence);
        }
        textChunks.add(currentTextChunk);

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        Set<String> checkedEntities = new HashSet<String>();
        for (StringBuilder textChunk : textChunks) {

            // use get
            // Crawler c = new Crawler();
            // JSONObject json = c
            // .getJSONDocument("http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities?apikey=" + API_KEY
            // + "&text=" + inputText + "&disambiguate=0&outputMode=json");

            try {

                PostMethod pm = createPostMethod(textChunk.toString());

                HTTPPoster poster = new HTTPPoster();
                String response = poster.handleRequest(pm);

                JSONObject json = new JSONObject(response);

                JSONArray entities = json.getJSONArray("entities");
                for (int i = 0; i < entities.length(); i++) {

                    JSONObject entity = (JSONObject) entities.get(i);

                    String entityName = entity.getString("text");
                    Entity namedEntity = new Entity(entityName, entity.getString("type"));

                    // skip entities that have been processed already
                    if (!checkedEntities.add(entityName)) {
                        continue;
                    }

                    // recognizedEntities.add(namedEntity);

                    // get locations of named entity
                    String escapedEntity = StringHelper.escapeForRegularExpression(entityName);
                    Pattern pattern = Pattern.compile("(?<=\\s)" + escapedEntity + "(?![0-9A-Za-z])|(?<![0-9A-Za-z])"
                            + escapedEntity + "(?=\\s)", Pattern.DOTALL);

                    Matcher matcher = pattern.matcher(inputText);
                    while (matcher.find()) {

                        int offset = matcher.start();

                        Annotation annotation = new Annotation(offset, namedEntity.getName(),
 namedEntity.getTagName());
                        annotations.add(annotation);
                    }

                }
            } catch (JSONException e) {
                LOGGER.error(getName() + " could not parse json, " + e.getMessage());
            }
        }

        annotations.sort();
        CollectionHelper.print(annotations);

        return annotations;
    }

    private PostMethod createPostMethod(String inputText) {

        PostMethod method = new PostMethod(" http://access.alchemyapi.com/calls/text/TextGetRankedNamedEntities");

        // set input content type
        method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        // set response/output format
        method.setRequestHeader("Accept", "application/json");

        try {
            method.setRequestEntity(new StringRequestEntity("text=" + URLEncoder.encode(inputText, "UTF-8")
                    + "&apikey=" + URLEncoder.encode(API_KEY, "UTF-8") + "&outputMode=json&disambiguate=0", "text/raw",
                    "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("encoding is not supported, " + e.getMessage());
        }

        return method;
    }

    /**
     * Tag the input text. Alchemy API does not require to specify a model.
     * 
     * @param inputText The text to be tagged.
     * @return The tagged text.
     */
    @Override
    public String tag(String inputText) {
        return super.tag(inputText);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        AlchemyNER tagger = new AlchemyNER();

        if (args.length > 0) {

            Options options = new Options();
            options.addOption(OptionBuilder.withLongOpt("inputText").withDescription("the text that should be tagged")
                    .hasArg().withArgName("text").withType(String.class).create());
            options.addOption(OptionBuilder.withLongOpt("outputFile")
                    .withDescription("the path and name of the file where the tagged text should be saved to").hasArg()
                    .withArgName("text").withType(String.class).create());

            HelpFormatter formatter = new HelpFormatter();

            CommandLineParser parser = new PosixParser();
            CommandLine cmd = null;
            try {
                cmd = parser.parse(options, args);

                String taggedText = tagger.tag(cmd.getOptionValue("inputText"));

                if (cmd.hasOption("outputFile")) {
                    FileHelper.writeToFile(cmd.getOptionValue("outputFile"), taggedText);
                } else {
                    System.out.println("No output file given so tagged text will be printed to the console:");
                    System.out.println(taggedText);
                }

            } catch (ParseException e) {
                LOGGER.debug("Command line arguments could not be parsed!");
                formatter.printHelp("FeedChecker", options);
            }

        }

        // // HOW TO USE ////
        // System.out.println(tagger.tag("The world's largest maker of solar inverters announced Monday that it will locate its first North American manufacturing plant in Denver. Some of them are also made in Salt Lake City or Cameron."));
        // tagger.tag("John J. Smith and the Nexus One location mention Seattle in the text John J. Smith lives in Seattle. He wants to buy an iPhone 4 or a Samsung i7110 phone.");

        // /////////////////////////// test /////////////////////////////
        EvaluationResult er = tagger
                .evaluate("data/datasets/ner/politician/text/testing.tsv", "", TaggingFormat.COLUMN);
        System.out.println(er.getMUCResultsReadable());
        System.out.println(er.getExactMatchResultsReadable());

    }

}