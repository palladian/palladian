package ws.palladian.extraction.location;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ws.palladian.extraction.content.PageContentExtractorException;
import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.tagger.WebKnoxNer;
import ws.palladian.extraction.location.sources.CachingLocationSource;
import ws.palladian.extraction.location.sources.GeonamesLocationSource;
import ws.palladian.extraction.location.sources.NewsSeecrLocationSource;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * Given a text, the LocationDetector finds mentioned locations and returns annotations.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class PalladianLocationExtractor extends LocationExtractor {

    // words that are unlikely to be a location
    private static final Set<String> skipWords;

    private final LocationSource locationSource;

    private final NamedEntityRecognizer entityRecognizer;

    static {
        skipWords = new HashSet<String>();
        skipWords.add("Monday");
        skipWords.add("Tuesday");
        skipWords.add("Wednesday");
        skipWords.add("Thursday");
        skipWords.add("Friday");
        skipWords.add("Saturday");
        skipWords.add("Sunday");
        skipWords.add("January");
        skipWords.add("February");
        skipWords.add("March");
        skipWords.add("April");
        skipWords.add("May");
        skipWords.add("June");
        skipWords.add("July");
        skipWords.add("August");
        skipWords.add("September");
        skipWords.add("October");
        skipWords.add("November");
        skipWords.add("December");
        skipWords.add("Parliament");
    }

    public PalladianLocationExtractor(String webKnoxApiKey, String geonamesUsername) {
        setName("Palladian Location Extractor");
        this.entityRecognizer = new WebKnoxNer(webKnoxApiKey);
        this.locationSource = new CachingLocationSource(new GeonamesLocationSource(geonamesUsername));
    }

    public PalladianLocationExtractor(String webKnoxApiKey, String mashapePublicKey, String mashapePrivateKey) {
        setName("Palladian Location Extractor");
        this.entityRecognizer = new WebKnoxNer(webKnoxApiKey);
        this.locationSource = new CachingLocationSource(new NewsSeecrLocationSource(mashapePublicKey, mashapePrivateKey));
    }

    @Override
    public String getModelFileEnding() {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        LOGGER.warn("the configModelFilePath is ignored");
        return getAnnotations(inputText);
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public List<Location> detectLocations(String text) {

        //        Set<String> locationConceptNames = new HashSet<String>();
        //        locationConceptNames.add("Country");
        //        locationConceptNames.add("Nation");
        //        locationConceptNames.add("County");
        //        locationConceptNames.add("City");
        //        locationConceptNames.add("Metropole");

        List<Location> locationEntities = CollectionHelper.newArrayList();

        // StopWordRemover stopWordRemover = new StopWordRemover();
        // text = stopWordRemover.removeStopWords(text);

        // get candidates which could be locations
        // Annotations taggedEntities = StringTagger.getTaggedEntities(text);

        Annotations taggedEntities = entityRecognizer.getAnnotations(text);

        //        Set<String> locationNames = new HashSet<String>();

        // try to find them in the database
        for (Annotation locationCandidate : taggedEntities) {

            // if (!(locationCandidate.getMostLikelyTagName().equalsIgnoreCase("country") || locationCandidate
            // .getMostLikelyTagName().equalsIgnoreCase("city"))) {
            // continue;
            // }

            // search entities by name
            List<Location> retrievedLocations = locationSource.retrieveLocations(locationCandidate.getEntity());

            // get all entities that are locations
            for (Location location : retrievedLocations) {

                // XXX make even more beautifuller!
                location.setPrimaryName(locationCandidate.getEntity());

                if (location.getPrimaryName().equalsIgnoreCase(locationCandidate.getEntity())
                        && !skipWords.contains(location.getPrimaryName())) {
                    // locationEntities.add(location);

                    // FIXME use common interface!
                    Location fixme = new Location(locationCandidate);
                    fixme.setAlternativeNames(location.getAlternativeNames());
                    fixme.setId(location.getId());
                    fixme.setLatitude(location.getLatitude());
                    fixme.setLongitude(location.getLongitude());
                    fixme.setPopulation(location.getPopulation());
                    fixme.setPrimaryName(location.getPrimaryName());
                    fixme.setType(location.getType());
                    fixme.setValue(location.getValue());
                    locationEntities.add(fixme);

                }
            }
        }

        // if we have cities and countries with the same name, we remove the cities
        return processCandidateList(locationEntities);
    }

    /**
     * <p>
     * Often we can find places with the same name at different locations. We need to find out which places are the most
     * likely.
     * </p>
     * <ol>
     * <li>First we replace all city entities if we also found a country entity with the same name.</li>
     * <li>If we have several cities with the same name we pick the one that is in a country of the list or if there is
     * no country, we pick the largest city.</li>
     * </ol>
     * 
     * @param locations The set of detected entity candidates.
     * @return A reduced set of entities containing only the most likely ones.
     */
    private List<Location> processCandidateList(List<Location> locations) {
        Set<Location> entitiesToRemove = new HashSet<Location>();

        Set<Location> countries = new HashSet<Location>();
        Set<Location> cities = new HashSet<Location>();
        Map<String, Set<Location>> citiesWithSameName = new HashMap<String, Set<Location>>();

        // STEP 1: if we have cities and countries with the same name, we remove the cities
        for (Location location : locations) {

            // check whether entity is a city and we have a country
            // boolean keepLocation = true;
            if (location.getType() == LocationType.CITY) {
                cities.add(location);
                if (citiesWithSameName.get(location.getPrimaryName()) != null) {
                    citiesWithSameName.get(location.getPrimaryName()).add(location);
                } else {
                    Set<Location> set = new HashSet<Location>();
                    set.add(location);
                    citiesWithSameName.put(location.getPrimaryName(), set);
                }

                for (Location entity2 : locations) {
                    if (entity2.getType() == LocationType.COUNTRY
                            && entity2.getPrimaryName().equalsIgnoreCase(location.getPrimaryName())) {
                        // keepLocation = false;
                        entitiesToRemove.add(location);
                    }
                }
            } else if (location.getType() == LocationType.COUNTRY) {
                countries.add(location);
            }

            // if (keepLocation) {
            // entitiesToRemove.add(entity);
            // }
        }

        // STEP 2: if we have several cities with the same name, we pick the one in the country or the largest
        // for (Set<Location> citySet : citiesWithSameName.values()) {
        // // cities with the same name
        // if (citySet.size() > 1) {
        //
        // // calculate distance to all countries, take the city closest to any country
        // Location closestCity = null;
        // double closestDistance = Integer.MAX_VALUE;
        // Location biggestCity = null;
        // long biggestPopulation = -1;
        // for (Location city : citySet) {
        //
        // // update biggest city
        // long population = getPopulation(city);
        // if (population > biggestPopulation) {
        // biggestCity = city;
        // biggestPopulation = population;
        // }
        //
        // // upate closest city to countries
        // for (Location country : countries) {
        //
        // double distance = getDistance(city, country);
        // if (distance < closestDistance) {
        // closestCity = city;
        // closestDistance = distance;
        // }
        //
        // }
        // }
        //
        // // we keep only one city
        // Location keepCity = null;
        // if (closestCity != null) {
        // keepCity = closestCity;
        // } else if (biggestCity != null) {
        // keepCity = biggestCity;
        // } else {
        // keepCity = citySet.iterator().next();
        // }
        //
        // citySet.remove(keepCity);
        //
        // for (Location entity : citySet) {
        // entitiesToRemove.add(entity);
        // }
        // // entitiesToRemove.addAll(citySet);
        // }
        // }

        // CollectionHelper.print(entitiesToRemove);

        for (Location entity : entitiesToRemove) {
            locations.remove(entity);
        }

        return locations;
    }

    private double getDistance(Location city, Location country) {
        double distance = Integer.MAX_VALUE;

        try {

            Double lat1 = city.getLatitude();
            Double lng1 = city.getLongitude();
            Double lat2 = country.getLatitude();
            Double lng2 = country.getLongitude();

            distance = MathHelper.computeDistanceBetweenWorldCoordinates(lat1, lng1, lat2, lng2);

        } catch (Exception e) {
        }

        return distance;
    }

    private long getPopulation(Location city) {
        long population = -1;
        population = city.getPopulation();
        return population;
    }

    public static void main(String[] args) throws PageContentExtractorException {
        // String text = "";
        //
        // PalladianContentExtractor pce = new PalladianContentExtractor();
        // text = pce.setDocument("http://www.bbc.co.uk/news/world-africa-17887914").getResultText();
        //
        // PalladianLocationExtractor locationDetector = new PalladianLocationExtractor();
        // Collection<Location> locations = locationDetector.detectLocations(text);
        //
        // CollectionHelper.print(locations);
    }

}
