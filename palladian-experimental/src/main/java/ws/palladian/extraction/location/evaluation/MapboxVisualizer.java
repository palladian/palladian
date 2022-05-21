package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.helper.io.FileHelper;

public class MapboxVisualizer {

	private static final String accessToken = "pk.eyJ1IjoicXFpbGlocSIsImEiOiJjbDNmanRocjIwc290M2VwcjIzZXM3MTMxIn0.uOuLUBJnq-zYNEIf1dpTeQ";

	public interface Colored {
		String getHexColor();
	}

	public static class ColoredGeoCoordinate implements GeoCoordinate, Colored {

		private final GeoCoordinate coordinate;
		private final String hexColor;

		public ColoredGeoCoordinate(GeoCoordinate coordinate, String hexColor) {
			this.coordinate = coordinate;
			this.hexColor = hexColor;
		}

		public ColoredGeoCoordinate(double lat, double lng, String hexColor) {
			this(new ImmutableGeoCoordinate(lat, lng), hexColor);
		}

		@Override
		public String getHexColor() {
			return hexColor;
		}

		public double getLatitude() {
			return coordinate.getLatitude();
		}

		public double getLongitude() {
			return coordinate.getLongitude();
		}

		public double distance(GeoCoordinate other) {
			return coordinate.distance(other);
		}

		public String toDmsString() {
			return coordinate.toDmsString();
		}

		public double[] getBoundingBox(double distance) {
			return coordinate.getBoundingBox(distance);
		}

		public GeoCoordinate getCoordinate(double distance, double bearing) {
			return coordinate.getCoordinate(distance, bearing);
		}

	}

	public static void writeHtml(Collection<? extends GeoCoordinate> coordinates, File outputPath) {
		List<String> buffer = new ArrayList<>();
		buffer.add("<!DOCTYPE html>");
		buffer.add("<html>");
		buffer.add("<head>");
		buffer.add("<meta charset='utf-8'>");
		buffer.add("<meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no'>");
		buffer.add("<link href='https://api.mapbox.com/mapbox-gl-js/v2.8.2/mapbox-gl.css' rel='stylesheet'>");
		buffer.add("<script src='https://api.mapbox.com/mapbox-gl-js/v2.8.2/mapbox-gl.js'></script>");
		buffer.add("<style>");
		buffer.add("body { margin: 0; padding: 0; }");
		buffer.add("#map { position: absolute; top: 0; bottom: 0; width: 100%; }");
		buffer.add("</style>");
		buffer.add("</head>");
		buffer.add("<body>");
		buffer.add("<div id='map'></div>");
		buffer.add("<script>");
		buffer.add(String.format("mapboxgl.accessToken = '%s';", accessToken));
		buffer.add("const map = new mapboxgl.Map({");
		buffer.add("  container: 'map',");
		buffer.add("  style: 'mapbox://styles/mapbox/streets-v11',");
		// buffer.add(" center: [-74.5, 40],");
		buffer.add("  zoom: 1");
		buffer.add("});");
		// write markers
		for (GeoCoordinate geoCoordinate : coordinates) {
			String color = "#000";
			if (geoCoordinate instanceof Colored) {
				color = ((Colored) geoCoordinate).getHexColor();

			}
			buffer.add(String.format("new mapboxgl.Marker({color:'%s'}).setLngLat([%s,%s]).addTo(map);", color,
					geoCoordinate.getLongitude(), geoCoordinate.getLatitude()));
		}

		buffer.add("</script>");
		buffer.add("</body>");
		buffer.add("</html>");
		FileHelper.writeToFile(outputPath.getAbsolutePath(), String.join("\n", buffer));
	}

	public static void main(String[] args) {
		List<? extends GeoCoordinate> coordinates = Arrays.asList(new ColoredGeoCoordinate(40, -74.5, "#f00"),
				new ImmutableGeoCoordinate(51.049259, 13.73836));
		writeHtml(coordinates, new File("/Users/pk/Desktop/map.html"));
	}

}
