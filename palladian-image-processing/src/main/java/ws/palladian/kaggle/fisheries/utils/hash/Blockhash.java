package ws.palladian.kaggle.fisheries.utils.hash;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import ws.palladian.utils.ImageUtils;

/**
 * Perceptual image hash calculation tool based on algorithm descibed in Block
 * Mean Value Based Image Perceptual Hashing by Bian Yang, Fan Gu and Xiamu Niu
 *
 * Copyright 2014 Commons Machinery http://commonsmachinery.se/ Distributed
 * under an MIT license, please see LICENSE in the top dir.
 * 
 * Ported from <a href="https://github.com/commonsmachinery/blockhash-js">this
 * JavaScript implementation</a>. The general structure, method and variable
 * names have been kept as in the JS, so that future changes can be integrated
 * more easily. Based on Git commit ef9e1ed.
 * 
 * @author pk
 */
public class Blockhash implements ImageHash {

	private static final class ImageData {

		final int width;
		final int height;
		/** The raw image data, as consecutive RGBA values. */
		final int[] data;

		/**
		 * Converts a {@link BufferedImage} to the data structure required by
		 * the hashing algorithm.
		 * 
		 * @param image
		 *            The image.
		 */
		ImageData(BufferedImage image) {
			width = image.getWidth();
			height = image.getHeight();
			data = new int[width * height * 4];
			int[] rgb = ImageUtils.getRGB(image);
			for (int i = 0; i < rgb.length; i++) {
				Color color = new Color(rgb[i], true);
				int idx = 4 * i;
				data[idx++] = color.getRed();
				data[idx++] = color.getGreen();
				data[idx++] = color.getBlue();
				data[idx++] = color.getAlpha();
			}
		}
	}

	private static float median(int[] data) {
		int[] mdarr = new int[data.length];
		System.arraycopy(data, 0, mdarr, 0, data.length);
		Arrays.sort(mdarr);
		if (mdarr.length % 2 == 0) {
			return (mdarr[mdarr.length / 2] + mdarr[mdarr.length / 2 + 1]) / 2.0f;
		}
		return mdarr[(int) Math.floor(mdarr.length / 2)];
	}

	private static void translate_blocks_to_bits(int[] blocks, int pixels_per_block) {
		int half_block_value = pixels_per_block * 256 * 3 / 2;
		int bandsize = blocks.length / 4;

		// Compare medians across four horizontal bands
		for (int i = 0; i < 4; i++) {
			float m = median(Arrays.copyOfRange(blocks, i * bandsize, (i + 1) * bandsize));
			for (int j = i * bandsize; j < (i + 1) * bandsize; j++) {
				int v = blocks[j];

				// Output a 1 if the block is brighter than the median.
				// With images dominated by black or white, the median may
				// end up being 0 or the max value, and thus having a lot
				// of blocks of value equal to the median. To avoid
				// generating hashes of all zeros or ones, in that case output
				// 0 if the median is in the lower value space, 1 otherwise
				blocks[j] = v > m || Math.abs(v - m) < 1 && m > half_block_value ? 1 : 0;
			}
		}
	};

	private static String bits_to_hexhash(int[] bitsArray) {
		StringBuilder hex = new StringBuilder();
		for (int i = 0; i < bitsArray.length; i += 4) {
			int decimal = 0;
			for (int j = 0; j < 4; j++) {
				decimal += bitsArray[i + 3 - j] << j;
			}
			hex.append(Integer.toString(decimal, 16));
		}
		return hex.toString();
	};

	private static String bmvbhash_even(ImageData data, int bits) {
		int blocksize_x = (int) Math.floor((float) data.width / bits);
		int blocksize_y = (int) Math.floor((float) data.height / bits);

		int[] result = new int[bits * bits];

		for (int y = 0; y < bits; y++) {
			for (int x = 0; x < bits; x++) {
				int total = 0;

				for (int iy = 0; iy < blocksize_y; iy++) {
					for (int ix = 0; ix < blocksize_x; ix++) {
						int cx = x * blocksize_x + ix;
						int cy = y * blocksize_y + iy;
						int ii = (cy * data.width + cx) * 4;

						int alpha = data.data[ii + 3];
						if (alpha == 0) {
							total += 765;
						} else {
							total += data.data[ii] + data.data[ii + 1] + data.data[ii + 2];
						}
					}
				}

				result[y * bits + x] = total;
			}
		}

		translate_blocks_to_bits(result, blocksize_x * blocksize_y);
		return bits_to_hexhash(result);
	};

	private static String bmvbhash(ImageData data, int bits) {
		int[] result = new int[bits * bits];

		int i, j, x, y;
		float block_width, block_height;
		float weight_top, weight_bottom, weight_left, weight_right;
		int block_top, block_bottom, block_left, block_right;
		float y_mod, y_frac, y_int;
		float x_mod, x_frac, x_int;
		int[][] blocks = new int[bits][];

		boolean even_x = data.width % bits == 0;
		boolean even_y = data.height % bits == 0;

		if (even_x && even_y) {
			return bmvbhash_even(data, bits);
		}

		// initialize blocks array with 0s
		for (i = 0; i < bits; i++) {
			blocks[i] = new int[bits];
		}

		block_width = (float) data.width / bits;
		block_height = (float) data.height / bits;

		for (y = 0; y < data.height; y++) {
			if (even_y) {
				// don't bother dividing y, if the size evenly divides by bits
				block_top = block_bottom = (int) Math.floor(y / block_height);
				weight_top = 1;
				weight_bottom = 0;
			} else {
				y_mod = (y + 1) % block_height;
				y_frac = y_mod - (int) Math.floor(y_mod);
				y_int = y_mod - y_frac;

				weight_top = 1 - y_frac;
				weight_bottom = y_frac;

				// y_int will be 0 on bottom/right borders and on block
				// boundaries
				if (y_int > 0 || y + 1 == data.height) {
					block_top = block_bottom = (int) Math.floor(y / block_height);
				} else {
					block_top = (int) Math.floor(y / block_height);
					block_bottom = (int) Math.ceil(y / block_height);
				}
			}

			for (x = 0; x < data.width; x++) {
				int ii = (y * data.width + x) * 4;

				float avgvalue, alpha = data.data[ii + 3];
				if (alpha == 0) {
					avgvalue = 765;
				} else {
					avgvalue = data.data[ii] + data.data[ii + 1] + data.data[ii + 2];
				}

				if (even_x) {
					block_left = block_right = (int) Math.floor(x / block_width);
					weight_left = 1;
					weight_right = 0;
				} else {
					x_mod = (x + 1) % block_width;
					x_frac = x_mod - (int) Math.floor(x_mod);
					x_int = x_mod - x_frac;

					weight_left = 1 - x_frac;
					weight_right = x_frac;

					// x_int will be 0 on bottom/right borders and on block
					// boundaries
					if (x_int > 0 || x + 1 == data.width) {
						block_left = block_right = (int) Math.floor(x / block_width);
					} else {
						block_left = (int) Math.floor(x / block_width);
						block_right = (int) Math.ceil(x / block_width);
					}
				}

				// add weighted pixel value to relevant blocks
				blocks[block_top][block_left] += avgvalue * weight_top * weight_left;
				blocks[block_top][block_right] += avgvalue * weight_top * weight_right;
				blocks[block_bottom][block_left] += avgvalue * weight_bottom * weight_left;
				blocks[block_bottom][block_right] += avgvalue * weight_bottom * weight_right;
			}
		}

		for (i = 0; i < bits; i++) {
			for (j = 0; j < bits; j++) {
				result[i * bits + j] = blocks[i][j];
			}
		}

		translate_blocks_to_bits(result, (int) (block_width * block_height));
		return bits_to_hexhash(result);
	};

	/**
	 * Calculate the block hash.
	 * 
	 * @param image
	 *            The image.
	 * @param bits
	 *            the number of bits in a row.
	 * @param method
	 *            (1) Quick and crude, non-overlapping blocks, (2) Precise but
	 *            slower, non-overlapping blocks.
	 * @return Hex value of the hash.
	 * @throws IllegalArgumentException
	 *             in case method is not equal 1 or 2.
	 */
	public static String blockhashData(BufferedImage image, int bits, int method) {

		ImageData imgData = new ImageData(image);

		if (method == 1) {
			return bmvbhash_even(imgData, bits);
		} else if (method == 2) {
			return bmvbhash(imgData, bits);
		} else {
			throw new IllegalArgumentException("Bad hashing method");
		}

	}

	@Override
	public String hash(BufferedImage image) {
		return blockhashData(image, 16, 2);
	};

}
