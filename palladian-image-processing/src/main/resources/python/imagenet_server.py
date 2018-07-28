"""Implementation of the Python ImageNet access"""

import sys
import imagenet_pb2
import time
import numpy as np
import tensorflow as tf

_ONE_DAY_IN_SECONDS = 60 * 60 * 24

_PORT = sys.argv[1]

_PATH_TO_GRAPH = sys.argv[2]

_TENSOR_NAME = sys.argv[3]

def create_graph():
	with tf.gfile.FastGFile(_PATH_TO_GRAPH, 'rb') as f:
		graph_def = tf.GraphDef()
		graph_def.ParseFromString(f.read())
		_ = tf.import_graph_def(graph_def, name='')

def run_inference_on_image(sess, image):
	# Some useful tensors:
	# 'softmax:0': A tensor containing the normalized prediction across
	#   1000 labels.
	# 'pool_3:0': A tensor containing the next-to-last layer containing 2048
	#   float description of the image.
	# 'DecodeJpeg/contents:0': A tensor containing a string providing JPEG
	#   encoding of the image.
	# Runs the softmax tensor by feeding the image_data as input to the graph.
	# softmax_tensor = sess.graph.get_tensor_by_name('softmax:0')
	tensor = sess.graph.get_tensor_by_name(_TENSOR_NAME)
	predictions = sess.run(tensor, {'DecodeJpeg/contents:0': image})
	predictions = np.squeeze(predictions)

	categories = imagenet_pb2.Categories()
	it = np.nditer(predictions, flags=['f_index'])
	while not it.finished:
		category = categories.category.add()
		category.nodeId = it.index
		category.score = it[0].item()
		it.iternext()

	return categories

class ImageNetServicer(imagenet_pb2.BetaImageNetServicer):
	"""Provides methods that implement functionality of ImageNet server."""

	def __init__(self):
		create_graph()
		self.sess = tf.Session()

	def Classify(self, request, context):
		return run_inference_on_image(self.sess, request.data)

if __name__ == '__main__':
	server = imagenet_pb2.beta_create_ImageNet_server(ImageNetServicer())
	server.add_insecure_port('[::]:' + _PORT)
	server.start()
	print('running on port ' + _PORT)
	try:
		while True:
			time.sleep(_ONE_DAY_IN_SECONDS)
	except KeyboardInterrupt:
		server.stop(0)
