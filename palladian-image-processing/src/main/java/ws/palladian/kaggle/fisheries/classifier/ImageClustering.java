package ws.palladian.kaggle.fisheries.classifier;

import static ws.palladian.helper.functional.Filters.fileExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.clustering.DBSCAN;
import ws.palladian.extraction.multimedia.ImageHandler;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.kaggle.fisheries.utils.Config;
import ws.palladian.kaggle.fisheries.utils.hash.AverageHash;
import ws.palladian.kaggle.fisheries.utils.hash.Blockhash;
import ws.palladian.kaggle.fisheries.utils.hash.GradientHash;
import ws.palladian.kaggle.fisheries.utils.hash.HashUtil;
import ws.palladian.kaggle.fisheries.utils.hash.ImageHash;

public class ImageClustering {
	public static void main(String[] args) throws IOException {
		Collection<File> files = FileHelper.getFiles(Config.getTrainingPath(), fileExtension(".jpg"));
		
		files = MathHelper.sample(files, 100);
		
		// ImageHash imageHash = new Blockhash();
		// ImageHash imageHash = new AverageHash();
		ImageHash imageHash = new GradientHash();
		
		Collection<Pair<File, String>> imageHashes = files.parallelStream().map(file -> {
			try {
				String hash = imageHash.hash(ImageHandler.load(file));
				System.out.println(file.getName() + " | " + hash);
				return Pair.of(file, hash);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}).collect(Collectors.toList());

		DBSCAN<Pair<File, String>> dbscan = new DBSCAN<>(12, 1,
				(i1, i2) -> HashUtil.hammingDistance(i1.getRight(), i2.getRight()));

		Set<Set<Pair<File, String>>> clusterResult = dbscan.cluster(imageHashes);
		for (Set<Pair<File, String>> entry : clusterResult) {
			System.out.println(entry.stream().map(e -> e.getLeft()).collect(Collectors.toList()));
		}
	}
}
