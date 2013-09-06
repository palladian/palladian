/**
 * This is <b>Palladians</b> model package containing all classes holding data for <b>Palladian</b> components
 * <p>
 * It manages {@link BasicFeatureVectorImpl}s and its containing {@link Feature}s. You can use it following the example 
 * code below.
 * <pre>
 * Classifier classifier = new Classifier();
 * FeatureVector featureVector = new FeatureVector();
 * NumericFeature ageFeature = new NumericFeature("age",23.0);
 * featureVector.add("age",ageFeature);
 * featureVector.get("age");
 * 		
 * Collection<Date> birthdays = new HashSet<Date>();
 * birthdays.add(new Date());
 * Feature<Collection<Date>> childVector = new Feature<Collection<Date>>("birthdays",birthdays);
 * 
 * System.out.println(classifier.classify(featureVector));
 * </pre>
 * <p>
 * The <b>Palladian</b> model package is structured like shown on the following class diagram:
 * <img src="doc-files/structure.png" width="1274" height="667" alt="Palladian Model Package"/>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 */
package ws.palladian.processing.features;

