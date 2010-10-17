cd target/classes
export MAVEN_REPOSITORY=/home/muthmann/.m2/repository
java -cp .:$MAVEN_REPOSITORY/log4j/log4j/1.2.12/log4j-1.2.12.jar:\
$MAVEN_REPOSITORY/commons-configuration/commons-configuration/1.6/commons-configuration-1.6.jar:\
$MAVEN_REPOSITORY/commons-lang/commons-lang/2.4/commons-lang-2.4.jar:\
$MAVEN_REPOSITORY/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar:\
$MAVEN_REPOSITORY/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar:\
$MAVEN_REPOSITORY/mysql/mysql-connector-java/5.1.12/mysql-connector-java-5.1.12.jar:\
$MAVEN_REPOSITORY/commons-cli/commons-cli/1.2/commons-cli-1.2.jar:\
$MAVEN_REPOSITORY/org/json/json/20090211/json-20090211.jar:\
$MAVEN_REPOSITORY/rome/rome/1.0/rome-1.0.jar:\
$MAVEN_REPOSITORY/jdom/jdom/1.0/jdom-1.0.jar:\
$MAVEN_REPOSITORY/jaxen/jaxen/1.1.1/jaxen-1.1.1.jar:\
$MAVEN_REPOSITORY/net/sourceforge/nekohtml/nekohtml/1.9.11/nekohtml-1.9.11.jar:\
$MAVEN_REPOSITORY/xerces/xercesImpl/2.6.2/xercesImpl-2.6.2.jar:\
$MAVEN_REPOSITORY/org/apache/commons/commons-math/2.1/commons-math-2.1.jar -Xms1024m -Xmx1024m tud.iir.news.DatasetCreator
